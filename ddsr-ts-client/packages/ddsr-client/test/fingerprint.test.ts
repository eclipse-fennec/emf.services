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
import { dirname, join } from 'node:path';
import { describe, expect, it } from 'vitest';
import type { ServiceInterface } from '@ddsr/model';
import { DDSRFactory } from '@ddsr/model';
import { canonicalForm, fingerprint } from '../src/fingerprint/service-description-fingerprint';
import { deserializeFromXmi } from '../src/xmi/xmi-support';
import { asRoots, firstOfClass } from '../src/internal/emf-util';
import { props } from '../src/properties';

// The same fixtures the Java golden test pins — the cross-language
// anchor for the frozen sd1 scheme (DECISIONS_PARITY D6).
const fixtures = join(
  dirname(fileURLToPath(import.meta.url)),
  '..', '..', '..', '..', 'itest', 'fixtures', 'fingerprint'
);

function goldenInterface(): ServiceInterface {
  const xmi = readFileSync(join(fixtures, 'payment.xmi'), 'utf8');
  const si = firstOfClass<ServiceInterface>(asRoots(deserializeFromXmi(xmi)), 'ServiceInterface');
  if (!si) throw new Error('fixture without ServiceInterface');
  return si;
}

describe('ServiceDescriptionFingerprint (sd1) — cross-language golden', () => {
  it('produces the exact golden canonical form', () => {
    const expected = readFileSync(join(fixtures, 'payment.canonical.txt'), 'utf8').replace(/\n$/, '');
    expect(canonicalForm(goldenInterface())).toBe(expected);
  });

  it('produces the exact golden hash', () => {
    const expected = readFileSync(join(fixtures, 'payment.sd1'), 'utf8').trim();
    expect(fingerprint(goldenInterface())).toBe(expected);
  });

  it('doc text does not move the hash (description excluded)', () => {
    const withDocs = goldenInterface();
    const stripped = goldenInterface();
    stripped.description = undefined as any;
    expect(fingerprint(withDocs)).toBe(fingerprint(stripped));
  });

  it('operation order moves the hash (declared order is contract)', () => {
    const si = goldenInterface();
    const first = si.operations.get ? (si.operations as any).get(0) : (si.operations as any)[0];
    // rebuild with swapped operations
    const factory = DDSRFactory.eINSTANCE;
    const swapped = factory.createServiceInterface();
    swapped.name = si.name;
    swapped.version = si.version;
    const ops = [...(Array.isArray(si.operations) ? si.operations : [])];
    if (ops.length === 0) {
      // EList form
      const list: any = si.operations;
      for (let i = 0; i < list.size(); i++) ops.push(list.get(i));
    }
    for (const op of [...ops].reverse()) swapped.operations.push(op);
    expect(fingerprint(swapped)).not.toBe(fingerprint(goldenInterface()));
    void first;
  });

  it('renders double/float property values as IEEE-754 bit patterns', () => {
    const factory = DDSRFactory.eINSTANCE;
    const si = factory.createServiceInterface();
    si.name = 'X';
    const ex = factory.createServiceException();
    ex.name = 'E';
    ex.properties.push(props.double('half', 0.5));
    ex.properties.push(props.float('one', 1.0));
    si.exceptions.push(ex);
    const canonical = canonicalForm(si)!;
    expect(canonical).toContain('pr|d|half|value=bits:3fe0000000000000');
    expect(canonical).toContain('pr|f|one|value=bits:3f800000');
  });

  it('escapes pipes, backslashes and newlines in text fields', () => {
    const factory = DDSRFactory.eINSTANCE;
    const si = factory.createServiceInterface();
    si.name = 'a|b\\c\nd';
    expect(canonicalForm(si)).toContain('I|a\\|b\\\\c\\nd|');
  });

  it('undefined for undefined input', () => {
    expect(fingerprint(undefined)).toBeUndefined();
    expect(canonicalForm(undefined)).toBeUndefined();
  });
});
