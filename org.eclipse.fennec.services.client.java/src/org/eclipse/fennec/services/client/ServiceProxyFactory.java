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

/**
 * Builds a typed Java proxy for a remote DDSR service. The proxy
 * implements the supplied interface and routes each method call
 * through the active {@link ServiceInvoker}, mapping
 * {@code method.getName()} to the DDSR operation name and method
 * parameters (positional, by index, or by {@code -parameters}-compiled
 * name if available) to the invoker's argument map.
 *
 * <p>Typical use: a small registrar component looks up a service
 * via {@code DdsrConsumer.find}, calls {@link #newProxy}, and
 * registers the result as an OSGi service with
 * {@code service.imported=true}. Consumers then {@code @Reference}
 * the typed interface and call it like any local service.
 *
 * <p>Provided by the active flavor bundle (REST today, MQTT later).
 */
public interface ServiceProxyFactory {

	/**
	 * @param serviceInterface a Java interface whose method names
	 *        match the DDSR operation names; return types should
	 *        match what the wire delivers (EObject for XMI responses)
	 * @param locator the previously discovered ServiceLocator that
	 *        carries the RestFlavor (or other flavor) routing data
	 */
	<T> T newProxy(Class<T> serviceInterface, ServiceLocator locator);
}
