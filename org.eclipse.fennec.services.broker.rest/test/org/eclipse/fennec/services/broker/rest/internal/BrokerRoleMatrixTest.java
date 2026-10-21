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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.ConnectionScheme;
import org.eclipse.fennec.services.MutualTlsScheme;
import org.eclipse.fennec.services.Principal;
import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.SecuritySchemeKind;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.common.CallPrincipal;
import org.eclipse.fennec.services.common.SecurityRequirements;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The broker's role matrix (SECURITY.md §3), as its own contract documents
 * state it — pinned operation by operation, because a role dropped from a
 * document is an operation anybody may call.
 */
class BrokerRoleMatrixTest {

	private final RestTestSupport.ResourceSets rsObjects = new RestTestSupport.ResourceSets();

	@AfterEach
	void clearThread() {
		CallPrincipal.clear();
	}

	static Stream<Arguments> matrix() {
		return Stream.of(
				Arguments.of("broker-catalog-api.xmi", "listCatalog", List.of()),
				Arguments.of("broker-catalog-api.xmi", "getCatalogEntry", List.of()),
				Arguments.of("broker-catalog-api.xmi", "addCatalogEntry", List.of(BrokerRoles.PROVIDER)),
				Arguments.of("broker-catalog-api.xmi", "deprecateCatalogEntry", List.of(BrokerRoles.ADMIN)),
				Arguments.of("broker-catalog-api.xmi", "removeCatalogEntry", List.of(BrokerRoles.ADMIN)),
				Arguments.of("broker-implementations-api.xmi", "publishImplementation", List.of(BrokerRoles.PROVIDER)),
				Arguments.of("broker-implementations-api.xmi", "modifyImplementation", List.of(BrokerRoles.PROVIDER)),
				Arguments.of("broker-implementations-api.xmi", "withdrawImplementation", List.of(BrokerRoles.PROVIDER)),
				Arguments.of("broker-lookup-api.xmi", "getServiceReferences", List.of(BrokerRoles.CONSUMER)),
				Arguments.of("broker-lookup-api.xmi", "heartbeat", List.of(BrokerRoles.PROVIDER)));
	}

	@ParameterizedTest(name = "{1} needs {2}")
	@MethodSource("matrix")
	void everyOperationAsksForItsRole(String document, String operation, List<String> roles) throws IOException {
		ServiceOperation op = operation(document, operation);

		assertThat(SecurityRequirements.demandsAuthentication(op)).as("nothing of the broker is anonymous").isTrue();
		SecurityRequirement requirement = SecurityRequirements.effective(op);
		assertThat(requirement.getScopes()).containsExactlyElementsOf(roles);
	}

	@Test
	void theHandWrittenResourcesAskByTheSameRule() {
		assertThat(BrokerRoles.refusal(BrokerRoles.ADMIN)).as("no principal: mode none").isNull();

		CallPrincipal.set(principal(BrokerRoles.CONSUMER));
		assertThat(BrokerRoles.refusal(BrokerRoles.CONSUMER)).isNull();
		assertThat(BrokerRoles.refusal(BrokerRoles.ADMIN).getStatus()).isEqualTo(403);
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

	private ServiceOperation operation(String document, String name) throws IOException {
		try (InputStream in = Files.newInputStream(Path.of("resources", document))) {
			List<EObject> roots = XmiCodec.readBundle(in, rsObjects).roots();
			ServiceInterface contract = (ServiceInterface) roots.get(1);
			return contract.getOperations().stream().filter(op -> name.equals(op.getName())).findFirst()
					.orElseThrow();
		}
	}

	private static Principal principal(String... roles) {
		Principal principal = ServicesFactory.eINSTANCE.createPrincipal();
		principal.setSubject("java-client");
		principal.getScopes().addAll(List.of(roles));
		return principal;
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
