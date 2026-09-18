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

package org.eclipse.fennec.services.rsa.registry;

import java.util.Map;
import java.util.Optional;

import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;

/**
 * Every service this framework knows, as a model.
 *
 * <p>Two kinds of provider arrive here and leave the same way. One
 * <strong>brings its model</strong>: the bundle declares where it is
 * through the {@link ServiceModelCapability capability}, the document is
 * read, and what it says is what is promised. The other is an
 * <strong>ordinary OSGi service</strong> with export properties, whose
 * contract is derived from its interface. After intake nothing
 * downstream can tell the two apart, which is the point — a consumer in
 * TypeScript, Python or Rust reaches both through the registry, and
 * neither of them has a Java interface to look at.
 *
 * <p>The same registry holds the other direction: what discovery learned
 * from the broker. {@link LocalServiceRegistry} is built for that — it
 * carries what this framework provides, what it is bound to, and a
 * handle on the remote registry the broker keeps.
 *
 * <p>Storage is an {@code EObjectRegistry} underneath: a named,
 * key-addressed store of EObjects, fed by files and by anything else
 * that can produce one. It is where intake lands; this is what the rest
 * of the system reads.
 */
public interface ServiceModels {

	/**
	 * The registry itself — what this framework provides, what it holds
	 * of others, and the remote registry behind them.
	 */
	LocalServiceRegistry registry();

	/** A contract by name, if this framework knows one. */
	Optional<ServiceInterface> contract(String name);

	/**
	 * The contract of a service that is about to be exported.
	 *
	 * <p>If the provider brought a model for it, that one — a hand-written
	 * contract says more than any derivation can, and overruling it would
	 * make the document decorative. Otherwise the interface is read, and
	 * the result is kept, so the same service exported twice is the same
	 * contract both times.
	 *
	 * @param serviceInterface the interface to export as
	 * @param properties       the service's properties; a native provider
	 *                         names its contract in them
	 */
	ServiceInterface contractFor(Class<?> serviceInterface, Map<String, ?> properties);

	/**
	 * Take an implementation into the registry — one this framework
	 * exports, or one discovery found elsewhere.
	 *
	 * @return closing it removes the implementation again
	 */
	AutoCloseable add(ServiceImplementation implementation);
}
