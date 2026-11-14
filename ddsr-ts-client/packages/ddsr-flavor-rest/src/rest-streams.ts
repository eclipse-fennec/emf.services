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

import type { ServiceOperation } from '@ddsr/model';
import {
  CE_TYPE_BROADCAST, CE_TYPE_INVOKE_REPLY, DdsrClientError, DdsrServiceError, DdsrTransportError, dataAsText,
  declaredByCode, declaredCode, declaredRaisedBy, decodeResult, encodeFailure, encodeResult, newEnvelope,
  readStructured, writeStructured,
} from '@ddsr/client';

/**
 * How a stream travels over REST (#252): as Server-Sent Events, one
 * event per value - the twin of the Java `RestStreams`.
 *
 * The data of an event is a CloudEvents message in structured mode - the
 * same message a value is over a messaging broker: `invoke.reply` for a
 * stream of its own, `broadcast` for one every receiver shares, each
 * holding a `ServiceInvocationResult` (XMI here). `value` is the next
 * value, `error` the last event of a stream that failed (the result holds
 * the Diagnostic, a declared error under its code), `end` says the stream
 * is over. A connection that closes without `end` broke off.
 *
 * The caller ends a stream by closing the connection; TCP is the
 * backpressure.
 */
export const STREAM_MEDIA_TYPE = 'text/event-stream';
export const EVENT_VALUE = 'value';
export const EVENT_ERROR = 'error';
export const EVENT_END = 'end';

/** Whether an operation answers with a stream - of its own or a shared one. */
export function streams(operation: ServiceOperation | undefined): boolean {
  const delivery = String(operation?.returnValue?.delivery ?? '');
  return delivery === 'STREAM' || delivery === 'BROADCAST';
}

/** One event: its name and its data, one `data:` line per line. */
export function frame(event: string, data?: string): string {
  if (!data) return `event: ${event}\ndata:\n\n`;
  const lines = data.replace(/\r\n?/g, '\n').split('\n');
  return `event: ${event}\n${lines.map(line => `data: ${line}`).join('\n')}\n\n`;
}

/** One event as it was read. */
export interface StreamEvent {
  event: string;
  data: string;
}

/**
 * The events of an SSE body, as they are asked for: the next chunk is
 * read only when the next event is wanted, so a slow reader leaves the
 * rest in the connection. Mirror of the Java `SseFrames.pumpEvents`.
 */
export async function* eventsOf(body: AsyncIterable<Uint8Array>): AsyncGenerator<StreamEvent> {
  const decoder = new TextDecoder();
  let pending = '';
  let data: string[] = [];
  let event: string | undefined;
  let dataSeen = false;
  for await (const chunk of body) {
    pending += decoder.decode(chunk, { stream: true });
    let newline: number;
    while ((newline = pending.indexOf('\n')) >= 0) {
      let line = pending.slice(0, newline);
      pending = pending.slice(newline + 1);
      if (line.endsWith('\r')) line = line.slice(0, -1);
      if (line === '') {
        if (dataSeen || event !== undefined) {
          yield { event: event ?? 'message', data: data.join('\n') };
        }
        data = [];
        event = undefined;
        dataSeen = false;
      } else if (line.startsWith('event:')) {
        event = line.slice('event:'.length).trim();
      } else if (line.startsWith('data:')) {
        let value = line.slice('data:'.length);
        if (value.startsWith(' ')) value = value.slice(1);
        data.push(value);
        dataSeen = true;
      }
    }
  }
}

/**
 * The values of a stream answered over REST: `value` yields, `error`
 * throws what its document says (a declared error as itself), `end`
 * returns, and a body that ends without `end` broke off.
 */
export async function* valuesOf(
  operation: ServiceOperation, events: AsyncIterable<StreamEvent>,
): AsyncGenerator<unknown> {
  for await (const { event, data } of events) {
    if (event === EVENT_VALUE || event === EVENT_ERROR) {
      const result = decodeResult(dataAsText(readStructured(data)));
      if (result.error !== undefined) {
        const declared = declaredByCode(operation, result.code);
        if (declared) throw new DdsrServiceError(result.error, declared);
        throw new DdsrClientError(`${operation.name} failed: ${result.error}`);
      }
      if (event === EVENT_ERROR) {
        throw new DdsrClientError(`the stream of ${operation.name} failed without saying why`);
      }
      yield result.value;
    } else if (event === EVENT_END) {
      return;
    }
  }
  throw new DdsrTransportError(
    `the stream of ${operation.name} broke off: the provider closed it without saying it was over`);
}

/**
 * A stream written as SSE events: one per value, `end` when it is over,
 * `error` with the Diagnostic when it failed. Leaving the loop over the
 * events (the caller went away) returns the stream it reads, which is
 * how the service learns that nobody listens any more.
 */
export async function* framesOf(
  operation: ServiceOperation | undefined, values: AsyncIterable<unknown>,
): AsyncGenerator<string> {
  try {
    for await (const value of values) {
      yield frame(EVENT_VALUE, message(CE_TYPE_INVOKE_REPLY, encodeResult(value)));
    }
  } catch (failure) {
    yield failureFrame(operation, failure, CE_TYPE_INVOKE_REPLY);
    return;
  }
  yield frame(EVENT_END);
}

