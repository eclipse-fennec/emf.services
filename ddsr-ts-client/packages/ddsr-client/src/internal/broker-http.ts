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

import type { EObject } from '@emfts/core';
import type { Diagnostic, ServiceInterface, ServiceProvider } from '@ddsr/model';
import { DDSRFactory } from '@ddsr/model';
import { serializeToXmi, deserializeFromXmi } from '../xmi/xmi-support';
import { asRoots, firstOfClass } from './emf-util';

export interface BrokerHttpOptions {
  /** Broker base URL, e.g. http://localhost:8887/ddsr/rest */
  brokerUrl: string;
  /** X-DDSR-Requestor value for catalog operations (audit). */
  requestor?: string;
  fetchFn?: typeof fetch;
}

const REQUESTOR_HEADER = 'X-DDSR-Requestor';
const SOURCE = 'org.gecko.ddsr.client.ts';

/**
 * Direct HTTP proxies for the broker REST API — the TS mirror of the
 * Java client's CatalogHttpProxy / ImplementationsHttpProxy / lookup
 * transport. The broker API is infrastructure, not a discovered
 * service, so these calls do NOT go through the FlavorPlugin pipeline.
 *
 * Wire details this class owns:
 * - publish/withdraw bodies are multi-root XMI: the ServiceProvider
 *   plus its referenced ServiceInterface stubs as sibling roots, so
 *   positional cross-references resolve on the broker.
 * - withdraw is POST /implementations/withdraw with that same body
 *   (the body-carrying DELETE variant is deprecated, D14).
 * - Diagnostics are read from the body regardless of HTTP status; a
 *   missing/unparseable body yields a synthesized ERROR diagnostic
 *   with code = HTTP status.
 * - EMF omits default-valued attributes, so an OK diagnostic arrives
 *   without severity/code — absent means OK/0, never "unknown".
 */
export class BrokerHttp {
  private readonly base: string;
  private readonly requestor: string;
  private readonly fetchFn: typeof fetch;

  constructor(options: BrokerHttpOptions) {
    this.base = options.brokerUrl.replace(/\/+$/, '');
    this.requestor = options.requestor ?? 'anonymous';
    this.fetchFn = options.fetchFn ?? globalThis.fetch.bind(globalThis);
  }

  get brokerUrl(): string {
    return this.base;
  }

  /** POST /implementations — provider plus interface stubs as siblings. */
  async publishImplementation(provider: ServiceProvider, interfaceStubs: ServiceInterface[]): Promise<Diagnostic> {
    return this.sendImplementation('/implementations', provider, interfaceStubs);
  }

  /**
   * POST /implementations/withdraw — same body shape as publish. The
   * canonical withdraw is a POST, not the body-carrying DELETE (many
   * HTTP stacks refuse a DELETE entity; see DECISIONS_PARITY D14).
   */
  async withdrawImplementation(provider: ServiceProvider, interfaceStubs: ServiceInterface[]): Promise<Diagnostic> {
    return this.sendImplementation('/implementations/withdraw', provider, interfaceStubs);
  }

  /** GET /references — returns all roots of the multi-root response. */
  async getReferences(
    interfaceName: string,
    filter?: string,
    flavors?: string,
    consumerId?: string
  ): Promise<unknown[]> {
    const params = new URLSearchParams({ interface: interfaceName });
    if (filter) params.set('filter', filter);
    if (flavors) params.set('flavors', flavors);
    if (consumerId) params.set('consumerId', consumerId);
    const response = await this.fetchFn(`${this.base}/references?${params}`, {
      headers: { Accept: 'application/xml' },
    });
    if (!response.ok) {
      const body = await response.text();
      throw new Error(`lookup failed: ${response.status} — ${body.slice(0, 300)}`);
    }
    const text = await response.text();
    if (!text.trim()) return [];
    return asRoots(deserializeFromXmi(text));
  }

  /** POST /catalog with X-DDSR-Requestor. */
  async addCatalogEntry(serviceInterface: ServiceInterface, requestor?: string): Promise<Diagnostic> {
    const response = await this.fetchFn(`${this.base}/catalog`, {
      method: 'POST',
      headers: this.xmlHeaders(requestor),
      body: serializeToXmi(serviceInterface as unknown as EObject),
    });
    return this.readDiagnostic(response);
  }

