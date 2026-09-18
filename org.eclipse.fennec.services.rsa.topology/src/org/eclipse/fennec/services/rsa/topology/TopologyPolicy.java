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

package org.eclipse.fennec.services.rsa.topology;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * What this topology manager does on its own.
 *
 * <p>{@code promiscuous} — the specification's word — exports every
 * service that asks and imports every endpoint anyone here waits for.
 * {@code manual} does neither: the admin is there, and whoever calls it
 * decides. The TCK is one such caller; it drives exports itself and
 * counts what is left afterwards, so a manager acting alongside it turns
 * every test red.
 */
@ObjectClassDefinition(name = "Fennec Services RSA Topology",
		description = "Whether services are exported and imported automatically, or left to whoever calls the admin.")
public @interface TopologyPolicy {

	String PID = "org.eclipse.fennec.services.rsa.topology";

	String PROMISCUOUS = "promiscuous";

	String MANUAL = "manual";

	@AttributeDefinition(name = "Policy",
			description = "promiscuous: export what asks, import what is waited for. manual: do nothing on your own.",
			options = { @org.osgi.service.metatype.annotations.Option(label = "promiscuous", value = PROMISCUOUS),
					@org.osgi.service.metatype.annotations.Option(label = "manual", value = MANUAL) })
	String policy() default PROMISCUOUS;
}
