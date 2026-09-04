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

import type { ModuleContext } from '@eclipse-daanse/tsm';
import { DdsrClientImpl } from '@ddsr/client';
import { bindFlavorPlugins, publishClient } from '@ddsr/tsm';
import { BROKER_URL } from '../contracts';
import { stub } from '../broker-stub-instance';

/**
 * The one module that knows there is a broker: it builds the client,
 * publishes it, and keeps its flavors in step. Which contracts are
 * mirrored is not its business — @ddsr/stubs ships a module per
 * contract that does that, and this one only has to exist first.
 */
export function activate(context: ModuleContext): void {
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    // Flavors arrive through the registry, so none are handed over here.
    flavorPlugins: [],
    consumerId: 'tsm-example',
    ...stub ? { fetchFn: stub.fetchFn, eventSource: stub.eventSource } : {},
  });

  publishClient(context.services, client);
  bindFlavorPlugins(context.services, client);
  context.log.info('client published');
}
