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

package org.eclipse.fennec.tags.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where the things come from that can be tagged — the seam that keeps the
 * tag service free of any one registry (#272). The services registry
 * offers contracts and implementations through it; the Model Atlas could
 * offer models the same way.
 */
public interface TagSubjects {

	/** The kinds of target this source knows, e.g. {@code contract}, {@code implementation}. */
	Set<String> kinds();

	/** The targets of a kind that exist right now; empty for a kind this source does not know. */
	Collection<Subject> subjects(String kind);

	/**
	 * One target as its source sees it.
	 *
	 * @param kind          what sort of thing, as in {@link TagSubjects#kinds()}
	 * @param key           which one, unique within its kind and stable
	 *                      across changes of the thing itself
	 * @param properties    what a rule may filter on; empty when the kind
	 *                      has none
	 * @param requestedTags tag paths the target asks for itself; a tag that
	 *                      exists and is self-assignable is carried with
	 *                      origin {@code SOURCE}
	 */
	record Subject(String kind, String key, Map<String, Object> properties, List<String> requestedTags) {

		public Subject {
			properties = properties == null ? Map.of() : Map.copyOf(properties);
			requestedTags = requestedTags == null ? List.of() : List.copyOf(requestedTags);
		}
	}
}
