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
import type { RestFlavor, RestOperationFlavor, ServiceOperation } from '@ddsr/model';
import { DdsrServiceError, DdsrTransportError } from '@ddsr/client';
import { RestFlavorPlugin } from '../src/rest-flavor-plugin';

function charge(): { operation: ServiceOperation; flavor: RestFlavor; opFlavor: RestOperationFlavor } {
  const f = DDSRFactory.eINSTANCE;
  const operation = f.createServiceOperation();
  operation.name = 'charge';
  const amount = f.createParameter();
  amount.name = 'amount';
  amount.type = 'double';
  operation.parameters.push(amount);
  const flavor = f.createRestFlavor();
  flavor.name = 'rest';
  flavor.host = 'http://provider.test:9091';
  flavor.basePath = '/payments';
  const opFlavor = f.createRestOperationFlavor();
  opFlavor.name = 'charge';
  opFlavor.method = 'POST';
  opFlavor.path = '/charge';
  opFlavor.operation = operation;
  flavor.operationFlavors.push(opFlavor);
  return { operation, flavor, opFlavor };
}

/** #59: a provider that does not answer is a DdsrTransportError, not a hang. */
describe('RestFlavorPlugin timeouts', () => {
  it('aborts a call that never answers after timeoutMillis', async () => {
    const neverAnswers = ((_url: any, init?: any) => new Promise<Response>((_resolve, reject) => {
      init.signal.addEventListener('abort', () => reject(init.signal.reason));
    })) as typeof fetch;
    const plugin = new RestFlavorPlugin({ fetchFn: neverAnswers, timeoutMillis: 30 });
    const { operation, flavor, opFlavor } = charge();

    const started = Date.now();
    await expect(plugin.invoke(operation, { amount: 1 }, flavor, opFlavor))
      .rejects.toSatisfy((error: unknown) => {
        expect(error).toBeInstanceOf(DdsrTransportError);
        expect((error as Error).message).toContain('timeout');
        return true;
      });
    expect(Date.now() - started).toBeLessThan(2000);
  });

  it('an unreachable peer (fetch rejects) is a transport error too', async () => {
    const refused = (async () => { throw new TypeError('fetch failed'); }) as unknown as typeof fetch;
    const plugin = new RestFlavorPlugin({ fetchFn: refused });
    const { operation, flavor, opFlavor } = charge();

    await expect(plugin.invoke(operation, { amount: 1 }, flavor, opFlavor))
      .rejects.toBeInstanceOf(DdsrTransportError);
  });

  it('an HTTP error answered by the provider is NOT a transport error (no rebind, no retry)', async () => {
    const serverError = (async () => new Response('boom', { status: 500 })) as unknown as typeof fetch;
    const plugin = new RestFlavorPlugin(serverError);
    const { operation, flavor, opFlavor } = charge();

    await expect(plugin.invoke(operation, { amount: 1 }, flavor, opFlavor))
      .rejects.toSatisfy((error: unknown) => {
        expect(error).not.toBeInstanceOf(DdsrTransportError);
        expect((error as Error).message).toContain('500');
        return true;
      });
  });

  it('a status the flavor binds to a declared error is that error', async () => {
    const f = DDSRFactory.eINSTANCE;
    const { operation, flavor, opFlavor } = charge();
    const declined = f.createServiceException();
    declined.name = 'Declined';
    declined.type = 'org.example.Declined';
    const binding = f.createRestExceptionBinding();
    binding.status = 402;
    binding.exception = declined;
    opFlavor.exceptionBindings.push(binding);
    const answers = (async () => new Response('card declined', { status: 402 })) as unknown as typeof fetch;
    const plugin = new RestFlavorPlugin(answers);

    await expect(plugin.invoke(operation, { amount: 1 }, flavor, opFlavor))
      .rejects.toSatisfy((error: unknown) => {
        expect(error).toBeInstanceOf(DdsrServiceError);
        expect(error).not.toBeInstanceOf(DdsrTransportError);
        expect((error as DdsrServiceError).exception.type).toBe('org.example.Declined');
        expect((error as DdsrServiceError).status).toBe(402);
        expect((error as Error).message).toBe('card declined');
        return true;
      });
  });

  it('an unbound status stays a plain error', async () => {
    const answers = (async () => new Response('nope', { status: 409 })) as unknown as typeof fetch;
    const plugin = new RestFlavorPlugin(answers);
    const { operation, flavor, opFlavor } = charge();

    await expect(plugin.invoke(operation, { amount: 1 }, flavor, opFlavor))
      .rejects.toSatisfy((error: unknown) => !(error instanceof DdsrServiceError));
  });

  it('timeoutMillis 0 disables the signal', async () => {
    let seenSignal: unknown = 'unset';
    const answers = (async (_url: any, init?: any) => {
      seenSignal = init?.signal;
      return new Response('<?xml version="1.0"?><x/>', { status: 200, headers: { 'Content-Type': 'application/xml' } });
    }) as typeof fetch;
    const plugin = new RestFlavorPlugin({ fetchFn: answers, timeoutMillis: 0 });
    const { operation, flavor, opFlavor } = charge();

    await plugin.invoke(operation, { amount: 1 }, flavor, opFlavor).catch(() => undefined);
    expect(seenSignal).toBeUndefined();
  });
});

