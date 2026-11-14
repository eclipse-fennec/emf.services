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

import type { ServiceEvent } from '@ddsr/model';
import { SseParser } from './sse-parser.js';
import { heardBy, readBroadcastEvent } from './broadcast-events.js';
import type { DdsrEventSource, EventSourceHandler, EventSubscription } from './event-source.js';
import { clientOrigin, withOrigin } from '../internal/client-origin.js';
import { withBearer, type TokenSupplier } from '../internal/bearer-token.js';

export interface RestEventSourceOptions {
  /** Broker base URL, e.g. http://localhost:8887/ddsr/rest */
  brokerUrl: string;
  /**
   * CSV of the FlavorKinds this consumer speaks: it hears only events about
   * services it can call (empty string = everything). The events are one
   * stream every receiver shares (#252), so the filter is this side's.
   */
  flavors?: string;
  /** Flat reconnect delay — same fixed-delay policy as the Java client. */
  reconnectSeconds?: number;
  /**
   * Which system this is, for the X-DDSR-Origin header (#132). The
   * stream carries it like every other call: on the Java side the same
   * job is done by a filter on the shared client, which covers the
   * event stream too, and a consumer that only ever listens would
   * otherwise be the one party the broker cannot place.
   */
  originLabel?: string;
  fetchFn?: typeof fetch;
  /**
   * Where the bearer token for the broker comes from, when the broker
   * runs in mode bearer (SECURITY.md §4) — e.g. `clientCredentials(...)`.
   * It goes on broker calls only, never on service invocations.
   */
  tokenSupplier?: TokenSupplier;
  log?: (message: string) => void;
}

/**
 * SSE event source against GET {brokerUrl}/events?flavors=… — the TS
 * mirror of the Java client's RestEventSource: hand-rolled SSE parsing,
 * flat reconnect delay (no backoff), onStreamEstablished() before any
 * event of a connection, undecodable payloads are logged and skipped
 * without tearing the stream down.
 */
export class RestEventSource implements DdsrEventSource {
  private readonly url: string;
  private readonly spoken: string[];
  private readonly reconnectMillis: number;
  private readonly fetchFn: typeof fetch;
  private readonly log: (message: string) => void;

  constructor(options: RestEventSourceOptions) {
    this.spoken = (options.flavors ?? 'REST').split(',').filter(kind => kind.trim().length > 0);
    this.url = `${options.brokerUrl.replace(/\/+$/, '')}/events`;
    this.reconnectMillis = (options.reconnectSeconds ?? 3) * 1000;
    this.fetchFn = withOrigin(
      withBearer(options.fetchFn ?? globalThis.fetch.bind(globalThis), options.tokenSupplier),
      clientOrigin(options.originLabel));
    this.log = options.log ?? ((m) => console.error(`[ddsr-events] ${m}`));
  }

  readonly transport = 'rest';

  open(handler: EventSourceHandler): EventSubscription {
    let running = true;
    let controller = new AbortController();
    let wakeSleep: (() => void) | undefined;

    const loop = async (): Promise<void> => {
      while (running) {
        controller = new AbortController();
        let established = false;
        try {
          const response = await this.fetchFn(this.url, {
            headers: { Accept: 'text/event-stream' },
            signal: controller.signal,
          });
          if (response.status === 204) {
            // The server's way of saying "stop" (WHATWG EventSource), and
            // the only one it has: every other answer is reconnected to,
            // which is what FR-Sync-Reconnect wants of a broker that is
            // briefly away. Jersey's SseEventSource had the same gap
            // until eclipse-ee4j/jersey#6119 (#171).
            this.log(`the broker answered ${this.url} with 204 — not reconnecting until the stream`
              + ' is asked for again');
            running = false;
            handler.onStreamEnded?.();
            return;
          }
          if (!response.ok || !response.body) {
            throw new Error(`event stream request failed: ${response.status}`);
          }
          // Snapshot before events — same ordering as the Java client.
          established = true;
          await handler.onStreamEstablished();
          await this.pump(response.body, handler);
          if (running) this.log('event stream closed by server, reconnecting');
        } catch (error) {
          if (running) this.log(`event stream error: ${String(error)}`);
        }
        if (established) handler.onStreamLost?.();
        if (running) {
          await new Promise<void>((resolve) => {
            wakeSleep = resolve;
            setTimeout(resolve, this.reconnectMillis);
          });
          wakeSleep = undefined;
        }
      }
    };
    const done = loop();

    return {
      close: async () => {
        running = false;
        controller.abort();
        wakeSleep?.();
        await done.catch(() => undefined);
      },
    };
  }

  private async pump(body: ReadableStream<Uint8Array>, handler: EventSourceHandler): Promise<void> {
    const parser = new SseParser();
    const decoder = new TextDecoder();
    const reader = body.getReader();
    try {
      for (;;) {
        const { done, value } = await reader.read();
        if (done) return;
        for (const payload of parser.feed(decoder.decode(value, { stream: true }))) {
          this.deliver(payload, handler);
        }
      }
    } finally {
      reader.releaseLock();
    }
  }

  private deliver(payload: string, handler: EventSourceHandler): void {
    if (!payload.trim()) return;
    let event: ServiceEvent;
    try {
      // A value of the broadcast (#252): a CloudEvent in structured mode,
      // the result whose value is the event. `end` carries no data; the
      // connection closes after it, and the reconnect is the snapshot.
      const read = readBroadcastEvent(payload);
      if (read.kind !== 'event') {
        if (read.kind === 'other') this.log(`ignoring a '${read.type}' message on the event stream`);
        return;
      }
      event = read.event;
    } catch (error) {
      this.log(`undecodable event payload skipped: ${String(error)}`);
      return;
    }
    if (!heardBy(event, this.spoken)) return;
    try {
      handler.onEvent(event);
    } catch (error) {
      this.log(`event handler failed: ${String(error)}`);
    }
  }
}
