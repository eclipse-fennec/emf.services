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
 * Diagnostic code constants — parity with Java DdsrDiagnostics.
 */
export const DiagnosticCode = {
  OK: 0,
  NETWORK_PARTITION: 100,
  CATALOG_HAS_LIVE_IMPLS: 200,
  CATALOG_ENTRY_NOT_FOUND: 201,
  CATALOG_ENTRY_ALREADY_EXISTS: 202,
  IMPL_INTERFACE_NOT_IN_CATALOG: 210,
  IMPL_OWNERSHIP_VIOLATION: 211,
  IMPL_NOT_PUBLISHED: 212,
  INTERFACE_DEPRECATED: 300,
  PERSISTENCE_FAILED: 500,
} as const;

export type DiagnosticCode = (typeof DiagnosticCode)[keyof typeof DiagnosticCode];
