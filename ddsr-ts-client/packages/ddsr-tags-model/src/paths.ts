/********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Data In Motion Consulting - initial implementation
 ********************************************************************/

import type { Tag } from './org/eclipse/fennec/tags/Tag.js';

/**
 * A tag's path: the ids from the root down, joined by '/' —
 * `payment/method/card`.
 *
 * In Java the path is derived in OCL and never travels; a tree read from
 * the wire carries the ids and the parent links, and this joins them —
 * the same rule the Java side states as
 * `if parent = null then id else parent.path + '/' + id endif`.
 */
export function tagPath(tag: Tag): string {
  const ids: string[] = [];
  for (let at: Tag | undefined = tag; at; at = parentOf(at)) {
    ids.unshift(at.id);
  }
  return ids.join('/');
}

/**
 * The tag above: the opposite reference where it is set, the container
 * otherwise — a tree built in TypeScript by adding children, or read from
 * the wire, has the containment but not always the opposite.
 */
function parentOf(tag: Tag): Tag | undefined {
  if (tag.parent) return tag.parent;
  const container = (tag as unknown as { eContainer?: () => unknown }).eContainer?.();
  const eClass = (container as { eClass?: () => { getName?: () => string } } | undefined)?.eClass?.();
  return eClass?.getName?.() === 'Tag' ? container as Tag : undefined;
}

/** Every tag of a tree, with its path, depth first. */
export function tagsByPath(roots: Iterable<Tag>): Map<string, Tag> {
  const byPath = new Map<string, Tag>();
  const visit = (tag: Tag) => {
    byPath.set(tagPath(tag), tag);
    for (const child of tag.children) visit(child);
  };
  for (const root of roots) visit(root);
  return byPath;
}
