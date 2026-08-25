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

import type { ServiceEvent, ServiceImplementation, ServiceInterface, ServiceProvider } from '@ddsr/model';
import type { DdsrServiceListener } from '../api/service-listener';
import type { DdsrEventSource, EventSubscription } from '../events/event-source';
import { toArray } from './emf-util';

interface Entry {
  interfaceName: string;
  filter: string | undefined;
  listener: DdsrServiceListener;
}

/**
 * Routes transport-delivered ServiceEvents to registered listeners —
 * the TS port of the Java client's ServiceListenerRegistry, with the
 * same rules:
 *
 * - Interest is per ServiceInterface.name; the stream is opened lazily
 *   on the first listener and closed on the last.
 * - A reference-id → interface-names map is maintained from lookup
 *   results (noteReference) and from self-contained REGISTERED events,
 *   so an UNREGISTERING that carries no interface info can still be
 *   routed to the right listeners.
 * - "Lieber überzustellen als verwerfen": when an event's interfaces
 *   cannot be determined at all, it is delivered to EVERY listener —
 *   over-delivery is recoverable, a silently dropped UNREGISTERING
 *   leaves a consumer bound to a dead service.
 * - On every (re)connect the snapshot refresh callback runs BEFORE
 *   events of that connection are delivered.
 */
export class ServiceListenerRegistry {
  private readonly entries: Entry[] = [];
  private readonly interfacesByReference = new Map<string, Set<string>>();
  private eventSource: DdsrEventSource | undefined;
  private subscription: EventSubscription | undefined;
  private readonly onSnapshotRefresh: () => void | Promise<void>;
  private readonly log: (message: string) => void;

  constructor(
    eventSource: DdsrEventSource | undefined,
    onSnapshotRefresh: () => void | Promise<void>,
    log?: (message: string) => void
  ) {
    this.eventSource = eventSource;
    this.onSnapshotRefresh = onSnapshotRefresh;
    this.log = log ?? ((m) => console.error(`[ddsr-listeners] ${m}`));
  }

  /**
   * Register a listener for an interface. Returns an unsubscribe
   * function. Without an event source the registration is accepted and
   * silently non-delivering — a client that cannot stream should not
   * fail, it should just not notify.
   */
  add(interfaceName: string, filter: string | undefined, listener: DdsrServiceListener): () => void {
    this.entries.push({ interfaceName, filter, listener });
    this.openIfNeeded();
    return () => this.remove(listener);
  }

  remove(listener: DdsrServiceListener): void {
    for (let i = this.entries.length - 1; i >= 0; i--) {
      if (this.entries[i].listener === listener) this.entries.splice(i, 1);
    }
    void this.closeIfUnused();
  }

  /** A late-bound transport became available; open if listeners wait. */
  transportAvailable(eventSource: DdsrEventSource): void {
    this.eventSource = eventSource;
    this.openIfNeeded();
  }

  subscribedInterfaces(): string[] {
    return [...new Set(this.entries.map(e => e.interfaceName))];
  }

  /**
   * The reference ids this consumer currently knows from lookups —
   * exactly the acquisition list of the session protocol
   * (ACQUISITION.md §4).
   */
  knownReferenceIds(): string[] {
    return [...this.interfacesByReference.keys()];
  }

  /** Remember which interfaces a reference serves (from lookup results). */
  noteReference(referenceId: string | undefined, interfaceNames: string[]): void {
    if (!referenceId || interfaceNames.length === 0) return;
    this.interfacesByReference.set(referenceId, new Set(interfaceNames));
  }

  /** Close the stream regardless of remaining listeners (client shutdown). */
  async close(): Promise<void> {
    const sub = this.subscription;
    this.subscription = undefined;
    if (sub) await sub.close();
  }

  onEvent(event: ServiceEvent): void {
    const affected = this.interfacesOf(event);
    for (const entry of [...this.entries]) {
      if (affected.size > 0 && !affected.has(entry.interfaceName)) continue;
      try {
        entry.listener(event);
      } catch (error) {
        this.log(`listener for '${entry.interfaceName}' failed: ${String(error)}`);
      }
    }
  }

  private openIfNeeded(): void {
    if (this.subscription || this.entries.length === 0 || !this.eventSource) return;
    this.subscription = this.eventSource.open({
      onEvent: (event) => this.onEvent(event),
      onStreamEstablished: () => this.onSnapshotRefresh(),
    });
  }

  private async closeIfUnused(): Promise<void> {
    if (this.entries.length > 0 || !this.subscription) return;
    const sub = this.subscription;
    this.subscription = undefined;
    await sub.close();
  }

  /**
   * Determine the interfaces an event concerns. Prefer the event
   * document itself (self-contained REGISTERED/UNREGISTERING); fall
   * back to what was remembered for the reference id. REGISTERED
   * memoizes, UNREGISTERING forgets after reading.
   */
  private interfacesOf(event: ServiceEvent): Set<string> {
    const referenceId = event.reference?.id;
    const fromDocument = this.interfacesFromDocument(event);
    if (fromDocument.size > 0) {
      if (referenceId && event.type === 'REGISTERED') {
        this.interfacesByReference.set(referenceId, new Set(fromDocument));
      }
      if (referenceId && event.type === 'UNREGISTERING') {
        this.interfacesByReference.delete(referenceId);
      }
      return fromDocument;
    }
    if (!referenceId) return new Set();
    const remembered = this.interfacesByReference.get(referenceId) ?? new Set<string>();
    if (event.type === 'UNREGISTERING') {
      this.interfacesByReference.delete(referenceId);
    }
    return new Set(remembered);
  }

  private interfacesFromDocument(event: ServiceEvent): Set<string> {
    const names = new Set<string>();
    const provider = event.reference?.provider as ServiceProvider | undefined;
    if (!provider) return names;
    for (const impl of toArray<ServiceImplementation>(provider.implementations)) {
      for (const si of toArray<ServiceInterface>(impl.serviceInterfaces)) {
        const name = si?.name;
        if (name) names.add(name);
      }
    }
    return names;
  }
}
