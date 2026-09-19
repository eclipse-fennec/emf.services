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

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentException;

/**
 * What the provider and the consumer role have in common: take the
 * settings, say what is wrong with them, write the derived
 * configurations in order, and take them down in the opposite order.
 *
 * <p>Both roles are ordinary configured components — they exist because
 * a deployment wrote their one configuration, and they do nothing
 * without it.
 */
abstract class RsaRoleConfiguration {

	private static final Logger LOG = Logger.getLogger(RsaRoleConfiguration.class.getName());

	private DerivedConfigurations derived;

	/** Whether this role serves endpoints, i.e. whether it has an HTTP stack. */
	abstract boolean serves();

	/** The configurations this role owns, in the order they are written. */
	abstract List<DerivedConfiguration> plan(RoleSettings settings);

	/** How this role names itself in the log. */
	abstract String role();

	void start(ConfigurationAdmin admin, RoleSettings settings) {
		derived = new DerivedConfigurations(admin, role());
		apply(settings);
	}

	/**
	 * A changed role configuration updates the derived ones in place.
	 *
	 * <p>Taking the change here rather than letting this component be
	 * destroyed and rebuilt is what keeps the stack below from being torn
	 * down and brought back up for a heartbeat interval nobody urgently
	 * needed changed.
	 */
	void update(RoleSettings settings) {
		apply(settings);
	}

	void stop() {
		if (derived != null) {
			derived.close();
			derived = null;
		}
	}

	private void apply(RoleSettings settings) {
		List<String> problems = RoleChecks.problems(settings, serves());
		if (!problems.isEmpty()) {
			String message = "[DDSR] the " + role() + " configuration cannot be used: "
					+ String.join("; ", problems);
			LOG.severe(message);
			// Failing here is the point: the role wrote nothing, and the
			// reason stands in the component's own failure rather than
			// surfacing minutes later as an export nobody can explain.
			throw new ComponentException(message);
		}
		RoleChecks.mismatches(settings).forEach(note -> LOG.warning("[DDSR] " + role() + ": " + note));
		try {
			derived.apply(plan(settings));
		} catch (IOException failure) {
			throw new ComponentException("[DDSR] the " + role() + " could not write its configurations", failure);
		}
	}
}
