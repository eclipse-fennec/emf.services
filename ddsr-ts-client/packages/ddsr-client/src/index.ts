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
export type { ServiceLocator, TrackedServiceLocator, LocatorState } from './api/service-locator';
export type { Registration } from './api/registration';
export type { DdsrServiceListener } from './api/service-listener';
export { DiagnosticCode } from './api/diagnostics';
export { DdsrClientError, DdsrTransportError } from './api/errors';
export { DdsrClientImpl } from './internal/ddsr-client-impl';
export type { DdsrClientOptions } from './internal/ddsr-client-impl';
export { ServiceLocatorImpl } from './proxy/service-locator-impl';
export { BrokerHttp, normalizeDiagnostic, isError } from './internal/broker-http';
// One runtime, one identity: a caller that builds both a client and a
// flavor plugin needs the same origin token in both, and the token is
// this function's answer rather than something to reassemble by hand.
export { clientOrigin, ORIGIN_HEADER } from './internal/client-origin';
export type { ClientOrigin } from './internal/client-origin';
export type { BrokerHttpOptions } from './internal/broker-http';
export { ServiceListenerRegistry } from './internal/service-listener-registry';
export type { DdsrEventSource, EventSourceHandler, EventSubscription } from './events/event-source';
export { RestEventSource } from './events/rest-event-source';
export type { RestEventSourceOptions } from './events/rest-event-source';
export { SseParser } from './events/sse-parser';
export {
  newEnvelope, writeStructured, readStructured, toHeaders, fromHeaders, dataAsText,
  isTextual, lifecycleTypeOf, lifecycleEventTypeOf,
  CE_SPEC_VERSION, CE_STRUCTURED_MEDIA_TYPE, CE_HEADER_PREFIX,
  CE_TYPE_INVOKE, CE_TYPE_INVOKE_REPLY, CE_TYPE_RESYNC,
  CE_EXTENSION_CORRELATION_ID, CE_EXTENSION_REPLY_TO,
} from './cloudevents/cloud-events';
export type { CloudEventEnvelope, CloudEventMessage } from './cloudevents/cloud-events';
export { attachShutdownHooks } from './lifecycle/shutdown-hooks';
export type { ShutdownHookOptions } from './lifecycle/shutdown-hooks';
export { serializeToXmi, deserializeFromXmi } from './xmi/xmi-support';
export {
  encodeInvocation, decodeInvocation, encodeResult, encodeFailure, decodeResult,
  propertyFor, valueOf,
} from './invocation/invocation';
export type { DecodedInvocation, DecodedResult } from './invocation/invocation';
export { propertyValue, propertiesOf, propertyOf, props, FINGERPRINT_PROPERTY } from './properties';
export { fingerprint, canonicalForm, FINGERPRINT_SCHEME } from './fingerprint/service-description-fingerprint';
export { implementationFingerprint, implementationCanonicalForm, IMPL_FINGERPRINT_SCHEME } from './fingerprint/service-implementation-fingerprint';
export type { PropertyValue } from './properties';
export { eClassName, toArray, asRoots, firstOfClass } from './internal/emf-util';
