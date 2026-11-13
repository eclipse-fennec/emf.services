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

package org.eclipse.fennec.services.broker.core.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.ModelReference;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.models.ModelSource;

/**
 * The gate's question about payload models (#256): does every model the
 * contract pins resolve, and - when the broker is strict - does the
 * contract pin every model its slots are typed with?
 */
final class PayloadModels {

	private PayloadModels() {
	}

	/**
	 * @param strict whether every package an {@code eType} names must be
	 *               listed in the contract's {@code models}
	 * @return what is wrong, or empty
	 */
	static Optional<String> violations(ServiceInterface contract, ModelSource source, boolean strict) {
		if (source == null || contract == null) {
			return Optional.empty();
		}
		List<String> wrong = new ArrayList<>();
		Set<String> pinned = new LinkedHashSet<>();
		for (ModelReference model : contract.getModels()) {
			pinned.add(model.getNsURI());
			if (model.getFingerprint() != null && !model.getFingerprint().isBlank()
					&& source.resolve(model.getNsURI(), model.getFingerprint()).isEmpty()) {
				wrong.add("the payload model " + model.getNsURI() + " at " + model.getFingerprint()
						+ " is not known here - hand it in before the contract");
			}
		}
		if (strict) {
			for (String nsURI : typedWith(contract)) {
				if (!pinned.contains(nsURI)) {
					wrong.add("a slot is typed with " + nsURI + ", which the contract does not pin in its models");
				}
			}
		}
		return wrong.isEmpty() ? Optional.empty() : Optional.of(String.join("; ", wrong));
	}

	/** The packages the contract's slots are typed with, Ecore's own left out. */
	static Set<String> typedWith(ServiceInterface contract) {
		Set<String> packages = new LinkedHashSet<>();
		for (ServiceOperation operation : contract.getOperations()) {
			add(packages, operation.getReturnValue());
			for (Parameter parameter : operation.getParameters()) {
				add(packages, parameter);
			}
		}
		packages.remove(EcorePackage.eNS_URI);
		return packages;
	}

	private static void add(Set<String> packages, Parameter slot) {
		if (slot == null || slot.getEType() == null) {
			return;
		}
		EClassifier type = slot.getEType();
		// A proxy on the broker by design: the payload model is not here,
		// and its href names the package.
		String nsURI = type.eIsProxy() ? EcoreUtil.getURI(type).trimFragment().toString()
				: type.getEPackage() != null ? type.getEPackage().getNsURI() : null;
		if (nsURI != null && !nsURI.isBlank()) {
			packages.add(nsURI);
		}
	}
}