/**
 * Binary mode (#101): the attributes ride as ce-* headers and the body
 * stays exactly the payload it was — which is why the envelope cost
 * nothing over HTTP and must keep costing nothing.
 */
describe('RestFlavorPlugin envelope', () => {
  function recordingFetch(): { fetchFn: typeof fetch; seen: () => Record<string, string> } {
    let headers: Record<string, string> = {};
    const fetchFn = (async (_url: any, init?: any) => {
      headers = { ...(init?.headers ?? {}) };
      return new Response('990.0', { status: 200, headers: { 'Content-Type': 'text/plain' } });
    }) as unknown as typeof fetch;
    return { fetchFn, seen: () => headers };
  }

  it('sends the call as a CloudEvent in binary mode', async () => {
    const { fetchFn, seen } = recordingFetch();
    const plugin = new RestFlavorPlugin({ fetchFn, originLabel: 'probe' });
    const { operation, flavor, opFlavor } = charge();

    await plugin.invoke(operation, { amount: 10 }, flavor, opFlavor);

    const headers = seen();
    expect(headers['ce-specversion']).toBe('1.0');
    expect(headers['ce-type']).toBe('org.eclipse.fennec.services.invoke');
    expect(headers['ce-source']).toBe('/consumer/probe');
    expect(headers['ce-subject']).toBe('charge');
    expect(headers['ce-id']).toBeTruthy();
    expect(headers['ce-datacontenttype'])
      .toBeUndefined();
    expect(headers.Accept)
      .toBe('application/xml');
  });

  it('lets a contract-bound header win over the envelope', async () => {
    const { fetchFn, seen } = recordingFetch();
    const plugin = new RestFlavorPlugin({ fetchFn });
    const f = DDSRFactory.eINSTANCE;
    const { operation, flavor, opFlavor } = charge();
    const binding = f.createRestParameterBinding();
    binding.parameter = operation.parameters[0];
    binding.binding = 'HEADER';
    binding.wireName = 'ce-subject';
    opFlavor.parameterBindings.push(binding);

    await plugin.invoke(operation, { amount: 7 }, flavor, opFlavor);

    expect(seen()['ce-subject'])
      .toBe('7');
  });

  it('notices an answer that correlates with a different call', async () => {
    const wrongAnswer = (async () => new Response('990.0', {
      status: 200,
      headers: { 'Content-Type': 'text/plain', 'ce-correlationid': 'somebody-elses-call' },
    })) as unknown as typeof fetch;
    const complaints: string[] = [];
    const plugin = new RestFlavorPlugin({ fetchFn: wrongAnswer, log: (m) => complaints.push(m) });
    const { operation, flavor, opFlavor } = charge();

    await plugin.invoke(operation, { amount: 1 }, flavor, opFlavor);

    expect(complaints).toHaveLength(1);
    expect(complaints[0]).toContain('somebody-elses-call');
  });
});

/**
 * #146: the call a cross-language trace hangs on. What this side
 * writes is what a Java provider reads back out — the format is W3C,
 * and neither end knows the other's language.
 */
