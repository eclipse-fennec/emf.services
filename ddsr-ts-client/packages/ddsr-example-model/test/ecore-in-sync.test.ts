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

/**
 * The example model is the Java one, byte for byte - the same rule as
 * packages/ddsr-model, for the same reason: a contract that names
 * `Person` by eType has to mean the same class on both sides.
 *
 * When this fails, copy the file across and regenerate:
 *
 *     cp ../../org.eclipse.fennec.services.examples.model/model/example.ecore \
 *        packages/ddsr-example-model/model/example.ecore
 *     pnpm --filter @ddsr/example-model generate
 */
describe('the TypeScript example model is the Java one', () => {
  const here = fileURLToPath(new URL('.', import.meta.url));
  const canonical = `${here}../../../../org.eclipse.fennec.services.examples.model/model/example.ecore`;
  const copy = `${here}../model/example.ecore`;

  it('is byte-identical to the canonical ecore', () => {
    expect(readFileSync(copy, 'utf8'),
      'packages/ddsr-example-model/model/example.ecore has drifted from the Java model - '
      + 'copy example.ecore over it, then pnpm --filter @ddsr/example-model generate')
      .toBe(readFileSync(canonical, 'utf8'));
  });
});
