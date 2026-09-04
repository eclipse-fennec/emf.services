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
import { Payment } from '../contracts';

/**
 * A module that uses a payment service. It waits for one, calls it, and
 * has no idea that the provider is somewhere behind a broker — the
 * registry is all it sees.
 */
export async function activate(context: ModuleContext): Promise<void> {
  const payment = context.services.getRequired(Payment);
  context.log.info('got a Payment service — calling charge(4200, "acct-1")');
  const answer = await payment.charge(4200, 'acct-1');
  context.log.info(`charge answered: ${JSON.stringify(answer)}`);
}

export function deactivate(context: ModuleContext): void {
  context.log.info('no Payment service any more — stopping');
}
