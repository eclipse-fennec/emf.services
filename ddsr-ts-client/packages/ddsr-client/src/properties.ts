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

import type {
  BoolProperty,
  DoubleProperty,
  FloatProperty,
  IntProperty,
  LongProperty,
  Property,
  ShortProperty,
  StringListProperty,
  StringProperty,
} from '@ddsr/model';
import { DDSRFactory } from '@ddsr/model';
import { eClassName, toArray } from './internal/emf-util';

const factory = DDSRFactory.eINSTANCE;

/**
 * Typed helpers around the DDSR property hierarchy (the model's
 * cross-language replacement for OSGi's Map<String, Object> service
 * properties). Values read from the wire are coerced along the
 * property's TYPE, not along what the XMI reader happened to produce —
 * the emf.ts reader currently yields all attribute values as strings.
 */
export type PropertyValue = string | number | boolean | string[];

/** The broker-computed service-description fingerprint (D6/FR-P6). */
export const FINGERPRINT_PROPERTY = 'ddsr.fingerprint';

export function propertyValue(property: Property): PropertyValue | undefined {
  const raw = (property as { value?: unknown }).value;
  switch (eClassName(property)) {
    case 'StringProperty':
      return raw === undefined || raw === null ? undefined : String(raw);
    case 'IntProperty':
    case 'ShortProperty':
    case 'LongProperty':
    case 'DoubleProperty':
    case 'FloatProperty':
      return raw === undefined || raw === null || raw === '' ? 0 : Number(raw);
    case 'BoolProperty':
      return raw === true || raw === 'true';
    case 'StringListProperty':
      // An empty list is a value ("no entries"), distinct from a
      // missing property — same semantics as OSGi String[] properties.
      // Entries are whitespace-free tokens by convention: EMF's
      // attribute wire form for many-valued attributes is space-
      // separated, so splitting here matches how the Java side reads
      // that form. The builder below enforces the convention.
      return toArray<unknown>((property as StringListProperty).value)
        .flatMap(v => String(v).split(/\s+/))
        .filter(v => v !== '');
    default:
      return raw === undefined || raw === null ? undefined : String(raw);
  }
}

/** All properties of a holder (reference, implementation, …) as a Map. */
export function propertiesOf(holder: { properties?: unknown }): Map<string, PropertyValue> {
  const result = new Map<string, PropertyValue>();
  for (const property of toArray<Property>(holder.properties)) {
    const name = property.name;
    if (!name) continue;
    const value = propertyValue(property);
    if (value !== undefined) result.set(name, value);
  }
  return result;
}

/** One property value by name, or undefined. */
export function propertyOf(holder: { properties?: unknown }, name: string): PropertyValue | undefined {
  for (const property of toArray<Property>(holder.properties)) {
    if (property.name === name) return propertyValue(property);
  }
  return undefined;
}

/** Builders for all eight property types. */
export const props = {
  string(name: string, value: string): StringProperty {
    const p = factory.createStringProperty();
    p.name = name;
    p.value = value;
    return p;
  },
  int(name: string, value: number): IntProperty {
    const p = factory.createIntProperty();
    p.name = name;
    p.value = value;
    return p;
  },
  long(name: string, value: number): LongProperty {
    const p = factory.createLongProperty();
    p.name = name;
    p.value = value;
    return p;
  },
  double(name: string, value: number): DoubleProperty {
    const p = factory.createDoubleProperty();
    p.name = name;
    p.value = value;
    return p;
  },
  float(name: string, value: number): FloatProperty {
    const p = factory.createFloatProperty();
    p.name = name;
    p.value = value;
    return p;
  },
  short(name: string, value: number): ShortProperty {
    const p = factory.createShortProperty();
    p.name = name;
    p.value = value;
    return p;
  },
  bool(name: string, value: boolean): BoolProperty {
    const p = factory.createBoolProperty();
    p.name = name;
    p.value = value;
    return p;
  },
  stringList(name: string, values: string[]): StringListProperty {
    const p = factory.createStringListProperty();
    p.name = name;
    for (const value of values) {
      if (/\s/.test(value) || value === '') {
        throw new Error(
          `StringListProperty entries must be non-empty whitespace-free tokens, got '${value}' — ` +
          `EMF's attribute wire form is space-separated and would not survive the round-trip`
        );
      }
      p.value.push(value);
    }
    return p;
  },
};
