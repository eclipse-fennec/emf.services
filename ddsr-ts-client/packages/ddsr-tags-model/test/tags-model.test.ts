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


import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { TagsFactory, tagPath, tagsByPath } from '../src/index.js';

describe('the TypeScript tag model', () => {
  const here = fileURLToPath(new URL('.', import.meta.url));

  it('is the Java one, byte for byte', () => {
    // When this fails: copy org.eclipse.fennec.tags.model/model/tags.ecore over
    // model/tags.ecore, then pnpm --filter @ddsr/tags-model generate.
    expect(readFileSync(`${here}../model/tags.ecore`, 'utf8'))
      .toBe(readFileSync(`${here}../../../../org.eclipse.fennec.tags.model/model/tags.ecore`, 'utf8'));
  });

  it('joins a tag path from the ids, as the derived Java path does', () => {
    const payment = TagsFactory.eINSTANCE.createTag();
    payment.id = 'payment';
    const method = TagsFactory.eINSTANCE.createTag();
    method.id = 'method';
    const card = TagsFactory.eINSTANCE.createTag();
    card.id = 'card';
    payment.children.push(method);
    method.children.push(card);

    expect(tagPath(card)).toBe('payment/method/card');
    expect([...tagsByPath([payment]).keys()]).toEqual(['payment', 'payment/method', 'payment/method/card']);
  });
});
