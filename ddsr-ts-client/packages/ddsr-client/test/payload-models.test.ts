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
import { DDSRFactory } from '@ddsr/model';
import { BrokerHttp, ensureModels, knowsModel, pinModels } from '../src/index';
import type { ModelBroker } from '../src/index';
import { deserializeFromXmi } from '../src/xmi/xmi-support';

const ecore = (nsURI: string) => `<?xml version="1.0" encoding="UTF-8"?>
<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="readings" nsURI="${nsURI}" nsPrefix="readings">
  <eClassifiers xsi:type="ecore:EClass" name="Reading">
    <eStructuralFeatures xsi:type="ecore:EAttribute" name="value" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EDouble"/>
  </eClassifiers>
</ecore:EPackage>`;

const contract = () => {
  const si = DDSRFactory.eINSTANCE.createServiceInterface();
  si.name = 'Readings';
  si.version = '1.0.0';
  return si;
};

/** A broker that keeps what it is handed, under a fingerprint of its own making. */
class FakeModelBroker implements ModelBroker {
  readonly kept = new Map<string, string>();
  constructor(private readonly takesModels = true) {}
  async handInModel(document: string): Promise<string | undefined> {
    if (!this.takesModels) return undefined;
    const nsURI = /nsURI="([^"]+)"/.exec(document)![1];
    this.kept.set(nsURI, document);
    return `fp1:${document.length}`;
  }
  async fetchModel(nsURI: string): Promise<string | undefined> {
    return this.kept.get(nsURI);
  }
}

// #256: payload models, handed in by a provider and fetched by a consumer.
describe('payload models', () => {
  it('a provider pins each model with the fingerprint the broker computed', async () => {
    const broker = new FakeModelBroker();
    const si = contract();
    const document = ecore('http://example.org/readings/pin/1.0');

    await pinModels(si, [document], broker);

    const models = [...(si.models as unknown as Iterable<{ nsURI: string; fingerprint: string }>)];
    expect(models.map(m => [m.nsURI, m.fingerprint])).toEqual([['http://example.org/readings/pin/1.0', `fp1:${document.length}`]]);
  });

  it('a broker that takes no models leaves the contract as it was', async () => {
    const si = contract();
    await pinModels(si, [ecore('http://example.org/readings/none/1.0')], new FakeModelBroker(false));
    expect([...(si.models as unknown as Iterable<unknown>)]).toEqual([]);
  });

  it('a consumer fetches a model it does not know, and then reads payloads of it', async () => {
    const nsURI = 'http://example.org/readings/fetched/1.0';
    const broker = new FakeModelBroker();
    broker.kept.set(nsURI, ecore(nsURI));
    const si = contract();
    const pinned = DDSRFactory.eINSTANCE.createModelReference();
    pinned.nsURI = nsURI;
    pinned.fingerprint = 'fp1:any';
    si.models.push(pinned);
    expect(knowsModel(nsURI)).toBe(false);

    expect(await ensureModels(si, broker)).toEqual([nsURI]);

    expect(knowsModel(nsURI)).toBe(true);
    const reading = deserializeFromXmi(`<?xml version="1.0" encoding="UTF-8"?>
<readings:Reading xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:readings="${nsURI}" value="4.5"/>`) as any;
    expect(reading.eClass().getName()).toBe('Reading');
    expect(await ensureModels(si, broker)).toEqual([]);
  });

  it('a model the broker does not have is said, by name', async () => {
    const si = contract();
    const pinned = DDSRFactory.eINSTANCE.createModelReference();
    pinned.nsURI = 'http://example.org/readings/missing/1.0';
    pinned.fingerprint = 'fp1:gone';
    si.models.push(pinned);

    await expect(ensureModels(si, new FakeModelBroker())).rejects.toThrow(/no payload model .*missing.* at fp1:gone/);
  });

  it('BrokerHttp hands in at PUT /models and fetches at GET /models', async () => {
    const calls: string[] = [];
    const fetchFn = (async (url: string, init?: RequestInit) => {
      calls.push(`${init?.method ?? 'GET'} ${url}`);
      if ((init?.method ?? 'GET') === 'PUT') return new Response('fp1:abc', { status: 201 });
      return url.includes('missing') ? new Response('no', { status: 404 }) : new Response('<ecore/>', { status: 200 });
    }) as unknown as typeof fetch;
    const broker = new BrokerHttp({ brokerUrl: 'http://broker.test/ddsr/rest', fetchFn });

    expect(await broker.handInModel('<ecore/>')).toBe('fp1:abc');
    expect(await broker.fetchModel('http://example.org/r/1.0', 'fp1:abc')).toBe('<ecore/>');
    expect(await broker.fetchModel('http://example.org/missing/1.0')).toBeUndefined();
    expect(calls[0]).toBe('PUT http://broker.test/ddsr/rest/models');
    expect(calls[1]).toContain('GET http://broker.test/ddsr/rest/models?nsURI=http%3A%2F%2Fexample.org%2Fr%2F1.0&fingerprint=fp1%3Aabc');
  });

  it('BrokerHttp says undefined when the broker takes no models', async () => {
    const fetchFn = (async () => new Response('', { status: 404 })) as unknown as typeof fetch;
    expect(await new BrokerHttp({ brokerUrl: 'http://broker.test', fetchFn }).handInModel('<ecore/>')).toBeUndefined();
  });
});
