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
package org.eclipse.fennec.services.broker.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.fennec.services.security.jwt.SecurityNames;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * The broker states one authentication mode for everything it serves
 * over REST (SECURITY.md §7a).
 *
 * <p>Four applications carry it — the default application with the
 * hand-written resources, and the three contracts the generic
 * distribution serves — and the danger is not one of them being wrong
 * but them disagreeing: a broker whose catalog asks for a token while
 * its implementations do not. So the four are held to the very same
 * placeholder, in both properties the mode needs.
 *
 * <p>That the hand-written resources require a mode is not asserted
 * here: whiteboard property types have class retention, so the
 * annotation is not visible to a plain test. The component XML carries
 * it, and the broker's live behaviour — 401 without a token, nothing
 * served without a verifier — is what shows it works.
 */
class BrokerAuthenticationTest {

	private static final String MODE = "$[env:DDSR_AUTH_MODE;default=$[prop:fennec.auth.mode;default=bearer]]";

	static Stream<String> brokerApplications() {
		return Stream.of(
				"org.eclipse.fennec.services.broker.rest.application",
				"org.eclipse.fennec.services.provider.rest~brokerCatalog",
				"org.eclipse.fennec.services.provider.rest~brokerImplementations",
				"org.eclipse.fennec.services.provider.rest~brokerLookup");
	}

	@ParameterizedTest
	@MethodSource("brokerApplications")
	void everyBrokerApplicationStatesTheSameModeAndRequiresItsFilter(String pid) throws IOException {
		JsonObject configuration = config().getJsonObject(pid);

		assertThat(configuration).as(pid).isNotNull();
		assertThat(configuration.getString(SecurityNames.AUTH)).isEqualTo(MODE);
		assertThat(configuration.getString("osgi.jakartars.extension.select"))
				.isEqualTo(SecurityNames.selectFilterFor(MODE));
	}

	private static JsonObject config() throws IOException {
		try (Reader in = Files.newBufferedReader(Path.of("configs", "config.json"));
				JsonReader json = Json.createReader(in)) {
			return json.readObject();
		}
	}
}
