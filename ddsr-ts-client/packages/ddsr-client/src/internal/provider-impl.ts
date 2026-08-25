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

import type {
  Diagnostic,
  LocalServiceRegistry,
  ServiceImplementation,
  ServiceInterface,
  ServiceProvider,
  ServiceReference,
} from '@ddsr/model';
import { DDSRFactory } from '@ddsr/model';
import type { DdsrProvider } from '../api/ddsr-provider';
import type { Registration } from '../api/registration';
import { DdsrClientError } from '../api/errors';
import { BrokerHttp, isError } from './broker-http';
import { eClassName, toArray } from './emf-util';
import { fingerprint as sd1 } from '../fingerprint/service-description-fingerprint';
import { implementationFingerprint } from '../fingerprint/service-implementation-fingerprint';
import { propertyValue } from '../properties';

/**
 * DdsrProvider implementation over the broker REST API — the TS mirror
 * of the Java client's ProviderImpl:
 *
 * - publish() sends the ServiceProvider with its ServiceInterface stubs
 *   as multi-root XMI (the broker requires exactly one provider root
 *   with exactly one implementation), then does a second roundtrip to
 *   discover the broker-assigned ServiceReference (the publish response
 *   is only a Diagnostic).
 * - withdraw() is idempotent and blocks until the broker has confirmed
 *   the unregistration — i.e. until consumers were informed (FR-P3).
 */
export class DdsrProviderImpl implements DdsrProvider {
  private readonly broker: BrokerHttp;
  private readonly registrations = new Map<string, RegistrationImpl>();

  constructor(broker: BrokerHttp) {
    this.broker = broker;
  }

  async publish(
    provider: ServiceProvider,
    implementation: ServiceImplementation
  ): Promise<Registration> {
    const implementations = toArray<ServiceImplementation>(provider.implementations);
    if (!implementations.includes(implementation)) {
      throw new DdsrClientError(
        'implementation must be contained in provider.implementations'
      );
    }
    if (implementations.length !== 1) {
      throw new DdsrClientError(
        `the broker accepts exactly one implementation per publish, got ${implementations.length}`
      );
    }
    const stubs = interfaceStubs(implementation);
    if (stubs.length === 0) {
      throw new DdsrClientError('implementation references no ServiceInterface');
    }

    // Idempotent reconnect (ACQUISITION.md §11.1): if the broker still
    // holds an identical registration — im1 match — reuse it instead of
    // re-publishing. Consumers then see no UNREGISTERING/REGISTERED
    // churn for a provider that merely restarted.
    const held = await this.brokerHeldReference(provider, implementation, stubs[0].name);
    if (held) {
      console.info(
        `[ddsr] publish skipped for '${implementation.implementationId}': ` +
        `broker already holds an identical registration (im1 match), reusing ${held.id}`
      );
      const reused = new RegistrationImpl(
        this.broker, provider, implementation, stubs, held, synthAlreadyPublished());
      this.registrations.set(held.id ?? '', reused);
      return reused;
    }

    const diagnostic = await this.broker.publishImplementation(provider, stubs);
    if (isError(diagnostic)) {
      throw new DdsrClientError(
        `publish failed: [${diagnostic.code}] ${diagnostic.message ?? 'unknown error'}`,
        diagnostic
      );
    }
    if (diagnostic.severity === 'WARNING') {
      console.warn(`[ddsr] publish warning: ${diagnostic.message}`);
    }

    const reference =
      (await this.discoverReference(provider, implementation, stubs[0].name))
      ?? pendingReference(implementation);

    const registration = new RegistrationImpl(this.broker, provider, implementation, stubs, reference, diagnostic);
    this.registrations.set(reference.id ?? '', registration);
    return registration;
  }

  registrationOf(referenceId: string): Registration | undefined {
    return this.registrations.get(referenceId);
  }

  /**
   * Withdraw every live registration, awaiting the broker's
   * confirmations. Part of the client shutdown path (D2/FR-P3).
   */
  async withdrawAll(): Promise<void> {
    const open = [...this.registrations.values()].filter(r => !r.isWithdrawn());
    const failures: string[] = [];
    for (const registration of open) {
      try {
        await registration.withdraw();
      } catch (error) {
        failures.push(String(error));
      }
    }
    if (failures.length > 0) {
      throw new DdsrClientError(`withdraw on close failed: ${failures.join('; ')}`);
    }
  }

  /**
   * The three-valued reconnect check (TS mirror of the Java client's
   * brokerHeldReference): a reference the broker already holds counts
   * as OURS, unchanged, exactly when its provider name matches and its
   * `ddsr.impl.fingerprint` decoration equals the locally computed im1
   * — im1 composes implementationId, endpoints and the sd1 contract
   * tokens, so an exact match can only be this implementation. Any
   * drift returns undefined (publish; the broker retires the old
   * entry), with the drift direction logged: sd1 equal → endpoint/
   * property drift, sd1 different → contract drift (warning).
   */
  private async brokerHeldReference(
    provider: ServiceProvider,
    implementation: ServiceImplementation,
    interfaceName: string | undefined
  ): Promise<ServiceReference | undefined> {
    if (!interfaceName || !provider.name) return undefined;
    const localIm1 = implementationFingerprint(implementation);
    let drifted: ServiceReference | undefined;
    try {
      for (const root of await this.broker.getReferences(interfaceName)) {
        if (eClassName(root) !== 'LocalServiceRegistry') continue;
        const registry = root as LocalServiceRegistry;
        for (const reference of toArray<ServiceReference>(registry.references)) {
          if (reference.provider?.name !== provider.name) continue;
          if (stringProperty(reference, 'ddsr.impl.fingerprint') === localIm1) {
            return reference;
          }
          drifted ??= reference;
        }
      }
    } catch {
      // unreachable broker — publish normally, the transport reports it
      return undefined;
    }
    if (drifted) {
      if (sd1Match(drifted, implementation)) {
        console.info(
          `[ddsr] re-publishing '${implementation.implementationId}': broker holds ` +
          `${drifted.id} with same contract (sd1) but drifted endpoint/properties (im1)`
        );
      } else {
        console.warn(
          `[ddsr] re-publishing '${implementation.implementationId}': broker holds ` +
          `${drifted.id} with a DIFFERENT contract (sd1 drift) — catalog and local model disagree`
        );
      }
    }
    return undefined;
  }

