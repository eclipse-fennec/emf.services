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

export type { FlavorPlugin } from './api/flavor-plugin';
export type { DdsrClient } from './api/ddsr-client';
export type { DdsrProvider } from './api/ddsr-provider';
export type { DdsrConsumer } from './api/ddsr-consumer';
export type { DdsrCatalog } from './api/ddsr-catalog';
export type { ServiceLocator } from './api/service-locator';
export type { Registration } from './api/registration';
export type { DdsrServiceListener } from './api/service-listener';
export { DiagnosticCode } from './api/diagnostics';
export { DdsrClientError } from './api/errors';
export { DdsrClientImpl } from './internal/ddsr-client-impl';
export type { DdsrClientOptions } from './internal/ddsr-client-impl';
export { ServiceLocatorImpl } from './proxy/service-locator-impl';
export { BrokerHttp, normalizeDiagnostic, isError } from './internal/broker-http';
export type { BrokerHttpOptions } from './internal/broker-http';
export { ServiceListenerRegistry } from './internal/service-listener-registry';
export type { DdsrEventSource, EventSourceHandler, EventSubscription } from './events/event-source';
export { RestEventSource } from './events/rest-event-source';
export type { RestEventSourceOptions } from './events/rest-event-source';
export { SseParser } from './events/sse-parser';
export { attachShutdownHooks } from './lifecycle/shutdown-hooks';
export type { ShutdownHookOptions } from './lifecycle/shutdown-hooks';
export { serializeToXmi, deserializeFromXmi } from './xmi/xmi-support';
export { propertyValue, propertiesOf, propertyOf, props, FINGERPRINT_PROPERTY } from './properties';
export { fingerprint, canonicalForm, FINGERPRINT_SCHEME } from './fingerprint/service-description-fingerprint';
export type { PropertyValue } from './properties';
export { eClassName, toArray, asRoots, firstOfClass } from './internal/emf-util';
