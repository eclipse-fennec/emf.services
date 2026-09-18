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
import type { Property, ServiceImplementation } from '@ddsr/model';
import { eClassName, toArray } from '../internal/emf-util';
import { fingerprint as sd1, propertyLine, esc, escListElement, sortByName } from './service-description-fingerprint';

/**
 * Content-based fingerprint of a ServiceImplementation — scheme tag
 * "im1". The TS mirror of
 * org.eclipse.fennec.services.fingerprint.ServiceImplementationFingerprint;
 * the canonical grammar is FROZEN with the tag and documented there.
 * The cross-language golden test pins both implementations to the same
 * fixture files (itest/fixtures/fingerprint).
 *
 * Composition, not re-traversal: the contract part folds the sd1 values
 * of the referenced interfaces in as opaque tokens (Merkle principle) —
 * an im1 comparison is only meaningful when the sd1 components agree,
 * which the three-valued reconnect check ensures.
 *
 * EMF omits wire defaults, the canonical form does not: unset enum and
 * boolean attributes render their MODEL default literal, exactly as the
 * Java generated getters do (FlavorKind → REST, HttpMethod → GET,
 * MqttFlavor.defaultQos → AT_LEAST_ONCE, MqttOperationFlavor.qos →
 * AT_MOST_ONCE, correlation → true, retained/defaultRetained → false).
 */
export const IMPL_FINGERPRINT_SCHEME = 'im1';

export function implementationFingerprint(
  implementation: ServiceImplementation | undefined
): string | undefined {
  if (!implementation) return undefined;
  const hash = createHash('sha256')
    .update(implementationCanonicalForm(implementation)!, 'utf8')
    .digest('hex');
  return `${IMPL_FINGERPRINT_SCHEME}:${hash}`;
}

export function implementationCanonicalForm(
  implementation: ServiceImplementation | undefined
): string | undefined {
  if (!implementation) return undefined;
  const impl = implementation as unknown as Record<string, unknown>;
  const lines: string[] = [];

  lines.push(
    `IM|${esc(str(impl.implementationId))}` +
    `|name=${esc(str(impl.name))}|version=${esc(str(impl.version))}`
  );

  for (const si of toArray<never>(impl.serviceInterfaces)) {
    lines.push(`  c|${sd1(si)}`);
  }

  for (const flavor of toArray<Record<string, unknown>>(impl.flavors)) {
    lines.push(`  ${flavorLine(flavor)}`);
    for (const opFlavor of toArray<Record<string, unknown>>(flavor.operationFlavors)) {
      lines.push(`    ${operationFlavorLine(opFlavor)}`);
      lines.push(...bindingLines(opFlavor));
    }
  }

  for (const property of sortByName(toArray<Property>(impl.properties))) {
    lines.push(`  ${propertyLine(property)}`);
  }
  return lines.join('\n');
}

// --------------------------------------------------------------------

function flavorLine(flavor: Record<string, unknown>): string {
  const cls = eClassName(flavor);
  let line =
    `F|${cls}|${esc(str(flavor.name))}` +
    `|kind=${enumText(flavor.kind, 'REST')}`;
  if (cls === 'RestFlavor') {
    line +=
      `|host=${esc(str(flavor.host))}` +
      `|basePath=${esc(str(flavor.basePath))}` +
      `|contentTypes=${list(flavor.contentTypes)}`;
  } else if (cls === 'MqttFlavor') {
    line +=
      `|brokers=${list(flavor.brokers)}` +
      `|requestTopic=${esc(str(flavor.requestTopic))}` +
      `|responseTopic=${esc(str(flavor.responseTopic))}` +
      `|defaultQos=${enumText(flavor.defaultQos, 'AT_LEAST_ONCE')}` +
      `|defaultRetained=${boolWithDefault(flavor.defaultRetained, false)}`;
  }
  return line;
}

/**
 * The bindings of one operation flavor, one line each and sorted by the
 * name of what they bind — a binding is a set entry, its order carries no
 * meaning. Rendered only where the flavor declares any, so an
 * implementation that binds nothing hashes exactly as it did before these
 * lines existed.
 *
 * They belong in im1 because they decide behaviour: since #74 a consumer
 * places every argument where the binding says, so two implementations
 * differing only in bindings are not the same endpoint.
 */
