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

import { serviceId } from '@eclipse-daanse/tsm';
import type { DdsrClient, FlavorPlugin } from '@ddsr/client';

/**
 * The names DDSR services carry inside a TSM registry.
 *
 * A service id is a string, so it survives a manifest, a target filter
 * and a capability; `serviceId<T>()` ties it to the contract it stands
 * for, so a consumer imports one name and a mismatch is a compile error.
 *
 * The strings are the ones the client documents: FlavorPlugins live
 * under `ddsr.flavor`, discovered rather than handed over (see the SPI
 * comment on FlavorPlugin), and the client itself under `ddsr.client`,
 * so a consumer module can inject it instead of constructing one.
 */
export const DdsrClientService = serviceId<DdsrClient>('ddsr.client');
export const FlavorPluginService = serviceId<FlavorPlugin>('ddsr.flavor');

/** The id a remote ServiceInterface is mirrored under, unless one is given. */
export function remoteServiceId(interfaceName: string): string {
  return `ddsr.remote.${interfaceName}`;
}
