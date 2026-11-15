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

package org.eclipse.fennec.services.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The links a runtime service lists, and the changes it counts (#297). */
class LinkStatesTest {

	private static LinkState link(String name, String state) {
		return () -> {
			LinkDTO dto = new LinkDTO();
			dto.name = name;
			dto.state = state;
			return dto;
		};
	}

	@Test
	void everyLinkIsListedAndEveryChangeCounted() {
		LinkStates links = new LinkStates();
		LinkState events = link("events", "connected");
		LinkState invoker = link("invoker", "gave-up");

		links.added(events);
		links.added(invoker);
		links.updated(invoker);

		assertThat(links.snapshot()).extracting(dto -> dto.name + "=" + dto.state)
				.containsExactly("events=connected", "invoker=gave-up");
		assertThat(links.changes()).isEqualTo(3);

		links.removed(events);
		assertThat(links.snapshot()).extracting(dto -> dto.name).containsExactly("invoker");
		assertThat(links.changes()).isEqualTo(4);
	}
}
