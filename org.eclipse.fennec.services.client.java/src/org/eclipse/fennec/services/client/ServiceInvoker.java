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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

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
	 * Which flavor an invoker speaks, as a service property — the value
	 * is a {@code FlavorKind} name such as {@code REST}, or several of
	 * them for an invoker that speaks more than one: the messaging
	 * invoker says {@code MQTT} and {@code AMQP}.
	 *
	 * <p>The proxy factory reads it to pick the invoker for a service
	 * from the flavors that service announces. An invoker that declares
	 * nothing is never picked, which is the safe direction: calling the
	 * wrong way is worse than not calling.
	 */
	String FLAVOR_PROPERTY = "ddsr.flavor";

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

	/**
	 * The same call for a caller that does not wait: the stage completes
	 * with what {@link #invoke} would have returned and fails with what
	 * it would have thrown.
	 *
	 * <p>What a transport makes of it is its own. One that has an
	 * answer told to it — a messaging broker delivers it — completes the
	 * stage from there and occupies no thread in between. One that can
	 * only wait, as an HTTP client waits for its response, makes the
	 * synchronous call on {@code blocking}, which is what this default
	 * does.
	 *
	 * @param blocking where a call that has to wait is made; a
	 *                 transport that does not wait never uses it
	 */
	default CompletionStage<Object> invokeAsync(ServiceLocator locator, String operationName,
			Map<String, Object> args, Executor blocking) {
		CompletableFuture<Object> outcome = new CompletableFuture<>();
		try {
			blocking.execute(() -> {
				try {
					outcome.complete(invoke(locator, operationName, args));
				} catch (Throwable failure) {
					outcome.completeExceptionally(failure);
				}
			});
		} catch (RuntimeException rejected) {
			outcome.completeExceptionally(new DdsrException("the call was not made: " + rejected.getMessage(),
					rejected));
		}
		return outcome;
	}
}
