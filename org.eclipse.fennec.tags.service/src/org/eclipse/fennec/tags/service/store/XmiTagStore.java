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


package org.eclipse.fennec.tags.service.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.tags.TagRegistry;
import org.eclipse.fennec.tags.TagsFactory;
import org.eclipse.fennec.tags.TagsPackage;
import org.eclipse.fennec.tags.service.TagStore;

/**
 * The tag registry as one XMI file — {@code broker-tags.xmi} beside the
 * broker's own {@code broker-state.xmi} (#272).
 *
 * <p>Kept apart from the broker state on purpose: the tags are an
 * administrator's hand work, and a change to the registry model that
 * starts the broker empty must not take them along. This file depends on
 * {@code tags.ecore} only.
 *
 * <p>A write goes to a file beside the target first and replaces it in
 * one move, so a crash in the middle leaves the previous version, not
 * half of a new one.
 */
public final class XmiTagStore implements TagStore {

	private final Path file;

	public XmiTagStore(Path file) {
		this.file = file;
	}

	/** Where the registry is kept. */
	public Path file() {
		return file;
	}

	@Override
	public TagRegistry load() throws IOException {
		if (!Files.exists(file)) {
			return TagsFactory.eINSTANCE.createTagRegistry();
		}
		Resource resource = newResource();
		try (InputStream in = Files.newInputStream(file)) {
			resource.load(in, Map.of());
		}
		if (resource.getContents().isEmpty() || !(resource.getContents().get(0) instanceof TagRegistry registry)) {
			throw new IOException(file + " holds no tag registry");
		}
		return registry;
	}

	@Override
	public void save(TagRegistry registry) throws IOException {
		Path parent = file.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Resource resource = newResource();
		// A copy, so the live registry keeps the resource it belongs to.
		resource.getContents().add(EcoreUtil.copy(registry));
		Path next = file.resolveSibling(file.getFileName() + ".next");
		try (OutputStream out = Files.newOutputStream(next)) {
			resource.save(out, Map.of());
		}
		Files.move(next, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	private Resource newResource() {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(TagsPackage.eNS_URI, TagsPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		return resourceSet.createResource(URI.createFileURI(file.toAbsolutePath().toString()));
	}
}