describe('RestFlavorPlugin tracing', () => {
  function watching() {
    const state = { called: undefined as string | undefined, failure: undefined as string | undefined,
      attributes: {} as Record<string, string | undefined>, ended: false };
    const tracer = {
      calling(operation: string, outbound: { set(name: string, value: string): void }) {
        state.called = operation;
        outbound.set('traceparent', '00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01');
        const span: any = {
          attribute(name: string, value?: string) { state.attributes[name] = value; return span; },
          failed(error: unknown) { state.failure = String((error as Error)?.message ?? error); },
          end() { state.ended = true; },
        };
        return span;
      },
      serving() { throw new Error('a consumer calls, it does not serve'); },
    };
    return { state, tracer: tracer as any };
  }

  function recordingFetch(): { fetchFn: typeof fetch; seen: () => Record<string, string> } {
    let headers: Record<string, string> = {};
    const fetchFn = (async (_url: any, init?: any) => {
      headers = { ...(init?.headers ?? {}) };
      return new Response('990.0', { status: 200, headers: { 'Content-Type': 'text/plain' } });
    }) as unknown as typeof fetch;
    return { fetchFn, seen: () => headers };
  }

  it('writes the caller context beside the envelope, and names the contract operation', async () => {
    const { state, tracer } = watching();
    const { fetchFn, seen } = recordingFetch();
    const plugin = new RestFlavorPlugin({ fetchFn, tracer, originLabel: 'ts-consumer' });
    const { operation, flavor, opFlavor } = charge();
    const contract = DDSRFactory.eINSTANCE.createServiceInterface();
    contract.name = 'Payment';
    contract.operations.push(operation);

    await plugin.invoke(operation, { amount: 10 }, flavor, opFlavor);

    expect(seen().traceparent).toBe('00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01');
    expect(seen()['ce-type'])
      .toBe('org.eclipse.fennec.services.invoke');
    expect(state.called).toBe('Payment/charge');
    expect(state.attributes['http.response.status_code']).toBe('200');
    expect(state.ended).toBe(true);
  });

  it('marks a provider that does not answer', async () => {
    const { state, tracer } = watching();
    const refused = (async () => { throw new TypeError('fetch failed'); }) as unknown as typeof fetch;
    const plugin = new RestFlavorPlugin({ fetchFn: refused, tracer });
    const { operation, flavor, opFlavor } = charge();

    await expect(plugin.invoke(operation, { amount: 10 }, flavor, opFlavor))
      .rejects.toBeInstanceOf(DdsrTransportError);

    expect(state.failure).toBe('fetch failed');
    expect(state.ended).toBe(true);
  });
});

/**
 * A provider call carries a token of the provider's audience — never the
 * broker's (SECURITY.md §1, §6); the twin of the Java
 * RestServiceInvokerBearerTest.
 */
describe('RestFlavorPlugin bearer', () => {
  function guarded(): ReturnType<typeof charge> {
    const fixture = charge();
    const f = DDSRFactory.eINSTANCE;
    const requirement = f.createSecurityRequirement();
    requirement.authenticated = true;
    requirement.scopes.push('payment-writer');
    fixture.operation.security = requirement;
    const scheme = f.createBearerTokenScheme();
    scheme.issuer = 'http://localhost:18080/realms/fennec';
    scheme.audiences.push('payments');
    fixture.flavor.securitySchemes.push(scheme);
    return fixture;
  }

  function recording(): { fetchFn: typeof fetch; seen: () => Record<string, string> } {
    let headers: Record<string, string> = {};
    const fetchFn = (async (_url: any, init?: any) => {
      headers = { ...(init?.headers ?? {}) };
      return new Response('990.0', { status: 200, headers: { 'Content-Type': 'text/plain' } });
    }) as unknown as typeof fetch;
    return { fetchFn, seen: () => headers };
  }

  it('asks for a token of the audience the scheme names', async () => {
    const asked: (string | undefined)[] = [];
    const { fetchFn, seen } = recording();
    const plugin = new RestFlavorPlugin({
      fetchFn,
      tokenSupplier: async (audience?: string) => {
        asked.push(audience);
        return `token-for-${audience}`;
      },
    });
    const { operation, flavor, opFlavor } = guarded();

    await plugin.invoke(operation, { amount: 10 }, flavor, opFlavor);

    expect(asked).toEqual(['payments']);
    expect(seen().Authorization).toBe('Bearer token-for-payments');
  });

  it('calls a target without a scheme without a token', async () => {
    const { fetchFn, seen } = recording();
    const plugin = new RestFlavorPlugin({ fetchFn, tokenSupplier: async () => 'broker-token' });
    const { operation, flavor, opFlavor } = charge();

    await plugin.invoke(operation, { amount: 10 }, flavor, opFlavor);

    expect(seen().Authorization).toBeUndefined();
  });

  it('fails a guarded call without a supplier, saying why', async () => {
    const { fetchFn } = recording();
    const plugin = new RestFlavorPlugin({ fetchFn });
    const { operation, flavor, opFlavor } = guarded();

    await expect(plugin.invoke(operation, { amount: 10 }, flavor, opFlavor))
      .rejects.toThrow(/no token supplier/);
  });
});
