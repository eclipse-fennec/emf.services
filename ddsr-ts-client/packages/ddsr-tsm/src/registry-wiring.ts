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

import type { ServiceRegistration, ServiceRegistry } from '@eclipse-daanse/tsm';
import type { DdsrClient, FlavorPlugin } from '@ddsr/client';
import { DdsrClientService, FlavorPluginService } from './contracts';

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
 * The flavor plugins a registry currently holds, for the client's
 * `flavorPlugins` option — the discovery the SPI comment on
 * FlavorPlugin describes.
 *
 * A snapshot: a plugin registered later is not in a client already
 * built from it. That is the honest shape for an option read once at
 * construction; a client that picks up flavors as they arrive needs the
 * client itself to hold the registry, which is a change to @ddsr/client
 * rather than to this package.
 */
export function flavorPluginsFrom(registry: ServiceRegistry, target?: string): FlavorPlugin[] {
  return target
    ? registry.getServices(FlavorPluginService, target)
    : registry.getServices(FlavorPluginService);
}
