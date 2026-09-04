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
import { RestFlavorPlugin } from '@ddsr/flavor-rest';
import { publishFlavorPlugin } from '@ddsr/tsm';
import { stub } from '../broker-stub-instance';

/**
 * The REST transport, as a module of its own. It activates after the
 * client was built — which is the case a snapshot would lose, and the
 * reason bindFlavorPlugins() exists.
 */
export function activate(context: ModuleContext): void {
  publishFlavorPlugin(context.services, new RestFlavorPlugin(stub?.fetchFn));
  context.log.info('REST flavor published');
}
