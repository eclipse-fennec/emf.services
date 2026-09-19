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

package org.eclipse.fennec.services.rsa.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;

/**
 * Which endpoints a listener asked to hear about.
 *
 * <p>A discovery does not decide who is interested; the listener says so
 * with {@code endpoint.listener.scope}, a set of filters evaluated
 * against the endpoint's own properties. What the listener then has to
 * be told is <em>which</em> of its filters matched — it may have asked
 * several questions and the answer is only useful with the question
 * attached.
 *
 * <p>Shared because more than one thing discovers: endpoints that arrive
 * over the wire and endpoints read out of a bundle's headers are the
 * same news to a listener, and they must not be matched by two slightly
 * different rules.
 */
public final class EndpointScopes {

	private static final Logger LOG = Logger.getLogger(EndpointScopes.class.getName());

	private EndpointScopes() {
	}

	/**
	 * {@code endpoint.listener.scope}, however it was said — one filter,
	 * an array of them, or a collection.
	 */
	public static List<String> of(Map<String, Object> listenerProperties) {
		Object scope = listenerProperties == null ? null
				: listenerProperties.get(EndpointEventListener.ENDPOINT_LISTENER_SCOPE);
		List<String> filters = new ArrayList<>();
		if (scope instanceof String single) {
			add(filters, single);
		} else if (scope instanceof String[] several) {
			for (String filter : several) {
				add(filters, filter);
			}
		} else if (scope instanceof Collection<?> several) {
			for (Object filter : several) {
				add(filters, filter == null ? null : filter.toString());
			}
		}
		return filters;
	}

	private static void add(List<String> filters, String filter) {
		if (filter != null && !filter.isBlank()) {
			filters.add(filter);
		}
	}

	/**
	 * The first filter of {@code scope} that the endpoint matches, or
	 * {@code null} when the listener did not ask for this one.
	 *
	 * <p>A filter that is not a filter is skipped rather than thrown:
	 * one listener's typo must not stop the others hearing.
	 */
	public static String matching(List<String> scope, EndpointDescription endpoint) {
		for (String filter : scope) {
			try {
				if (endpoint.matches(filter)) {
					return filter;
				}
			} catch (IllegalArgumentException malformed) {
				LOG.log(Level.FINE, "[DDSR] a listener's scope is not a filter: " + filter, malformed);
			}
		}
		return null;
	}
}
