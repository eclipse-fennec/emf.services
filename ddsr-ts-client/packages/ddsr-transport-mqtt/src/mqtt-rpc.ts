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
 * The MQTT request/response convention (WIRE_CHANNELS.md).
 * FROZEN once cross-language — the consumer plugin and the provider
 * dispatcher on any side must agree byte-for-byte.
 *
 * Since #101 a call is two CloudEvents. The envelope carries what the
 * transport cannot: MQTT 3.1.1 has no response-topic and no
 * correlation-data (they arrive with MQTT 5, which the Java paho v3
 * client does not speak), so the reply address is the `replyto`
 * extension and the answer points back through `correlationid`. The
 * payload is the call itself — `ServiceInvocation`, and
 * `ServiceInvocationResult` coming back — which is what gives a
 * message-only transport typed arguments and modelled ones at all.
 *
 * - request topic:  MessagingOperationFlavor.channel, else
 *                   `<MessagingFlavor.channel>/<operation.name>`
 * - reply topic:    `<base>/<consumer>/<event id>` with base =
 *                   MessagingOperationFlavor.replyChannel, else
 *                   MessagingFlavor.replyChannel. A flavor that names
 *                   neither leaves the reply channel to the consumer,
 *                   and this track then listens under `ddsr/reply`.
 *                   The CONSUMER segment is what
 *                   makes the separation writable: a broker ACL can
 *                   grant `subscribe <base>/<me>/#` and nothing wider,
 *                   where a topic separated only by an unguessable id
 *                   keeps peers apart by obscurity. It also lets a
 *                   caller subscribe once for all its calls and tell
 *                   them apart by `correlationid`.
 * - request:        CloudEvent, structured mode, `type` …invoke,
 *                   `replyto` = the reply topic, data = the invocation
 * - response:       CloudEvent, `type` …invoke.reply, `correlationid` =
 *                   the request's id, data = the result (a value, or a
 *                   Diagnostic that says why there is none)
 * - qos:            MessagingOperationFlavor.qos, else MessagingFlavor.defaultQos,
 *                   else AT_LEAST_ONCE (the model default)
 * - retained:       never — a request/response is a transition, not a
 *                   state (same argument as for lifecycle events)
 */

import type { TlsOptions } from '@ddsr/client';
import { DDSRPackage } from '@ddsr/model';
import { connectAmqp, isAmqpUrl } from './amqp-client.js';
import { SESSION, SESSION_LEASE } from './messaging-session.js';
import type { AmqpConnectOptions } from './amqp-client.js';
import type { ServiceOperation } from '@ddsr/model';
import {
  CE_EXTENSION_CORRELATION_ID, CE_EXTENSION_REPLY_TO,
  CE_TYPE_INVOKE, CE_TYPE_INVOKE_REPLY,
  dataAsText, decodeInvocation, decodeResult, encodeFailure, encodeInvocation, encodeResult,
  newEnvelope, readStructured, writeStructured,
} from '@ddsr/client';

/** What this side can write a call in. XMI, and only XMI, for now. */
const INVOCATION_CONTENT_TYPE = 'application/xml';

/**
 * The encoding the contract declares for one direction of a call, and
 * whether this side can produce it (#100).
 *
 * <p>The MQTT path used to write XMI whatever the contract said. The
 * label on the wire was honest and the two ends agreed, which is
 * exactly what makes that kind of bug survive: a contract declaring
 * protobuf would have been served XMI by both, silently, for as long as
 * nobody compared the document with the contract.
 *
 * <p>So it is asked, and a declaration this side cannot honour is
 * refused rather than quietly ignored — the same rule the XMI codec
 * applies to a content type nothing is registered for.
 */
function declaredContentType(
  opFlavor: OperationFlavorLike, direction: 'consumes' | 'produces',
): string {
  const declared = toList(opFlavor[direction])[0];
  if (!declared || declared === INVOCATION_CONTENT_TYPE) {
    return INVOCATION_CONTENT_TYPE;
  }
  throw new Error(
    `the contract declares ${direction}=${declared} for this operation, and the TypeScript`
    + ` side can only write ${INVOCATION_CONTENT_TYPE} on MQTT — see #100`);
}

