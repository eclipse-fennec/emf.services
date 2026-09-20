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
 * CloudEvents 1.0 — the envelope every message travels in (#101).
 *
 * The TypeScript mirror of `org.eclipse.fennec.services.cloudevents`.
 * Both sides write the same two content modes: structured JSON where a
 * transport carries nothing but messages, `ce-*` headers over HTTP
 * where the body stays the payload.
 *
 * The payload is bytes here too, and for the same two reasons as on the
 * Java side: a lifecycle event's document is multi-root, and the
 * encoding is the contract's choice since #100 — this module says which
 * one was used and does not take it back.
 *
 * The envelope is a plain type rather than the generated CloudEvents
 * model. It is JSON on the wire in structured mode and headers in
 * binary mode, so nothing here needs EMF; when the model is generated
 * for TypeScript it can replace the type without moving a byte of the
 * wire.
 */

import { ServiceEventType } from '@ddsr/model';

/** The attributes of one event. Extensions are kept apart, as the spec does. */
export interface CloudEventEnvelope {
  specversion: string;
  id: string;
  source: string;
  type: string;
  datacontenttype?: string;
  dataschema?: string;
  subject?: string;
  /** RFC 3339, the same shape `Date.toISOString()` produces. */
  time?: string;
  extensions: Record<string, string>;
}

/** An envelope and the payload bytes that arrived with it. */
export interface CloudEventMessage {
  attributes: CloudEventEnvelope;
  data?: Uint8Array;
}

/** The only spec version this registry writes or accepts. */
export const CE_SPEC_VERSION = '1.0';

/** Media type of a structured-mode message, JSON event format. */
export const CE_STRUCTURED_MEDIA_TYPE = 'application/cloudevents+json';

/** Prefix of the binary-mode attribute headers over HTTP. */
export const CE_HEADER_PREFIX = 'ce-';

const TYPE_PREFIX = 'org.eclipse.fennec.services.';

/** A call, as an event of its own. */
export const CE_TYPE_INVOKE = `${TYPE_PREFIX}invoke`;

/** The answer to a call, correlated by {@link CE_EXTENSION_CORRELATION_ID}. */
export const CE_TYPE_INVOKE_REPLY = `${TYPE_PREFIX}invoke.reply`;

/** Everything a subscriber knows may be stale; take a fresh snapshot. */
export const CE_TYPE_RESYNC = `${TYPE_PREFIX}resync`;

/** Extension carrying the id of the request an answer belongs to. */
export const CE_EXTENSION_CORRELATION_ID = 'correlationid';

/** Extension carrying the topic an answer is expected on. */
export const CE_EXTENSION_REPLY_TO = 'replyto';

const LIFECYCLE_TYPES: Record<string, ServiceEventType> = {
  [`${TYPE_PREFIX}unspecified`]: ServiceEventType.UNSPECIFIED,
  [`${TYPE_PREFIX}registered`]: ServiceEventType.REGISTERED,
  [`${TYPE_PREFIX}modified`]: ServiceEventType.MODIFIED,
  [`${TYPE_PREFIX}unregistering`]: ServiceEventType.UNREGISTERING,
  [`${TYPE_PREFIX}modified.endmatch`]: ServiceEventType.MODIFIED_ENDMATCH,
  [`${TYPE_PREFIX}upgrade.available`]: ServiceEventType.UPGRADE_AVAILABLE,
  [`${TYPE_PREFIX}retired`]: ServiceEventType.RETIRED,
};

const LIFECYCLE_NAMES: Record<string, string> = Object.fromEntries(
  Object.entries(LIFECYCLE_TYPES).map(([type, literal]) => [literal, type]));

/** The event type for one lifecycle transition. */
export function lifecycleTypeOf(eventType: ServiceEventType | string): string {
  const type = LIFECYCLE_NAMES[eventType as string];
  if (!type) throw new Error(`not a lifecycle transition: ${String(eventType)}`);
  return type;
}

/**
 * The lifecycle transition a type names, or `undefined` when the type
 * is not one of ours — which on a shared transport is normal and not an
 * error.
 */
export function lifecycleEventTypeOf(type: string | undefined): ServiceEventType | undefined {
  return type ? LIFECYCLE_TYPES[type] : undefined;
}

/** Reserved member names: everything else in a message is an extension. */
const RESERVED = new Set([
  'specversion', 'id', 'source', 'type', 'datacontenttype', 'dataschema',
  'subject', 'time', 'data', 'data_base64',
]);

/** An envelope with the required attributes filled, a fresh id and the current time. */
export function newEnvelope(
  type: string, source: string, datacontenttype?: string,
): CloudEventEnvelope {
  return {
    specversion: CE_SPEC_VERSION,
    id: randomId(),
    source,
    type,
    ...(datacontenttype ? { datacontenttype } : {}),
    time: new Date().toISOString(),
    extensions: {},
  };
}

/**
 * Whether a payload in this encoding can be a JSON string rather than
 * base64. No content type means JSON by the specification's default,
 * which is textual too.
 */
export function isTextual(contentType?: string): boolean {
  if (!contentType || !contentType.trim()) return true;
  const type = contentType.toLowerCase().split(';')[0].trim();
  return type.startsWith('text/') || type.endsWith('/json') || type.endsWith('+json')
    || type.endsWith('/xml') || type.endsWith('+xml');
}

/** One message in structured mode: envelope and payload in one JSON document. */
export function writeStructured(
  envelope: CloudEventEnvelope, data?: Uint8Array | string,
): Uint8Array {
  const message: Record<string, unknown> = {
    specversion: required(envelope.specversion, 'specversion'),
    id: required(envelope.id, 'id'),
    source: required(envelope.source, 'source'),
    type: required(envelope.type, 'type'),
  };
  if (envelope.datacontenttype) message.datacontenttype = envelope.datacontenttype;
  if (envelope.dataschema) message.dataschema = envelope.dataschema;
  if (envelope.subject) message.subject = envelope.subject;
  if (envelope.time) message.time = envelope.time;
  for (const [name, value] of Object.entries(envelope.extensions ?? {})) {
    if (!RESERVED.has(name) && value !== undefined && value !== null) message[name] = value;
  }
  if (data !== undefined) {
    const bytes = typeof data === 'string' ? new TextEncoder().encode(data) : data;
    if (isTextual(envelope.datacontenttype)) {
      message.data = new TextDecoder().decode(bytes);
    } else {
      message.data_base64 = toBase64(bytes);
    }
  }
  return new TextEncoder().encode(JSON.stringify(message));
}

/**
 * The inverse of {@link writeStructured}. Throws when the document is
 * not JSON or not a CloudEvent — one unreadable message is for the
 * caller to skip, never a reason to end a subscription.
 */
export function readStructured(payload: Uint8Array | string): CloudEventMessage {
  const text = typeof payload === 'string' ? payload : new TextDecoder().decode(payload);
  let parsed: unknown;
  try {
    parsed = JSON.parse(text);
  } catch (error) {
    throw new Error(`not a JSON object: ${String(error)}`);
  }
  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    throw new Error('not a JSON object');
  }
  const message = parsed as Record<string, unknown>;
  const attributes: CloudEventEnvelope = {
    specversion: stringMember(message, 'specversion', true)!,
    id: stringMember(message, 'id', true)!,
    source: stringMember(message, 'source', true)!,
    type: stringMember(message, 'type', true)!,
    extensions: {},
  };
  const datacontenttype = stringMember(message, 'datacontenttype', false);
  if (datacontenttype) attributes.datacontenttype = datacontenttype;
  const dataschema = stringMember(message, 'dataschema', false);
  if (dataschema) attributes.dataschema = dataschema;
  const subject = stringMember(message, 'subject', false);
  if (subject) attributes.subject = subject;
  const time = stringMember(message, 'time', false);
  if (time) attributes.time = time;
  for (const [name, value] of Object.entries(message)) {
    if (!RESERVED.has(name) && typeof value === 'string') attributes.extensions[name] = value;
  }
  let data: Uint8Array | undefined;
  if (typeof message.data_base64 === 'string') {
    data = fromBase64(message.data_base64);
  } else if (typeof message.data === 'string') {
    data = new TextEncoder().encode(message.data);
  }
  return { attributes, data };
}

