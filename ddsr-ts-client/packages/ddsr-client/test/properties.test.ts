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
