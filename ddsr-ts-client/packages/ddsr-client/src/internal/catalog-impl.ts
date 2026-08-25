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

import type { Diagnostic, ServiceInterface } from '@ddsr/model';
import type { DdsrCatalog } from '../api/ddsr-catalog';
import { DdsrClientError } from '../api/errors';
import { BrokerHttp, isError } from './broker-http';

const CATALOG_ENTRY_ALREADY_EXISTS = 202;

export class DdsrCatalogImpl implements DdsrCatalog {
  private readonly broker: BrokerHttp;

  constructor(broker: BrokerHttp) {
    this.broker = broker;
  }

  async ensureEntry(serviceInterface: ServiceInterface, requestor?: string): Promise<Diagnostic> {
    const diagnostic = await this.broker.addCatalogEntry(serviceInterface, requestor);
    if (isError(diagnostic) && diagnostic.code !== CATALOG_ENTRY_ALREADY_EXISTS) {
      throw new DdsrClientError(
        `catalog entry '${serviceInterface.name}' rejected: [${diagnostic.code}] ${diagnostic.message ?? ''}`,
        diagnostic
      );
    }
    return diagnostic;
  }

  addEntry(serviceInterface: ServiceInterface, requestor?: string): Promise<Diagnostic> {
    return this.broker.addCatalogEntry(serviceInterface, requestor);
  }

  getEntry(name: string): Promise<ServiceInterface | undefined> {
    return this.broker.getCatalogEntry(name);
  }

  deprecateEntry(name: string, details?: ServiceInterface, requestor?: string): Promise<Diagnostic> {
    return this.broker.deprecateCatalogEntry(name, details, requestor);
  }

  removeEntry(name: string, requestor?: string): Promise<Diagnostic> {
    return this.broker.removeCatalogEntry(name, requestor);
  }
}
