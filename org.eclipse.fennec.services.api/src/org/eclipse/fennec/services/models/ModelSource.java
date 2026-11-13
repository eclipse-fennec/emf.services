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

package org.eclipse.fennec.services.models;

import java.util.Optional;

/**
 * Where the payload models a broker knows come from (#256).
 *
 * <p>A source answers for a package by its nsURI and, where a contract
 * pins one, the fingerprint of the version meant. Which source a broker
 * uses is its configuration: a service of this type carries
 * {@link #PROPERTY} - {@code embedded} for the store in the broker,
 * {@code atlas} for the Model Atlas - and the broker names the one it
 * takes as the target of its reference.
 */
public interface ModelSource {

	/** Service property naming the kind of source, e.g. {@code embedded} or {@code atlas}. */
	String PROPERTY = "fennec.models";

	/**
	 * @param nsURI       the package's namespace URI
	 * @param fingerprint the version meant ({@code fp1:...}), or null for
	 *                    the one this source would hand out now
	 * @return the model, or empty when this source has no such version
	 */
	Optional<ModelDocument> resolve(String nsURI, String fingerprint);
}
