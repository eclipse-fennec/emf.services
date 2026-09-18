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

/**
 * How a bundle says that it brings a service model.
 *
 * <p>A capability rather than a configuration, because the bundle is the
 * one that knows: the document sits inside it, and a deployment should
 * not have to be told about a file it cannot see. It is also readable
 * without loading anything — the wiring answers, no class is resolved,
 * no component started.
 *
 * <pre>
 * Provide-Capability: org.eclipse.fennec.services.model; \
 *     org.eclipse.fennec.services.model=Payment; \
 *     path=model/payment.xmi; \
 *     version:Version=1.0.0
 * </pre>
 *
 * <p>The namespace is deliberately not one of the {@code ddsr.*} names:
 * those are wire names, frozen until the rename (#4), and this one never
 * travels — it is between a bundle and the framework it sits in.
 */
public final class ServiceModelCapability {

	/** The capability namespace, and the attribute naming the contract. */
	public static final String NAMESPACE = "org.eclipse.fennec.services.model";

	/** Where the document is, relative to the bundle root. */
	public static final String PATH = "path";

	/** The contract's version, when the document does not carry one. */
	public static final String VERSION = "version";

	/**
	 * The service property by which a running service names the contract
	 * it implements. The same property the generic REST distribution
	 * looks an implementation up by (#84) — a native provider says it
	 * once and both find it.
	 */
	public static final String CONTRACT_PROPERTY = "ddsr.contract";

	/**
	 * The service property by which one instance of a contract is told
	 * from another. Two services of the same contract are two
	 * implementations, and something has to say which registration
	 * belongs to which.
	 */
	public static final String INSTANCE_PROPERTY = "ddsr.service.id";

	private ServiceModelCapability() {
	}
}
