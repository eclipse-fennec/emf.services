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
package org.eclipse.fennec.services.security.jwt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.eclipse.fennec.services.Principal;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.common.CallPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

/**
 * Mode none asks for nothing, and lends nobody's identity: a pooled
 * thread that just served a bearer application must arrive here empty.
 */
class NoAuthFilterTest {

	private final NoAuthFilter filter = new NoAuthFilter();

	@AfterEach
	void clearThread() {
		CallPrincipal.clear();
	}

	@Test
	void aRequestPassesWithoutBeingTouched() {
		ContainerRequestContext request = mock(ContainerRequestContext.class);

		filter.filter(request);

		verifyNoInteractions(request);
	}

	@Test
	void whatThePreviousCallLeftOnTheThreadIsGoneOnTheWayIn() {
		CallPrincipal.set(somebody());

		filter.filter(mock(ContainerRequestContext.class));

		assertThat(CallPrincipal.current()).isNull();
	}

	@Test
	void andOnTheWayOut() {
		CallPrincipal.set(somebody());

		filter.filter(mock(ContainerRequestContext.class), mock(ContainerResponseContext.class));

		assertThat(CallPrincipal.current()).isNull();
	}

	private static Principal somebody() {
		Principal principal = ServicesFactory.eINSTANCE.createPrincipal();
		principal.setSubject("provider-a");
		return principal;
	}
}
