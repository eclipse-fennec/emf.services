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
import { BrokerHttp, isError } from './broker-http';
import { DdsrProviderImpl } from './provider-impl';
import { DdsrConsumerImpl } from './consumer-impl';
import { DdsrCatalogImpl } from './catalog-impl';
import { ClientRuntimeImpl } from './client-runtime-impl';
import { newRuntimeId } from './client-origin';
import { ServiceListenerRegistry } from './service-listener-registry';
import { RestEventSource } from '../events/rest-event-source';
import type { DdsrEventSource } from '../events/event-source';
import type { CallTracer } from '@ddsr/telemetry';

export interface DdsrClientOptions {
  /** Broker base URL, e.g. http://localhost:8887/ddsr/rest */
  brokerUrl: string;
  /** Loaded flavor plugins for application-service invocation. */
  flavorPlugins?: FlavorPlugin[];
  /** X-DDSR-Requestor for catalog operations (audit). */
  requestor?: string;
  /**
   * Which system this deployment is, for the X-DDSR-Origin header every
   * broker call carries (#132): a name such as payments-prod-eu. The
   * other half of the origin is a per-process id from the runtime.
   * Deliberately not a host name — name the system, not the machine.
   * Without it the broker records this client as "unnamed".
   */
  originLabel?: string;
  /**
   * consumerId sent with lookups and used for the broker session. When
   * empty, the client calls itself `consumer-<uuid>`, as the Java client
   * does: without an id there is no session, and a consumer without a
   * session holds leases the broker cannot see (#167). A generated id
   * is new on every start; a deployment that wants a stable one sets it.
   */
  consumerId?: string;
  /**
   * Renewal interval of the idempotent session PUT (acquire+release+
   * heartbeat in one, ACQUISITION.md §4). Should be half the broker's
   * expiry. Default 600; 0 disables sessions.
   */
  sessionIntervalSeconds?: number;
  /**
   * Provider liveness (#52, UPDATE_POLICY.md §4): interval of the
   * heartbeat sent for every live registration. The broker retires a
   * registration after two missed heartbeats (PROVIDER_LOST); a
   * registration the broker lost is published again. Default 30;
   * 0 disables heartbeats — the broker then never retires this provider
   * for silence.
   */
  providerHeartbeatSeconds?: number;
  /** CSV FlavorKind filter for the event stream; default "REST". */
  eventFlavors?: string;
  /** Flat SSE reconnect delay in seconds; default 3 (Java parity). */
  reconnectSeconds?: number;
  /** Injectable transport seam (tests). */
  fetchFn?: typeof fetch;
  /** Injectable event source (tests / alternative transports). */
  eventSource?: DdsrEventSource;
  /**
   * UPDATE_POLICY.md §3: rebind locators to the successor as soon as the
   * broker announces UPGRADE_AVAILABLE (true), or keep the current
   * registration until the broker retires it (false, default).
   */
  greedyRebind?: boolean;
  /**
   * Whoever is watching calls (#146). Optional in the same way it is
   * on the Java side: a runtime without telemetry installs none, and
   * nothing on the wire changes when it does.
   */
  tracer?: CallTracer;
  /**
   * Fixes the per-process half of the origin, so that a runtime which
   * also builds a flavor plugin can wear one identity in both (#125).
   */
  originRuntimeId?: string;
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
  readonly runtime: ClientRuntimeImpl;

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
    this.runtime = new ClientRuntimeImpl(provider, consumer, consumer.listeners);
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
    const consumerId = options.consumerId?.trim() ? options.consumerId : `consumer-${newRuntimeId()}`;
    const broker = new BrokerHttp({
      brokerUrl: options.brokerUrl,
      requestor: options.requestor,
      originLabel: options.originLabel,
      fetchFn: options.fetchFn,
      tracer: options.tracer,
      originRuntimeId: options.originRuntimeId,
    });
    const eventSource = options.eventSource ?? new RestEventSource({
      brokerUrl: options.brokerUrl,
      originLabel: options.originLabel,
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
    const consumer = new DdsrConsumerImpl(
      broker, plugins, supportedFlavors, listeners, consumerId, options.greedyRebind ?? false);
    consumerRef = consumer;

    const provider = new DdsrProviderImpl(broker);
    const catalog = new DdsrCatalogImpl(broker);
    const client = new DdsrClientImpl(options.brokerUrl, provider, consumer, catalog);
    client.broker = broker;
    client.consumerId = consumerId;
    const interval = options.sessionIntervalSeconds ?? 600;
    if (interval > 0) {
      // The current set of known reference ids IS the acquisition list,
      // so the first PUT carries whatever the lookups right after
      // construction found.
      client.sessionTimer = repeat(interval, () => void client.renewSession());
    }
    const heartbeat = options.providerHeartbeatSeconds ?? 30;
    if (heartbeat > 0) {
      client.heartbeatTimer = repeat(heartbeat, () => void client.heartbeatRegistrations(heartbeat));
    }
    return client;
  }

  private broker: BrokerHttp | undefined;
  private consumerId: string | undefined;
  private sessionTimer: Repeating | undefined;
  private heartbeatTimer: Repeating | undefined;

  /** Provider liveness (#52): one heartbeat per live registration; see DdsrProviderImpl.heartbeatAll. */
  async heartbeatRegistrations(intervalSeconds: number): Promise<number> {
    try {
      return await this.provider.heartbeatAll(intervalSeconds);
    } catch (error) {
      console.error(`[ddsr] provider heartbeat failed, retrying next interval: ${String(error)}`);
      return 0;
    }
  }

  /**
   * Puts the session with every reference id this client knows. The
   * client does this on its own, first after at most 5 s and then every
   * `sessionIntervalSeconds`; calling it is only needed by a caller that
   * wants a lease reported sooner than that.
   */
  async renewSession(): Promise<void> {
    if (!this.broker || !this.consumerId) return;
    try {
      const diagnostic = await this.broker.putConsumerSession(
        this.consumerId,
        [...this.consumer.listeners.knownReferenceIds()]
      );
      if (isError(diagnostic)) {
        // A refusal does not throw: the broker, or a proxy in front of
        // it, answered, and the answer was no. Said as loudly as a
        // network failure, because until the next renewal the broker
        // holds none of this client's leases (#170).
        console.error(`[ddsr] session renewal refused (${diagnostic.code}), retrying next interval: `
          + `${diagnostic.message ?? ''}`);
      }
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
    this.sessionTimer?.stop();
    this.sessionTimer = undefined;
    this.heartbeatTimer?.stop();
    this.heartbeatTimer = undefined;
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

/** A task on a fixed interval, as far as close() is concerned. */
interface Repeating {
  stop(): void;
}

/**
 * Runs a task shortly after construction and then every interval — the
 * schedule the Java client uses, `scheduleAtFixedRate(task,
 * min(interval, 5), interval)`. A bare setInterval first fires after a
 * whole interval, which for a session is 600 s in which the broker knows
 * nothing of this client (#170).
 */
function repeat(intervalSeconds: number, task: () => void): Repeating {
  let every: ReturnType<typeof setInterval> | undefined;
  const first = setTimeout(() => {
    task();
    every = setInterval(task, intervalSeconds * 1000);
    unref(every);
  }, Math.min(intervalSeconds, 5) * 1000);
  unref(first);
  return {
    stop() {
      clearTimeout(first);
      if (every) clearInterval(every);
    },
  };
}

/** Node: a timer must not keep the process alive on its own. */
function unref(timer: ReturnType<typeof setTimeout>): void {
  (timer as { unref?: () => void }).unref?.();
}
