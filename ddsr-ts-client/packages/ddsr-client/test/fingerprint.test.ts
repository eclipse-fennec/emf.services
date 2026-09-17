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

// Issue #41: typed slots and multiplicity. The scheme tag stays sd1
// because the new fields are rendered only where they deviate from what
// the old model could already say.
const TYPED_XMI = `<?xml version="1.0" encoding="UTF-8"?>
<services:ServiceInterface xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:services="http://eclipse.org/fennec/services/1.0" name="DataSetService" version="1.0.0">
  <operations name="get">
    <parameters name="id" type="string"/>
    <returnValue name="result">
      <eType href="http://example.org/atlas/1.0#//DataSet"/>
    </returnValue>
  </operations>
  <operations name="list">
    <parameters name="limit" type="int" lowerBound="0" optional="true"/>
    <returnValue name="result" upperBound="-1">
      <eType href="http://example.org/atlas/1.0#//DataSet"/>
    </returnValue>
  </operations>
</services:ServiceInterface>`;

function typedInterface(): ServiceInterface {
  const si = firstOfClass<ServiceInterface>(asRoots(deserializeFromXmi(TYPED_XMI)), 'ServiceInterface');
  if (!si) throw new Error('fixture without ServiceInterface');
  return si;
}

describe('ServiceDescriptionFingerprint (sd1) — typed slots and multiplicity (#41)', () => {
  it('renders the metamodel URI of an unresolved eType', () => {
    expect(canonicalForm(typedInterface())).toContain(
      'O|get|returnType=|returnEType=http://example.org/atlas/1.0#//DataSet'
    );
  });

  it('separates a single instance from a list of them', () => {
    const canonical = canonicalForm(typedInterface())!;
    const [get, list] = canonical.split('\n').filter(l => l.startsWith('  O|'));
    expect(get).not.toContain('returnUpper');
    expect(list).toContain('|returnLower=1|returnUpper=-1');
  });

  it('a single-valued slot hashes the same however it states its lower bound', () => {
    // The trap this closes: a factory-built Parameter answers the model
    // default 1, one parsed from an XMI document that predates the bounds
    // answers nothing at all, and an optional parameter written
    // consistently says 0. All three mean "one value, may be omitted" and
    // `optional` already carries that — so none of them may move the
    // hash, in either language.
    const factory = DDSRFactory.eINSTANCE;
    const build = (lowerBound: number | undefined) => {
      const si = factory.createServiceInterface();
      si.name = 'Payment';
      const op = factory.createServiceOperation();
      op.name = 'charge';
      const p = factory.createParameter();
      p.name = 'currency';
      p.type = 'string';
      p.optional = true;
      if (lowerBound !== undefined) p.lowerBound = lowerBound;
      op.parameters.push(p);
      si.operations.push(op);
      return si;
    };
    expect(fingerprint(build(0))).toBe(fingerprint(build(undefined)));
    expect(fingerprint(build(1))).toBe(fingerprint(build(undefined)));
  });

  it('a stated multiplicity does move the hash', () => {
    const factory = DDSRFactory.eINSTANCE;
    const build = (upperBound: number) => {
      const si = factory.createServiceInterface();
      si.name = 'DataSetService';
      const op = factory.createServiceOperation();
      op.name = 'list';
      const result = factory.createParameter();
      result.name = 'result';
      result.type = 'DataSet';
      result.upperBound = upperBound;
      op.returnValue = result;
      si.operations.push(op);
      return si;
    };
    expect(fingerprint(build(-1))).not.toBe(fingerprint(build(1)));
  });
});
