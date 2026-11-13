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

/**
 * The duplex session of Gecko Messaging, spoken from TypeScript.
 *
 * A stream a service returns (Parameter.delivery = STREAM) travels in a
 * session: the call opens it, the responder accepts it on a channel of
 * its own, the requester says it is ready, and the responder sends one
 * message per value until the stream is over - then it closes the
 * session. Either side closes it early, and each keeps the other alive
 * with pings within the session's lease; a side that hears nothing for
 * a lease ends the session. That is how a stream the caller left stops
 * at its source, also when the caller went away without a word.
 *
 * The control messages are CloudEvents like every other message here;
 * what makes them control is the `replysession` extension. The names
 * and the rules are the messaging library's (org.gecko.messaging,
 * SessionClient / SessionResponder), so a Java node and a TypeScript
 * node open sessions with each other in either direction.
 */

import { randomUUID } from 'node:crypto';
import {
  CE_EXTENSION_CORRELATION_ID,
  newEnvelope,
  readStructured,
  writeStructured,
} from '@ddsr/client';
import type { MqttRpcClientLike } from './mqtt-rpc.js';

/** The extension that makes a message control: `open`, `accept`, `ready`, `ping` or `close`. */
export const SESSION = 'replysession';
/** Of an `accept`: the channel the requester sends the session's messages to. */
export const SESSION_CHANNEL = 'replysessionchannel';
/** Of an `open` and an `accept`: the lease in milliseconds. */
export const SESSION_LEASE = 'replysessionlease';
/** A responder that failed or refused says so with this, and a short reason. */
export const REPLY_ERROR = 'replyerror';
/**
 * Where generated reply channels start. A responder answers a request
 * whose reply channel starts here without further configuration.
 */
export const SESSION_REPLY_PREFIX = 'osgi/messaging/reply';

export const DEFAULT_LEASE_MS = 10_000;
const MIN_LEASE_MS = 1_000;
const MAX_LEASE_MS = 300_000;

/** The type of the control messages this side writes; the messaging library reads none. */
const CONTROL_TYPE = 'org.eclipse.fennec.services.session';

export type SessionKind = 'open' | 'accept' | 'ready' | 'ping' | 'close';

