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
package org.eclipse.fennec.services.broker.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;

import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.ConnectionScheme;
import org.eclipse.fennec.services.MutualTlsScheme;
import org.eclipse.fennec.services.SecuritySchemeKind;
import org.junit.jupiter.api.Test;

/**
 * The scheme the broker's own flavors name, per mode (SECURITY.md §7c) —
 * the deployment fact the self-publisher adds to the documents.
 */
class BrokerSelfPublisherSchemeTest {

	@Test
	void onlyModeNoneIsAnOpenBroker() {
		assertThat(BrokerSelfPublisher.isOpen(config("none", ""))).isTrue();
		assertThat(BrokerSelfPublisher.isOpen(config(" none ", ""))).isTrue();
		assertThat(BrokerSelfPublisher.isOpen(config("bearer", "http://localhost:18080/realms/fennec"))).isFalse();
		assertThat(BrokerSelfPublisher.isOpen(config("mtls", ""))).isFalse();
		assertThat(BrokerSelfPublisher.isOpen(config(null, ""))).isFalse();
	}

	@Test
	void anOpenBrokerSaysThatNothingChecksPerCall() {
		assertThat(BrokerSelfPublisher.schemeOf(config("none", ""))).isInstanceOf(ConnectionScheme.class);
	}

	@Test
	void aSecuredBrokerNamesItsIssuer() {
		assertThat(BrokerSelfPublisher.schemeOf(config("bearer", "http://localhost:18080/realms/fennec")))
				.isInstanceOfSatisfying(BearerTokenScheme.class, scheme -> {
					assertThat(scheme.getIssuer()).isEqualTo("http://localhost:18080/realms/fennec");
					assertThat(scheme.getAudiences()).containsExactly("ddsr-broker");
				});
	}

	@Test
	void aBrokerOfClientCertificatesNamesTheAuthoritiesItTrusts() {
		assertThat(BrokerSelfPublisher.schemeOf(config("mtls", "")))
				.isInstanceOfSatisfying(MutualTlsScheme.class, scheme -> {
					assertThat(scheme.getKind()).isEqualTo(SecuritySchemeKind.MUTUAL_TLS);
					assertThat(scheme.getTrustedIssuers()).containsExactly("CN=Fennec Harness CA,O=Fennec");
				});
	}

	@Test
	void aSecuredBrokerWithoutAnIssuerNamesNothing() {
		assertThat(BrokerSelfPublisher.schemeOf(config("bearer", ""))).isNull();
	}

	private static BrokerSelfPublisher.Config config(String mode, String issuer) {
		return new BrokerSelfPublisher.Config() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return BrokerSelfPublisher.Config.class;
			}

			@Override
			public String public_url() {
				return "http://localhost:8887/ddsr/rest";
			}

			@Override
			public String auth_mode() {
				return mode;
			}

			@Override
			public String issuer() {
				return issuer;
			}

			@Override
			public String[] audiences() {
				return new String[] { "ddsr-broker" };
			}

			@Override
			public String jwks_uri() {
				return "";
			}

			@Override
			public String scope_claim() {
				return "realm_access.roles";
			}

			@Override
			public String[] trusted_issuers() {
				return new String[] { "CN=Fennec Harness CA,O=Fennec", " " };
			}
		};
	}
}
