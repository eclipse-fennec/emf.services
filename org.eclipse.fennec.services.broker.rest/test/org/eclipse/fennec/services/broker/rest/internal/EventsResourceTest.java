/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.broker.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.fennec.services.FlavorKind;
import org.junit.jupiter.api.Test;

/** GET /events?flavors= parsing (#56). */
class EventsResourceTest {

	@Test
	void csvIsParsedCaseInsensitivelyIgnoringBlanksAndUnknownTokens() {
		assertThat(EventsResource.parseFlavors(" rest, MQTT ,, grpc ")).containsExactly(FlavorKind.REST, FlavorKind.MQTT);
	}

	@Test
	void absentOrBlankMeansNoFilter() {
		assertThat(EventsResource.parseFlavors(null)).isEmpty();
		assertThat(EventsResource.parseFlavors("  ")).isEmpty();
	}
}
