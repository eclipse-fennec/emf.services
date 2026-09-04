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

import { BrokerStub } from './broker-stub';

/**
 * The stub, or nothing when BROKER_URL points at a real broker. Shared
 * by the modules, because the example is one process — a real
 * deployment has no such file.
 */
export const stub: BrokerStub | undefined = process.env.BROKER_URL
  ? undefined
  : new BrokerStub();
