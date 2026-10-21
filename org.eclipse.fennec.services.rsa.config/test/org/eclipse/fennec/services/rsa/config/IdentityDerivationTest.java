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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Who a node is, said once in its role and derived into the token
 * supplier, the verifier, the distribution and the client (SECURITY.md §6).
 */
class IdentityDerivationTest {

	private static final String REALM = "http://localhost:18080/realms/fennec";

	private static final NodeIdentity IDENTITY = new NodeIdentity(REALM + "/protocol/openid-connect/token",
			"rsa-example", "rsa-example-secret", "", REALM, REALM + "/protocol/openid-connect/certs",
			List.of("rsa-example"), "realm_access.roles", "azp");

	@Test
	void theIdentityComesFirstSoItGoesLast() {
		List<DerivedConfiguration> plan = Derivation.forProvider(provider(IDENTITY, ""));

		assertThat(plan.get(0).label()).isEqualTo(Derivation.OAUTH_PID);
		assertThat(plan.get(1).label()).isEqualTo(Derivation.VERIFIER_PID);
	}

	@Test
	void theTokenSupplierGetsTheClientCredentials() {
		assertThat(byLabel(Derivation.forProvider(provider(IDENTITY, "")), Derivation.OAUTH_PID).properties())
				.containsEntry("token.uri", REALM + "/protocol/openid-connect/token")
				.containsEntry("client.id", "rsa-example")
				.containsEntry("client.secret", "rsa-example-secret");
	}

	@Test
	void theVerifierAndTheDistributionNameTheSameIssuer() {
		List<DerivedConfiguration> plan = Derivation.forProvider(provider(IDENTITY, ""));

		assertThat(byLabel(plan, Derivation.VERIFIER_PID).properties())
				.containsEntry("issuer", REALM)
				.containsEntry("scope.claim", "realm_access.roles")
				.containsEntry("subject.claim", "azp");
		assertThat((String[]) byLabel(plan, Derivation.VERIFIER_PID).properties().get("audiences"))
				.containsExactly("rsa-example");
		assertThat(byLabel(plan, Derivation.DISTRIBUTION_PID).properties())
				.as("what a guarded flavor tells a consumer is what this node accepts")
				.containsEntry("issuer", REALM)
				.containsEntry("jwks.uri", REALM + "/protocol/openid-connect/certs")
				.containsEntry("scope.claim", "realm_access.roles");
	}

	@Test
	void theClientIdIsTheConsumerIdUnlessOneIsSaid() {
		assertThat(byLabel(Derivation.forProvider(provider(IDENTITY, "")), Derivation.CLIENT_PID).properties())
				.containsEntry("consumer.id", "rsa-example");
		assertThat(byLabel(Derivation.forProvider(provider(IDENTITY, "node-a-consumer")), Derivation.CLIENT_PID)
				.properties()).containsEntry("consumer.id", "node-a-consumer");
	}

	@Test
	void aConsumingNodeShowsATokenButVerifiesNothing() {
		List<DerivedConfiguration> plan = Derivation.forConsumer(provider(IDENTITY, ""));

		assertThat(plan.get(0).label()).isEqualTo(Derivation.OAUTH_PID);
		assertThat(plan).noneMatch(entry -> entry.label().equals(Derivation.VERIFIER_PID));
	}

	@Test
	void aNodeWithoutAnIdentityIsWhatItWas() {
		List<DerivedConfiguration> plan = Derivation.forProvider(provider(NodeIdentity.NONE, ""));

		assertThat(plan).noneMatch(entry -> entry.label().equals(Derivation.OAUTH_PID)
				|| entry.label().equals(Derivation.VERIFIER_PID));
		assertThat(byLabel(plan, Derivation.DISTRIBUTION_PID).properties()).containsOnlyKeys("public.url");
		assertThat(byLabel(plan, Derivation.CLIENT_PID).properties()).doesNotContainKey("consumer.id");
	}

	@Test
	void halfAnIdentityIsAProblemBeforeAnythingIsWritten() {
		NodeIdentity noSecret = new NodeIdentity(REALM + "/protocol/openid-connect/token", "rsa-example", "", "",
				"", "", List.of(), "", "");
		NodeIdentity noAudience = new NodeIdentity("", "", "", "", REALM, REALM + "/certs", List.of(), "", "");

		assertThat(SettingsChecks.problems(provider(noSecret, ""), true))
				.anyMatch(problem -> problem.contains("client.secret"));
		assertThat(SettingsChecks.problems(provider(noAudience, ""), true))
				.anyMatch(problem -> problem.contains("audiences"));
		assertThat(SettingsChecks.problems(provider(IDENTITY, ""), true)).isEmpty();
	}

	@Test
	void audiencesReadBackAsANewArrayAreNotAChange() {
		// Configuration Admin hands back a new array; equal content must not
		// rewrite — and reconfigure the verifier — on every apply.
		assertThat(DerivedConfigurations.same(Map.of("audiences", new String[] { "rsa-example" }),
				Map.of("audiences", new String[] { "rsa-example" }))).isTrue();
		assertThat(DerivedConfigurations.same(Map.of("audiences", new String[] { "rsa-example" }),
				Map.of("audiences", new String[] { "other" }))).isFalse();
	}

	private static RsaSettings provider(NodeIdentity identity, String consumerId) {
		return new RsaSettings("http://broker:8887/ddsr/rest", "", "", 9095, "0.0.0.0", "services",
				true, "ddsrHttp", "node-a", "1.0.0", "fennec.rest", "promiscuous", "promiscuous",
				30, 0, consumerId, "tcp://localhost:1883", "ddsr/rpc", "", "", identity);
	}

	private static DerivedConfiguration byLabel(List<DerivedConfiguration> plan, String label) {
		return plan.stream().filter(entry -> entry.label().equals(label)).findFirst().orElseThrow();
	}
}