/** Whether a value is a stream to be sent value by value. */
export function isAsyncIterable(value: unknown): value is AsyncIterable<unknown> {
  return typeof value === 'object' && value !== null
    && typeof (value as { [Symbol.asyncIterator]?: unknown })[Symbol.asyncIterator] === 'function';
}

/**
 * A stream every receiver shares, over REST (#252) - the twin of the Java
 * `RestBroadcast`: the provider is the distribution point. The source is
 * asked when the first receiver comes, and every value goes to every
 * receiver at the source's pace. Each receiver has a buffer of its own
 * under the contract's overflow policy: `DISCARD_OLDEST` drops its oldest
 * value, `FAIL` ends it with an `error` event once it has written what it
 * holds, `BLOCK` holds the source until there is room. When the last
 * receiver goes, the source's stream is returned.
 */
export class RestBroadcast {
  private readonly receivers = new Set<Receiver>();
  private running: AsyncIterator<unknown> | undefined;
  /** A run is on, or being started: the next receiver joins it. */
  private active = false;

  constructor(
    private readonly operation: ServiceOperation | undefined,
    private readonly source: () => Promise<AsyncIterable<unknown>>,
    private readonly policy: string,
    private readonly size = 256,
  ) {}

  /** One more receiver: the events of its answer. */
  async *receiver(): AsyncGenerator<string> {
    const receiver = new Receiver(this.size, this.policy, this.operation);
    this.receivers.add(receiver);
    if (!this.active) {
      this.active = true;
      void this.run();
    }
    try {
      for (let frame = await receiver.take(); frame !== undefined; frame = await receiver.take()) {
        yield frame;
        if (receiver.closed) return;
      }
    } finally {
      receiver.gone();
      this.receivers.delete(receiver);
      if (this.receivers.size === 0 && this.active) {
        const running = this.running;
        this.running = undefined;
        this.active = false;
        void running?.return?.(undefined);
      }
    }
  }

  private async run(): Promise<void> {
    let iterator: AsyncIterator<unknown>;
    try {
      iterator = (await this.source())[Symbol.asyncIterator]();
    } catch (failure) {
      this.over(failureFrame(this.operation, failure, CE_TYPE_BROADCAST));
      return;
    }
    if (!this.active) {
      void iterator.return?.(undefined);
      return;
    }
    this.running = iterator;
    try {
      for (let next = await iterator.next(); !next.done; next = await iterator.next()) {
        if (this.running !== iterator) return;
        const frame_ = frame(EVENT_VALUE, message(CE_TYPE_BROADCAST, encodeResult(next.value)));
        for (const receiver of this.receivers) await receiver.offer(frame_);
      }
      if (this.running === iterator) this.over(frame(EVENT_END));
    } catch (failure) {
      if (this.running === iterator) this.over(failureFrame(this.operation, failure, CE_TYPE_BROADCAST));
    }
  }

  private over(last: string): void {
    this.running = undefined;
    this.active = false;
    for (const receiver of this.receivers) receiver.last(last);
  }
}

function failureFrame(operation: ServiceOperation | undefined, failure: unknown, type: string): string {
  const declared = declaredRaisedBy(operation, failure);
  return frame(EVENT_ERROR, message(type, declared
    ? encodeFailure((failure as Error)?.message || declared.name || 'declared error', declaredCode(declared) ?? 0)
    : encodeFailure(String(failure), 0)));
}

/** A result document as the CloudEvents message an SSE event carries. */
function message(type: string, document: string): string {
  return new TextDecoder().decode(writeStructured(newEnvelope(type, '/provider/ts', 'application/xml'), document));
}

/** One receiver of a broadcast: its buffer, and the last event once there is one. */
class Receiver {
  private readonly buffer: string[] = [];
  private final: string | undefined;
  private wake: (() => void) | undefined;
  private room: (() => void) | undefined;
  private left = false;
  closed = false;

  constructor(private readonly size: number, private readonly policy: string,
    private readonly operation: ServiceOperation | undefined) {}

  async offer(event: string): Promise<void> {
    if (this.left || this.final !== undefined) return;
    while (this.buffer.length >= this.size) {
      if (this.policy === 'DISCARD_OLDEST') {
        this.buffer.shift();
      } else if (this.policy === 'BLOCK') {
        await new Promise<void>(resolve => { this.room = resolve; });
        if (this.left) return;
      } else {
        this.final = failureFrame(this.operation, new Error(`the broadcast of ${this.operation?.name} fell behind:`
          + ` its buffer of ${this.size} values was full and the contract says a receiver that falls behind fails`),
          CE_TYPE_BROADCAST);
        this.notify();
        return;
      }
    }
    this.buffer.push(event);
    this.notify();
  }

  last(event: string): void {
    if (this.final === undefined) this.final = event;
    this.notify();
  }

  /** The next event to write, waiting for one; undefined once the last was taken. */
  async take(): Promise<string | undefined> {
    while (this.buffer.length === 0 && this.final === undefined) {
      await new Promise<void>(resolve => { this.wake = resolve; });
    }
    const room = this.room;
    this.room = undefined;
    room?.();
    if (this.buffer.length > 0) return this.buffer.shift();
    if (this.closed) return undefined;
    this.closed = true;
    return this.final;
  }

  gone(): void {
    this.left = true;
    const room = this.room;
    this.room = undefined;
    room?.();
  }

  private notify(): void {
    const wake = this.wake;
    this.wake = undefined;
    wake?.();
  }
}