/**
 * The attributes as HTTP headers, for binary mode — everything but
 * `datacontenttype`, which in binary mode IS the message's own
 * `Content-Type`.
 */
export function toHeaders(envelope: CloudEventEnvelope): Record<string, string> {
  const headers: Record<string, string> = {
    [`${CE_HEADER_PREFIX}specversion`]: required(envelope.specversion, 'specversion'),
    [`${CE_HEADER_PREFIX}id`]: required(envelope.id, 'id'),
    [`${CE_HEADER_PREFIX}source`]: required(envelope.source, 'source'),
    [`${CE_HEADER_PREFIX}type`]: required(envelope.type, 'type'),
  };
  if (envelope.dataschema) headers[`${CE_HEADER_PREFIX}dataschema`] = envelope.dataschema;
  if (envelope.subject) headers[`${CE_HEADER_PREFIX}subject`] = envelope.subject;
  if (envelope.time) headers[`${CE_HEADER_PREFIX}time`] = envelope.time;
  for (const [name, value] of Object.entries(envelope.extensions ?? {})) {
    if (!RESERVED.has(name) && value != null) headers[`${CE_HEADER_PREFIX}${name}`] = value;
  }
  return headers;
}

/**
 * The envelope a message carries in its headers, or `undefined` when it
 * carries none — which is a normal answer: binary mode is additive, so
 * a peer that has not gained the envelope yet still sends a body that
 * never depended on it.
 */
