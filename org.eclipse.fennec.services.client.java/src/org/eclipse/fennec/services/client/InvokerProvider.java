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

import java.util.Set;

import org.eclipse.fennec.services.FlavorKind;

/**
 * The invokers a runtime has, and — as a service property — the flavors
 * it has one for.
 *
 * <p>There is one of these in a framework. It collects every
 * {@link ServiceInvoker} and says in {@link #FLAVORS} which flavors can
 * be called right now, so that a component that depends on particular
 * flavors can wait for them with nothing but the target of a reference:
 * {@code (&(fennec.invokers=REST)(fennec.invokers=MQTT))} is satisfied
 * once both are there and stops being satisfied when one of them goes.
 * That is how a client is kept from telling the broker it speaks a
 * flavor it has no invoker for.
 */
public interface InvokerProvider {

	/**
	 * The service property naming the flavors there is an invoker for:
	 * the {@code FlavorKind} names, as a {@code String[]}. An invoker is
	 * reachable through {@link #invokerFor} before its flavor is listed
	 * here, and still reachable when it is no longer listed.
	 */
	String FLAVORS = "fennec.invokers";

	/** The target that waits for the REST invoker — the default of a client. */
	String REST_TARGET = "(" + FLAVORS + "=REST)";

	/** The invoker that speaks a flavor, or null when this runtime has none. */
	ServiceInvoker invokerFor(FlavorKind kind);

	/** The flavors there is an invoker for at this moment. */
	Set<FlavorKind> flavors();
}
