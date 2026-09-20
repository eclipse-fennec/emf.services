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
 * A node that serves services and announces them: one configuration
 * instead of nine.
 *
 * <p>The nine were not independent. The client's broker URL and the
 * discovery's broker URL had to be the same, because discovery talks to
 * the broker through that very client. The distribution's public URL
 * had to agree with the HTTP port and the context path, in three
 * places. The model registry's name was boilerplate that could not be
 * anything else. Stating a fact twice means it can disagree with
 * itself, and nothing said so until a consumer dialled an address
 * nobody served.
 *
 * <p>So this states each fact once and derives the rest. What a
 * deployment still decides is here; what follows from a decision is in
 * {@link Derivation}.
 */
@Designate(ocd = RsaProvider.Config.class)
@Component(name = RsaProvider.PID, configurationPid = RsaProvider.PID,
		configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class RsaProvider extends NodeConfiguration {

	public static final String PID = "org.eclipse.fennec.services.rsa.provider";

	@ObjectClassDefinition(name = "Fennec Services RSA Provider Role",
			description = "Everything a node that exports services has to decide. The individual "
					+ "configurations for the client, the transports, the registry, the admin and the "
					+ "topology manager are derived from this one.")
	public @interface Config {

		@AttributeDefinition(name = "Broker URL",
				description = "Where the DDSR broker is. The client and the discovery both use it, "
						+ "because discovery talks to the broker through that very client.")
		String broker_url() default "http://localhost:8887/ddsr/rest";

		@AttributeDefinition(name = "HTTP port",
				description = "The port this node serves its endpoints on.")
		int http_port() default 8080;

		@AttributeDefinition(name = "HTTP host",
				description = "The interface to bind.")
		String http_host() default "0.0.0.0";

		@AttributeDefinition(name = "Context path",
				description = "The servlet context this node's endpoints live under.")
		String context_path() default "services";

		@AttributeDefinition(name = "Manage the HTTP stack",
				description = "Whether this node writes the HTTP runtime and whiteboard configurations. "
						+ "Turn it off where something else already owns them; then state the public URL.")
		boolean manage_http() default true;

		@AttributeDefinition(name = "HTTP id",
				description = "Ties the whiteboard to the HTTP runtime this node writes. Only matters "
						+ "where several HTTP runtimes exist in one framework.")
		String http_id() default "ddsrHttp";

		@AttributeDefinition(name = "Public URL",
				description = "The address consumers should dial. Leave empty to derive it from the port "
						+ "and the context path, which is right unless something sits in front of this node.")
		String public_url() default "";

		@AttributeDefinition(name = "Public host",
				description = "The host name to derive the public URL from. Leave empty for localhost.")
		String public_host() default "";

		@AttributeDefinition(name = "Registry name",
				description = "What this node's local service registry is called.")
		String registry_name() default "rsa-provider";

		@AttributeDefinition(name = "Default version",
				description = "The version a contract gets when it names none.")
		String default_version() default "1.0.0";

		@AttributeDefinition(name = "Flavor",
				description = "The RSA configuration type this node speaks. One admin is configured per "
						+ "flavor, which is what makes a second one a second configuration rather than a "
						+ "change to the admin.")
		String flavor() default "fennec.rest";

		@AttributeDefinition(name = "Export policy",
				description = "promiscuous exports everything that asks; manual exports nothing on its own.")
		String policy() default "promiscuous";

		@AttributeDefinition(name = "Import policy",
				description = "promiscuous imports everything anyone waits for; manual imports nothing.")
		String import_policy() default "promiscuous";

		@AttributeDefinition(name = "Heartbeat seconds",
				description = "How often this node tells the broker it is still there.")
		long provider_heartbeat_seconds() default 30;

		@AttributeDefinition(name = "Session interval seconds",
				description = "The consumer session interval; 0 switches sessions off.")
		long session_interval_seconds() default 0;

		@AttributeDefinition(name = "Consumer id",
				description = "How this node names itself to the broker. Empty lets the client choose.")
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
		return true;
	}

	@Override
	List<DerivedConfiguration> plan(RsaSettings settings) {
		return Derivation.forProvider(settings);
	}

	@Override
	String name() {
		return "RSA provider";
	}

	static RsaSettings settings(Config config) {
		return new RsaSettings(config.broker_url(), config.public_url(), config.public_host(),
				config.http_port(), config.http_host(), config.context_path(), config.manage_http(),
				config.http_id(), config.registry_name(), config.default_version(), config.flavor(),
				config.policy(), config.import_policy(), config.provider_heartbeat_seconds(),
				config.session_interval_seconds(), config.consumer_id());
	}
}
