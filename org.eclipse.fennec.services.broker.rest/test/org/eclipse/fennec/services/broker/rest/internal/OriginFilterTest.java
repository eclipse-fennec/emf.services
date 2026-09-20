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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.fennec.services.common.CallOrigin;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * The transport half of #125: the header becomes the bound origin of
 * the call, and nothing survives the call.
 *
 * <p>The leak case is the one worth a test. Request threads are pooled,
 * so an origin left behind by one request would be attributed to the
 * next caller on the same thread — an audit record naming the wrong
 * system is worse than one naming none.
 */
class OriginFilterTest {

	private static final String TOKEN = "payments-prod-eu/5f2b8c1e-0f4a-4a6e-9b3d-7c1a2e8f4d60";

	private final OriginFilter filter = new OriginFilter();

	@AfterEach
	void tearDown() {
		CallOrigin.clear();
	}

	@Test
	@DisplayName("binds the origin the request carried")
	void bindsTheHeader() {
		filter.filter(requestWith(TOKEN));

		assertThat(CallOrigin.current()).isNotNull();
		assertThat(CallOrigin.current().token()).isEqualTo(TOKEN);
		assertThat(CallOrigin.requestor(null)).isEqualTo(TOKEN);
	}

	@Test
	@DisplayName("a request without the header leaves nothing bound")
	void withoutTheHeaderNothingIsBound() {
		filter.filter(requestWith(null));

		assertThat(CallOrigin.current()).isNull();
		assertThat(CallOrigin.requestor(null)).isEqualTo(ClientOrigin.ANONYMOUS);
	}

	@Test
	@DisplayName("a request without the header does not inherit the previous caller's origin")
	void aPooledThreadDoesNotInherit() {
		filter.filter(requestWith(TOKEN));

		filter.filter(requestWith(null));

		assertThat(CallOrigin.current()).isNull();
		assertThat(CallOrigin.requestor(null)).isEqualTo(ClientOrigin.ANONYMOUS);
	}

	@Test
	@DisplayName("the binding is released when the response goes out")
	void responseReleasesTheBinding() {
		filter.filter(requestWith(TOKEN));

		filter.filter(requestWith(TOKEN), null);

		assertThat(CallOrigin.current()).isNull();
	}

	private static ContainerRequestContext requestWith(String token) {
		ContainerRequestContext request = mock(ContainerRequestContext.class);
		when(request.getHeaderString(ClientOrigin.HEADER)).thenReturn(token);
		return request;
	}
}
