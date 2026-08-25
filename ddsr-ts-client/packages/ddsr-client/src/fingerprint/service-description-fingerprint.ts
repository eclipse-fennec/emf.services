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

import { createHash } from 'node:crypto';
import type { Parameter, Property, ServiceException, ServiceInterface, ServiceOperation } from '@ddsr/model';
import { eClassName, toArray } from '../internal/emf-util';

/**
 * Content-based fingerprint of a ServiceInterface — scheme tag "sd1".
 * The TS mirror of org.gecko.ddsr.fingerprint.ServiceDescriptionFingerprint;
 * the canonical grammar is FROZEN with the tag and documented there. The
 * cross-language golden test pins both implementations to the same
 * fixture files (itest/fixtures/fingerprint) — any divergence between
 * the two implementations is a bug in one of them, never a "dialect".
 *
 * One deliberate porting detail: the emf.ts XMI reader currently yields
 * attribute values as strings, so every numeric/boolean rendering here
 * coerces along the property/parameter TYPE before rendering — the
 * canonical form is computed over model semantics, not reader artifacts.
 */
export const FINGERPRINT_SCHEME = 'sd1';

export function fingerprint(serviceInterface: ServiceInterface | undefined): string | undefined {
  if (!serviceInterface) return undefined;
  const hash = createHash('sha256')
    .update(canonicalForm(serviceInterface)!, 'utf8')
    .digest('hex');
  return `${FINGERPRINT_SCHEME}:${hash}`;
}

export function canonicalForm(serviceInterface: ServiceInterface | undefined): string | undefined {
  if (!serviceInterface) return undefined;
  const lines: string[] = [];

  const status = serviceInterface.status || 'ACTIVE';
  lines.push(`I|${esc(serviceInterface.name)}|version=${esc(serviceInterface.version)}|status=${status}`);

  const exceptions = sortByName(toArray<ServiceException>(serviceInterface.exceptions));
  for (const ex of exceptions) {
    lines.push(`  X|${esc(ex.name)}|type=${esc(ex.type)}|version=${esc(ex.version)}`);
    for (const property of sortByName(toArray<Property>(ex.properties))) {
      lines.push(`    ${propertyLine(property)}`);
    }
  }

  for (const op of toArray<ServiceOperation>(serviceInterface.operations)) {
    lines.push(`  O|${esc(op.name)}|returnType=${esc(op.returnType)}`);
    for (const p of toArray<Parameter>(op.parameters)) {
      lines.push(
        `    p|${esc(p.name)}|type=${esc(p.type)}|index=${intText(p.index)}` +
        `|optional=${boolText(p.optional)}|defaultValue=${esc(p.defaultValue)}`
      );
    }
    for (const ex of sortByName(toArray<ServiceException>(op.exceptions))) {
      lines.push(`    x|${esc(ex.name)}`);
    }
  }
  return lines.join('\n');
}

// --------------------------------------------------------------------

function sortByName<T extends { name?: string }>(items: T[]): T[] {
  // nulls first, then natural (code point) order — same as the Java
  // Comparator.nullsFirst(naturalOrder()) over Java Strings.
  return [...items].sort((a, b) => {
    const an = a.name;
    const bn = b.name;
    if (an == null && bn == null) return 0;
    if (an == null) return -1;
    if (bn == null) return 1;
    return an < bn ? -1 : an > bn ? 1 : 0;
  });
}

function propertyLine(property: Property): string {
  const raw = (property as { value?: unknown }).value;
  let tag: string;
  let value: string;
  switch (eClassName(property)) {
    case 'StringProperty':
      tag = 'S';
      value = esc(raw == null ? undefined : String(raw));
      break;
    case 'IntProperty':
      tag = 'i';
      value = intText(raw);
      break;
    case 'LongProperty':
      tag = 'l';
      value = intText(raw);
      break;
    case 'DoubleProperty':
      tag = 'd';
      value = `bits:${doubleBits(raw)}`;
      break;
    case 'FloatProperty':
      tag = 'f';
      value = `bits:${floatBits(raw)}`;
      break;
    case 'ShortProperty':
      tag = 's';
      value = intText(raw);
      break;
    case 'BoolProperty':
      tag = 'b';
      value = boolText(raw);
      break;
    case 'StringListProperty': {
      tag = 'SL';
      const elements = toArray<unknown>(raw).map(e => escListElement(String(e)));
      value = `${elements.length}:${elements.join(',')}`;
      break;
    }
    default:
      // Unknown subclass — conservative: visible, hash moves.
      tag = '?';
      value = esc(eClassName(property));
  }
  return `pr|${tag}|${esc(property.name)}|value=${value}`;
}

/** Exact decimal text for int/long/short — BigInt keeps 64-bit longs exact. */
function intText(raw: unknown): string {
  if (raw === undefined || raw === null || raw === '') return '0';
  try {
    return BigInt(typeof raw === 'number' ? Math.trunc(raw) : String(raw).trim()).toString();
  } catch {
    return '0';
  }
}

function boolText(raw: unknown): string {
  return raw === true || raw === 'true' ? 'true' : 'false';
}

function doubleBits(raw: unknown): string {
  const view = new DataView(new ArrayBuffer(8));
  view.setFloat64(0, numberOf(raw));
  return view.getBigUint64(0).toString(16).padStart(16, '0');
}

function floatBits(raw: unknown): string {
  const view = new DataView(new ArrayBuffer(4));
  view.setFloat32(0, numberOf(raw));
  return view.getUint32(0).toString(16).padStart(8, '0');
}

function numberOf(raw: unknown): number {
  if (typeof raw === 'number') return raw;
  if (raw === undefined || raw === null || raw === '') return 0;
  const parsed = Number(raw);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function esc(raw: string | undefined | null): string {
  if (!raw) return '';
  let out = '';
  for (const c of raw) {
    switch (c) {
      case '\\': out += '\\\\'; break;
      case '|': out += '\\|'; break;
      case '\n': out += '\\n'; break;
      case '\r': out += '\\r'; break;
      default: out += c;
    }
  }
  return out;
}

function escListElement(raw: string): string {
  return esc(raw).replaceAll(',', '\\,');
}
