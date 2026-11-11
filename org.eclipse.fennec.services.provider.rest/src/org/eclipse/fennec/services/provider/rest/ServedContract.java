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

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.ServiceImplementation;
import org.osgi.service.condition.Condition;

/**
 * A contract this node serves from a configuration — and whether it is
 * to be announced right now.
 *
 * <p>Registered by {@link GenericRestDistribution} as a
 * {@link Condition}, and that is what it is: {@code publish} is
 * {@code false} while the endpoint is not there yet, and {@code true}
 * once the whiteboard lists the application and the configuration asks
 * for an announcement. Whoever announces binds it with a target on that
 * property, so "served first, then told" (FR-P3) is not something a
 * component works out by asking around — it is the moment a mandatory
 * reference becomes satisfied, and "withdrawn first, then gone" the
 * moment it stops being.
 *
 * <p>It also carries what the announcement is made of, which is why
 * the same object is registered under this type as well: the document
 * the dispatcher serves from, so nothing states the contract twice.
 */
public final class ServedContract implements Condition {

	/** {@code osgi.condition.id} of every contract served from a configuration. */
	public static final String ID = "fennec.distribution.served";

	/** Which distribution this is: the PID of the configuration that serves it. */
	public static final String DISTRIBUTION = "fennec.distribution";

	/** Whether the contract is to be announced now: served, and asked to. */
	public static final String PUBLISH = "publish";

	private final String distribution;

	private final GenericRestDistribution.Config config;

	private final ServiceImplementation implementation;

	private final ResourceSet resourceSet;

	private final Object mode;

	ServedContract(String distribution, GenericRestDistribution.Config config,
			ServiceImplementation implementation, ResourceSet resourceSet, Object mode) {
		this.distribution = distribution;
		this.config = config;
		this.implementation = implementation;
		this.resourceSet = resourceSet;
		this.mode = mode;
	}

	/** The configuration that serves it. */
	GenericRestDistribution.Config config() {
		return config;
	}

	/** What is served, as the document says it. */
	ServiceImplementation implementation() {
		return implementation;
	}

	/** The resource set the document was read through. */
	ResourceSet resourceSet() {
		return resourceSet;
	}

	/** The authentication mode the application states. */
	Object mode() {
		return mode;
	}

	/** What the service is registered with. */
	Dictionary<String, Object> properties(boolean publish) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Condition.CONDITION_ID, ID);
		properties.put(DISTRIBUTION, distribution);
		properties.put("ddsr.contract", implementation.getServiceInterfaces().get(0).getName());
		properties.put(PUBLISH, String.valueOf(publish));
		return properties;
	}

	/** The target that selects this distribution while it is to be announced. */
	static String announceTarget(String distribution) {
		return "(&(" + DISTRIBUTION + "=" + escape(distribution) + ")(" + PUBLISH + "=true))";
	}

	/** A value as it may stand in an LDAP filter. */
	static String escape(String value) {
		StringBuilder escaped = new StringBuilder();
		for (char c : value.toCharArray()) {
			if (c == '(' || c == ')' || c == '*' || c == '\\') {
				escaped.append('\\');
			}
			escaped.append(c);
		}
		return escaped.toString();
	}
}
