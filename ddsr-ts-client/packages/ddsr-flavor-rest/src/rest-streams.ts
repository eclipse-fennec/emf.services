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
  DdsrClientError, DdsrServiceError, DdsrTransportError, declaredByCode, declaredCode, declaredRaisedBy,
  decodeResult, encodeFailure, encodeResult,
} from '@ddsr/client';

/**
 * How a stream travels over REST (#252): as Server-Sent Events, one
 * event per value - the twin of the Java `RestStreams`.
 *
 * Every value is a `ServiceInvocationResult` document, the same one a
 * value has over messaging. `value` is the next value, `error` the last
 * event of a stream that failed (the document holds the Diagnostic, a
 * declared error under its code), `end` says the stream is over. A
 * connection that closes without `end` broke off. The document is XMI
 * here, one `data:` line per line.
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

/** One event: its name and the document as data. */
export function frame(event: string, document?: string): string {
  if (!document) return `event: ${event}\ndata:\n\n`;
  const lines = document.replace(/\r\n?/g, '\n').split('\n');
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
      const result = decodeResult(data);
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
      yield frame(EVENT_VALUE, encodeResult(value));
    }
  } catch (failure) {
    const declared = declaredRaisedBy(operation, failure);
    yield frame(EVENT_ERROR, declared
      ? encodeFailure((failure as Error)?.message || declared.name || 'declared error', declaredCode(declared) ?? 0)
      : encodeFailure(String(failure), 0));
    return;
  }
  yield frame(EVENT_END);
}

/** Whether a value is a stream to be sent value by value. */
export function isAsyncIterable(value: unknown): value is AsyncIterable<unknown> {
  return typeof value === 'object' && value !== null
    && typeof (value as { [Symbol.asyncIterator]?: unknown })[Symbol.asyncIterator] === 'function';
}
