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

import java.net.URI;
import java.util.Optional;

import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceReference;

/**
 * Bundle of "everything a consumer needs to invoke a service":
 * the reference (identity + properties), the implementation
 * (interfaces + flavors), and convenience accessors for the
 * transport details.
 *
 * <p>Constructed by the SDK from the multi-root lookup response.
 * Model objects inside are detached copies — callers may inspect
 * but must not pass them back into the SDK without a fresh deep
 * copy (EMF containment is exclusive).
 */
public interface ServiceLocator {

	ServiceReference reference();

	ServiceImplementation implementation();

	/**
	 * The first {@link RestFlavor} on the implementation, if any.
	 * Multi-flavor implementations expose all of them via
	 * {@code implementation().getFlavors()}.
	 */
	Optional<RestFlavor> restFlavor();

	/**
	 * Convenience: full URL for the named operation under this
	 * locator's REST flavor, computed as
	 * {@code brokerHost + restFlavor.basePath + operationFlavor.path}.
	 * Empty if the locator has no REST flavor or no operation flavor
	 * with that name.
	 */
	Optional<URI> urlFor(String operationName);
}
