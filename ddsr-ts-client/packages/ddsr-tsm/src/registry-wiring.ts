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
  ObservableServiceRegistry, ServiceRegistration, ServiceRegistry, ServiceRegistryListener,
} from '@eclipse-daanse/tsm';
import type { DdsrClient, FlavorPlugin } from '@ddsr/client';
import { DdsrClientService, FlavorPluginService } from './contracts.js';

/**
 * Publish a client, so consumer modules inject it instead of building
 * one — the module that owns the broker URL is then the only one that
 * knows it.
 */
export function publishClient(
  registry: ServiceRegistry,
  client: DdsrClient,
  options: { providedBy?: string; ranking?: number } = {}
): ServiceRegistration {
  return registry.register(DdsrClientService, client, {
    providedBy: options.providedBy ?? 'ddsr.client',
    ranking: options.ranking,
  });
}

/**
 * Publish a flavor plugin under `ddsr.flavor`, with its kind as a
 * property so a filter can pick one: `(flavorKind=MQTT)`.
 *
 * `providedBy` defaults to the kind, which is what keeps a REST and an
 * MQTT plugin side by side — TSM identifies a registration by provider,
 * so two plugins registering anonymously would replace one another.
 */
export function publishFlavorPlugin(
  registry: ServiceRegistry,
  plugin: FlavorPlugin,
  options: { providedBy?: string; ranking?: number } = {}
): ServiceRegistration {
  return registry.register(FlavorPluginService, plugin, {
    providedBy: options.providedBy ?? `ddsr.flavor.${plugin.flavorKind}`,
    ranking: options.ranking,
    properties: { flavorKind: plugin.flavorKind },
  });
}

/**
 * The flavor plugins a registry currently holds — a snapshot, for the
 * client's `flavorPlugins` option at construction time.
 *
 * For a client that should keep up with plugins arriving and leaving,
 * use {@link bindFlavorPlugins} instead.
 */
export function flavorPluginsFrom(registry: ServiceRegistry, target?: string): FlavorPlugin[] {
  return target
    ? registry.getServices(FlavorPluginService, target)
    : registry.getServices(FlavorPluginService);
}

/** What a client has to offer to be fed plugins as they come and go. */
export interface FlavorSink {
  flavorAvailable(plugin: FlavorPlugin): void;
  flavorUnavailable(plugin: FlavorPlugin): void;
}

/**
 * Keep a client's flavor plugins in step with the registry: hand over
 * what is there now, then everything that arrives, and take back what
 * goes away.
 *
 * This is what the Java client does through Declarative Services, where
 * `bindEventSource()` is a bind method rather than an injected field
 * precisely because "the arrival of a transport has to be acted on: a
 * listener may already be registered and waiting". The registry plays
 * the part DS plays there; the client learns nothing about either.
 *
 * Ordering is why this matters. A plugin module and the module holding
 * the client are separate units, so either can win the race — and with
 * a snapshot the loser's plugins are lost without a sound.
 *
 * @returns a detach function; it stops listening and leaves the client
 *          with the plugins it currently has.
 */
export function bindFlavorPlugins(
  registry: ServiceRegistry,
  client: FlavorSink,
  options: { target?: string } = {}
): () => void {
  for (const plugin of flavorPluginsFrom(registry, options.target)) {
    client.flavorAvailable(plugin);
  }

  const observable = registry as ServiceRegistry & Partial<ObservableServiceRegistry>;
  if (typeof observable.addListener !== 'function') {
    // A registry that reports nothing leaves us with the snapshot — the
    // plugins present now still work, later ones cannot be noticed.
    return () => {};
  }

  const listener: ServiceRegistryListener = {
    onServiceEvent: (event) => {
      if (event.serviceId !== FlavorPluginService) return;
      const plugin = event.service as FlavorPlugin | undefined;
      if (!plugin) return;
      if (event.type === 'registered' || event.type === 'updated') {
        client.flavorAvailable(plugin);
      } else if (event.type === 'unregistered' || event.type === 'modified-endmatch') {
        client.flavorUnavailable(plugin);
      }
    },
  };
  observable.addListener(listener, options.target ? { filter: options.target } : {});
  return () => observable.removeListener?.(listener);
}
