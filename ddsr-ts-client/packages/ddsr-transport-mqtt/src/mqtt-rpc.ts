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
 * - request topic:  MqttOperationFlavor.requestTopic, else
 *                   `<MqttFlavor.requestTopic>/<operation.name>`
 * - reply topic:    `<base>/<consumer>/<event id>` with base =
 *                   MqttOperationFlavor.responseTopic, else
 *                   MqttFlavor.responseTopic, else
 *                   `<requestTopic>/reply`. The CONSUMER segment is what
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
 * - qos:            MqttOperationFlavor.qos, else MqttFlavor.defaultQos,
 *                   else AT_LEAST_ONCE (the model default)
 * - retained:       never — a request/response is a transition, not a
 *                   state (same argument as for lifecycle events)
 */

import type { ServiceOperation } from '@ddsr/model';
import {
  CE_EXTENSION_CORRELATION_ID, CE_EXTENSION_REPLY_TO, CE_TYPE_INVOKE, CE_TYPE_INVOKE_REPLY,
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
}

/** A decoded answer: which call it answers, and what it says. */
export interface MqttRpcResponse {
  correlationId: string;
  value?: unknown;
  error?: string;
}

/** Small model slices — the TS model types the enums as plain strings. */
interface FlavorLike {
  requestTopic?: string;
  responseTopic?: string;
  defaultQos?: string;
}
interface OperationFlavorLike {
  requestTopic?: string;
  responseTopic?: string;
  qos?: string;
  name?: string;
  operation?: { name?: string };
  consumes?: unknown;
  produces?: unknown;
}

export function requestTopicFor(flavor: FlavorLike, opFlavor: OperationFlavorLike): string {
  if (opFlavor.requestTopic) return opFlavor.requestTopic;
  const operationName = opFlavor.operation?.name ?? opFlavor.name ?? '';
  const base = (flavor.requestTopic ?? '').replace(/\/+$/, '');
  if (!base) throw new Error('MqttFlavor carries no requestTopic and the operation flavor none either');
  return `${base}/${operationName}`;
}

export function replyBaseFor(flavor: FlavorLike, opFlavor: OperationFlavorLike): string {
  return (
    opFlavor.responseTopic
    ?? flavor.responseTopic
    ?? `${requestTopicFor(flavor, opFlavor)}/reply`
  ).replace(/\/+$/, '');
}

/**
 * A name as a topic level: MQTT's own separator and wildcards cannot
 * appear in one, and a consumer calling itself `a/#` must not thereby
 * name a subtree it was never given.
 */
export function topicSegment(name: string | undefined): string {
  if (!name || !name.trim()) return 'anonymous';
  return name.replace(/[/+#\s]/g, '_');
}

/** Where one consumer's answers arrive: its own subtree under the base. */
export function replySubtreeFor(
  flavor: FlavorLike, opFlavor: OperationFlavorLike, consumer: string,
): string {
  return `${replyBaseFor(flavor, opFlavor)}/${topicSegment(consumer)}`;
}

/** The topic one call's answer arrives on. */
export function replyTopicFor(
  flavor: FlavorLike, opFlavor: OperationFlavorLike, consumer: string, requestId: string,
): string {
  return `${replySubtreeFor(flavor, opFlavor, consumer)}/${topicSegment(requestId)}`;
}

/**
 * The QoS of one operation: its own, or the flavor's default.
 *
 * An operation that says nothing reads back as `AT_MOST_ONCE`, because
 * that is the first literal of the enum and the model gives
 * `MqttOperationFlavor.qos` no default of its own — EMF reports it on
 * both sides of the wire, so there is no value that means "unset". This
 * function therefore treats `AT_MOST_ONCE` from an operation as silence
 * and lets `defaultQos` apply, which is the precedence the model
 * documents.
 *
 * The price: an operation cannot deliberately step DOWN to at-most-once
 * under a higher flavor default.
 *
 * #81 made `qos` unsettable, which fixed this on the Java side —
 * `isSetQos()` there is real state. It does NOT fix it here, and the
 * generated code says why: `@emfts/codegen` initialises the field to
 * the type default and derives "is set" from the value,
 * `this._qos !== MqttQos.AT_MOST_ONCE`. That is the same conflation
 * this function works around, one layer down, so the workaround stays
 * and stays exact. Lifting it needs unset state in the TypeScript
 * codegen, not another change to the model.
 */
export function qosFor(flavor: FlavorLike, opFlavor: OperationFlavorLike): 0 | 1 | 2 {
  const stated = opFlavor.qos === 'AT_MOST_ONCE' ? undefined : opFlavor.qos;
  const literal = stated ?? flavor.defaultQos ?? 'AT_LEAST_ONCE';
  switch (literal) {
    case 'AT_MOST_ONCE': return 0;
    case 'EXACTLY_ONCE': return 2;
    default: return 1;
  }
}

/**
 * One call, as the message that travels: the envelope says where the
 * answer goes, the payload says what is being called with what.
 */
export function encodeRequest(
  operation: ServiceOperation, args: Record<string, unknown>, replyTo: string, source: string,
  opFlavor: OperationFlavorLike = {},
): { payload: Uint8Array; id: string } {
  const envelope = newEnvelope(CE_TYPE_INVOKE, source, declaredContentType(opFlavor, 'consumes'));
  envelope.subject = operation.name ?? undefined;
  envelope.extensions[CE_EXTENSION_REPLY_TO] = replyTo;
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
  return {
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
  opFlavor: OperationFlavorLike = {},
): Uint8Array {
  const envelope = newEnvelope(CE_TYPE_INVOKE_REPLY, source,
      declaredContentType(opFlavor, 'produces'));
  envelope.extensions[CE_EXTENSION_CORRELATION_ID] = requestId;
  const document = failure !== undefined ? encodeFailure(failure) : encodeResult(value);
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

/** Default factory — a live mqtt.js connection. */
export async function connectMqtt(brokerUrl: string, clientId: string): Promise<MqttRpcClientLike> {
  const { connectAsync } = await import('mqtt');
  return await connectAsync(brokerUrl, { clientId }) as unknown as MqttRpcClientLike;
}
