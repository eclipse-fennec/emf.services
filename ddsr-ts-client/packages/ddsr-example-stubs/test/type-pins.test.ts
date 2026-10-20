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
import { generate } from '@ddsr/codegen';

/**
 * Holds the TypeScript rendering to type-pins.tsv, which states every
 * slot's type in both languages (#151). The Java column is held to the
 * m2t output once that side reads the table too (#185); until then this
 * pins the TypeScript half against what java-types.mtl produces today.
 */
const here = fileURLToPath(new URL('.', import.meta.url));

interface Pin {
  contract: string;
  operation: string;
  slot: string;
  optional: boolean;
  typescript: string;
}

function pins(): Pin[] {
  return readFileSync(`${here}../type-pins.tsv`, 'utf8')
    .split('\n')
    .filter(line => line.trim() !== '' && !line.startsWith('#'))
    .map(line => {
      const [contract, operation, slot, optional, , typescript] = line.split('\t');
      return { contract, operation, slot, optional: optional === 'true', typescript };
    });
}

/** The signature the table describes, as @ddsr/codegen writes it. */
function signature(operation: string, rows: Pin[]): string {
  const returned = rows.find(pin => pin.slot === 'return')?.typescript ?? 'void';
  const parameters = rows
    .filter(pin => pin.slot !== 'return' && pin.slot !== 'throws')
    .map(pin => `${pin.slot}${pin.optional ? '?' : ''}: ${pin.typescript}`);
  return `${operation}(${parameters.join(', ')}): Promise<${returned}>;`;
}

function byOperation(): Pin[][] {
  const operations = new Map<string, Pin[]>();
  for (const pin of pins()) {
    const key = `${pin.contract}#${pin.operation}`;
    operations.set(key, [...(operations.get(key) ?? []), pin]);
  }
  return [...operations.values()];
}

function source(contract: string): string {
  return readFileSync(`${here}../src/${contract}.ts`, 'utf8');
}

describe('the TypeScript rendering is the one type-pins.tsv describes', () => {
  it('reads the table at all', () => {
    expect(byOperation().length).toBeGreaterThan(0);
  });

  it.each(byOperation().map(rows => [`${rows[0].contract}.${rows[0].operation}`, rows] as const))(
    '%s', (_name, rows) => {
      expect(source(rows[0].contract)).toContain(signature(rows[0].operation, rows));
    });

  it('throws the class the table names for a declared error', () => {
    for (const pin of pins().filter(entry => entry.slot === 'throws')) {
      const contract = source(pin.contract);
      expect(contract).toContain(`@throws {${pin.typescript}}`);
      expect(contract).toContain(`import { ${pin.typescript} } from './${pin.typescript}.js';`);
    }
  });

  it('is what the binding generates today, not a stale checkout', async () => {
    // The checked-in stubs are what the pins are read against. Generating
    // again into memory must give the same files, or the table was held to
    // an old output.
    const fresh = await generate({
      binding: `${here}../binding/example-ts.xmi`,
      outDir: `${here}../.pins-check`,
      dryRun: true,
    });
    for (const file of fresh.contents) {
      expect(file.content, file.path).toBe(readFileSync(`${here}../${file.path}`, 'utf8'));
    }
  });
});
