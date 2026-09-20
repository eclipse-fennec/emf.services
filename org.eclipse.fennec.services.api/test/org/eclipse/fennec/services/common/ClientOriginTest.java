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

package org.eclipse.fennec.services.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What an origin says, and what it survives.
 *
 * <p>The token is an audit record that travels in a header, so the two
 * properties that matter are that it round-trips and that nothing a
 * deployment can configure makes it unreadable for the next reader.
 */
class ClientOriginTest {

	private static final String UUID_LIKE = "5f2b8c1e-0f4a-4a6e-9b3d-7c1a2e8f4d60";

	@Test
	@DisplayName("carries the label and the framework uuid")
	void labelAndFrameworkUuid() {
		ClientOrigin origin = ClientOrigin.of("payments-prod-eu", UUID_LIKE);

		assertThat(origin.label()).isEqualTo("payments-prod-eu");
		assertThat(origin.runtimeId()).isEqualTo(UUID_LIKE);
		assertThat(origin.token()).isEqualTo("payments-prod-eu/" + UUID_LIKE);
	}

	@Test
	@DisplayName("outside a framework the runtime id is the JVM's, and it is stable")
	void withoutAFrameworkTheJvmIdIsUsed() {
		ClientOrigin first = ClientOrigin.of("a-client", null);
		ClientOrigin second = ClientOrigin.of("a-client", "  ");

		assertThat(first.runtimeId()).isNotBlank();
		assertThat(second.runtimeId()).isEqualTo(first.runtimeId());
	}

	@Test
	@DisplayName("a client that did not name itself is still distinguishable from one that is not there")
	void missingLabelBecomesUnnamed() {
		assertThat(ClientOrigin.of(null, UUID_LIKE).label()).isEqualTo(ClientOrigin.UNNAMED);
		assertThat(ClientOrigin.of("   ", UUID_LIKE).label()).isEqualTo(ClientOrigin.UNNAMED);
	}

	@Test
	@DisplayName("round-trips through the wire token")
	void roundTrip() {
		ClientOrigin origin = ClientOrigin.of("payments-prod-eu", UUID_LIKE);

		assertThat(ClientOrigin.parse(origin.token())).isEqualTo(origin);
	}

	@Test
	@DisplayName("nothing sent is nothing to record")
	void nothingSent() {
		assertThat(ClientOrigin.parse(null)).isNull();
		assertThat(ClientOrigin.parse("  ")).isNull();
	}

	@Test
	@DisplayName("a token without a runtime id is kept as a label, not refused")
	void labelOnlyTokenSurvives() {
		ClientOrigin origin = ClientOrigin.parse("some-old-client");

		assertThat(origin.label()).isEqualTo("some-old-client");
		assertThat(origin.runtimeId()).isEmpty();
		assertThat(origin.token()).isEqualTo("some-old-client");
	}

	@Test
	@DisplayName("a label containing the separator cannot make the token unreadable")
	void separatorInTheLabelIsNeutralised() {
		ClientOrigin origin = ClientOrigin.of("team/payments", UUID_LIKE);

		assertThat(origin.label()).isEqualTo("team_payments");
		assertThat(ClientOrigin.parse(origin.token())).isEqualTo(origin);
	}

	@Test
	@DisplayName("a label with a newline cannot smuggle a second header")
	void controlCharactersAreNeutralised() {
		ClientOrigin origin = ClientOrigin.of("evil\r\nX-Other: value", UUID_LIKE);

		assertThat(origin.token()).doesNotContain("\r").doesNotContain("\n");
		assertThat(ClientOrigin.parse(origin.token())).isEqualTo(origin);
	}

	@Test
	@DisplayName("the runtime id splits off at the first separator, whatever follows")
	void parseSplitsAtTheFirstSeparator() {
		ClientOrigin origin = ClientOrigin.parse("a-label/" + UUID_LIKE + "/trailing");

		assertThat(origin.label()).isEqualTo("a-label");
		assertThat(origin.runtimeId()).isEqualTo(UUID_LIKE + "/trailing");
	}
}
