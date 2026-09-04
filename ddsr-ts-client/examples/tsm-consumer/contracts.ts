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
 * The contract the consumer module writes against — generated from the
 * catalog by @ddsr/codegen, not written here.
 *
 * Worth the detour: this file used to declare it by hand as
 * `charge(amount, accountId)`, and the catalog says
 * `charge(amount, currency?)`. The example passed an account id as a
 * currency for as long as that stood, and nothing complained — the
 * broker returned a number either way.
 *
 * The service id says nothing about being remote. The module could be
 * handed a local implementation under the same id and would not notice.
 */
export type { Payment } from '@ddsr/stubs';
export { Payment as PaymentService } from '@ddsr/stubs/PaymentModule';

export const BROKER_URL = process.env.BROKER_URL ?? 'http://broker.example/ddsr/rest';
