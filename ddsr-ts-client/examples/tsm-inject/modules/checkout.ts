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

import { activate, component, inject } from '@eclipse-daanse/tsm/decorators';
import { Payment } from '@ddsr/stubs/PaymentModule';
import type { Payment as PaymentContract } from '@ddsr/stubs';

/**
 * A component that charges an account.
 *
 * Everything that makes Payment a remote service is absent here: no
 * broker URL, no client, no lookup, no undefined to handle, no listener
 * for the provider going away. The constructor asks for the contract
 * and gets it — and if none is there, this class is not constructed at
 * all.
 *
 * `Payment` is the service id from the generated module; the type of
 * the same name is the generated contract. They are separate namespaces
 * in TypeScript, which is why both can be called what the service is
 * called.
 */
@component()
export class Checkout {
  constructor(@inject(Payment) private readonly payments: PaymentContract) {}

  @activate()
  async start(): Promise<void> {
    console.log('[checkout] charging 4200 EUR');
    const remaining = await this.payments.charge(4200, 'EUR');
    console.log(`[checkout] remaining balance: ${remaining}`);

    const balance = await this.payments.getBalance('acct-1');
    console.log(`[checkout] getBalance('acct-1'): ${balance}`);
  }
}
