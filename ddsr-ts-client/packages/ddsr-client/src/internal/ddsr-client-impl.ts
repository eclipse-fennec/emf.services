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

import type { DdsrClient } from '../api/ddsr-client';
import type { DdsrCatalog } from '../api/ddsr-catalog';
import type { FlavorPlugin } from '../api/flavor-plugin';
import { BrokerHttp } from './broker-http';
import { DdsrProviderImpl } from './provider-impl';
import { DdsrConsumerImpl } from './consumer-impl';
import { DdsrCatalogImpl } from './catalog-impl';
import { ServiceListenerRegistry } from './service-listener-registry';
import { RestEventSource } from '../events/rest-event-source';
import type { DdsrEventSource } from '../events/event-source';

export interface DdsrClientOptions {
  /** Broker base URL, e.g. http://localhost:8887/ddsr/rest */
  brokerUrl: string;
  /** Loaded flavor plugins for application-service invocation. */
  flavorPlugins?: FlavorPlugin[];
  /** X-DDSR-Requestor for catalog operations (audit). */
  requestor?: string;
  /** consumerId sent with lookups. */
  consumerId?: string;
  /** CSV FlavorKind filter for the event stream; default "REST". */
  eventFlavors?: string;
  /** Flat SSE reconnect delay in seconds; default 3 (Java parity). */
  reconnectSeconds?: number;
  /** Injectable transport seam (tests). */
  fetchFn?: typeof fetch;
  /** Injectable event source (tests / alternative transports). */
  eventSource?: DdsrEventSource;
}

/**
 * Concrete DdsrClient. Wires the broker HTTP proxies, the SSE event
 * source, the listener registry (with snapshot-on-reconnect) and the
 * provider/consumer/catalog facades.
 */
export class DdsrClientImpl implements DdsrClient {
  readonly brokerUrl: string;
  readonly provider: DdsrProviderImpl;
  readonly consumer: DdsrConsumerImpl;
  readonly catalog: DdsrCatalog;

  private constructor(
    brokerUrl: string,
    provider: DdsrProviderImpl,
    consumer: DdsrConsumerImpl,
    catalog: DdsrCatalog
  ) {
    this.brokerUrl = brokerUrl;
    this.provider = provider;
    this.consumer = consumer;
    this.catalog = catalog;
  }

  /**
   * Create a DdsrClient. Construction is synchronous — no network I/O
   * happens until the first call; the event stream opens lazily with
   * the first listener.
   */
  static create(options: DdsrClientOptions): DdsrClientImpl;
  /** @deprecated use the options form */
  static create(brokerUrl: string, flavorPlugins: FlavorPlugin[]): DdsrClientImpl;
  static create(
    optionsOrUrl: DdsrClientOptions | string,
    legacyPlugins?: FlavorPlugin[]
  ): DdsrClientImpl {
    const options: DdsrClientOptions = typeof optionsOrUrl === 'string'
      ? { brokerUrl: optionsOrUrl, flavorPlugins: legacyPlugins ?? [] }
      : optionsOrUrl;

    const plugins = options.flavorPlugins ?? [];
    const broker = new BrokerHttp({
      brokerUrl: options.brokerUrl,
      requestor: options.requestor,
      fetchFn: options.fetchFn,
    });
    const eventSource = options.eventSource ?? new RestEventSource({
      brokerUrl: options.brokerUrl,
      flavors: options.eventFlavors ?? 'REST',
      reconnectSeconds: options.reconnectSeconds,
      fetchFn: options.fetchFn,
    });

    // The registry needs the consumer's snapshot refresh; resolved via
    // a late-bound indirection because both reference each other.
    let consumerRef: DdsrConsumerImpl | undefined;
    const listeners = new ServiceListenerRegistry(
      eventSource,
      () => consumerRef?.refreshFromSnapshot()
    );
    const supportedFlavors = plugins.map(p => p.flavorKind);
    const consumer = new DdsrConsumerImpl(broker, plugins, supportedFlavors, listeners, options.consumerId);
    consumerRef = consumer;

    const provider = new DdsrProviderImpl(broker);
    const catalog = new DdsrCatalogImpl(broker);
    return new DdsrClientImpl(options.brokerUrl, provider, consumer, catalog);
  }

  /**
   * Shutdown in FR-P3 order: first withdraw every live registration and
   * WAIT for the broker's confirmations (consumers are informed before
   * this resolves), then close the event stream. A caller providing a
   * service must keep its endpoint serving until close() resolves.
   */
  async close(): Promise<void> {
    try {
      await this.provider.withdrawAll();
    } finally {
      await this.consumer.listeners.close();
    }
  }
}
