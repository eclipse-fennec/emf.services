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

/**
 * A {@link ModelSource} that takes models: a provider hands in the
 * packages its contracts use before it registers them.
 */
public interface ModelStore extends ModelSource {

	/**
	 * Keep a model. Its fingerprint is computed here, over the package
	 * the document defines; what the sender claims is not asked.
	 *
	 * @param ecore the Ecore document of one package
	 * @return the model as kept, with its fingerprint
	 * @throws IllegalArgumentException when the document defines no
	 *                                  package, or one without an nsURI
	 */
	ModelDocument store(byte[] ecore);
}