/** A channel of one session, under the generated prefix and the owner's name. */
export function sessionChannelFor(owner: string): string {
  const name = owner.replace(/[/#+.*]/g, '_') || 'node';
  return `${SESSION_REPLY_PREFIX}/${name}/${randomUUID()}`;
}

/** A control message of a session. */
export function encodeControl(
  sessionId: string, source: string, kind: SessionKind, extra: Record<string, string> = {},
): Uint8Array {
  const envelope = newEnvelope(CONTROL_TYPE, source);
  envelope.extensions[CE_EXTENSION_CORRELATION_ID] = sessionId;
  envelope.extensions[SESSION] = kind;
  Object.assign(envelope.extensions, extra);
  return writeStructured(envelope);
}

/** What a received message says about the session it belongs to. */
export interface SessionMessage {
  correlationId?: string;
  /** Set for a control message, absent for one of the session's own. */
  kind?: string;
  channel?: string;
  lease?: number;
  /** A responder that failed or refused. */
  error?: string;
}

export function readSessionMessage(payload: Uint8Array): SessionMessage {
  const extensions = readStructured(payload).attributes.extensions;
  const lease = extensions[SESSION_LEASE] !== undefined ? Number(extensions[SESSION_LEASE]) : undefined;
  return {
    correlationId: extensions[CE_EXTENSION_CORRELATION_ID],
    kind: extensions[SESSION],
    channel: extensions[SESSION_CHANNEL],
    lease: lease !== undefined && Number.isFinite(lease) && lease > 0 ? lease : undefined,
    error: extensions[REPLY_ERROR],
  };
}

/** The lease a responder grants for the one asked for. */
export function grantedLease(asked: number | undefined): number {
  return Math.min(MAX_LEASE_MS, Math.max(MIN_LEASE_MS, asked ?? DEFAULT_LEASE_MS));
}

/**
 * What happens to a value that arrives when the queue of a stream is
 * full: the oldest is dropped (latest wins), or the stream fails.
 * Holding the broker back for one stream is what the messaging library
 * does on the Java side; mqtt.js and amqplib deliver without being asked,
 * and holding back their connection would hold back every other call.
 */
export type OverflowPolicy = 'DISCARD_OLDEST' | 'FAIL';

export interface StreamBuffer {
  /** Values a stream may hold that the caller has not taken yet; default 256. */
  size?: number;
  /** Default `FAIL`: a stream that loses values says so. */
  policy?: OverflowPolicy;
}

export const DEFAULT_STREAM_BUFFER: Required<StreamBuffer> = { size: 256, policy: 'FAIL' };

/** A queue with a bound and a rule for what does not fit. */
export class BoundedQueue<T> {
  private readonly items: T[] = [];
  /** Values dropped by `DISCARD_OLDEST`. */
  dropped = 0;

  constructor(private readonly size: number, private readonly policy: OverflowPolicy) {
    if (!(size > 0)) throw new Error(`a stream buffer holds at least one value, not ${size}`);
  }

  /** @returns false when the value did not fit and the policy is to fail */
  push(item: T): boolean {
    if (this.items.length >= this.size) {
      if (this.policy === 'FAIL') return false;
      this.items.shift();
      this.dropped++;
    }
    this.items.push(item);
    return true;
  }

  shift(): T | undefined {
    return this.items.shift();
  }

  get length(): number {
    return this.items.length;
  }
}

/** Wakes one waiter, and is ready to be waited on again. */
class Signal {
  private wake: (() => void) | undefined;

  notify(): void {
    const wake = this.wake;
    this.wake = undefined;
    wake?.();
  }

  wait(): Promise<void> {
    return new Promise(resolve => { this.wake = resolve; });
  }
}

export interface RequestedSession {
  client: MqttRpcClientLike;
  /** Where the responder listens: the operation's request channel. */
  requestTopic: string;
  qos: 0 | 1 | 2;
  /** This node's name, for the channel it listens on. */
  owner: string;
  /** The CloudEvents source of the control messages. */
  source: string;
  /** The request that opens the session, for its id and the channel to answer on. */
  open: (sessionId: string, replyChannel: string, lease: number) => Uint8Array;
  /** How long the responder may take to accept. */
  timeoutMs: number;
  leaseMs?: number;
  buffer?: StreamBuffer;
  /** Hands this session every message on the connection that carries its id. */
  register: (sessionId: string, deliver: (payload: Uint8Array) => void) => void;
  unregister: (sessionId: string) => void;
  /** What a caller is told, for the operation and where it was called. */
  describe: string;
}

/**
 * The requesting side of one session: the payload of every message the
 * responder sends in it, as they are asked for. Nothing is sent before
 * the first one is asked for. Leaving the iteration closes the session,
 * and the responder is told.
 */
export async function* requestSession(session: RequestedSession): AsyncGenerator<Uint8Array> {
  const { client, qos } = session;
  const id = randomUUID();
  const channel = sessionChannelFor(session.owner);
  const buffer = { ...DEFAULT_STREAM_BUFFER, ...session.buffer };
  const queue = new BoundedQueue<Uint8Array>(buffer.size, buffer.policy);
  const arrived = new Signal();
  let lease = session.leaseMs ?? DEFAULT_LEASE_MS;
  let sendChannel: string | undefined;
  let ended: { error?: Error } | undefined;
  let closedByResponder = false;
  let lastSeen = Date.now();
  let lastSent = Date.now();
  let keeper: ReturnType<typeof setInterval> | undefined;

  const end = (error?: Error): void => {
    if (!ended) ended = { error };
    arrived.notify();
  };
  const sendControl = (kind: SessionKind): void => {
    if (!sendChannel) return;
    lastSent = Date.now();
    client.publishAsync(sendChannel, encodeControl(id, session.source, kind), { qos, retain: false })
      .catch(() => undefined);
  };

  session.register(id, payload => {
    let message: SessionMessage;
    try {
      message = readSessionMessage(payload);
    } catch {
      return;
    }
    if (message.correlationId !== id) return;
    lastSeen = Date.now();
    if (message.error !== undefined) {
      // A refusal or a responder that failed; the library sends no more
      // than a short reason.
      closedByResponder = message.kind === 'close' || message.kind === undefined;
      end(new Error(`the provider ended ${session.describe}: ${message.error}`));
      return;
    }
    switch (message.kind) {
      case undefined:
        if (!queue.push(payload)) {
          end(new Error(`${session.describe} brought more than ${buffer.size} values the caller had not`
            + ' taken; the stream was given up'));
        }
        arrived.notify();
        return;
      case 'accept':
        if (sendChannel || !message.channel) return;
        sendChannel = message.channel;
        if (message.lease) lease = message.lease;
        arrived.notify();
        return;
      case 'close':
        closedByResponder = true;
        end();
        return;
      default:
        // ping, or a kind this side does not need
        return;
    }
  });
  try {
    await client.subscribeAsync(channel, { qos });
    await client.publishAsync(session.requestTopic, session.open(id, channel, lease), { qos, retain: false });
    lastSent = Date.now();

    const acceptBy = Date.now() + session.timeoutMs;
    while (!sendChannel && !ended) {
      const left = acceptBy - Date.now();
      if (left <= 0) {
        throw new Error(`${session.describe}: the provider did not accept the session within ${session.timeoutMs}ms`);
      }
      let timer: ReturnType<typeof setTimeout> | undefined;
      await Promise.race([arrived.wait(), new Promise<void>(resolve => { timer = setTimeout(resolve, left); })]);
      clearTimeout(timer);
    }
    // Set by the listener, which the compiler cannot follow.
    const early = ended as { error?: Error } | undefined;
    if (early?.error) throw early.error;
    if (early) return;

    sendControl('ready');
    const period = Math.max(1, Math.floor(lease / 3));
    keeper = setInterval(() => {
      const now = Date.now();
      if (now - lastSeen > lease) {
        end(new Error(`${session.describe}: nothing from the provider within the lease of ${lease}ms`));
        return;
      }
      if (now - lastSent >= period) sendControl('ping');
    }, period);
    keeper.unref?.();

    for (;;) {
      const next = queue.shift();
      if (next !== undefined) {
        yield next;
        continue;
      }
      if (ended) {
        if (ended.error) throw ended.error;
        return;
      }
      await arrived.wait();
    }
  } finally {
    if (keeper) clearInterval(keeper);
    if (!closedByResponder) sendControl('close');
    session.unregister(id);
    await client.unsubscribeAsync(channel).catch(() => undefined);
  }
}

export interface RespondedSession {
  client: MqttRpcClientLike;
  /** The id of the session: the correlation of the request that opened it. */
  sessionId: string;
  /** Where the requester listens. */
  replyTo: string;
  /** The lease the requester asked for. */
  askedLease?: number;
  owner: string;
  source: string;
  qos: 0 | 1 | 2;
  /** The values to send, asked for when the requester is ready. */
  start: () => Promise<AsyncIterable<unknown>>;
  /** One message of the session for a value. */
  value: (value: unknown) => Uint8Array;
  /** The message that says why the stream broke off, before the session ends. */
  failure: (error: unknown) => Uint8Array;
  /** Where this side's channel is listened on; the server routes by it. */
  listen: (channel: string, deliver: (payload: Uint8Array) => void) => void;
  unlisten: (channel: string) => void;
  log: (message: string) => void;
}

/**
 * The responding side of one session: accepted on a channel of its own,
 * sends what `start` yields once the requester is ready, and closes the
 * session when that is over. A requester that closes it, or goes silent
 * for its lease, stops the iteration - and with it the stream it was fed
 * from.
 */
export async function respondSession(session: RespondedSession): Promise<void> {
  const { client, qos, sessionId: id } = session;
  const channel = sessionChannelFor(session.owner);
  const lease = grantedLease(session.askedLease);
  let lastSeen = Date.now();
  let lastSent = Date.now();
  let ended = false;
  let started = false;
  let iterator: AsyncIterator<unknown> | undefined;
  let keeper: ReturnType<typeof setInterval> | undefined;

  const publish = async (payload: Uint8Array, to = session.replyTo): Promise<void> => {
    lastSent = Date.now();
    await client.publishAsync(to, payload, { qos, retain: false });
  };
  const control = (kind: SessionKind, extra: Record<string, string> = {}): Promise<void> =>
    publish(encodeControl(id, session.source, kind, extra)).catch(() => undefined);
  const end = async (tell: boolean): Promise<void> => {
    if (ended) return;
    ended = true;
    if (keeper) clearInterval(keeper);
    session.unlisten(channel);
    await client.unsubscribeAsync(channel).catch(() => undefined);
    if (tell) await control('close');
    // Stops the stream at its source; a pending next() returns first.
    void iterator?.return?.().catch(() => undefined);
  };

  const run = async (): Promise<void> => {
    try {
      iterator = (await session.start())[Symbol.asyncIterator]();
      while (!ended) {
        const next = await iterator.next();
        if (next.done || ended) break;
        await publish(session.value(next.value));
      }
    } catch (error) {
      if (!ended) {
        session.log(`the stream of session ${id} broke off: ${String(error)}`);
        // The reason as the session's last message, a declared error
        // under its code; the library's own failure mark says no more
        // than that something failed.
        await publish(session.failure(error)).catch(() => undefined);
      }
    }
    await end(true);
  };

  session.listen(channel, payload => {
    let message: SessionMessage;
    try {
      message = readSessionMessage(payload);
    } catch {
      return;
    }
    if (message.correlationId !== id) return;
    lastSeen = Date.now();
    if (message.kind === 'ready' && !started) {
      started = true;
      void run();
    } else if (message.kind === 'close') {
      // The requester left: nothing more to tell it.
      void end(false);
    }
    // A message of the requester's own: a stream argument, not carried yet (#248).
  });
  await client.subscribeAsync(channel, { qos });
  const period = Math.max(1, Math.floor(lease / 3));
  keeper = setInterval(() => {
    const now = Date.now();
    if (now - lastSeen > lease) {
      session.log(`session ${id}: nothing from the requester within the lease of ${lease}ms; ended`);
      void end(false);
      return;
    }
    if (now - lastSent >= period) void control('ping');
  }, period);
  keeper.unref?.();
  await control('accept', { [SESSION_CHANNEL]: channel, [SESSION_LEASE]: String(lease) });
}
