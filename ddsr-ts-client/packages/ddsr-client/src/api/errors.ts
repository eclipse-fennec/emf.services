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

import type { Diagnostic } from '@ddsr/model';

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
