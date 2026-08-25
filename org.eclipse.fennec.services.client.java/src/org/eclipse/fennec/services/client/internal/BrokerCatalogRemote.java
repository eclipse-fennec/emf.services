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

package org.eclipse.fennec.services.client.internal;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceInterface;

/**
 * Demo Java stub for the broker's {@code BrokerCatalog} ServiceInterface.
 * Method names mirror the DDSR operation names (not the Java embedded
 * {@code org.eclipse.fennec.services.broker.core.BrokerCatalog} naming) so a
 * reflective proxy can map them 1:1 to the catalog's
 * {@code RestOperationFlavor}s.
 *
 * <p>Hand-written for the prototype. A future code generator would
 * emit this kind of interface from the DDSR ServiceInterface XMI.
 */
public interface BrokerCatalogRemote {

	RemoteServiceRegistry listCatalog();

	Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor);

	Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor);

	Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor);
}