/** EMF lists arrive as an EList or a plain array, depending on the path. */
function toList(value: unknown): string[] {
  if (!value) return [];
  if (Array.isArray(value)) return value as string[];
  const elist = value as { size?: () => number; get?: (i: number) => string };
  if (typeof elist.size === 'function' && typeof elist.get === 'function') {
    return Array.from({ length: elist.size() }, (_, i) => elist.get!(i));
  }
  return [];
}

/** A decoded request: who asked, what they asked for, where the answer goes. */
export interface MqttRpcRequest {
  /** The request event's id — what the answer correlates with. */
  id: string;
  replyTo: string;
  operation: string;
  args: Record<string, unknown>;
  source: string;
  /** Set when the call opens a session: a stream it is answered with. */
  session?: { id: string; lease?: number };
}

/** A decoded answer: which call it answers, and what it says. */
export interface MqttRpcResponse {
  correlationId: string;
  value?: unknown;
  error?: string;
  /** The code of the failure's Diagnostic - which declared error it is, if any. */
  code?: number;
}

/** Small model slices — the TS model types the enums as plain strings. */
interface FlavorLike {
  channel?: string;
  replyChannel?: string;
  defaultQos?: string;
  kind?: string;
  brokers?: unknown;
}
interface OperationFlavorLike {
  channel?: string;
  replyChannel?: string;
  qos?: string;
  name?: string;
  operation?: { name?: string };
  consumes?: unknown;
  produces?: unknown;
}

/**
 * Whether a flavor speaks AMQP: its kind says so, or - for a flavor
 * without one - the scheme of its first broker. Everything else is
 * MQTT. The rule the Java side reads a flavor by (MessagingFlavors).
 */
export function isAmqpFlavor(flavor: FlavorLike): boolean {
  if (flavor.kind === 'AMQP') return true;
  if (flavor.kind === 'MQTT') return false;
  const brokers = Array.isArray(flavor.brokers) ? flavor.brokers
    : flavor.brokers && typeof (flavor.brokers as Iterable<unknown>)[Symbol.iterator] === 'function'
      ? [...(flavor.brokers as Iterable<unknown>)] : [];
  return String(brokers[0] ?? '').toLowerCase().startsWith('amqp');
}

/**
 * What separates the levels of a channel on this flavor's protocol: `/`
 * between the levels of an MQTT topic, `.` between the words of an AMQP
 * routing key. The one thing the protocol decides about a channel.
 */
export function separatorFor(flavor: FlavorLike): '/' | '.' {
  return isAmqpFlavor(flavor) ? '.' : '/';
}

/** A channel without the separators it may end in. */
function trimmed(channel: string | undefined): string {
  return (channel ?? '').replace(/[/.]+$/, '');
}

export function requestTopicFor(flavor: FlavorLike, opFlavor: OperationFlavorLike): string {
  if (opFlavor.channel) return opFlavor.channel;
  const operationName = opFlavor.operation?.name ?? opFlavor.name ?? '';
  const base = trimmed(flavor.channel);
  if (!base) throw new Error('the MessagingFlavor carries no channel and the operation flavor none either');
  return `${base}${separatorFor(flavor)}${operationName}`;
}

/**
 * Where a consumer listens when the flavor names no place for answers.
 * The flavor then leaves the reply channel to the consumer's
 * connection; a Java node lets its messaging instance provide one, and
 * this track — which talks to the broker itself — takes a tree of its
 * own. Either way the call names the exact channel, so the provider
 * needs to know nothing about it.
 */
export const OWN_REPLY_BASE = 'ddsr/reply';

/**
 * Where answers to one operation go, or undefined when the flavor names
 * no place for them — which is a statement, not a gap.
 */
export function namedReplyBaseFor(
  flavor: FlavorLike, opFlavor: OperationFlavorLike,
): string | undefined {
  const named = trimmed(opFlavor.replyChannel || flavor.replyChannel);
  return named || undefined;
}

export function replyBaseFor(flavor: FlavorLike, opFlavor: OperationFlavorLike): string {
  return namedReplyBaseFor(flavor, opFlavor)
    ?? (isAmqpFlavor(flavor) ? OWN_REPLY_BASE.replaceAll('/', '.') : OWN_REPLY_BASE);
}

