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

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * A node that only consumes remote services: the same one
 * configuration, minus everything that only a server needs.
 *
 * <p>It has no HTTP stack and no distribution, and it says so: its
 * admin is configured against the distribution that exports nothing,
 * rather than against a reference that happens to find nobody. A node
 * that exports nothing on purpose and a node whose distribution failed
 * to come up should not look the same.
 */
@Designate(ocd = RsaConsumer.Config.class)
@Component(name = RsaConsumer.PID, configurationPid = RsaConsumer.PID,
		configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class RsaConsumer extends NodeConfiguration {

	public static final String PID = "org.eclipse.fennec.services.rsa.consumer";

	@ObjectClassDefinition(name = "Fennec Services RSA Consumer Role",
			description = "Everything a node that only imports services has to decide. The individual "
					+ "configurations for the client, the discovery, the registry, the admin and the "
					+ "topology manager are derived from this one.")
	public @interface Config {

		@AttributeDefinition(name = "Broker URL",
				description = "Where the DDSR broker is. The client and the discovery both use it.")
		String broker_url() default "http://localhost:8887/ddsr/rest";

		@AttributeDefinition(name = "Registry name",
				description = "What this node's local service registry is called.")
		String registry_name() default "rsa-consumer";

		@AttributeDefinition(name = "Default version",
				description = "The version a contract gets when it names none.")
		String default_version() default "1.0.0";

		@AttributeDefinition(name = "Flavor",
				description = "The RSA configuration type this node speaks.")
		String flavor() default "fennec.rest";

		@AttributeDefinition(name = "Import policy",
				description = "promiscuous imports everything anyone waits for; manual imports nothing.")
		String import_policy() default "promiscuous";

		@AttributeDefinition(name = "Export policy",
				description = "Kept for completeness; a consumer has nothing to export, and its admin "
						+ "points at the distribution that exports nothing.")
		String policy() default "promiscuous";

		@AttributeDefinition(name = "Heartbeat seconds",
				description = "How often this node tells the broker it is still there.")
		long provider_heartbeat_seconds() default 30;

		@AttributeDefinition(name = "Session interval seconds",
				description = "The consumer session interval; 0 switches sessions off.")
		long session_interval_seconds() default 0;

		@AttributeDefinition(name = "Consumer id",
				description = "How this node names itself to the broker.")
		String consumer_id() default "";
	}

	@Reference
	private ConfigurationAdmin configAdmin;

	@Activate
	void activate(Config config) {
		start(configAdmin, settings(config));
	}

	@Modified
	void modified(Config config) {
		update(settings(config));
	}

	@Deactivate
	void deactivate() {
		stop();
	}

	@Override
	boolean serves() {
		return false;
	}

	@Override
	List<DerivedConfiguration> plan(RsaSettings settings) {
		return Derivation.forConsumer(settings);
	}

	@Override
	String name() {
		return "RSA consumer";
	}

	static RsaSettings settings(Config config) {
		return new RsaSettings(config.broker_url(), "", "", 0, "", "", false, "",
				config.registry_name(), config.default_version(), config.flavor(),
				config.policy(), config.import_policy(), config.provider_heartbeat_seconds(),
				config.session_interval_seconds(), config.consumer_id());
	}
}
