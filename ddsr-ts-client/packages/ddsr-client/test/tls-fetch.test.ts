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

import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { tlsFetch } from '../src/tls/tls-fetch.js';

/**
 * The fetch the SDK's seams are handed when a node shows a certificate
 * (SECURITY.md §7d). What it does with TLS options is node:https's; what
 * is tested here is that it is a fetch the SDK can live with — status,
 * headers and bodies both ways, a body-less 204, a stream read as it
 * arrives (the event stream), and an abort that ends the call.
 */
describe('tlsFetch', () => {
  let server: Server;
  let base: string;
  const fetchFn = tlsFetch({});

  beforeAll(async () => {
    server = createServer((request, response) => {
      if (request.url === '/echo') {
        const chunks: Buffer[] = [];
        request.on('data', c => chunks.push(c as Buffer));
        request.on('end', () => {
          response.writeHead(201, { 'Content-Type': 'text/plain', 'X-Seen': String(request.headers['x-currency']) });
          response.end(`${request.method} ${Buffer.concat(chunks).toString('utf8')}`);
        });
      } else if (request.url === '/empty') {
        response.writeHead(204);
        response.end();
      } else if (request.url === '/stream') {
        response.writeHead(200, { 'Content-Type': 'text/event-stream' });
        response.write('data: one\n\n');
        setTimeout(() => response.end('data: two\n\n'), 50);
      } else if (request.url === '/slow') {
        setTimeout(() => response.end('late'), 2000);
      } else {
        response.writeHead(404);
        response.end('nothing here');
      }
    });
    await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
    base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
  });

  afterAll(async () => {
    server.closeAllConnections();
    await new Promise<void>(resolve => server.close(() => resolve()));
  });

  it('sends a body and headers, and gives back status, headers and body', async () => {
    const response = await fetchFn(`${base}/echo`, {
      method: 'POST', body: 'amount=10', headers: { 'X-Currency': 'EUR' },
    });

    expect(response.status).toBe(201);
    expect(response.headers.get('x-seen')).toBe('EUR');
    expect(await response.text()).toBe('POST amount=10');
  });

  it('answers a 204 without a body', async () => {
    const response = await fetchFn(`${base}/empty`, { method: 'DELETE' });

    expect(response.status).toBe(204);
    expect(response.body).toBeNull();
  });

  it('passes an error status on rather than throwing', async () => {
    const response = await fetchFn(`${base}/missing`);

    expect(response.ok).toBe(false);
    expect(response.status).toBe(404);
    expect(await response.text()).toBe('nothing here');
  });

  it('streams, so the event stream sees an event before the next one is sent', async () => {
    const response = await fetchFn(`${base}/stream`);
    const reader = response.body!.getReader();

    const first = await reader.read();
    expect(new TextDecoder().decode(first.value)).toBe('data: one\n\n');
    let rest = '';
    for (let chunk = await reader.read(); !chunk.done; chunk = await reader.read()) {
      rest += new TextDecoder().decode(chunk.value);
    }
    expect(rest).toBe('data: two\n\n');
  });

  it('ends the call when its signal aborts', async () => {
    await expect(fetchFn(`${base}/slow`, { signal: AbortSignal.timeout(100) })).rejects.toThrow();
  });
});
