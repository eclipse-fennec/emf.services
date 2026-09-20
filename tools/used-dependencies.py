#!/usr/bin/env python3
#
# Copyright (c) 2026 Contributors to the Eclipse Foundation.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Data In Motion - initial API and implementation
#
"""Narrow a bnd workspace's dependency list to what the build actually uses.

`bnd repo deps` exports every artifact the workspace's Maven *indexes* offer.
An index is a catalogue, not a bill of materials: this workspace's offers ~400
artifacts and the build names ~90 of them. Sending the other 300 to the Eclipse
IP team asks for review of code this project never compiles against, ships or
resolves (issue #90).

So this reads the workspace the way bnd does — every `-buildpath`, `-testpath`,
`-runpath`, `-runbundles`, `-runfw` and `-runrequires` in every `bnd.bnd`,
`*.bndrun` and `cnf/ext/*.bnd`, plus the same instructions in the templates of
every `-library` those files switch on — and keeps the coordinates whose bundle
is named there.

The mapping from a Maven coordinate to a bundle symbolic name comes from the
jar's own manifest when the jar is in the local Maven repository, and from the
artifact's name when it is not. A coordinate nobody can resolve to a bundle we
name is dropped; a bundle we name that no coordinate explains is reported,
because that is the shape a mistake in here would take.
"""

import argparse
import os
import re
import sys
import zipfile
from pathlib import Path

# Instructions whose values name bundles. The suffix form (`-buildpath.emf`)
# is how bnd lets a library add to one, so the match is on the prefix.
BUNDLE_INSTRUCTIONS = (
    "-buildpath",
    "-testpath",
    "-runpath",
    "-runbundles",
    "-runfw",
    "-runrequires",
    "-runblacklist",
)

# `-runblacklist` names bundles the build refuses; they are read so that a
# blacklisted bundle is not mistaken for an unexplained one, and then dropped.
EXCLUDED_INSTRUCTIONS = ("-runblacklist",)

IDENTITY = re.compile(r"id\s*=\s*['\"]?([A-Za-z0-9_.\-]+)")


def clauses(value):
    """Split a bnd list on its commas, and not on a version range's."""
    parts, depth, quote, current = [], 0, None, []
    for character in value:
        if quote:
            if character == quote:
                quote = None
        elif character in "'\"":
            quote = character
        elif character in "[(":
            depth += 1
        elif character in "])":
            depth = max(0, depth - 1)
        elif character == "," and depth == 0:
            parts.append("".join(current))
            current = []
            continue
        current.append(character)
    parts.append("".join(current))
    return [part.strip() for part in parts if part.strip()]


def properties_of(path):
    """A bnd file as {instruction: raw value}, with continuations joined."""
    lines = [line for line in path.read_text(encoding="utf-8", errors="replace").splitlines()
             if not line.strip().startswith(("#", "//"))]
    logical, current = [], ""
    for line in lines:
        stripped = line.strip()
        if not stripped:
            # A blank line ends an instruction even when the line before it
            # was left with a trailing backslash, which happens in hand-kept
            # lists and would otherwise swallow the next instruction whole.
            if current:
                logical.append(current)
                current = ""
            continue
        if current:
            current += " " + stripped.rstrip("\\").strip() if stripped.endswith("\\") \
                else " " + stripped
        else:
            current = stripped.rstrip("\\").strip() if stripped.endswith("\\") else stripped
        if not stripped.endswith("\\"):
            logical.append(current)
            current = ""
    if current:
        logical.append(current)

    properties = {}
    for line in logical:
        for separator in (":", "="):
            if separator in line:
                name, _, value = line.partition(separator)
                name = name.strip()
                if name and " " not in name:
                    properties[name] = value.strip()
                break
    return properties


def bundles_in(value, macros):
    """The bundle names a bnd instruction value mentions."""
    names = set()
    for reference in re.findall(r"\$\{([A-Za-z0-9_.\-]+)\}", value):
        if reference in macros:
            names |= bundles_in(macros[reference], macros)
    for clause in clauses(value):
        if clause.startswith("${"):
            continue
        if "bnd.identity" in clause:
            found = IDENTITY.search(clause)
            if found:
                names.add(found.group(1))
            continue
        name = clause.split(";")[0].strip().strip("'\"")
        if name and not name.startswith("$"):
            names.add(name)
    return names


def library_templates(root):
    """{library switch: [template bnd files]} from the expanded cnf cache."""
    templates = {}
    cache = root / "cnf" / "cache"
    if not cache.is_dir():
        return templates
    for manifest in cache.glob("*/expanded/*/META-INF/MANIFEST.MF"):
        text = manifest.read_text(encoding="utf-8", errors="replace")
        # Manifest headers wrap at 72 characters, mid-token.
        text = text.replace("\r\n", "\n").replace("\n ", "")
        for switch in re.findall(r"bnd\.library\s*=\s*([A-Za-z0-9_.\-]+)", text):
            home = manifest.parent.parent
            templates.setdefault(switch, []).extend(
                sorted(path for path in home.rglob("*.bnd") if path.is_file()))
    return templates


