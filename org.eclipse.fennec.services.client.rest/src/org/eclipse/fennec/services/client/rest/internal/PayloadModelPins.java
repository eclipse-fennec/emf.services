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

package org.eclipse.fennec.services.client.rest.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.fennec.services.ModelReference;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * A provider's half of payload models (#256): before a contract goes to
 * the broker, every package its slots are typed with is handed in, and
 * the contract pins it with the fingerprint the broker answered.
 *
 * <p>The broker's fingerprint, not one computed here: it is the one its
 * gate checks, so a pin written from it always resolves. A package the
 * contract pins already keeps its pin; it is handed in all the same, so
 * a broker that restarted has it again. A broker that takes no models
 * (one older than #256) leaves the contract as it was.
 */
final class PayloadModelPins {

	private static final Logger LOG = Logger.getLogger(PayloadModelPins.class.getName());

	private PayloadModelPins() {
	}

	/**
	 * @param handIn hands one Ecore document to the broker and answers its
	 *               fingerprint, or empty when the broker takes no models
	 */
	static void pin(ServiceInterface contract, Function<byte[], Optional<String>> handIn) {
		for (EPackage ePackage : packagesOf(contract).values()) {
			byte[] ecore;
			try {
				ecore = ecoreOf(ePackage);
			} catch (IOException | RuntimeException unwritable) {
				LOG.log(Level.WARNING, "[DDSR-Client] the payload model " + ePackage.getNsURI()
						+ " could not be written; the contract goes without it", unwritable);
				continue;
			}
			Optional<String> fingerprint = handIn.apply(ecore);
			if (fingerprint.isEmpty()) {
				return;
			}
			if (contract.getModels().stream().noneMatch(m -> ePackage.getNsURI().equals(m.getNsURI()))) {
				ModelReference model = ServicesFactory.eINSTANCE.createModelReference();
				model.setNsURI(ePackage.getNsURI());
				model.setFingerprint(fingerprint.get());
				contract.getModels().add(model);
			}
		}
	}

	/** The packages the contract's slots are typed with and this side has: Ecore's and the registry's own left out. */
	static Map<String, EPackage> packagesOf(ServiceInterface contract) {
		Map<String, EPackage> packages = new LinkedHashMap<>();
		for (ServiceOperation operation : contract.getOperations()) {
			add(packages, operation.getReturnValue());
			for (Parameter parameter : operation.getParameters()) {
				add(packages, parameter);
			}
		}
		return packages;
	}

	private static void add(Map<String, EPackage> packages, Parameter slot) {
		EClassifier type = slot == null ? null : slot.getEType();
		if (type == null || type.eIsProxy() || type.getEPackage() == null) {
			return;
		}
		EPackage ePackage = type.getEPackage();
		while (ePackage.getESuperPackage() != null) {
			ePackage = ePackage.getESuperPackage();
		}
		// Ecore and the registry's own model are on every node already.
		if (ePackage.getNsURI() != null && !EcorePackage.eNS_URI.equals(ePackage.getNsURI())
				&& !ServicesPackage.eNS_URI.equals(ePackage.getNsURI())) {
			packages.putIfAbsent(ePackage.getNsURI(), ePackage);
		}
	}

	/**
	 * A package as the Ecore document that defines it.
	 *
	 * <p>Written from the resource it lives in - a generated package has
	 * one under its nsURI - so references inside it stay inside the
	 * document, as in the .ecore it was generated from, and the broker
	 * computes the very fingerprint fennec EMF generated. A copy would
	 * keep pointing at the original classes, and the broker would hash
	 * proxies. Only a package without a resource of its own is copied.
	 */
	static byte[] ecoreOf(EPackage ePackage) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Resource home = ePackage.eResource();
		if (home != null && home.getContents().size() == 1 && home.getContents().get(0) == ePackage) {
			home.save(out, Map.of());
			return out.toByteArray();
		}
		Resource resource = new EcoreResourceFactoryImpl().createResource(URI.createURI(ePackage.getNsURI()));
		new ResourceSetImpl().getResources().add(resource);
		resource.getContents().add(EcoreUtil.copy(ePackage));
		resource.save(out, Map.of());
		return out.toByteArray();
	}
}
