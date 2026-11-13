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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.eclipse.fennec.services.models.ModelDocument;
import org.eclipse.fennec.services.models.ModelSource;
import org.eclipse.fennec.services.models.ModelStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The broker's own store of payload models (#256): what providers hand
 * in before they register the contracts that use them.
 *
 * <p>Kept by nsURI and fingerprint, and never changed: another version
 * of a package is another entry. The fingerprint is computed here over
 * the package the document defines, with the same scheme fennec EMF
 * generates into every package ({@code fp1}), so a pin written by a Java
 * provider from its generated package and one checked here agree.
 *
 * <p>In memory: a broker that restarts forgets the models with the
 * registrations, and a provider that registers again hands them in again.
 */
@Component(service = { ModelSource.class, ModelStore.class }, property = ModelSource.PROPERTY + "=embedded")
public final class EmbeddedModelStore implements ModelStore {

	private static final Logger LOG = Logger.getLogger(EmbeddedModelStore.class.getName());

	static final String ORIGIN = "embedded";

	private final Function<EPackage, String> fingerprints;

	/** nsURI, then fingerprint. */
	private final Map<String, Map<String, ModelDocument>> models = new ConcurrentHashMap<>();

	/** The version of each nsURI handed in last: what an unpinned question gets. */
	private final Map<String, String> latest = new ConcurrentHashMap<>();

	@Activate
	public EmbeddedModelStore(@Reference FingerprintService fingerprints) {
		this(ePackage -> fingerprints.fingerprint(ePackage));
	}

	EmbeddedModelStore(Function<EPackage, String> fingerprints) {
		this.fingerprints = fingerprints;
	}

	@Override
	public ModelDocument store(byte[] ecore) {
		EPackage ePackage = packageOf(ecore);
		String fingerprint = fingerprints.apply(ePackage);
		if (fingerprint == null || fingerprint.isBlank()) {
			throw new IllegalArgumentException("no fingerprint could be computed for " + ePackage.getNsURI());
		}
		ModelDocument document = new ModelDocument(ePackage.getNsURI(), fingerprint, ecore, ORIGIN);
		ModelDocument kept = models.computeIfAbsent(ePackage.getNsURI(), nsURI -> new ConcurrentHashMap<>())
				.putIfAbsent(fingerprint, document);
		latest.put(ePackage.getNsURI(), fingerprint);
		if (kept == null) {
			LOG.info(() -> "[DDSR] payload model " + ePackage.getNsURI() + " kept as " + fingerprint);
		}
		return kept != null ? kept : document;
	}

	@Override
	public Optional<ModelDocument> resolve(String nsURI, String fingerprint) {
		Map<String, ModelDocument> versions = nsURI == null ? null : models.get(nsURI);
		if (versions == null) {
			return Optional.empty();
		}
		String version = fingerprint != null ? fingerprint : latest.get(nsURI);
		return version == null ? Optional.empty() : Optional.ofNullable(versions.get(version));
	}

	/** The one package an Ecore document defines. */
	static EPackage packageOf(byte[] ecore) {
		if (ecore == null || ecore.length == 0) {
			throw new IllegalArgumentException("an empty document defines no package");
		}
		ResourceSet resources = new ResourceSetImpl();
		resources.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
		Resource resource = resources.createResource(URI.createURI("model.ecore"));
		try {
			resource.load(new ByteArrayInputStream(ecore), Map.of());
		} catch (IOException | RuntimeException unreadable) {
			throw new IllegalArgumentException("not an Ecore document: " + unreadable.getMessage(), unreadable);
		}
		EObject root = resource.getContents().isEmpty() ? null : resource.getContents().get(0);
		if (!(root instanceof EPackage ePackage)) {
			throw new IllegalArgumentException("the document defines no package");
		}
		if (ePackage.getNsURI() == null || ePackage.getNsURI().isBlank()) {
			throw new IllegalArgumentException("the package " + ePackage.getName() + " names no nsURI");
		}
		return ePackage;
	}
}
