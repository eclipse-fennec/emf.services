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

package org.eclipse.fennec.services.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.fennec.services.FlavorKind;

/**
 * The comma-separated list of flavor kinds, as it travels.
 *
 * <p>It appears in three places — the lookup's {@code flavors} query
 * parameter, the event stream's, and the client's own configuration —
 * and it used to be parsed in three places too, once per bundle that
 * needed it. One convention deserves one implementation, not three
 * that can drift.
 *
 * <p>Reading is deliberately forgiving: an unknown token is skipped
 * with a warning rather than refused. A consumer that names a flavor
 * this broker has never heard of is asking for something it will not
 * get, which the empty result already says; failing the whole request
 * would turn a harmless misunderstanding into an outage.
 */
public final class Flavors {

	private static final Logger LOG = Logger.getLogger(Flavors.class.getName());

	private Flavors() {
	}

	/**
	 * The flavor kinds named in a comma-separated list.
	 *
	 * @param csv the list, possibly {@code null} or blank
	 * @return the kinds named, or an empty set — which every caller
	 *         reads as "no filter", not as "nothing matches"
	 */
	public static Set<FlavorKind> parse(String csv) {
		Set<FlavorKind> kinds = EnumSet.noneOf(FlavorKind.class);
		if (csv == null || csv.isBlank()) {
			return kinds;
		}
		for (String token : csv.split(",")) {
			String name = token.trim().toUpperCase();
			if (name.isEmpty()) {
				continue;
			}
			try {
				kinds.add(FlavorKind.valueOf(name));
			} catch (IllegalArgumentException unknown) {
				LOG.warning("[DDSR] ignoring unknown flavor '" + token.trim() + "'");
			}
		}
		return kinds;
	}

	/** The same list, as it goes back onto the wire. */
	public static String csv(Collection<FlavorKind> kinds) {
		if (kinds == null || kinds.isEmpty()) {
			return null;
		}
		return kinds.stream().map(FlavorKind::getLiteral).collect(Collectors.joining(","));
	}

	/** The kinds as a list, in the order they were named. */
	public static List<FlavorKind> list(String csv) {
		return new ArrayList<>(parse(csv));
	}
}
