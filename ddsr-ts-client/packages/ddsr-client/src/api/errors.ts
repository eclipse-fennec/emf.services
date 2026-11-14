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

import type { Diagnostic, ServiceException } from '@ddsr/model';

/**
 * Error carrying the broker Diagnostic that caused it — the TS mirror
 * of the Java client's DdsrException.
 */
export class DdsrClientError extends Error {
  readonly diagnostic: Diagnostic | undefined;

  constructor(message: string, diagnostic?: Diagnostic) {
    super(message);
    this.name = 'DdsrClientError';
    this.diagnostic = diagnostic;
  }
}

/**
 * The remote end could not be reached or did not answer in time —
 * connect refused, timeout, reset. The call may never have arrived; a
 * tracked locator rebinds away from the registration and retries the
 * invocation once (#59). Thrown by flavor plugins.
 */
export class DdsrTransportError extends DdsrClientError {
  constructor(message: string, readonly cause?: unknown) {
    super(message);
    this.name = 'DdsrTransportError';
  }
}

/**
 * The provider answered, and the answer is one of the errors the
 * contract declares: `exception` is that ServiceException. Over REST it
 * is recognised by the status its flavor binds it to - the twin of what
 * RestDispatcher writes - and over MQTT, which has no status, by the
 * code of the Diagnostic the answer carries. Not a transport error, so
 * nothing retries it: calling again would get the same answer.
 *
 * Generated stubs turn it into the class their binding generates for
 * the exception type, so a caller can write `instanceof`.
 */
export class DdsrServiceError extends DdsrClientError {
  constructor(message: string, readonly exception: ServiceException, readonly status?: number) {
    super(message);
    this.name = 'DdsrServiceError';
  }
}

/**
 * Nothing implements the contract right now: the implementation a
 * locator or proxy was bound to went away and the lookup finds no
 * other. The contract may well still be in the catalog — it outlives its
 * implementations (#271) — so this passes: once an implementation is
 * registered again, the next call through the same proxy binds to it.
 *
 * Not a transport error, so nothing retries it. To wait, listen for
 * REGISTERED on the interface instead of calling in a loop. The twin of
 * the Java client's DdsrServiceUnavailableException.
 */
export class DdsrServiceUnavailableError extends DdsrClientError {
  constructor(readonly interfaceName: string, readonly lastReference?: string, readonly parked = false) {
    super(
      `no implementation of '${interfaceName}' is available` +
      (parked ? ': the last one is parked and could not be brought back' : '') +
      (lastReference ? ` (last bound to ${lastReference})` : '')
    );
    this.name = 'DdsrServiceUnavailableError';
  }
}