export function fromHeaders(
  headers: Record<string, string> | Headers, contentType?: string,
): CloudEventEnvelope | undefined {
  const lower: Record<string, string> = {};
  const entries = headers instanceof Headers
    ? [...headers.entries()]
    : Object.entries(headers);
  for (const [name, value] of entries) {
    if (value != null) lower[name.toLowerCase()] = String(value);
  }
  const id = lower[`${CE_HEADER_PREFIX}id`];
  if (!id) return undefined;
  const envelope: CloudEventEnvelope = {
    specversion: lower[`${CE_HEADER_PREFIX}specversion`] ?? CE_SPEC_VERSION,
    id,
    source: lower[`${CE_HEADER_PREFIX}source`] ?? '',
    type: lower[`${CE_HEADER_PREFIX}type`] ?? '',
    extensions: {},
  };
  const dataschema = lower[`${CE_HEADER_PREFIX}dataschema`];
  if (dataschema) envelope.dataschema = dataschema;
  const subject = lower[`${CE_HEADER_PREFIX}subject`];
  if (subject) envelope.subject = subject;
  const time = lower[`${CE_HEADER_PREFIX}time`];
  if (time) envelope.time = time;
  if (contentType && contentType.trim()) envelope.datacontenttype = contentType;
  for (const [name, value] of Object.entries(lower)) {
    if (!name.startsWith(CE_HEADER_PREFIX)) continue;
    const attribute = name.slice(CE_HEADER_PREFIX.length);
    if (!RESERVED.has(attribute)) envelope.extensions[attribute] = value;
  }
  return envelope;
}

/** The payload as text — the shape every document on this wire has. */
export function dataAsText(message: CloudEventMessage): string {
  return message.data ? new TextDecoder().decode(message.data) : '';
}

function required(value: string | undefined, name: string): string {
  if (!value || !value.trim()) throw new Error(`required attribute '${name}' is not set`);
  return value;
}

function stringMember(
  message: Record<string, unknown>, name: string, mandatory: boolean,
): string | undefined {
  const value = message[name];
  if (typeof value === 'string') return value;
  if (mandatory) throw new Error(`required attribute '${name}' is missing or not a string`);
  return undefined;
}

function randomId(): string {
  const cryptoApi = globalThis.crypto as Crypto | undefined;
  if (typeof cryptoApi?.randomUUID === 'function') return cryptoApi.randomUUID();
  return `${Date.now().toString(16)}-${Math.random().toString(16).slice(2)}`;
}

const BASE64_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

/**
 * Base64 without an environment assumption: `btoa`/`Buffer` exist in
 * one runtime each, and this module is read by both a Node process and
 * whatever a browser bundle turns it into.
 */
function toBase64(bytes: Uint8Array): string {
  let out = '';
  for (let i = 0; i < bytes.length; i += 3) {
    const triplet = (bytes[i] << 16) | ((bytes[i + 1] ?? 0) << 8) | (bytes[i + 2] ?? 0);
    const remaining = bytes.length - i;
    out += BASE64_ALPHABET[(triplet >> 18) & 63]
      + BASE64_ALPHABET[(triplet >> 12) & 63]
      + (remaining > 1 ? BASE64_ALPHABET[(triplet >> 6) & 63] : '=')
      + (remaining > 2 ? BASE64_ALPHABET[triplet & 63] : '=');
  }
  return out;
}

function fromBase64(text: string): Uint8Array {
  const clean = text.replace(/[^A-Za-z0-9+/]/g, '');
  const bytes = new Uint8Array(Math.floor((clean.length * 3) / 4));
  let out = 0;
  for (let i = 0; i < clean.length; i += 4) {
    const quad = (BASE64_ALPHABET.indexOf(clean[i]) << 18)
      | (BASE64_ALPHABET.indexOf(clean[i + 1]) << 12)
      | ((clean[i + 2] ? BASE64_ALPHABET.indexOf(clean[i + 2]) : 0) << 6)
      | (clean[i + 3] ? BASE64_ALPHABET.indexOf(clean[i + 3]) : 0);
    if (out < bytes.length) bytes[out++] = (quad >> 16) & 0xff;
    if (out < bytes.length) bytes[out++] = (quad >> 8) & 0xff;
    if (out < bytes.length) bytes[out++] = quad & 0xff;
  }
  return bytes;
}
