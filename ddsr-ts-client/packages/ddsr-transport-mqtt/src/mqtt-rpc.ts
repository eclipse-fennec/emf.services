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
 * The MQTT request/response convention (A2 Etappe 2, WIRE_CHANNELS.md).
 * FROZEN once cross-language — the consumer plugin and the provider
 * dispatcher on any side must agree byte-for-byte.
 *
 * MQTT 3.1.1 has no response-topic or correlation-data properties
 * (they arrive with MQTT 5, which the Java paho v3 client does not
 * speak), so both travel IN the request envelope:
 *
 * - request topic:  MqttOperationFlavor.requestTopic, else
 *                   `<MqttFlavor.requestTopic>/<operation.name>`
 * - reply topic:    chosen by the CONSUMER as `<base>/<correlationId>`
 *                   with base = MqttOperationFlavor.responseTopic, else
 *                   MqttFlavor.responseTopic, else
 *                   `<requestTopic>/reply` — one topic per request, so
 *                   a subscription never sees a foreign answer
 * - request JSON:   {"correlationId":"<uuid>","replyTo":"<topic>",
 *                    "args":{<named parameters>}}
 * - response JSON:  {"correlationId":"<uuid>","result":<value>} on
 *                   success, {"correlationId":"<uuid>","error":"<msg>"}
 *                   on failure
 * - qos:            MqttOperationFlavor.qos, else MqttFlavor.defaultQos,
 *                   else AT_LEAST_ONCE (the model default)
 * - retained:       never — a request/response is a transition, not a
 *                   state (same argument as for lifecycle events)
 */

export interface MqttRpcRequest {
  correlationId: string;
  replyTo: string;
  args: Record<string, unknown>;
}

export interface MqttRpcResponse {
  correlationId: string;
  result?: unknown;
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

export function encodeRequest(request: MqttRpcRequest): Uint8Array {
  return new TextEncoder().encode(JSON.stringify(request));
}

export function decodeRequest(payload: Uint8Array): MqttRpcRequest {
  const parsed = JSON.parse(new TextDecoder().decode(payload)) as Partial<MqttRpcRequest>;
  if (!parsed.correlationId || !parsed.replyTo) {
    throw new Error('request envelope without correlationId/replyTo');
  }
  return { correlationId: parsed.correlationId, replyTo: parsed.replyTo, args: parsed.args ?? {} };
}

export function encodeResponse(response: MqttRpcResponse): Uint8Array {
  return new TextEncoder().encode(JSON.stringify(response));
}

export function decodeResponse(payload: Uint8Array): MqttRpcResponse {
  const parsed = JSON.parse(new TextDecoder().decode(payload)) as Partial<MqttRpcResponse>;
  if (!parsed.correlationId) {
    throw new Error('response envelope without correlationId');
  }
  return parsed as MqttRpcResponse;
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
