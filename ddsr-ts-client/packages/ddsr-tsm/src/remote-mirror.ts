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

import type { ServiceRegistration, ServiceRegistry, ServiceProperties } from '@eclipse-daanse/tsm';
import type { DdsrConsumer, ServiceLocator } from '@ddsr/client';
import { propertiesOf, serviceProxy } from '@ddsr/client';
import type { ServiceEvent } from '@ddsr/model';
import { remoteServiceId } from './contracts.js';

export interface RemoteMirrorOptions {
  /** The registry the remote services appear in. */
  registry: ServiceRegistry;
  /** The consumer that does the looking up and the invoking. */
  consumer: DdsrConsumer;
  /** ServiceInterface.name to mirror. */
  interfaceName: string;
  /** Service id to register under; defaults to `ddsr.remote.<interfaceName>`. */
  id?: string;
  /** LDAP filter, passed to the broker and kept for the listener. */
  filter?: string;
  /**
   * An sd1 fingerprint. With it the broker returns only implementations
   * whose contract hashes to exactly this, so what lands in the registry
   * answers to the contract the consumer was built against - a provider
   * that has since changed its operations is not mirrored at all.
   */
  fingerprint?: string;
  log?: (message: string) => void;
}

export interface RemoteMirror {
  readonly interfaceName: string;
  readonly id: string;
  /** Reference ids currently registered, in no particular order. */
  mirrored(): string[];
  /** Pull the broker's current answer and reconcile the registry against it. */
  refresh(): Promise<void>;
  /** Stop listening and withdraw everything this mirror registered. */
  close(): Promise<void>;
}

/**
 * Put the implementations of one remote ServiceInterface into a TSM
 * registry, and keep them there for as long as the broker says they
 * exist.
 *
 * What a consumer gains is that the distinction stops mattering: a
 * module injects `ddsr.remote.Payment` and gets a service whose methods
 * it calls, without knowing whether the provider sits in this process or
 * behind the broker. This is what OSGi calls a Remote Services
 * distribution provider — the registry is the seam, and only the thing
 * on the far side of it changed.
 *
 * Each implementation is registered on its own, identified by the
 * broker's reference id, so several providers of one interface coexist
 * and the ranking decides which one `get()` answers with. Its DDSR
 * properties come along, which is what makes a target filter work on
 * this side: `(payments.sandbox=true)` selects over the properties the
 * provider published, and `service.ranking` becomes the TSM ranking it
 * already means.
 *
 * The listener is the point of the whole thing. A provider that goes
 * away is withdrawn here too, so a component holding it is deactivated
 * by the registry rather than left calling a dead endpoint — the part
 * every consumer would otherwise write again around
 * `addServiceListener`.
 */
export async function mirrorRemoteServices(options: RemoteMirrorOptions): Promise<RemoteMirror> {
  const { registry, consumer, interfaceName, filter, fingerprint } = options;
  const id = options.id ?? remoteServiceId(interfaceName);
  const log = options.log ?? ((m: string) => console.error(`[ddsr-tsm] ${m}`));

  const registrations = new Map<string, ServiceRegistration>();

  function register(locator: ServiceLocator): void {
    const referenceId = locator.reference?.id;
    if (!referenceId || registrations.has(referenceId)) return;

    const properties = propertiesFor(locator);
    registrations.set(referenceId, registry.register(id, serviceProxy(locator), {
      // Identifies the registration, so a second provider of the same
      // interface accumulates instead of replacing the first.
      providedBy: `ddsr:${referenceId}`,
      ranking: rankingOf(properties),
      properties,
    }));
    log(`mirrored ${interfaceName} reference ${referenceId} as '${id}'`);
  }

  function withdraw(referenceId: string): void {
    const registration = registrations.get(referenceId);
    if (!registration) return;
    registrations.delete(referenceId);
    registration.unregister();
    log(`withdrew ${interfaceName} reference ${referenceId}`);
  }

  async function refresh(): Promise<void> {
    const locators = await consumer.find(interfaceName, filter, fingerprint);
    const live = new Set<string>();
    for (const locator of locators) {
      const referenceId = locator.reference?.id;
      if (!referenceId) continue;
      live.add(referenceId);
      register(locator);
    }
    // Anything the broker no longer lists is gone, whether or not an
    // UNREGISTERING for it ever arrived — a reconnect can swallow one.
    for (const referenceId of [...registrations.keys()]) {
      if (!live.has(referenceId)) withdraw(referenceId);
    }
  }

  const onEvent = (event: ServiceEvent): void => {
    const referenceId = event.reference?.id;
    // An UNREGISTERING for something we hold needs no round trip: the
    // broker has already said what happened.
    if (String(event.type) === 'UNREGISTERING' && referenceId && registrations.has(referenceId)) {
      withdraw(referenceId);
      return;
    }
    void refresh().catch(error => log(`refresh after ${String(event.type)} failed: ${String(error)}`));
  };

  const unsubscribe = consumer.addServiceListener(interfaceName, filter, onEvent);
  await refresh();

  return {
    interfaceName,
    id,
    mirrored: () => [...registrations.keys()],
    refresh,
    close: async () => {
      unsubscribe();
      for (const referenceId of [...registrations.keys()]) withdraw(referenceId);
    },
  };
}

/**
 * The DDSR properties of the reference, as TSM service properties. Both
 * sides mean the same thing by a property — a name and a scalar a filter
 * selects on — so this is a change of carrier, not of meaning.
 */
function propertiesFor(locator: ServiceLocator): ServiceProperties {
  const properties: ServiceProperties = {};
  for (const [key, value] of propertiesOf(locator.reference)) {
    properties[key] = value;
  }
  const referenceId = locator.reference?.id;
  if (referenceId) properties['ddsr.reference.id'] = referenceId;
  const interfaceName = locator.serviceInterface?.name;
  if (interfaceName) properties['ddsr.interface'] = interfaceName;
  return properties;
}

/** `service.ranking` means in TSM what it means in OSGi and in DDSR. */
function rankingOf(properties: ServiceProperties): number {
  const raw = properties['service.ranking'];
  const ranking = typeof raw === 'number' ? raw : Number(raw);
  return Number.isFinite(ranking) ? ranking : 0;
}
