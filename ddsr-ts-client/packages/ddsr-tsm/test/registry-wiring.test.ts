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
import { DefaultServiceRegistry } from '@eclipse-daanse/tsm';
import type { DdsrClient, FlavorPlugin } from '@ddsr/client';
import { DdsrClientService, FlavorPluginService } from '../src/contracts';
import { bindFlavorPlugins, flavorPluginsFrom, publishClient, publishFlavorPlugin } from '../src/registry-wiring';
import type { FlavorSink } from '../src/registry-wiring';

function plugin(kind: string): FlavorPlugin {
  return {
    flavorKind: kind,
    canHandle: () => true,
    invoke: async () => kind,
  };
}

describe('DDSR services in a TSM registry', () => {
  it('two flavor plugins stand side by side rather than replacing each other', () => {
    const registry = new DefaultServiceRegistry();
    publishFlavorPlugin(registry, plugin('REST'));
    publishFlavorPlugin(registry, plugin('MQTT'));

    expect(registry.countProviders(FlavorPluginService)).toBe(2);
    expect(flavorPluginsFrom(registry).map(p => p.flavorKind).sort()).toEqual(['MQTT', 'REST']);
  });

  it('a plugin is selectable by its kind', () => {
    const registry = new DefaultServiceRegistry();
    publishFlavorPlugin(registry, plugin('REST'));
    publishFlavorPlugin(registry, plugin('MQTT'));

    expect(flavorPluginsFrom(registry, '(flavorKind=MQTT)').map(p => p.flavorKind)).toEqual(['MQTT']);
  });

  it('ranking decides which plugin comes first', () => {
    const registry = new DefaultServiceRegistry();
    publishFlavorPlugin(registry, plugin('REST'), { ranking: 1 });
    publishFlavorPlugin(registry, plugin('MQTT'), { ranking: 10 });

    expect(registry.get(FlavorPluginService)?.flavorKind).toBe('MQTT');
  });

  it('the client is injectable, and withdrawn when its registration goes', () => {
    const registry = new DefaultServiceRegistry();
    const client = { brokerUrl: 'http://localhost:8887/ddsr/rest' } as DdsrClient;

    const registration = publishClient(registry, client);
    expect(registry.get(DdsrClientService)).toBe(client);

    registration.unregister();
    expect(registry.get(DdsrClientService)).toBeUndefined();
  });

  describe('a client kept in step with the registry', () => {
    /** Records what a client would be told, in order. */
    function recordingClient() {
      const kinds: string[] = [];
      const sink: FlavorSink = {
        flavorAvailable: (p) => kinds.push(`+${p.flavorKind}`),
        flavorUnavailable: (p) => kinds.push(`-${p.flavorKind}`),
      };
      return { sink, kinds };
    }

    it('hands over what is already there', () => {
      const registry = new DefaultServiceRegistry();
      publishFlavorPlugin(registry, plugin('REST'));
      const { sink, kinds } = recordingClient();

      bindFlavorPlugins(registry, sink);

      expect(kinds).toEqual(['+REST']);
    });

    it('a plugin registered afterwards still reaches the client', () => {
      const registry = new DefaultServiceRegistry();
      const { sink, kinds } = recordingClient();
      bindFlavorPlugins(registry, sink);

      // The race a snapshot loses: the plugin module activated second.
      publishFlavorPlugin(registry, plugin('MQTT'));

      expect(kinds).toEqual(['+MQTT']);
    });

    it('a withdrawn plugin is taken back', () => {
      const registry = new DefaultServiceRegistry();
      const registration = publishFlavorPlugin(registry, plugin('MQTT'));
      const { sink, kinds } = recordingClient();
      bindFlavorPlugins(registry, sink);

      registration.unregister();

      expect(kinds).toEqual(['+MQTT', '-MQTT']);
    });

    it('detaching stops the updates and keeps what the client has', () => {
      const registry = new DefaultServiceRegistry();
      publishFlavorPlugin(registry, plugin('REST'));
      const { sink, kinds } = recordingClient();
      const detach = bindFlavorPlugins(registry, sink);

      detach();
      publishFlavorPlugin(registry, plugin('MQTT'));

      expect(kinds).toEqual(['+REST']);
    });
  });
});
