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
package org.eclipse.fennec.services.rsa.distribution.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.provider.rest.RestDistribution;
import org.eclipse.fennec.services.rsa.spi.FlavorDistribution;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

/**
 * The node's token issuer, said in the flavor of a guarded contract — and
 * nowhere else (SECURITY.md §6).
 */
class RestDistributionProviderSecurityTest {

	/** Remembers what it was asked to serve. */
	private static final class Serving implements RestDistribution {

		RestFlavor served;

		@Override
		public Served serve(RestFlavor flavor, Object service, String name) {
			this.served = flavor;
			return new Served() {
				@Override
				public String basePath() {
					return flavor.getBasePath();
				}

				@Override
				public void close() {
					// nothing mounted
				}
			};
		}
	}

	@Test
	void aGuardedContractIsServedWithTheNodesBearerScheme() {
		Serving serving = new Serving();
		RestDistributionProvider provider = provider(serving, "http://localhost:18080/realms/fennec");

		provider.export(new Object(), contract(true), Map.of());

		assertThat(serving.served.getSecuritySchemes()).singleElement().isInstanceOfSatisfying(BearerTokenScheme.class,
				scheme -> {
					assertThat(scheme.getIssuer()).isEqualTo("http://localhost:18080/realms/fennec");
					assertThat(scheme.getAudiences()).containsExactly("rsa-example");
					assertThat(scheme.getScopeClaim()).isEqualTo("realm_access.roles");
				});
		assertThat(provider.supportedIntents()).containsExactly("osgi.basic", FlavorDistribution.BEARER_INTENT);
	}

	@Test
	void anOpenContractStaysWhatItWas() {
		Serving serving = new Serving();

		provider(serving, "http://localhost:18080/realms/fennec").export(new Object(), contract(false), Map.of());

		assertThat(serving.served.getSecuritySchemes()).isEmpty();
	}

	@Test
	void withoutAnIssuerNothingIsPromisedAndNoSchemeIsInvented() {
		Serving serving = new Serving();
		RestDistributionProvider provider = provider(serving, "");

		provider.export(new Object(), contract(true), Map.of());

		assertThat(serving.served.getSecuritySchemes())
				.as("serving refuses a guarded contract without one — fail closed, not here").isEmpty();
		assertThat(provider.supportedIntents()).containsExactly("osgi.basic");
	}

	private static RestDistributionProvider provider(RestDistribution distribution, String issuer) {
		RestDistributionProvider provider = new RestDistributionProvider();
		try {
			Field field = RestDistributionProvider.class.getDeclaredField("distribution");
			field.setAccessible(true);
			field.set(provider, distribution);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		provider.activate(context(), config(issuer));
		return provider;
	}

	private static BundleContext context() {
		return (BundleContext) Proxy.newProxyInstance(BundleContext.class.getClassLoader(),
				new Class<?>[] { BundleContext.class }, (proxy, method, args) -> "getProperty".equals(method.getName())
						? "0f3c9a5e-0000-0000-0000-000000000000" : null);
	}

	private static RestDistributionProvider.Config config(String issuer) {
		return new RestDistributionProvider.Config() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return RestDistributionProvider.Config.class;
			}

			@Override
			public String public_url() {
				return "http://localhost:9095/services";
			}

			@Override
			public String issuer() {
				return issuer;
			}

			@Override
			public String[] audiences() {
				return new String[] { "rsa-example" };
			}

			@Override
			public String jwks_uri() {
				return "http://localhost:18080/realms/fennec/protocol/openid-connect/certs";
			}

			@Override
			public String scope_claim() {
				return "realm_access.roles";
			}
		};
	}

	private static ServiceInterface contract(boolean guarded) {
		ServiceInterface contract = ServicesFactory.eINSTANCE.createServiceInterface();
		contract.setName("Greeter");
		contract.setVersion("1.0.0");
		ServiceOperation greet = ServicesFactory.eINSTANCE.createServiceOperation();
		greet.setName("greet");
		contract.getOperations().add(greet);
		if (guarded) {
			SecurityRequirement requirement = ServicesFactory.eINSTANCE.createSecurityRequirement();
			requirement.setAuthenticated(true);
			requirement.getScopes().add("greeter");
			contract.setSecurity(requirement);
		}
		return contract;
	}
}
