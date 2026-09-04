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
import { RestFlavorPlugin } from '@ddsr/flavor-rest';
import { bindFlavorPlugins, publishClient } from '@ddsr/tsm';
import { BROKER_URL, stub } from '../broker-stub';

/**
 * The only module that knows a broker exists. It publishes the client
 * and the REST flavor; which contracts get mirrored is the business of
 * the generated modules.
 */
export function activate(context: ModuleContext): void {
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    flavorPlugins: [],
    consumerId: 'tsm-inject-example',
    ...stub ? { fetchFn: stub.fetchFn, eventSource: stub.eventSource } : {},
  });

  publishClient(context.services, client);
  bindFlavorPlugins(context.services, client);
  context.services.register('ddsr.flavor', new RestFlavorPlugin(stub?.fetchFn), {
    providedBy: 'ddsr.flavor.REST',
    properties: { flavorKind: 'REST' },
  });
  context.log.info('client and REST flavor published');
}
