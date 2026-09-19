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

package org.eclipse.fennec.services.rsa.config;

import java.util.Map;

/**
 * One configuration a role derives, named the way a deployment would
 * have written it by hand.
 *
 * <p>{@code name} is what turns a plain PID into a factory
 * configuration: {@code EObjectRegistry} with the name
 * {@code ddsrContracts} is the {@code EObjectRegistry~ddsrContracts}
 * that a {@code config.json} spells out. A {@code null} name means the
 * PID stands alone.
 *
 * @param pid the PID, or the factory PID when {@code name} is set
 * @param name the factory configuration's name, or {@code null}
 * @param properties what the configuration says
 */
public record DerivedConfiguration(String pid, String name, Map<String, Object> properties) {

	/** A configuration for a PID that stands alone. */
	public static DerivedConfiguration of(String pid, Map<String, Object> properties) {
		return new DerivedConfiguration(pid, null, properties);
	}

	/** A configuration of a factory, under the given name. */
	public static DerivedConfiguration ofFactory(String factoryPid, String name, Map<String, Object> properties) {
		return new DerivedConfiguration(factoryPid, name, properties);
	}

	public boolean isFactory() {
		return name != null;
	}

	/** How a {@code config.json} would name this one. */
	public String label() {
		return isFactory() ? pid + "~" + name : pid;
	}
}
