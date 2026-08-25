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

package org.eclipse.fennec.services.client;

import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;

/**
 * Provider-side facade. The provider builds a {@link ServiceProvider}
 * containing exactly one {@link ServiceImplementation} (model
 * containment ownership) and calls {@link #publish}. The returned
 * {@link Registration} is the handle to call {@code withdraw()} on
 * later.
 *
 * <p>The SDK guarantees: a successful publish yields a Registration
 * with a non-null reference id. WARNING-severity diagnostics (e.g.
 * publishing against a deprecated interface) propagate via
 * {@link Registration#diagnostic()} but do not block.
 */
public interface DdsrProvider {

	/**
	 * Publish an implementation. Returns the resulting Registration on
	 * success (severity OK or WARNING); throws {@link DdsrException}
	 * if the broker refuses the publish (ERROR/CANCEL severity).
	 */
	Registration publish(ServiceProvider self, ServiceImplementation implementation);
}
