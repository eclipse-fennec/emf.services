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

import { describe, expect, it } from 'vitest';
import { ServiceEventType } from '@ddsr/model';
import {
  CE_EXTENSION_CORRELATION_ID, CE_EXTENSION_REPLY_TO, CE_TYPE_INVOKE,
  dataAsText, fromHeaders, lifecycleEventTypeOf, lifecycleTypeOf, newEnvelope,
  readStructured, toHeaders, writeStructured,
} from '../src/cloudevents/cloud-events';

/**
 * The same cases as the Java `CloudEventCodecTest`, deliberately: the
 * envelope is a cross-language contract, so what is pinned here is the
 * wire — member names, which payload ends up base64, and the one header
 * binary mode must not write.
 */
describe('cloud events', () => {
  const XMI = '<?xml version="1.0"?><services:ServiceEvent/>';

  function envelope(datacontenttype?: string) {
    const built = newEnvelope(CE_TYPE_INVOKE, '/consumer/probe', datacontenttype);
    built.subject = 'ref-1';
    built.time = '2023-11-14T22:13:20Z';
    return built;
  }

  it('carries a textual payload as a JSON string', () => {
    const message = new TextDecoder().decode(writeStructured(envelope('application/xml'), XMI));

    expect(message).toContain('"specversion":"1.0"');
    expect(message).toContain('"type":"org.eclipse.fennec.services.invoke"');
    expect(message).toContain('"datacontenttype":"application/xml"');
    expect(message).toContain('"subject":"ref-1"');
    expect(message).toContain('"time":"2023-11-14T22:13:20Z"');
    expect(message).not.toContain('data_base64');

    const read = readStructured(message);
    expect(dataAsText(read)).toBe(XMI);
    expect(read.attributes.subject).toBe('ref-1');
  });

  it('carries a binary payload as base64', () => {
    const protobuf = new Uint8Array([0x0a, 0x03, 0xff, 0x00, 0x7f]);

    const message = writeStructured(envelope('application/x-protobuf'), protobuf);

    expect(new TextDecoder().decode(message)).toContain('"data_base64":"CgP/AH8="');
    expect(readStructured(message).data).toEqual(protobuf);
  });

  it('stores a JSON payload directly as a JSON value', () => {
    const document = '{"name": "Zürich", "values": [1, true, null]}';

    const message = writeStructured(envelope('application/json'), document);

    // The format forbids a JSON document wrapped in a string (§3.1.1).
    expect(new TextDecoder().decode(message))
      .toContain('"data":{"name":"Zürich","values":[1,true,null]}');
    expect(new TextDecoder().decode(message)).not.toContain('data_base64');
    // The same text the Java codec reads back, character for character.
    expect(dataAsText(readStructured(message))).toBe('{"name":"Zürich","values":[1,true,null]}');
  });

  it('counts a structured suffix as JSON too', () => {
    const message = writeStructured(envelope('application/vnd.acme+json; charset=utf-8'), '[1,2]');

    expect(new TextDecoder().decode(message)).toContain('"data":[1,2]');
  });

  it('does not send data declared JSON that is not JSON', () => {
    expect(() => writeStructured(envelope('application/json'), '<not json/>'))
      .toThrow(/declared JSON/);
  });

  it('still reads a JSON document a peer wrapped in a string', () => {
    const wrapped = '{"specversion":"1.0","id":"1","source":"/s","type":"t",'
      + '"datacontenttype":"application/json","data":"{\\"a\\": 1}"}';

    expect(dataAsText(readStructured(wrapped))).toBe('{"a": 1}');
  });

  it('keeps a plain string under a JSON content type a JSON string', () => {
    const plain = '{"specversion":"1.0","id":"1","source":"/s","type":"t",'
      + '"datacontenttype":"application/json","data":"just a string"}';

    // A string is a JSON string value; it is not parsed as a document (§3.1.2).
    expect(dataAsText(readStructured(plain))).toBe('"just a string"');
  });

  it('reads XML another implementation sent as base64 as the same document', () => {
    const base64 = Buffer.from(XMI, 'utf8').toString('base64');
    const message = '{"specversion":"1.0","id":"1","source":"/s","type":"t",'
      + `"datacontenttype":"application/xml","data_base64":"${base64}"}`;

    expect(dataAsText(readStructured(message))).toBe(XMI);
  });

  it('writes neither data member for an event that has no payload', () => {
    const message = new TextDecoder().decode(writeStructured(envelope(), undefined));

    expect(message).not.toContain('"data"');
    expect(message).not.toContain('data_base64');
    expect(readStructured(message).data).toBeUndefined();
  });

  it('keeps an extension it does not know', () => {
    const built = envelope('application/xml');
    built.extensions[CE_EXTENSION_REPLY_TO] = 'ddsr/rpc/Payment/charge/reply/7';
    built.extensions.traceparent = '00-abc-def-01';

    const read = readStructured(writeStructured(built));

    expect(read.attributes.extensions[CE_EXTENSION_REPLY_TO])
      .toBe('ddsr/rpc/Payment/charge/reply/7');
    expect(read.attributes.extensions.traceparent).toBe('00-abc-def-01');
  });

  it('refuses a document that is not a cloud event', () => {
    expect(() => readStructured('{"id":"1"}')).toThrow(/specversion/);
    expect(() => readStructured('not json')).toThrow();
  });

  it('refuses to write an envelope missing a required attribute', () => {
    const built = envelope('application/xml');
    built.source = '';

    expect(() => writeStructured(built)).toThrow(/source/);
  });

  it('leaves the content type to the message in binary mode', () => {
    const built = envelope('application/x-protobuf');
    built.extensions[CE_EXTENSION_CORRELATION_ID] = 'req-1';

    const headers = toHeaders(built);

    expect(headers['ce-specversion']).toBe('1.0');
    expect(headers['ce-type']).toBe(CE_TYPE_INVOKE);
    expect(headers['ce-source']).toBe('/consumer/probe');
    expect(headers['ce-subject']).toBe('ref-1');
    expect(headers['ce-correlationid']).toBe('req-1');
    expect(headers['ce-id']).toBeTruthy();
    expect(headers['ce-datacontenttype']).toBeUndefined();
  });

  it('reads headers back case-insensitively, content type becomes the encoding', () => {
    const read = fromHeaders({
      'CE-SpecVersion': '1.0',
      'Ce-Id': 'e-1',
      'ce-source': '/broker',
      'ce-Type': CE_TYPE_INVOKE,
      'ce-correlationid': 'req-1',
      'Content-Length': '17',
    }, 'application/xml');

    expect(read?.id).toBe('e-1');
    expect(read?.type).toBe(CE_TYPE_INVOKE);
    expect(read?.datacontenttype).toBe('application/xml');
    expect(read?.extensions[CE_EXTENSION_CORRELATION_ID]).toBe('req-1');
    expect(read?.extensions['content-length']).toBeUndefined();
  });

  it('reads a message without an envelope as none', () => {
    expect(fromHeaders({ 'Content-Type': 'application/xml' }, 'application/xml')).toBeUndefined();
  });

  it('gives every lifecycle type a name that reads back', () => {
    for (const type of Object.values(ServiceEventType)) {
      expect(lifecycleEventTypeOf(lifecycleTypeOf(type))).toBe(type);
    }
  });

  it('does not take a foreign type for a lifecycle event', () => {
    expect(lifecycleEventTypeOf('com.example.something.happened')).toBeUndefined();
    expect(lifecycleEventTypeOf(CE_TYPE_INVOKE)).toBeUndefined();
    expect(lifecycleEventTypeOf(undefined)).toBeUndefined();
  });
});
