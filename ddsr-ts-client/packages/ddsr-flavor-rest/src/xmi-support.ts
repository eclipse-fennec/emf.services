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

// The XMI helpers moved into @ddsr/client (they are transport-neutral
// and the client's broker proxies need them without a dependency cycle).
// Re-exported here for compatibility. Note: deserializeFromXmi is
// synchronous now — @emfts/core's loadFromString is synchronous.
export { serializeToXmi, deserializeFromXmi } from '@ddsr/client';
