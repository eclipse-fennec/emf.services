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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What counts as the whiteboard serving the address a provider
 * publishes.
 *
 * <p>The first version of this check compared whole URLs and warned on
 * every correct setup, because a whiteboard reports the interfaces it
 * bound and a provider publishes a name. The TCK's own run said so.
 */
class PublicEndpointCheckTest {

	@Test
	@DisplayName("a bound interface serves what localhost publishes, trailing slash and all")
	void hostIsNotCompared() {
		assertThat(PublicEndpointCheck.sameAddress("http://192.168.178.36:9096/services/",
				"http://localhost:9096/services")).isTrue();
	}

	@Test
	@DisplayName("an IPv6 endpoint is no different")
	void ipv6IsNotSpecial() {
		assertThat(PublicEndpointCheck.sameAddress("http://[fd24:865f:7e06:0:ac0d:4ba3:ca81:167]:9096/services/",
				"http://localhost:9096/services")).isTrue();
	}

	@Test
	@DisplayName("another port is a mismatch, which is what a typo looks like")
	void portIsCompared() {
		assertThat(PublicEndpointCheck.sameAddress("http://192.168.178.36:9097/services/",
				"http://localhost:9096/services")).isFalse();
	}

	@Test
	@DisplayName("another context path is a mismatch too")
	void pathIsCompared() {
		assertThat(PublicEndpointCheck.sameAddress("http://192.168.178.36:9096/api/",
				"http://localhost:9096/services")).isFalse();
	}

	@Test
	@DisplayName("an endpoint that is not a URL is simply not a match")
	void unreadableEndpoint() {
		assertThat(PublicEndpointCheck.sameAddress("not a url", "http://localhost:9096/services")).isFalse();
	}
}
