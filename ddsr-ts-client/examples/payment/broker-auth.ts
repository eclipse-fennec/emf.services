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
 * How the examples reach a broker in mode bearer — the same environment
 * variables the Java nodes read (DDSR_OAUTH_*, DDSR_CONSUMER_ID), so one
 * harness can hand both languages their identity the same way.
 *
 * Without DDSR_OAUTH_TOKEN_URI nothing changes: no token, and the
 * consumer id each example chooses itself.
 */
import { clientCredentials, type TokenSupplier } from '@ddsr/client';

/** The client-credentials supplier, or undefined against an open broker. */
export function brokerTokens(): TokenSupplier | undefined {
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
 * The consumer id: DDSR_CONSUMER_ID, else the client id — on a bearer
 * broker a session belongs to the name the token carries — else the
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
