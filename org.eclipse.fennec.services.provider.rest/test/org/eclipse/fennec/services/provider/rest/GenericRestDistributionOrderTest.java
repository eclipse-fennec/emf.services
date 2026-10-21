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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.fennec.services.client.Registration;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import jakarta.ws.rs.core.Application;

/**
 * FR-P3 both ways for the configuration-driven distribution (#204): it
 * registers its application itself, so it can announce after the
 * whiteboard serves and withdraw before the endpoint goes.
 */
class GenericRestDistributionOrderTest {

	@Test
	@SuppressWarnings("unchecked")
	void onTheWayDownTheWithdrawalComesBeforeTheEndpointGoes() throws Exception {
		GenericRestDistribution distribution = new GenericRestDistribution();
		Registration announced = mock(Registration.class);
		ServiceRegistration<Application> application = mock(ServiceRegistration.class);
		set(distribution, "registration", announced);
		set(distribution, "application", application);

		distribution.deactivate();

		InOrder order = inOrder(announced, application);
		order.verify(announced).withdraw();
		order.verify(application).unregister();
	}

	@Test
	void theRegistrationCarriesWhatDsWouldHavePutOnIt() {
		Dictionary<String, Object> component = new Hashtable<>();
		component.put("osgi.jakartars.application.base", "/persons");
		component.put("fennec.auth", "bearer");
		component.put("ddsr.distribution", "rest");
		component.put("component.id", 17L);
		component.put(".secret", "private by the configuration admin's convention");
		component.put(Constants.SERVICE_ID, 99L);
		component.put(Constants.OBJECTCLASS, new String[] { "x" });

		Dictionary<String, Object> service = GenericRestDistribution.serviceProperties(component);

		assertThat(service.get("osgi.jakartars.application.base")).isEqualTo("/persons");
		assertThat(service.get("fennec.auth")).isEqualTo("bearer");
		assertThat(service.get("ddsr.distribution")).as("what keeps transports from mistaking it for an implementation")
				.isEqualTo("rest");
		assertThat(service.get("component.id")).isEqualTo(17L);
		assertThat(service.get(".secret")).isNull();
		assertThat(service.get(Constants.SERVICE_ID)).isNull();
		assertThat(service.get(Constants.OBJECTCLASS)).isNull();
	}

	private static void set(Object target, String field, Object value) throws Exception {
		Field declared = GenericRestDistribution.class.getDeclaredField(field);
		declared.setAccessible(true);
		declared.set(target, value);
	}
}
