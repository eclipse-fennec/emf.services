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
import { bindFlavorPlugins, mirrorRemoteServices, publishClient } from '@ddsr/tsm';
import type { RemoteMirror } from '@ddsr/tsm';
import { BROKER_URL } from '../contracts';
import { stub } from '../broker-stub-instance';

let mirror: RemoteMirror | undefined;

/**
 * The one module that knows there is a broker. It builds the client,
 * puts it in the registry, keeps its flavors in step, and mirrors the
 * Payment implementations — after this, nothing else in the process
 * mentions DDSR.
 */
export async function activate(context: ModuleContext): Promise<void> {
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    // Flavors arrive through the registry, so none are handed over here.
    flavorPlugins: [],
    consumerId: 'tsm-example',
    ...stub ? { fetchFn: stub.fetchFn, eventSource: stub.eventSource } : {},
  });

  publishClient(context.services, client);
  bindFlavorPlugins(context.services, client);

  mirror = await mirrorRemoteServices({
    registry: context.services,
    consumer: client.consumer,
    interfaceName: 'Payment',
    log: (message) => context.log.info(message),
  });

  context.log.info(`mirroring ${mirror.mirrored().length} Payment implementation(s)`);
}

export async function deactivate(): Promise<void> {
  await mirror?.close();
}
