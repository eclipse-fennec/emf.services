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

import java.util.Arrays;
import java.util.Objects;

/**
 * One payload model: its Ecore document and what identifies it.
 *
 * <p>The document travels as bytes, as the Ecore XMI that defines the
 * package: this bundle reads and writes no documents, and a model is
 * handed on as it was received.
 *
 * @param nsURI       the namespace URI of the package
 * @param fingerprint the fingerprint of this version ({@code fp1:...}),
 *                    computed over the package, never taken on trust
 * @param ecore       the Ecore document
 * @param origin      where it came from, for a reader: {@code embedded},
 *                    the Model Atlas scope and stage, ...
 */
public record ModelDocument(String nsURI, String fingerprint, byte[] ecore, String origin) {

	public ModelDocument {
		Objects.requireNonNull(nsURI, "nsURI");
		Objects.requireNonNull(fingerprint, "fingerprint");
		Objects.requireNonNull(ecore, "ecore");
		ecore = ecore.clone();
	}

	@Override
	public byte[] ecore() {
		return ecore.clone();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ModelDocument that && nsURI.equals(that.nsURI)
				&& fingerprint.equals(that.fingerprint) && Arrays.equals(ecore, that.ecore)
				&& Objects.equals(origin, that.origin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nsURI, fingerprint, Arrays.hashCode(ecore), origin);
	}

	@Override
	public String toString() {
		return "ModelDocument[" + nsURI + " " + fingerprint + " from " + origin + "]";
	}
}
