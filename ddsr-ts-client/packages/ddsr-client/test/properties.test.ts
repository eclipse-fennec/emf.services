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

import { describe, expect, it } from 'vitest';
import type { ServiceImplementation } from '@ddsr/model';
import { DDSRFactory } from '@ddsr/model';
import { propertiesOf, propertyOf, props } from '../src/properties';
import { serializeToXmi, deserializeFromXmi } from '../src/xmi/xmi-support';
import { asRoots, firstOfClass } from '../src/internal/emf-util';

/** All eight property types on one implementation. */
function allTypes(): ServiceImplementation {
  const implementation = DDSRFactory.eINSTANCE.createServiceImplementation();
  implementation.name = 'impl';
  implementation.properties.push(props.string('lang', 'typescript'));
  implementation.properties.push(props.int('service.ranking', 7));
  implementation.properties.push(props.long('big', 9007199254740991));
  implementation.properties.push(props.double('ratio', 0.5));
  implementation.properties.push(props.float('scale', 1.25));
  implementation.properties.push(props.short('small', 12));
  implementation.properties.push(props.bool('experimental', true));
  implementation.properties.push(props.stringList('tags', ['payments', 'demo']));
  implementation.properties.push(props.stringList('empty', []));
  return implementation;
}

describe('property round-trip fidelity (FR-P5)', () => {
  it('all eight types survive an XMI round-trip with their TYPES intact', () => {
    const xmi = serializeToXmi(allTypes() as any);
    const back = firstOfClass<ServiceImplementation>(
      asRoots(deserializeFromXmi(xmi)),
      'ServiceImplementation'
    )!;
    const values = propertiesOf(back);

    expect(values.get('lang')).toBe('typescript');
    expect(values.get('service.ranking')).toBe(7);
    expect(values.get('big')).toBe(9007199254740991);
    expect(values.get('ratio')).toBe(0.5);
    expect(values.get('scale')).toBe(1.25);
    expect(values.get('small')).toBe(12);
    expect(values.get('experimental')).toBe(true);
    expect(values.get('tags')).toEqual(['payments', 'demo']);
  });

  it('an empty StringListProperty stays distinguishable from a missing property', () => {
    const xmi = serializeToXmi(allTypes() as any);
    const back = firstOfClass<ServiceImplementation>(
      asRoots(deserializeFromXmi(xmi)),
      'ServiceImplementation'
    )!;
    expect(propertyOf(back, 'empty')).toEqual([]);
    expect(propertyOf(back, 'does-not-exist')).toBeUndefined();
  });

  it('coerces values along the property type, not the reader output', () => {
    // Java-written wire form: everything is an attribute string.
    const xmi = `<?xml version="1.0" encoding="UTF-8"?>
<services:ServiceImplementation xmlns:services="http://eclipse.org/fennec/services/1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" name="impl">
  <properties xsi:type="services:IntProperty" name="rank" value="42"/>
  <properties xsi:type="services:BoolProperty" name="flag" value="true"/>
  <properties xsi:type="services:DoubleProperty" name="half" value="0.5"/>
</services:ServiceImplementation>`;
    const back = firstOfClass<ServiceImplementation>(
      asRoots(deserializeFromXmi(xmi)),
      'ServiceImplementation'
    )!;
    expect(propertyOf(back, 'rank')).toBe(42);
    expect(propertyOf(back, 'flag')).toBe(true);
    expect(propertyOf(back, 'half')).toBe(0.5);
  });
});

describe('StringListProperty entries the attribute form cannot carry', () => {
  function withTags(values: string[]): ServiceImplementation {
    const implementation = DDSRFactory.eINSTANCE.createServiceImplementation();
    implementation.name = 'impl';
    implementation.properties.push(props.stringList('tags', values));
    return implementation;
  }

  function roundTrip(values: string[]): unknown {
    const xmi = serializeToXmi(withTags(values) as any);
    const back = firstOfClass<ServiceImplementation>(
      asRoots(deserializeFromXmi(xmi)),
      'ServiceImplementation'
    )!;
    return propertyOf(back, 'tags');
  }

  it('an entry carrying whitespace survives the round-trip as one entry', () => {
    // Joining with a space is only reversible while no entry contains
    // whitespace: ['a b', 'c'] would read back as three entries. The
    // writer switches to <value> elements for exactly this case.
    expect(roundTrip(['a b', 'c'])).toEqual(['a b', 'c']);
  });

  it('an empty entry stays an entry', () => {
    expect(roundTrip(['', 'c'])).toEqual(['', 'c']);
  });

  it('plain tokens keep the compact attribute form', () => {
    // The two sides do not pick the same form: Java writes an element
    // per entry throughout, we keep the compact one where it survives.
    // Harmless as long as each side reads both — pinned from the other
    // direction in StringListPropertyWireFormTest.
    const xmi = serializeToXmi(withTags(['payments', 'demo']) as any);
    expect(xmi).toContain('value="payments demo"');
    expect(roundTrip(['payments', 'demo'])).toEqual(['payments', 'demo']);
  });

  it('reads the Java-written element form without splitting an entry again', () => {
    // The form the Java broker writes for the same case — see
    // itest/fixtures/fingerprint/payment-impl.xmi.
    const xmi = `<?xml version="1.0" encoding="UTF-8"?>
<services:ServiceImplementation xmlns:services="http://eclipse.org/fennec/services/1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" name="impl">
  <properties xsi:type="services:StringListProperty" name="tags">
    <value>a b</value>
    <value>c</value>
  </properties>
</services:ServiceImplementation>`;
    const back = firstOfClass<ServiceImplementation>(
      asRoots(deserializeFromXmi(xmi)),
      'ServiceImplementation'
    )!;
    expect(propertyOf(back, 'tags')).toEqual(['a b', 'c']);
  });

  it('reads the Java-written attribute form as separate entries', () => {
    const xmi = `<?xml version="1.0" encoding="UTF-8"?>
<services:ServiceImplementation xmlns:services="http://eclipse.org/fennec/services/1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" name="impl">
  <properties xsi:type="services:StringListProperty" name="tags" value="payments demo"/>
</services:ServiceImplementation>`;
    const back = firstOfClass<ServiceImplementation>(
      asRoots(deserializeFromXmi(xmi)),
      'ServiceImplementation'
    )!;
    expect(propertyOf(back, 'tags')).toEqual(['payments', 'demo']);
  });
});