function bindingLines(opFlavor: Record<string, unknown>): string[] {
  if (eClassName(opFlavor) !== 'RestOperationFlavor') return [];
  const lines: string[] = [];

  const parameterBindings = toArray<Record<string, unknown>>(opFlavor.parameterBindings);
  for (const binding of sortByBoundName(parameterBindings, 'parameter')) {
    const parameter = binding.parameter as { name?: string } | undefined;
    lines.push(
      `      pb|${esc(parameter?.name)}` +
      `|binding=${enumText(binding.binding, 'BODY')}` +
      `|wireName=${esc(str(binding.wireName))}`
    );
  }

  const exceptionBindings = toArray<Record<string, unknown>>(opFlavor.exceptionBindings);
  for (const binding of sortByBoundName(exceptionBindings, 'exception')) {
    const raised = binding.exception as { name?: string } | undefined;
    lines.push(`      xb|${esc(raised?.name)}|status=${statusText(binding.status)}`);
  }
  return lines;
}

/**
 * The status as exact decimal text. The XMI reader hands attribute values
 * over as strings, so this coerces along the model type — the canonical
 * form is computed over model semantics, not reader artefacts. An unset
 * status is the model default 500.
 */
function statusText(raw: unknown): string {
  if (raw === undefined || raw === null || raw === '') return '500';
  const value = typeof raw === 'number' ? Math.trunc(raw) : Number.parseInt(String(raw).trim(), 10);
  return Number.isNaN(value) ? '500' : String(value);
}

function sortByBoundName(
  bindings: Record<string, unknown>[],
  feature: string
): Record<string, unknown>[] {
  return [...bindings].sort((a, b) => {
    const an = (a[feature] as { name?: string } | undefined)?.name;
    const bn = (b[feature] as { name?: string } | undefined)?.name;
    if (an == null && bn == null) return 0;
    if (an == null) return -1;
    if (bn == null) return 1;
    return an < bn ? -1 : an > bn ? 1 : 0;
  });
}

function operationFlavorLine(opFlavor: Record<string, unknown>): string {
  const cls = eClassName(opFlavor);
  const operation = opFlavor.operation as { name?: string } | undefined;
  let line =
    `o|${cls}|${esc(str(opFlavor.name))}` +
    `|operation=${esc(operation?.name)}` +
    `|consumes=${list(opFlavor.consumes)}` +
    `|produces=${list(opFlavor.produces)}`;
  if (cls === 'RestOperationFlavor') {
    line +=
      `|method=${enumText(opFlavor.method, 'GET')}` +
      `|path=${esc(str(opFlavor.path))}` +
      `|returnCodes=${list(opFlavor.returnCodes)}`;
  } else if (cls === 'MqttOperationFlavor') {
    line +=
      `|requestTopic=${esc(str(opFlavor.requestTopic))}` +
      `|responseTopic=${esc(str(opFlavor.responseTopic))}` +
      `|qos=${enumText(opFlavor.qos, 'AT_MOST_ONCE')}` +
      `|retained=${boolWithDefault(opFlavor.retained, false)}` +
      `|correlation=${boolWithDefault(opFlavor.correlation, true)}` +
      `|returnPath=${esc(str(opFlavor.returnPath))}`;
  }
  return line;
}

/** <count>:<e1,e2,…> — the sd1 StringList rule. */
function list(values: unknown): string {
  const elements = toArray<unknown>(values).map(e => escListElement(String(e)));
  return `${elements.length}:${elements.join(',')}`;
}

/** Unset enum attributes render the MODEL default (Java getter parity). */
function enumText(raw: unknown, modelDefault: string): string {
  if (raw === undefined || raw === null || raw === '') return modelDefault;
  return esc(String(raw));
}

/** Unset boolean attributes render the MODEL default (Java getter parity). */
function boolWithDefault(raw: unknown, modelDefault: boolean): string {
  if (raw === undefined || raw === null || raw === '') return String(modelDefault);
  return raw === true || raw === 'true' ? 'true' : 'false';
}

function str(raw: unknown): string | undefined {
  return raw == null ? undefined : String(raw);
}