  /**
   * The publish response carries no reference id, so mirror the Java
   * client: look the interface up and match by implementationId (and,
   * as fallback, by provider name).
   */
  private async discoverReference(
    provider: ServiceProvider,
    implementation: ServiceImplementation,
    interfaceName: string | undefined
  ): Promise<ServiceReference | undefined> {
    if (!interfaceName) return undefined;
    try {
      const roots = await this.broker.getReferences(interfaceName);
      for (const root of roots) {
        if (eClassName(root) !== 'LocalServiceRegistry') continue;
        const registry = root as LocalServiceRegistry;
        const references = toArray<ServiceReference>(registry.references);
        for (const remoteProvider of toArray<ServiceProvider>(registry.providers)) {
          for (const impl of toArray<ServiceImplementation>(remoteProvider.implementations)) {
            const idMatch = implementation.implementationId
              && impl.implementationId === implementation.implementationId;
            const nameMatch = remoteProvider.name === provider.name;
            if (!idMatch && !nameMatch) continue;
            const reference = references.find(
              r => r.provider === remoteProvider || r.provider?.name === remoteProvider.name
            );
            if (reference?.id) return reference;
          }
        }
      }
    } catch (error) {
      console.error(`[ddsr] reference discovery after publish failed: ${String(error)}`);
    }
    return undefined;
  }
}

function interfaceStubs(implementation: ServiceImplementation): ServiceInterface[] {
  return toArray<ServiceInterface>(implementation.serviceInterfaces);
}

function stringProperty(reference: ServiceReference, name: string): string | undefined {
  for (const property of toArray<{ name?: string }>(reference.properties)) {
    if (property.name === name && eClassName(property) === 'StringProperty') {
      const value = propertyValue(property as never);
      return value === undefined ? undefined : String(value);
    }
  }
  return undefined;
}

/** All local sd1 values equal the broker's decoration on the reference. */
function sd1Match(reference: ServiceReference, implementation: ServiceImplementation): boolean {
  const interfaces = toArray<ServiceInterface>(implementation.serviceInterfaces);
  return interfaces.every(si => {
    const key = interfaces.length === 1 ? 'ddsr.fingerprint' : `ddsr.fingerprint.${si.name}`;
    return stringProperty(reference, key) === sd1(si);
  });
}

function pendingReference(implementation: ServiceImplementation): ServiceReference {
  const reference = DDSRFactory.eINSTANCE.createServiceReference();
  reference.id = `pending:${implementation.name}`;
  return reference;
}

class RegistrationImpl implements Registration {
  private readonly broker: BrokerHttp;
  private readonly provider: ServiceProvider;
  readonly implementation: ServiceImplementation;
  private readonly stubs: ServiceInterface[];
  readonly reference: ServiceReference;
  private publishDiagnostic: Diagnostic;
  private withdrawn = false;
  private inFlight: Promise<Diagnostic> | undefined;

  constructor(
    broker: BrokerHttp,
    provider: ServiceProvider,
    implementation: ServiceImplementation,
    stubs: ServiceInterface[],
    reference: ServiceReference,
    publishDiagnostic: Diagnostic
  ) {
    this.broker = broker;
    this.provider = provider;
    this.implementation = implementation;
    this.stubs = stubs;
    this.reference = reference;
    this.publishDiagnostic = publishDiagnostic;
  }

  /** The diagnostic of the last broker interaction (publish/withdraw). */
  diagnostic(): Diagnostic {
    return this.publishDiagnostic;
  }

  isWithdrawn(): boolean {
    return this.withdrawn;
  }

  /**
   * Idempotent; resolves only after the broker confirmed the
   * unregistration — at that point the UNREGISTERING event has been
   * fanned out to consumers, so the caller may shut its endpoint down.
   */
  async withdraw(): Promise<Diagnostic> {
    if (this.withdrawn) return synthOk();
    if (this.inFlight) return this.inFlight;
    this.inFlight = (async () => {
      try {
        const diagnostic = await this.broker.withdrawImplementation(this.provider, this.stubs);
        if (!isError(diagnostic)) {
          this.withdrawn = true;
          this.publishDiagnostic = diagnostic;
        }
        return diagnostic;
      } finally {
        this.inFlight = undefined;
      }
    })();
    return this.inFlight;
  }
}

function synthAlreadyPublished(): Diagnostic {
  const diagnostic = DDSRFactory.eINSTANCE.createDiagnostic();
  diagnostic.severity = 'OK';
  diagnostic.code = 0;
  diagnostic.source = 'org.gecko.ddsr.client.ts';
  diagnostic.message = 'already published — broker holds an identical registration (im1 match)';
  return diagnostic;
}

function synthOk(): Diagnostic {
  const diagnostic = DDSRFactory.eINSTANCE.createDiagnostic();
  diagnostic.severity = 'OK';
  diagnostic.code = 0;
  diagnostic.source = 'org.gecko.ddsr.client.ts';
  diagnostic.message = 'already withdrawn';
  return diagnostic;
}
