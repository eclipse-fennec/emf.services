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

import { Agent as HttpsAgent, request as httpsRequest } from 'node:https';
import { request as httpRequest, type IncomingMessage } from 'node:http';
import { Readable } from 'node:stream';

/**
 * What a node shows and believes on a TLS connection (SECURITY.md §7d).
 * Contents, not paths: reading the files is the caller's business, the
 * same way the Java client is handed a key store rather than told where
 * one might be.
 */
export interface TlsOptions {
  /** The node's certificate and key as one PKCS12 bundle — the same file a Java node uses. */
  pfx?: Buffer;
  /** The password of the PKCS12 bundle. */
  passphrase?: string;
  /** The node's certificate, PEM — instead of pfx. */
  cert?: string | Buffer;
  /** The node's key, PEM — instead of pfx. */
  key?: string | Buffer;
  /** The authorities whose server certificates this node believes, PEM; without it Node's own. */
  ca?: string | Buffer;
}

/** Statuses that carry no body; a web Response refuses one for them. */
const NO_BODY = new Set([101, 103, 204, 205, 304]);

/**
 * A fetch that shows a client certificate — the TypeScript twin of the
 * Java client's key store and trust store, and the reason the SDK takes
 * a fetchFn at every seam: hand this to the broker client, the event
 * stream and the REST flavor plugin, and every call the node makes
 * carries its certificate.
 *
 * Over node:https rather than through the global fetch, whose TLS
 * options are not part of its API; plain http: URLs go through the same
 * code without an agent. Bodies are streamed both ways as the event
 * stream needs; an AbortSignal (the plugin's timeout) ends the request.
 */
export function tlsFetch(options: TlsOptions): typeof fetch {
  const agent = new HttpsAgent({
    pfx: options.pfx,
    passphrase: options.passphrase,
    cert: options.cert,
    key: options.key,
    ca: options.ca,
    keepAlive: true,
  });
  const fetchWithCertificate = (input: string | URL | Request, init?: RequestInit): Promise<Response> =>
    send(agent, input, init);
  return fetchWithCertificate as typeof fetch;
}

async function send(agent: HttpsAgent, input: string | URL | Request, init?: RequestInit): Promise<Response> {
  const url = new URL(typeof input === 'string' || input instanceof URL ? input : input.url);
  const method = init?.method ?? (input instanceof Request ? input.method : 'GET');
  const headers = new Headers(init?.headers ?? (input instanceof Request ? input.headers : undefined));
  const body = await bodyOf(init?.body);
  if (body !== undefined && !headers.has('content-length')) {
    headers.set('content-length', String(body.byteLength));
  }
  const signal = init?.signal ?? undefined;
  const secure = url.protocol === 'https:';

  return new Promise<Response>((resolve, reject) => {
    if (signal?.aborted) {
      reject(abortReason(signal));
      return;
    }
    const request = (secure ? httpsRequest : httpRequest)(url, {
      method,
      headers: Object.fromEntries(headers.entries()),
      ...(secure ? { agent } : {}),
    }, (response: IncomingMessage) => {
      const status = response.statusCode ?? 0;
      const responseHeaders = new Headers();
      for (const [name, value] of Object.entries(response.headers)) {
        if (value === undefined) continue;
        for (const one of Array.isArray(value) ? value : [value]) responseHeaders.append(name, one);
      }
      const stream = NO_BODY.has(status) ? null
        : (Readable.toWeb(response) as unknown as ReadableStream<Uint8Array>);
      if (NO_BODY.has(status)) response.resume();
      resolve(new Response(stream, { status, statusText: response.statusMessage, headers: responseHeaders }));
    });
    const onAbort = () => request.destroy(abortReason(signal!));
    signal?.addEventListener('abort', onAbort, { once: true });
    request.on('error', error => reject(error));
    request.on('close', () => signal?.removeEventListener('abort', onAbort));
    if (body !== undefined) request.write(body);
    request.end();
  });
}

async function bodyOf(body: BodyInit | null | undefined): Promise<Buffer | undefined> {
  if (body === undefined || body === null) return undefined;
  if (typeof body === 'string') return Buffer.from(body, 'utf8');
  if (body instanceof Uint8Array) return Buffer.from(body);
  if (body instanceof ArrayBuffer) return Buffer.from(new Uint8Array(body));
  // Blob, URLSearchParams, FormData, streams: let the platform encode them.
  return Buffer.from(await new Response(body).arrayBuffer());
}

function abortReason(signal: AbortSignal): Error {
  const reason = signal.reason;
  if (reason instanceof Error) return reason;
  const error = new Error('This operation was aborted');
  error.name = 'AbortError';
  return error;
}
