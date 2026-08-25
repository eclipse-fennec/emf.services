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

import java.util.Map;

/**
 * Generic remote invocation against a previously discovered
 * {@link ServiceLocator}. Replaces hand-written client stubs while
 * the project has no code generator — the catalog itself drives the
 * call (operation name, HTTP method, path, parameter shape).
 *
 * <p>An OSGi service provided by the active flavor bundle
 * (REST today, MQTT later). The interface is transport-agnostic;
 * each flavor picks its own argument-marshalling convention.
 */
public interface ServiceInvoker {

	/**
	 * Invoke {@code operationName} on the service identified by
	 * {@code locator}, passing the named arguments. The flavor module
	 * is responsible for translating arguments into the wire
	 * representation (e.g. query parameters, JSON body, XMI body).
	 *
	 * @return the parsed result. For XMI responses this is an
	 *         {@code EObject}; for plain text it is a {@code String};
	 *         {@code null} when the response has no body.
	 * @throws DdsrException on transport failure or unsupported
	 *         marshalling shape
	 */
	Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args);
}
