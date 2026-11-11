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

package org.eclipse.fennec.services.client.announce;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.function.Supplier;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.ServiceImplementation;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.condition.Condition;

/**
 * A contract this node serves from a configuration — and whether it is
 * to be announced right now.
 *
 * <p>A distribution registers one as a {@link Condition}, and that is
 * what it is: {@code publish} is {@code false} while the contract is
 * not reachable yet, and {@code true} once it is — the whiteboard lists
 * the application, the messaging broker has the subscriptions — and the
 * configuration asks for an announcement. Whoever announces binds it
 * with a target on that property, so "served first, then told" (FR-P3)
 * is not something a component works out by asking around: it is the
 * moment a mandatory reference becomes satisfied, and "withdrawn first,
 * then gone" the moment it stops being.
 *
 * <p>It also carries what the announcement is made of, which is why
 * the same object is registered under this type as well: the document
 * the distribution serves from, so nothing states the contract twice.
 * The same for every transport — what differs is only what a transport
 * writes into the flavor before it is announced.
 */
public final class ServedContract implements Condition {

	/** {@code osgi.condition.id} of every contract served from a configuration. */
	public static final String ID = "fennec.distribution.served";

	/** Which distribution this is: the PID of the configuration that serves it. */
	public static final String DISTRIBUTION = "fennec.distribution";

	/** The name of the contract, for whoever reads the registry. */
	public static final String CONTRACT = "fennec.distribution.contract";

	/** Whether the contract is to be announced now: served, and asked to. */
	public static final String PUBLISH = "publish";

	/** The factory PID a distribution derives one configuration of per announced contract. */
	public static final String ANNOUNCEMENT_PID = "org.eclipse.fennec.services.client.announcement";

	/** The configuration property that selects the served contract. */
	public static final String SERVED_TARGET = "served.target";

	private final String distribution;

	private final String requestor;

	private final String brokerUrl;

	private final ServiceImplementation implementation;

	private final ResourceSet resourceSet;

	private final Supplier<String> deployed;

	/**
	 * @param distribution   the PID of the configuration that serves it
	 * @param requestor      who enters the contract in the catalog
	 * @param brokerUrl      the broker's base URL, to name the contract's
	 *                       catalog entry by
	 * @param implementation what is served, as the document says it
	 * @param resourceSet    the resource set the document was read through
	 * @param deployed       writes into the flavor what only the
	 *                       deployment knows — where this instance is
	 *                       reachable, how a caller proves itself — and
	 *                       answers where that is, for the log
	 */
	public ServedContract(String distribution, String requestor, String brokerUrl,
			ServiceImplementation implementation, ResourceSet resourceSet, Supplier<String> deployed) {
		this.distribution = distribution;
		this.requestor = requestor;
		this.brokerUrl = brokerUrl == null ? "" : brokerUrl;
		this.implementation = implementation;
		this.resourceSet = resourceSet;
		this.deployed = deployed;
	}

	String requestor() {
		return requestor;
	}

	String brokerUrl() {
		return brokerUrl;
	}

	ServiceImplementation implementation() {
		return implementation;
	}

	ResourceSet resourceSet() {
		return resourceSet;
	}

	/** Fill in the deployment facts; answers where the contract is reachable. */
	String deploy() {
		return deployed.get();
	}

	/** What the service is registered with. */
	public Dictionary<String, Object> properties(boolean publish) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Condition.CONDITION_ID, ID);
		properties.put(DISTRIBUTION, distribution);
		// Not `ddsr.contract`: that is the property a transport searches
		// the implementation of a contract by, and this is not one — a
		// dispatcher that found it would try to invoke the contract's
		// operations on a condition.
		properties.put(CONTRACT, implementation.getServiceInterfaces().get(0).getName());
		properties.put(PUBLISH, String.valueOf(publish));
		return properties;
	}

	/** The interfaces a served contract is registered under. */
	public static String[] serviceTypes() {
		return new String[] { Condition.class.getName(), ServedContract.class.getName() };
	}

	/**
	 * The configuration of the component that announces one served
	 * contract: one factory configuration, named after the distribution,
	 * whose only statement is which served contract it waits for. The
	 * distribution writes it when its own configuration says
	 * {@code publish} and deletes it when it goes.
	 */
	public static Configuration announcementFor(ConfigurationAdmin configurations, String distribution)
			throws IOException {
		Configuration derived = configurations.getFactoryConfiguration(ANNOUNCEMENT_PID,
				distribution.replaceAll("[^A-Za-z0-9_-]", "-"), "?");
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVED_TARGET, announceTarget(distribution));
		derived.update(properties);
		return derived;
	}

	/** The target that selects one distribution while it is to be announced. */
	public static String announceTarget(String distribution) {
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