  /** PUT /catalog/{name}/deprecate — optional body with reason/replacedBy. */
  async deprecateCatalogEntry(name: string, details?: ServiceInterface, requestor?: string): Promise<Diagnostic> {
    const response = await this.fetchFn(`${this.base}/catalog/${encodeURIComponent(name)}/deprecate`, {
      method: 'PUT',
      headers: this.xmlHeaders(requestor),
      body: details ? serializeToXmi(details as unknown as EObject) : undefined,
    });
    return this.readDiagnostic(response);
  }

  /** DELETE /catalog/{name}. */
  async removeCatalogEntry(name: string, requestor?: string): Promise<Diagnostic> {
    const response = await this.fetchFn(`${this.base}/catalog/${encodeURIComponent(name)}`, {
      method: 'DELETE',
      headers: { Accept: 'application/xml', [REQUESTOR_HEADER]: requestor ?? this.requestor },
    });
    return this.readDiagnostic(response);
  }

  /** GET /catalog/{name} — undefined on 404 (plain-text body, no Diagnostic). */
  async getCatalogEntry(name: string): Promise<ServiceInterface | undefined> {
    const response = await this.fetchFn(`${this.base}/catalog/${encodeURIComponent(name)}`, {
      headers: { Accept: 'application/xml' },
    });
    if (response.status === 404) return undefined;
    if (!response.ok) {
      const body = await response.text();
      throw new Error(`catalog read failed: ${response.status} — ${body.slice(0, 300)}`);
    }
    const text = await response.text();
    if (!text.trim()) return undefined;
    return firstOfClass<ServiceInterface>(asRoots(deserializeFromXmi(text)), 'ServiceInterface');
  }

  private async sendImplementation(
    path: '/implementations' | '/implementations/withdraw',
    provider: ServiceProvider,
    interfaceStubs: ServiceInterface[]
  ): Promise<Diagnostic> {
    const body = serializeToXmi(
      provider as unknown as EObject,
      ...(interfaceStubs as unknown as EObject[])
    );
    const response = await this.fetchFn(`${this.base}${path}`, {
      method: 'POST',
      headers: this.xmlHeaders(),
      body,
    });
    return this.readDiagnostic(response);
  }

  private xmlHeaders(requestor?: string): Record<string, string> {
    return {
      'Content-Type': 'application/xml',
      Accept: 'application/xml',
      [REQUESTOR_HEADER]: requestor ?? this.requestor,
    };
  }

  /**
   * Read a Diagnostic from any response, mirroring the Java client's
   * CatalogHttpProxy.readDiagnostic: the entity is read regardless of
   * status (4xx-with-Diagnostic works); a missing or unparseable body
   * becomes a synthesized ERROR diagnostic with code = HTTP status.
   */
  private async readDiagnostic(response: Response): Promise<Diagnostic> {
    const text = await response.text().catch(() => '');
    if (text.trim()) {
      try {
        const diagnostic = firstOfClass<Diagnostic>(asRoots(deserializeFromXmi(text)), 'Diagnostic');
        if (diagnostic) return normalizeDiagnostic(diagnostic);
      } catch {
        // fall through to the synthesized diagnostic
      }
    }
    const synthesized = DDSRFactory.eINSTANCE.createDiagnostic();
    synthesized.severity = response.ok ? 'OK' : 'ERROR';
    synthesized.code = response.ok ? 0 : response.status;
    synthesized.message = text.trim() || `HTTP ${response.status}`;
    synthesized.source = SOURCE;
    return synthesized;
  }
}

/**
 * EMF omits attributes at their default value, so a success diagnostic
 * arrives as bare `<ddsr:Diagnostic source="…"/>`. Absent severity/code
 * mean OK/0 by the model's defaults.
 */
export function normalizeDiagnostic(diagnostic: Diagnostic): Diagnostic {
  if (!diagnostic.severity) diagnostic.severity = 'OK';
  // The emf.ts reader currently yields attribute values as strings, so
  // coerce numerics defensively (finding recorded in DECISIONS_PARITY).
  const code = diagnostic.code as unknown;
  if (code === undefined || code === null || code === '') {
    diagnostic.code = 0;
  } else if (typeof code !== 'number') {
    const parsed = Number(code);
    diagnostic.code = Number.isNaN(parsed) ? 0 : parsed;
  }
  return diagnostic;
}

/** True when the diagnostic blocks the operation (ERROR or CANCEL). */
export function isError(diagnostic: Diagnostic | undefined): boolean {
  const severity = diagnostic?.severity;
  return severity === 'ERROR' || severity === 'CANCEL';
}
