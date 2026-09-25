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

import type { Diagnostic, MqttFlavor, RestFlavor, ServiceFlavor, ServiceInterface } from '@ddsr/model';
import type { BindingDTO, ClientRuntime, ClientRuntimeDTO, PublishedDTO } from '../api/client-runtime';
import type { DdsrConsumerImpl } from './consumer-impl';
import type { DdsrProviderImpl } from './provider-impl';
import type { ServiceListenerRegistry } from './service-listener-registry';
import { eClassName, toArray } from './emf-util';

/**
 * What this client holds, as data (#167) — the twin of the Java
 * `ClientRuntimeImpl`, and deliberately built the same way so that the
 * two report the same thing under the same names.
 *
 * <p>The change count is detected rather than reported, for the reason
 * Java gives: a client's state changes in many places — a locator
 * rebinds inside event handling, a stream drops on its own — and a
 * counter bumped at every one of them is a counter that misses one. A
 * fingerprint of what a snapshot would say, compared against the last
 * one seen, cannot.
 */
export class ClientRuntimeImpl implements ClientRuntime {
  private lastFingerprint: string;
  private changes = 0;

  constructor(
    private readonly provider: DdsrProviderImpl,
    private readonly consumer: DdsrConsumerImpl,
    private readonly listeners: ServiceListenerRegistry,
  ) {
    this.lastFingerprint = this.fingerprint();
  }

  snapshot(): ClientRuntimeDTO {
    return {
      consumerId: this.consumer.identity(),
      changeCount: this.changeCount(),
      takenAt: Date.now(),
      supportedFlavors: this.consumer.flavorsSpoken(),
      published: this.published(),
      bindings: this.bindings(),
      eventStreamConnected: this.listeners.isStreamConnected(),
      eventTransport: this.listeners.eventTransport(),
    };
  }

  /** How often this runtime has changed: a number that only ever grows. */
  changeCount(): number {
    const current = this.fingerprint();
    if (current !== this.lastFingerprint) {
      this.lastFingerprint = current;
      this.changes++;
    }
    return this.changes;
  }

  private fingerprint(): string {
    const parts: string[] = [String(this.listeners.isStreamConnected())];
    for (const registration of this.provider.published()) {
      parts.push(`p:${registration.reference.id ?? ''}`);
    }
    for (const locator of this.consumer.tracked()) {
      parts.push(`b:${locator.reference.id ?? ''}:${locator.state}`);
    }
    return parts.join('|');
  }

  private published(): PublishedDTO[] {
    return this.provider.published().map(registration => {
      const implementation = registration.implementation;
      const referenceId = registration.reference.id || undefined;
      return {
        implementationId: implementation.implementationId,
        version: implementation.version,
        contracts: toArray<ServiceInterface>(implementation.serviceInterfaces)
          .map(contract => contract?.name)
          .filter((name): name is string => !!name),
        referenceId,
        // Live means the broker is holding it, which is what having a
        // reference means; a withdrawn one has left this list already.
        live: !!referenceId && !registration.isWithdrawn(),
        failure: failureOf(registration.diagnostic()),
      };
    });
  }

  private bindings(): BindingDTO[] {
    return this.consumer.tracked().map(locator => ({
      contract: locator.interfaceName,
      filter: locator.filter,
      referenceId: locator.reference.id || undefined,
      endpoint: addressOf(locator.flavors()),
      state: locator.state,
    }));
  }
}

/** Only the message: a watcher has a string-shaped hole to put it in. */
function failureOf(diagnostic: Diagnostic | undefined): string | undefined {
  if (!diagnostic || !diagnostic.severity || diagnostic.severity === 'OK') {
    return undefined;
  }
  return `${diagnostic.severity}: ${diagnostic.message ?? ''}`;
}

/** Where a binding calls, as its flavor states it — the same form Java reports. */
function addressOf(flavors: ServiceFlavor[]): string | undefined {
  for (const flavor of flavors) {
    // The kind as the model says it, or the class for a flavor decoded without one.
    const kind: string | undefined = flavor.kind ?? eClassName(flavor);
    if (kind === 'REST' || kind === 'RestFlavor') {
      const rest = flavor as RestFlavor;
      return (rest.host ?? '') + (rest.basePath ?? '');
    }
    if (kind === 'MQTT' || kind === 'MqttFlavor') {
      const mqtt = flavor as MqttFlavor;
      const brokers = toArray<string>(mqtt.brokers);
      if (brokers.length > 0) return `${brokers[0]} ${mqtt.requestTopic}`;
    }
  }
  return undefined;
}
