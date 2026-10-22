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

/**
 * How the examples reach a secured broker — the same environment
 * variables the Java nodes read (DDSR_OAUTH_*, DDSR_TLS_*,
 * DDSR_BROKER_AUTHENTICATION, DDSR_CONSUMER_ID), so one harness can hand
 * both languages their identity the same way.
 *
 * Without any of them nothing changes: no token, no certificate, and the
 * consumer id each example chooses itself.
 */
import { readFileSync } from 'node:fs';
import { clientCredentials, tlsFetch, type TlsOptions, type TokenSupplier } from '@ddsr/client';

/**
 * The token the broker is shown, or undefined against an open broker —
 * and against one the node proves itself to by certificate
 * (DDSR_BROKER_AUTHENTICATION=certificate): discovery over mTLS, while
 * calls to providers keep their tokens (providerTokens).
 */
export function brokerTokens(): TokenSupplier | undefined {
  if (process.env.DDSR_BROKER_AUTHENTICATION === 'certificate') {
    return undefined;
  }
  return providerTokens();
}

/** The client-credentials supplier for calls to guarded providers, or undefined without one. */
export function providerTokens(): TokenSupplier | undefined {
  const tokenUri = process.env.DDSR_OAUTH_TOKEN_URI;
  if (!tokenUri) {
    return undefined;
  }
  return clientCredentials({
    tokenUri,
    clientId: required('DDSR_OAUTH_CLIENT_ID'),
    clientSecret: required('DDSR_OAUTH_CLIENT_SECRET'),
    scope: process.env.DDSR_OAUTH_SCOPE,
  });
}

/**
 * The fetch every call of the node goes through: one that shows its
 * certificate when DDSR_TLS_KEYSTORE names one (PKCS12, the same file a
 * Java node uses, password DDSR_TLS_KEYSTORE_PASSWORD), believing the
 * authorities in DDSR_TLS_CA (PEM — Node reads no PKCS12 trust store);
 * undefined, and so the platform's fetch, otherwise.
 */
export function nodeFetch(): typeof fetch | undefined {
  const tls = nodeTls();
  return tls ? tlsFetch(tls) : undefined;
}

/**
 * The same certificate and authorities as TLS options, for the MQTT
 * transports (mqtts:// brokers); undefined without DDSR_TLS_*.
 */
export function nodeTls(): TlsOptions | undefined {
  const keyStore = process.env.DDSR_TLS_KEYSTORE;
  const ca = process.env.DDSR_TLS_CA;
  if (!keyStore && !ca) {
    return undefined;
  }
  return {
    pfx: keyStore ? readFileSync(keyStore) : undefined,
    passphrase: process.env.DDSR_TLS_KEYSTORE_PASSWORD,
    ca: ca ? readFileSync(ca) : undefined,
  };
}

/**
 * The consumer id: DDSR_CONSUMER_ID, else the client id — on a bearer
 * broker a session belongs to the name the token carries, on an mtls
 * broker to the certificate's CN, which is the client id too — else the
 * example's own choice.
 */
export function consumerId(fallback?: string): string | undefined {
  return process.env.DDSR_CONSUMER_ID || process.env.DDSR_OAUTH_CLIENT_ID || fallback;
}

function required(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`DDSR_OAUTH_TOKEN_URI is set, so ${name} is needed as well`);
  }
  return value;
}
