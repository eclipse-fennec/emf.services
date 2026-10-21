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

export type { FlavorPlugin } from './api/flavor-plugin.js';
export type { DdsrClient } from './api/ddsr-client.js';
export type { DdsrProvider } from './api/ddsr-provider.js';
export type { DdsrConsumer } from './api/ddsr-consumer.js';
export type { DdsrCatalog } from './api/ddsr-catalog.js';
export type { ClientRuntime, ClientRuntimeDTO, BindingDTO, PublishedDTO } from './api/client-runtime.js';
export type { ServiceLocator, TrackedServiceLocator, LocatorState } from './api/service-locator.js';
export type { Registration } from './api/registration.js';
export type { DdsrServiceListener } from './api/service-listener.js';
export { DiagnosticCode } from './api/diagnostics.js';
export { DdsrClientError, DdsrServiceError, DdsrTransportError } from './api/errors.js';
export { DdsrClientImpl } from './internal/ddsr-client-impl.js';
export type { DdsrClientOptions } from './internal/ddsr-client-impl.js';
export { ServiceLocatorImpl } from './proxy/service-locator-impl.js';
export { serviceProxy } from './proxy/service-proxy.js';
export type { ExceptionClasses } from './proxy/service-proxy.js';
export { BrokerHttp, normalizeDiagnostic, isError } from './internal/broker-http.js';
// One runtime, one identity: a caller that builds both a client and a
// flavor plugin needs the same origin token in both, and the token is
// this function's answer rather than something to reassemble by hand.
export { clientOrigin, ORIGIN_HEADER } from './internal/client-origin.js';
export { clientCredentials, withBearer, RENEW_BEFORE_SECONDS } from './internal/bearer-token.js';
export { effectiveRequirement, demandsAuthentication, bearerScheme } from './internal/security-requirements.js';
export type { TokenSupplier, ClientCredentialsOptions } from './internal/bearer-token.js';
export type { ClientOrigin } from './internal/client-origin.js';
export type { BrokerHttpOptions } from './internal/broker-http.js';
export { ServiceListenerRegistry } from './internal/service-listener-registry.js';
export type { DdsrEventSource, EventSourceHandler, EventSubscription } from './events/event-source.js';
export { RestEventSource } from './events/rest-event-source.js';
export type { RestEventSourceOptions } from './events/rest-event-source.js';
export { SseParser } from './events/sse-parser.js';
export {
  newEnvelope, writeStructured, readStructured, toHeaders, fromHeaders, dataAsText,
  isTextual, lifecycleTypeOf, lifecycleEventTypeOf,
  CE_SPEC_VERSION, CE_STRUCTURED_MEDIA_TYPE, CE_HEADER_PREFIX,
  CE_TYPE_INVOKE, CE_TYPE_INVOKE_REPLY, CE_TYPE_RESYNC,
  CE_EXTENSION_CORRELATION_ID, CE_EXTENSION_REPLY_TO,
} from './cloudevents/cloud-events.js';
export type { CloudEventEnvelope, CloudEventMessage } from './cloudevents/cloud-events.js';
export { attachShutdownHooks } from './lifecycle/shutdown-hooks.js';
export type { ShutdownHookOptions } from './lifecycle/shutdown-hooks.js';
export { serializeToXmi, deserializeFromXmi } from './xmi/xmi-support.js';
export {
  encodeInvocation, decodeInvocation, encodeResult, encodeFailure, decodeResult,
  propertyFor, valueOf,
} from './invocation/invocation.js';
export type { DecodedInvocation, DecodedResult } from './invocation/invocation.js';
export { propertyValue, propertiesOf, propertyOf, props, FINGERPRINT_PROPERTY } from './properties.js';
export { fingerprint, canonicalForm, FINGERPRINT_SCHEME } from './fingerprint/service-description-fingerprint.js';
export { implementationFingerprint, implementationCanonicalForm, IMPL_FINGERPRINT_SCHEME } from './fingerprint/service-implementation-fingerprint.js';
export type { PropertyValue } from './properties.js';
export { eClassName, toArray, asRoots, firstOfClass } from './internal/emf-util.js';
