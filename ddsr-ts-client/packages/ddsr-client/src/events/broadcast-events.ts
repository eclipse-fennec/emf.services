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

import type { ServiceEvent, ServiceFlavor, ServiceImplementation } from '@ddsr/model';
import { CE_TYPE_BROADCAST, CE_TYPE_BROADCAST_END, dataAsText, readStructured } from '../cloudevents/cloud-events.js';
import { decodeResult } from '../invocation/invocation.js';
import { toArray } from '../internal/emf-util.js';

/**
 * The broker's events as a stream every receiver shares (#252) - the
 * twin of what the Java event sources read. A value is a CloudEvents
 * `broadcast` holding the result whose value is the ServiceEvent, with
 * the reference, provider and contracts beside it; `broadcast.end` (over
 * a messaging broker) means the broker lost an event and every receiver
 * re-reads.
 */
export type BroadcastEvent = { kind: 'event'; event: ServiceEvent } | { kind: 'end' } | { kind: 'other'; type: string };

export function readBroadcastEvent(message: string): BroadcastEvent {
  const read = readStructured(message);
  const type = read.attributes.type;
  if (type === CE_TYPE_BROADCAST_END) return { kind: 'end' };
  if (type !== CE_TYPE_BROADCAST) return { kind: 'other', type };
  const document = dataAsText(read);
  if (!document) throw new Error('a value of the broadcast arrived without its document');
  const result = decodeResult(document);
  if (result.error !== undefined) throw new Error(`the broker's events failed: ${result.error}`);
  const event = result.value as ServiceEvent | undefined;
  if (!event || typeof event !== 'object') throw new Error('the value of the broadcast is no ServiceEvent');
  return { kind: 'event', event };
}

/**
 * Whether a receiver that speaks these flavors hears this event: the
 * filter of the receiving side, read from the provider's implementations
 * in the document. Unknown flavors, or a receiver that names none, hear
 * it - over-deliver rather than drop. The twin of Java's EventDocument.heardBy.
 */
export function heardBy(event: ServiceEvent, spoken: string[]): boolean {
  const wanted = spoken.map(kind => kind.trim().toUpperCase()).filter(kind => kind.length > 0);
  const provider = (event.reference as { provider?: { implementations?: unknown } } | undefined)?.provider;
  if (wanted.length === 0 || !provider) return true;
  const offered = new Set<string>();
  for (const implementation of toArray<ServiceImplementation>(provider.implementations)) {
    for (const flavor of toArray<ServiceFlavor>(implementation.flavors)) {
      if (flavor.kind !== undefined && flavor.kind !== null) offered.add(String(flavor.kind).toUpperCase());
    }
  }
  return offered.size === 0 || wanted.some(kind => offered.has(kind));
}
