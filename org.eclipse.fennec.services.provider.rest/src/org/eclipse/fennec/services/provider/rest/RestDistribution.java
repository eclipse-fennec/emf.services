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

package org.eclipse.fennec.services.provider.rest;

import org.eclipse.fennec.services.RestFlavor;

/**
 * Serve a REST flavor that was not read from a bundle.
 *
 * <p>{@link GenericRestDistribution} covers the case where the model
 * sits in a bundle and a configuration names it. The other case is a
 * model that only exists at runtime — an OSGi service exported through
 * Remote Service Admin has its contract derived the moment it is
 * exported (#24), and there is no document to point a configuration at.
 *
 * <p>What is served is the same thing either way: a flavor, an
 * implementation behind it, and the dispatcher between them. So the
 * difference is only where the model comes from, and that is what this
 * hands over.
 */
public interface RestDistribution {

	/**
	 * Mount an endpoint for {@code flavor} and answer its requests by
	 * invoking {@code service}.
	 *
	 * @param flavor  what to serve — its operation flavors decide which
	 *                request means which operation, and its
	 *                {@code basePath} is where the endpoint is mounted
	 * @param service the object the operations are invoked on; it has to
	 *                carry a method per operation, by name and parameter
	 *                count
	 * @param name    a name for the endpoint, so a runtime that lists
	 *                its applications shows something one can find
	 * @return the mounted endpoint; closing it takes the endpoint down
	 */
	Served serve(RestFlavor flavor, Object service, String name);

	/** A mounted endpoint. */
	interface Served extends AutoCloseable {

		/** Where it answers, relative to the deployment's own base. */
		String basePath();

		/** Take it down. Idempotent. */
		@Override
		void close();
	}
}
