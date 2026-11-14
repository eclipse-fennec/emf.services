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

import java.io.IOException;

import org.eclipse.fennec.tags.TagRegistry;

/**
 * Where the tag registry is kept. The service reads it once and writes it
 * after every change it acknowledges; a change whose write fails is
 * rolled back. A database or a search index can take over behind this
 * seam later (#272).
 */
public interface TagStore {

	/** The registry as last written, or an empty one the first time. */
	TagRegistry load() throws IOException;

	/** Writes the registry; returns only once it is kept. */
	void save(TagRegistry registry) throws IOException;
}
