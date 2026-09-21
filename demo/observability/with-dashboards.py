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
"""Add the dashboards in a directory to the observability pod definition.

Grafana provisions dashboards from files. Pasting a dashboard into the pod
YAML would make it a block of quoted JSON that nobody edits twice, so the
dashboards stay ordinary files and this puts them into a ConfigMap on the way
to `podman kube play`.
"""

import argparse
import json
import pathlib
import sys

import yaml


def documents(path):
    return [document for document in yaml.safe_load_all(path.read_text()) if document]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stack", required=True, type=pathlib.Path)
    parser.add_argument("--dashboards", required=True, type=pathlib.Path)
    parser.add_argument("--out", required=True, type=pathlib.Path)
    arguments = parser.parse_args()

    stack = documents(arguments.stack)
    dashboards = sorted(arguments.dashboards.glob("*.json"))
    if not dashboards:
        print("no dashboards found — the stack still runs, with Explore only", file=sys.stderr)

    files = {}
    for dashboard in dashboards:
        # Read as JSON rather than copied verbatim: a dashboard that does
        # not parse should fail here and not in Grafana's log.
        files[dashboard.name] = json.dumps(json.loads(dashboard.read_text()), indent=2)

    stack.append({
        "apiVersion": "v1",
        "kind": "ConfigMap",
        "metadata": {"name": "grafana-dashboard-files"},
        "data": files,
    })
    stack.append({
        "apiVersion": "v1",
        "kind": "ConfigMap",
        "metadata": {"name": "grafana-dashboard-provider"},
        "data": {"dashboards.yaml": yaml.safe_dump({
            "apiVersion": 1,
            "providers": [{
                "name": "fennec",
                "type": "file",
                "allowUiUpdates": True,
                "options": {"path": "/var/lib/grafana/dashboards", "foldersFromFilesStructure": False},
            }],
        }, sort_keys=False)},
    })

    pod = next(document for document in stack if document.get("kind") == "Pod")
    grafana = next(container for container in pod["spec"]["containers"]
                   if container["name"] == "grafana")
    grafana.setdefault("volumeMounts", []).extend([
        {"name": "grafana-dashboard-provider", "mountPath": "/etc/grafana/provisioning/dashboards"},
        {"name": "grafana-dashboard-files", "mountPath": "/var/lib/grafana/dashboards"},
    ])
    pod["spec"].setdefault("volumes", []).extend([
        {"name": "grafana-dashboard-provider", "configMap": {"name": "grafana-dashboard-provider"}},
        {"name": "grafana-dashboard-files", "configMap": {"name": "grafana-dashboard-files"}},
    ])

    arguments.out.write_text(yaml.safe_dump_all(stack, sort_keys=False))
    print(f">> wrote {arguments.out} ({len(files)} dashboard(s))")


if __name__ == "__main__":
    main()
