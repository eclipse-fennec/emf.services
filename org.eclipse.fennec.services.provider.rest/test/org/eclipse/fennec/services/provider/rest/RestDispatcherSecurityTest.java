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
package org.eclipse.fennec.services.provider.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.Principal;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.common.CallPrincipal;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * The contract's requirements held against the verified caller, operation
 * by operation (SECURITY.md §4) — and how a served contract decides
 * whether they are held at all.
 */
class RestDispatcherSecurityTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	/** The service behind the contract. */
	public static final class Payments {

		public String getBalance() {
			return "1234";
		}

		public String charge() {
			return "1000";
		}

		public String ping() {
			return "pong";
		}
	}

	@AfterEach
	void clearThread() {
		CallPrincipal.clear();
	}

	// ------------------------------------------------------------------
	// the dispatcher

	@Test
	void withoutAPrincipalAGuardedOperationIs401() {
		Response answer = call(true, "/balance");

		assertThat(answer.getStatus()).isEqualTo(401);
		assertThat(answer.getHeaderString(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
	}

	@Test
	void aPrincipalWithoutTheRoleIs403AndHearsWhichOne() {
		CallPrincipal.set(principal("payment-reader"));

		Response answer = call(true, "/charge");

		assertThat(answer.getStatus()).isEqualTo(403);
		assertThat(answer.getEntity()).isEqualTo("charge needs payment-writer");
	}

	@Test
	void aPrincipalWithTheRoleIsServed() {
		CallPrincipal.set(principal("payment-reader"));

		assertThat(call(true, "/balance").getStatus()).isEqualTo(200);
	}

	@Test
	void anOperationThatPermitsAllNeedsNoPrincipal() {
		assertThat(call(true, "/ping").getStatus()).isEqualTo(200);
	}

	@Test
	void inModeNoneTheRequirementsArePassedOver() {
		assertThat(call(false, "/charge").getStatus()).isEqualTo(200);
	}

	// ------------------------------------------------------------------
	// the mode a served contract runs in

	@Test
	void aConfiguredBearerModeIsEnforced() {
		assertThat(ServedSecurity.enforcedByConfiguration("Payment", flavor(), "bearer")).isTrue();
	}

	@Test
	void aGuardedContractUnderNoStatedModeIsNotServed() {
		assertThatThrownBy(() -> ServedSecurity.enforcedByConfiguration("Payment", flavor(), null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("fennec.auth");
	}

	@Test
	void openHasToBeSaid() {
		assertThat(ServedSecurity.enforcedByConfiguration("Payment", flavor(), "none")).isFalse();
	}

	@Test
	void aBearerSchemeOnTheFlavorPutsTheModeOnTheApplication() {
		RestFlavor flavor = flavor();
		flavor.getSecuritySchemes().add(F.createBearerTokenScheme());
		Hashtable<String, Object> properties = new Hashtable<>();

		assertThat(ServedSecurity.enforcedByFlavor("Payment", flavor, properties)).isTrue();
		assertThat(properties)
				.as("ping permits all, so anonymous callers must reach the dispatcher")
				.containsEntry("fennec.auth", "bearer-optional")
				.containsEntry("osgi.jakartars.extension.select", "(osgi.jakartars.name=fennec-auth-bearer-optional)");
	}

	@Test
	void whenEveryOperationIsGuardedTheStrictModeIsUsed() {
		RestFlavor flavor = flavor();
		flavor.getOperationFlavors().get(2).getOperation().setSecurity(null);
		flavor.getSecuritySchemes().add(F.createBearerTokenScheme());
		Hashtable<String, Object> properties = new Hashtable<>();

		ServedSecurity.enforcedByFlavor("Payment", flavor, properties);

		assertThat(properties).containsEntry("fennec.auth", "bearer");
	}

	@Test
	void theOptionalModeIsEnforcedFromConfigurationToo() {
		assertThat(ServedSecurity.enforcedByConfiguration("Payment", flavor(), "bearer-optional")).isTrue();
	}

	@Test
	void aClientCertificateSchemeServesInModeMtls() {
		RestFlavor flavor = flavor();
		flavor.getSecuritySchemes().add(F.createMutualTlsScheme());
		Hashtable<String, Object> properties = new Hashtable<>();

		assertThat(ServedSecurity.enforcedByFlavor("Payment", flavor, properties)).isTrue();
		assertThat(properties)
				.as("a certificate belongs to the connection: there is no optional variant, even with ping open")
				.containsEntry("fennec.auth", "mtls")
				.containsEntry("osgi.jakartars.extension.select", "(osgi.jakartars.name=fennec-auth-mtls)");
	}

	@Test
	void modeMtlsIsEnforcedFromConfigurationToo() {
		assertThat(ServedSecurity.enforcedByConfiguration("Payment", flavor(), "mtls")).isTrue();
	}

	@Test
	void aGuardedContractOnAFlavorWithoutASchemeIsNotServed() {
		assertThatThrownBy(() -> ServedSecurity.enforcedByFlavor("Payment", flavor(), new Hashtable<>()))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void behindAConnectionSchemeSomethingInFrontChecks() {
		RestFlavor flavor = flavor();
		flavor.getSecuritySchemes().add(F.createConnectionScheme());
		Hashtable<String, Object> properties = new Hashtable<>();

		assertThat(ServedSecurity.enforcedByFlavor("Payment", flavor, properties)).isFalse();
		assertThat(properties).isEmpty();
	}

	@Test
	void anOpenContractIsServedAsBefore() {
		RestFlavor flavor = flavor();
		flavor.getOperationFlavors().forEach(of -> of.getOperation().setSecurity(null));
		((ServiceInterface) flavor.getOperationFlavors().get(0).getOperation().eContainer()).setSecurity(null);
		Hashtable<String, Object> properties = new Hashtable<>();

		assertThat(ServedSecurity.enforcedByFlavor("Payment", flavor, properties)).isFalse();
		assertThat(properties).as("nothing an RSA child framework without the filters would trip over").isEmpty();
	}

	// ------------------------------------------------------------------

	private static Response call(boolean enforce, String path) {
		RestDispatcher dispatcher = new RestDispatcher(flavor(), () -> new One(new Payments()), "Payment",
				new NoResourceSets(), CallTracer.NONE, enforce);
		return dispatcher.dispatch("GET", path, name -> List.of(), Map.<String, String>of()::get, null);
	}

	private static Principal principal(String... scopes) {
		Principal principal = F.createPrincipal();
		principal.setSubject("payments-node");
		principal.getScopes().addAll(List.of(scopes));
		return principal;
	}

	/**
	 * Payment with a reader default, charge overriding it with writer, and
	 * ping permitting all — the shape @RolesAllowed and @PermitAll derive.
	 */
	private static RestFlavor flavor() {
		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Payment");
		contract.setSecurity(requirement(true, "payment-reader"));
		RestFlavor flavor = F.createRestFlavor();
		flavor.setName("payment-rest");
		add(flavor, contract, "getBalance", "/balance", null);
		add(flavor, contract, "charge", "/charge", requirement(true, "payment-writer"));
		add(flavor, contract, "ping", "/ping", requirement(false));
		return flavor;
	}

	private static void add(RestFlavor flavor, ServiceInterface contract, String name, String path,
			SecurityRequirement security) {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName(name);
		operation.setSecurity(security);
		contract.getOperations().add(operation);
		RestOperationFlavor operationFlavor = F.createRestOperationFlavor();
		operationFlavor.setName(name);
		operationFlavor.setMethod(HttpMethod.GET);
		operationFlavor.setPath(path);
		operationFlavor.setOperation(operation);
		operationFlavor.getProduces().add("text/plain");
		flavor.getOperationFlavors().add(operationFlavor);
	}

	private static SecurityRequirement requirement(boolean authenticated, String... scopes) {
		SecurityRequirement requirement = F.createSecurityRequirement();
		requirement.setAuthenticated(authenticated);
		requirement.getScopes().addAll(List.of(scopes));
		return requirement;
	}

	private static final class NoResourceSets implements ComponentServiceObjects<ResourceSet> {

		@Override
		public ResourceSet getService() {
			throw new UnsupportedOperationException("not needed for these tests");
		}

		@Override
		public void ungetService(ResourceSet service) {
			// nothing was handed out
		}

		@Override
		public ServiceReference<ResourceSet> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	private static final class One implements ServiceObjects<Object> {

		private final Object service;

		One(Object service) {
			this.service = service;
		}

		@Override
		public Object getService() {
			return service;
		}

		@Override
		public void ungetService(Object instance) {
			// shared instance
		}

		@Override
		public ServiceReference<Object> getServiceReference() {
			return null;
		}
	}
}
