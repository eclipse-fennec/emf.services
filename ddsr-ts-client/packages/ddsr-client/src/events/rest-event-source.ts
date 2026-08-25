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
import { deserializeFromXmi } from '../xmi/xmi-support';
import { asRoots, firstOfClass } from '../internal/emf-util';
import { SseParser } from './sse-parser';
import type { DdsrEventSource, EventSourceHandler, EventSubscription } from './event-source';

export interface RestEventSourceOptions {
  /** Broker base URL, e.g. http://localhost:8887/ddsr/rest */
  brokerUrl: string;
  /** CSV FlavorKind filter for ?flavors= (empty string = no filter). */
  flavors?: string;
  /** Flat reconnect delay — same fixed-delay policy as the Java client. */
  reconnectSeconds?: number;
  fetchFn?: typeof fetch;
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
  private readonly reconnectMillis: number;
  private readonly fetchFn: typeof fetch;
  private readonly log: (message: string) => void;

  constructor(options: RestEventSourceOptions) {
    const flavors = options.flavors ?? 'REST';
    const base = options.brokerUrl.replace(/\/+$/, '');
    this.url = flavors.trim()
      ? `${base}/events?flavors=${encodeURIComponent(flavors)}`
      : `${base}/events`;
    this.reconnectMillis = (options.reconnectSeconds ?? 3) * 1000;
    this.fetchFn = options.fetchFn ?? globalThis.fetch.bind(globalThis);
    this.log = options.log ?? ((m) => console.error(`[ddsr-events] ${m}`));
  }

  open(handler: EventSourceHandler): EventSubscription {
    let running = true;
    let controller = new AbortController();
    let wakeSleep: (() => void) | undefined;

    const loop = async (): Promise<void> => {
      while (running) {
        controller = new AbortController();
        try {
          const response = await this.fetchFn(this.url, {
            headers: { Accept: 'text/event-stream' },
            signal: controller.signal,
          });
          if (!response.ok || !response.body) {
            throw new Error(`event stream request failed: ${response.status}`);
          }
          // Snapshot before events — same ordering as the Java client.
          await handler.onStreamEstablished();
          await this.pump(response.body, handler);
          if (running) this.log('event stream closed by server, reconnecting');
        } catch (error) {
          if (running) this.log(`event stream error: ${String(error)}`);
        }
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
    let event: ServiceEvent | undefined;
    try {
      event = firstOfClass<ServiceEvent>(asRoots(deserializeFromXmi(payload)), 'ServiceEvent');
    } catch (error) {
      this.log(`undecodable event payload skipped: ${String(error)}`);
      return;
    }
    if (!event) {
      this.log('event payload without ServiceEvent root ignored');
      return;
    }
    try {
      handler.onEvent(event);
    } catch (error) {
      this.log(`event handler failed: ${String(error)}`);
    }
  }
}
