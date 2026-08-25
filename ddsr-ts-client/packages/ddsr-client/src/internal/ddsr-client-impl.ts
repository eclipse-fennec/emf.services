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
  /** consumerId sent with lookups and used for the broker session. */
  consumerId?: string;
  /**
   * Renewal interval of the idempotent session PUT (acquire+release+
   * heartbeat in one, ACQUISITION.md §4); requires consumerId. Should
   * be half the broker's expiry. Default 600; 0 disables sessions.
   */
  sessionIntervalSeconds?: number;
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
    const client = new DdsrClientImpl(options.brokerUrl, provider, consumer, catalog);
    client.broker = broker;
    client.consumerId = options.consumerId;
    const interval = options.sessionIntervalSeconds ?? 600;
    if (options.consumerId && interval > 0) {
      // First PUT shortly after construction, then the flat interval —
      // the current set of known reference ids IS the acquisition list.
      client.sessionTimer = setInterval(() => {
        void client.renewSession();
      }, interval * 1000);
      // Node: the timer must not keep the process alive on its own.
      (client.sessionTimer as { unref?: () => void }).unref?.();
    }
    return client;
  }

  private broker: BrokerHttp | undefined;
  private consumerId: string | undefined;
  private sessionTimer: ReturnType<typeof setInterval> | undefined;

  /** Exposed for tests and for an eager first lease after lookups. */
  async renewSession(): Promise<void> {
    if (!this.broker || !this.consumerId) return;
    try {
      await this.broker.putConsumerSession(
        this.consumerId,
        [...this.consumer.listeners.knownReferenceIds()]
      );
    } catch (error) {
      // Best-effort: a missed renewal is ordinary silence for the TTL.
      console.error(`[ddsr] session renewal failed, retrying next interval: ${String(error)}`);
    }
  }

  /**
   * Shutdown in FR-P3 order: first withdraw every live registration and
   * WAIT for the broker's confirmations (consumers are informed before
   * this resolves), then close the event stream. A caller providing a
   * service must keep its endpoint serving until close() resolves.
   */
  async close(): Promise<void> {
    if (this.sessionTimer) {
      clearInterval(this.sessionTimer);
      this.sessionTimer = undefined;
    }
    try {
      await this.provider.withdrawAll();
      // Shutdown-notify BEFORE the streams close (FR-P3 order): the
      // broker releases the leases immediately instead of waiting for
      // the TTL. Best-effort — a dead broker must not stall shutdown.
      if (this.broker && this.consumerId) {
        await this.broker.deleteConsumerSession(this.consumerId).catch(error => {
          console.error(`[ddsr] session release failed, broker will expire it: ${String(error)}`);
        });
      }
    } finally {
      await this.consumer.listeners.close();
    }
  }
}
