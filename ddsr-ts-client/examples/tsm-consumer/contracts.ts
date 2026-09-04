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
import { remoteServiceId } from '@ddsr/tsm';

/**
 * The contract the consumer module writes against. It says nothing
 * about the broker, and nothing about being remote — which is the
 * point: the module could be given a local implementation of the same
 * id and would not notice.
 */
export interface Payment {
  charge(amount: number, accountId: string): Promise<unknown>;
  getBalance(accountId: string): Promise<unknown>;
}

export const Payment = serviceId<Payment>(remoteServiceId('Payment'));

export const BROKER_URL = process.env.BROKER_URL ?? 'http://broker.example/ddsr/rest';
