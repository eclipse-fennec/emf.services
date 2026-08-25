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
// The value import registers the DDSR package with the XMI reader —
// without it the fixture parses to featureless stubs.
import { DDSRFactory } from '@ddsr/model';
import type { ServiceImplementation, ServiceProvider } from '@ddsr/model';
import {
  implementationCanonicalForm,
  implementationFingerprint,
} from '../src/fingerprint/service-implementation-fingerprint';
import { fingerprint as sd1 } from '../src/fingerprint/service-description-fingerprint';
import { deserializeFromXmi } from '../src/xmi/xmi-support';
import { asRoots, firstOfClass, toArray } from '../src/internal/emf-util';

// The same fixtures the Java golden test pins — the cross-language
// anchor for the frozen im1 scheme (ACQUISITION.md §11.1).
const fixtures = join(
  dirname(fileURLToPath(import.meta.url)),
  '..', '..', '..', '..', 'itest', 'fixtures', 'fingerprint'
);

function goldenImplementation(): ServiceImplementation {
  const xmi = readFileSync(join(fixtures, 'payment-impl.xmi'), 'utf8');
  const provider = firstOfClass<ServiceProvider>(
    asRoots(deserializeFromXmi(xmi)), 'ServiceProvider');
  if (!provider) throw new Error('fixture without ServiceProvider');
  const impl = toArray<ServiceImplementation>(provider.implementations)[0];
  if (!impl) throw new Error('fixture provider without implementation');
  return impl;
}

describe('ServiceImplementationFingerprint (im1) — cross-language golden', () => {
  it('produces the exact golden canonical form', () => {
    const expected = readFileSync(join(fixtures, 'payment-impl.canonical.txt'), 'utf8')
      .replace(/\n$/, '');
    expect(implementationCanonicalForm(goldenImplementation())).toBe(expected);
  });

  it('produces the exact golden hash', () => {
    const expected = readFileSync(join(fixtures, 'payment-impl.im1'), 'utf8').trim();
    expect(implementationFingerprint(goldenImplementation())).toBe(expected);
  });

  it('composes over sd1, not over the traversal', () => {
    const impl = goldenImplementation();
    const si = toArray<never>((impl as Record<string, unknown>).serviceInterfaces)[0];
    expect(implementationCanonicalForm(impl)).toContain(`\n  c|${sd1(si)}\n`);
  });

  it('endpoint drift moves im1 without touching sd1', () => {
    const before = goldenImplementation();
    const drifted = goldenImplementation();
    const rest = toArray<Record<string, unknown>>(
      (drifted as Record<string, unknown>).flavors)[0];
    rest.host = 'http://elsewhere:8080';
    const siBefore = toArray<never>((before as Record<string, unknown>).serviceInterfaces)[0];
    const siAfter = toArray<never>((drifted as Record<string, unknown>).serviceInterfaces)[0];
    expect(sd1(siAfter)).toBe(sd1(siBefore));
    expect(implementationFingerprint(drifted)).not.toBe(implementationFingerprint(before));
  });

  it('doc text does not move the hash (description excluded)', () => {
    const withDocs = goldenImplementation();
    const stripped = goldenImplementation();
    (stripped as Record<string, unknown>).description = undefined;
    expect(implementationFingerprint(withDocs)).toBe(implementationFingerprint(stripped));
  });

  it('covers the mqtt flavor fields (Java parity)', () => {
    const factory = DDSRFactory.eINSTANCE;
    const impl = factory.createServiceImplementation();
    impl.name = 'mqtt-impl';
    impl.implementationId = 'fixture:mqtt:1';
    const mqtt = factory.createMqttFlavor();
    mqtt.name = 'mqtt';
    mqtt.requestTopic = 'req/topic';
    (mqtt as Record<string, unknown>).defaultQos = 'AT_LEAST_ONCE';
    mqtt.brokers.push('tcp://localhost:1883');
    impl.flavors.push(mqtt);

    const canonical = implementationCanonicalForm(impl)!;
    expect(canonical).toContain('F|MqttFlavor|mqtt|');
    expect(canonical).toContain('|brokers=1:tcp://localhost:1883');
    expect(canonical).toContain('|requestTopic=req/topic');
    expect(canonical).toContain('|defaultQos=AT_LEAST_ONCE');
  });

  it('undefined for undefined input', () => {
    expect(implementationFingerprint(undefined)).toBeUndefined();
    expect(implementationCanonicalForm(undefined)).toBeUndefined();
  });
});