def workspace_bundles(root):
    """Every bundle this workspace names, libraries followed."""
    files = sorted(root.glob("*/bnd.bnd"))
    files += sorted(root.glob("*/*.bndrun"))
    files += sorted((root / "cnf").glob("*.bnd"))
    files += sorted((root / "cnf" / "ext").glob("*.bnd"))

    parsed = {path: properties_of(path) for path in files}

    libraries = set()
    for properties in parsed.values():
        for name, value in properties.items():
            if name == "-library" or name.startswith("-library."):
                libraries |= set(clauses(value))

    templates = library_templates(root)
    for switch in sorted(libraries):
        for template in templates.get(switch, []):
            parsed[template] = properties_of(template)

    # Macros are a workspace-wide namespace in bnd: `${junit}` in a project
    # is defined by the test library, not by the project. Resolving them per
    # file would leave every library-provided list unread.
    macros = {}
    for properties in parsed.values():
        for name, value in properties.items():
            if not name.startswith("-"):
                macros.setdefault(name, value)

    named, blacklisted = set(), set()
    for properties in parsed.values():
        for name, value in properties.items():
            if not name.startswith(tuple(BUNDLE_INSTRUCTIONS)):
                continue
            target = blacklisted if name.split(".")[0] in EXCLUDED_INSTRUCTIONS else named
            target |= bundles_in(value, macros)

    return named - blacklisted, sorted(libraries - set(templates))


def symbolic_name_of(coordinate, maven_repository):
    """The bundle symbolic name of a Maven coordinate, if the jar is here."""
    group, artifact, version = coordinate.split(":", 2)
    jar = maven_repository.joinpath(*group.split("."), artifact, version,
                                    f"{artifact}-{version}.jar")
    if not jar.is_file():
        return None
    try:
        with zipfile.ZipFile(jar) as archive:
            manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
    except (KeyError, OSError, zipfile.BadZipFile):
        return None
    manifest = manifest.replace("\r\n", "\n").replace("\n ", "")
    found = re.search(r"Bundle-SymbolicName:\s*([^;\s]+)", manifest)
    return found.group(1).strip() if found else None


def names_for(coordinate, maven_repository):
    """What this coordinate could be called on a buildpath."""
    group, artifact, _ = coordinate.split(":", 2)
    # A buildpath may name a bundle by its symbolic name, by the artifact
    # alone, or in bnd's `groupId:artifactId` form — all three appear in
    # this workspace.
    candidates = {artifact, f"{group}.{artifact}", f"{group}:{artifact}"}
    symbolic = symbolic_name_of(coordinate, maven_repository)
    if symbolic:
        candidates.add(symbolic)
    return candidates


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--root", required=True, type=Path, help="the bnd workspace")
    parser.add_argument("--deps", required=True, type=Path, help="every coordinate the indexes offer")
    parser.add_argument("--out", required=True, type=Path, help="where to write the used coordinates")
    parser.add_argument("--maven-repository", type=Path,
                        default=Path(os.environ.get("MAVEN_REPO_LOCAL",
                                                    Path.home() / ".m2" / "repository")))
    arguments = parser.parse_args()

    named, unresolved_libraries = workspace_bundles(arguments.root)
    offered = [line.strip() for line in arguments.deps.read_text().splitlines() if line.strip()]

    used, explained = [], set()
    for coordinate in offered:
        if coordinate.count(":") < 2:
            continue
        candidates = names_for(coordinate, arguments.maven_repository)
        hit = candidates & named
        if hit:
            used.append(coordinate)
            explained |= hit

    arguments.out.write_text("\n".join(sorted(used)) + "\n")

    # Bundles this workspace builds are not third-party content and need no
    # coordinate; everything else that went unexplained is worth saying out
    # loud, because it is either a name this script failed to map or an
    # artifact the index does not offer at all.
    own = {path.name for path in arguments.root.glob("*") if (path / "bnd.bnd").is_file()}
    unexplained = sorted(named - explained - own)

    print(f">> {len(used)} of {len(offered)} offered coordinates are named by this build")

    # A parser that stops understanding the workspace would quietly produce a
    # short list, and a short list is exactly what this file is not allowed to
    # be: it would take third-party content out of IP review by accident. Any
    # real workspace names more than a handful of bundles.
    if offered and len(used) < 20:
        print(">> ERROR: too few coordinates matched to be believable — refusing to write a "
              "DEPENDENCIES that would understate what this project uses.", file=sys.stderr)
        return 1
    if unresolved_libraries:
        print(f">> libraries without an expanded template (run a build first): "
              f"{', '.join(unresolved_libraries)}", file=sys.stderr)
    if unexplained:
        print(f">> {len(unexplained)} named bundle(s) matched no coordinate:", file=sys.stderr)
        for name in unexplained:
            print(f">>   {name}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