/**
 * A name as a topic level: MQTT's own separator and wildcards cannot
 * appear in one, and a consumer calling itself `a/#` must not thereby
 * name a subtree it was never given.
 */
export function topicSegment(name: string | undefined): string {
  if (!name || !name.trim()) return 'anonymous';
  // The separators and wildcards of both protocols: a name is one level
  // of a topic and one word of a routing key alike.
  return name.replace(/[/+#.*\s]/g, '_');
}

/** Where one consumer's answers arrive: its own subtree under the base. */
export function replySubtreeFor(
  flavor: FlavorLike, opFlavor: OperationFlavorLike, consumer: string,
): string {
  return `${replyBaseFor(flavor, opFlavor)}${separatorFor(flavor)}${topicSegment(consumer)}`;
}

/** The topic one call's answer arrives on. */
export function replyTopicFor(
  flavor: FlavorLike, opFlavor: OperationFlavorLike, consumer: string, requestId: string,
): string {
  return `${replySubtreeFor(flavor, opFlavor, consumer)}${separatorFor(flavor)}${topicSegment(requestId)}`;
}

/**
 * The QoS of one operation: its own, or the flavor's default.
 *
 * The model makes `MessagingOperationFlavor.qos` unsettable (#81), and that
 * is what makes the precedence expressible at all: without unset state
 * an operation reads back as `AT_MOST_ONCE` — the enum's first literal
 * — so every operation looks like an override and `defaultQos` could
 * never apply.
 *
 * Asked through `eIsSet`, which is real state since
 * `@emfts/codegen` 0.0.2-next.6 and the same question the Java side
 * asks with `isSetQos()`. An operation can now step DOWN to
 * at-most-once under a higher flavor default, which the earlier
 * workaround here could not express.
 *
 * A plain object — a fixture, a hand-built flavor — has no unset state
 * to ask about, so its value counts as stated. That is the honest
 * reading: something that is not a model cannot distinguish silence
 * from a choice.
 */
export function qosFor(flavor: FlavorLike, opFlavor: OperationFlavorLike): 0 | 1 | 2 {
  const literal = statedQos(opFlavor) ?? flavor.defaultQos ?? 'AT_LEAST_ONCE';
  switch (literal) {
    case 'AT_MOST_ONCE': return 0;
    case 'EXACTLY_ONCE': return 2;
    default: return 1;
  }
}

/** What the operation itself says about QoS, or nothing. */
function statedQos(opFlavor: OperationFlavorLike): string | undefined {
  const reflective = opFlavor as {
    eClass?: () => { getEStructuralFeature(name: string): unknown } | undefined;
    eIsSet?: (feature: unknown) => boolean;
  };
  const feature = reflective.eClass?.()?.getEStructuralFeature('qos');
  if (feature && reflective.eIsSet) {
    return reflective.eIsSet(feature) ? opFlavor.qos : undefined;
  }
  return opFlavor.qos;
}

/**
 * One call, as the message that travels: the envelope says where the
 * answer goes, the payload says what is being called with what.
 */
export function encodeRequest(
  operation: ServiceOperation, args: Record<string, unknown>, replyTo: string, source: string,
  opFlavor: OperationFlavorLike = {}, extensions: Record<string, string> = {},
): { payload: Uint8Array; id: string } {
  const envelope = newEnvelope(CE_TYPE_INVOKE, source, declaredContentType(opFlavor, 'consumes'));
  envelope.subject = operation.name ?? undefined;
  // The schema the document adheres to: the namespace of its package —
  // an absolute URI, which is why it is the package and not a class.
  envelope.dataschema = DDSRPackage.eNS_URI;
  envelope.extensions[CE_EXTENSION_REPLY_TO] = replyTo;
  Object.assign(envelope.extensions, extensions);
  return {
    payload: writeStructured(envelope, encodeInvocation(operation, args)),
    id: envelope.id,
  };
}

/** The inverse, for the provider side. */
export function decodeRequest(payload: Uint8Array): MqttRpcRequest {
  const message = readStructured(payload);
  const replyTo = message.attributes.extensions[CE_EXTENSION_REPLY_TO];
  if (message.attributes.type !== CE_TYPE_INVOKE) {
    throw new Error(`not a call: ${message.attributes.type}`);
  }
  if (!replyTo) {
    throw new Error('a call without a reply address cannot be answered');
  }
  const invocation = decodeInvocation(dataAsText(message));
  const extensions = message.attributes.extensions;
  // A call that opens a session (a stream it answers with): its id is
  // the correlation every message of the session carries.
  const opensSession = extensions[SESSION] === 'open' && extensions[CE_EXTENSION_CORRELATION_ID];
  const lease = Number(extensions[SESSION_LEASE]);
  return {
    ...(opensSession
      ? { session: { id: extensions[CE_EXTENSION_CORRELATION_ID]!, lease: Number.isFinite(lease) ? lease : undefined } }
      : {}),
    id: message.attributes.id,
    replyTo,
    operation: invocation.operation,
    args: invocation.args,
    source: message.attributes.source,
  };
}

/** The answer to a call: a second event, correlated by the request's id. */
export function encodeResponse(
  requestId: string, source: string, value: unknown, failure?: string,
  opFlavor: OperationFlavorLike = {}, failureCode = 0,
): Uint8Array {
  const envelope = newEnvelope(CE_TYPE_INVOKE_REPLY, source,
      declaredContentType(opFlavor, 'produces'));
  envelope.dataschema = DDSRPackage.eNS_URI;
  envelope.extensions[CE_EXTENSION_CORRELATION_ID] = requestId;
  const document = failure !== undefined ? encodeFailure(failure, failureCode) : encodeResult(value);
  return writeStructured(envelope, document);
}

/** The inverse, for the consumer side. */
export function decodeResponse(payload: Uint8Array): MqttRpcResponse {
  const message = readStructured(payload);
  const correlationId = message.attributes.extensions[CE_EXTENSION_CORRELATION_ID];
  if (!correlationId) {
    throw new Error('an answer that names no call cannot be delivered to one');
  }
  return { correlationId, ...decodeResult(dataAsText(message)) };
}

/**
 * The slice of an mqtt.js client the RPC pieces need — injectable for
 * tests (same seam pattern as MqttClientLike of the event source).
 */
export interface MqttRpcClientLike {
  on(event: 'message', listener: (topic: string, payload: Uint8Array) => void): unknown;
  subscribeAsync(topicFilter: string, options?: { qos?: 0 | 1 | 2 }): Promise<unknown>;
  unsubscribeAsync(topicFilter: string): Promise<unknown>;
  publishAsync(topic: string, payload: Uint8Array | string,
    options?: { qos?: 0 | 1 | 2; retain?: boolean }): Promise<unknown>;
  endAsync(force?: boolean): Promise<void>;
}

/**
 * Default factory — a live mqtt.js connection. Connects with mqtt.js. With `tls` on an mqtts:// (or ssl://) URL the
 * node shows its certificate and believes the given authorities
 * (SECURITY.md §7d) — the same `TlsOptions` a node hands `tlsFetch`.
 */
/**
 * The connection a broker URL asks for: AMQP for amqp:// and amqps://,
 * MQTT for everything else. One factory, because which protocol a
 * flavor travels over is said by its broker and by nothing else.
 */
export async function connectMessaging(
  brokerUrl: string, clientId: string, tls?: TlsOptions, options: AmqpConnectOptions = {},
): Promise<MqttRpcClientLike> {
  return isAmqpUrl(brokerUrl)
    ? await connectAmqp(brokerUrl, clientId, tls, options)
    : await connectMqtt(brokerUrl, clientId, tls);
}

export async function connectMqtt(brokerUrl: string, clientId: string, tls?: TlsOptions): Promise<MqttRpcClientLike> {
  const { connectAsync } = await import('mqtt');
  return await connectAsync(brokerUrl, { clientId, ...tlsConnectOptions(tls) }) as unknown as MqttRpcClientLike;
}

/** What mqtt.js hands Node's tls.connect: the certificate, its password, the authorities. */
export function tlsConnectOptions(tls?: TlsOptions): Record<string, unknown> {
  if (!tls) return {};
  return Object.fromEntries(Object.entries({
    pfx: tls.pfx, passphrase: tls.passphrase, cert: tls.cert, key: tls.key, ca: tls.ca, rejectUnauthorized: true,
  }).filter(([, value]) => value !== undefined));
}
