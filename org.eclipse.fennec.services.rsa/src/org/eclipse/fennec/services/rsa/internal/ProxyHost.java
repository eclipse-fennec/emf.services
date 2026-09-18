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

package org.eclipse.fennec.services.rsa.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * The bundle that imported proxies are registered from.
 *
 * <p>Not this one. Registered from this bundle, an imported service was
 * never handed to the consumer that asked for it: the framework judges
 * class-space compatibility between registrant and requester, and this
 * bundle — with a full wiring of its own but no view of the consumer's
 * interface — did not pass. Registered from a bundle that carries
 * nothing but a manifest, it does. Found by running both, not by
 * reasoning about the framework's rule; the rule was tried and lost.
 *
 * <p>The pattern is the virtual proxy host of the DOSGi implementation
 * in {@code org.geckoprojects.rsa}. It also keeps this bundle honest: a
 * {@code DynamicImport-Package: *} here would have worked too, and would
 * have given the RSA core a view of every package in the framework to
 * do it.
 *
 * <p>One per framework, found again by symbolic name if it is already
 * there from an earlier life of this component.
 */
final class ProxyHost {

	static final String SYMBOLIC_NAME = "org.eclipse.fennec.services.rsa.host";

	private ProxyHost() {
	}

	/** The host bundle, installed and started if it is not there yet. */
	static Bundle in(BundleContext framework) throws BundleException, IOException {
		for (Bundle candidate : framework.getBundles()) {
			if (SYMBOLIC_NAME.equals(candidate.getSymbolicName())) {
				if (candidate.getState() != Bundle.ACTIVE) {
					candidate.start();
				}
				return candidate;
			}
		}
		Bundle host = framework.installBundle(SYMBOLIC_NAME, manifestOnly(framework));
		host.start();
		return host;
	}

	private static ByteArrayInputStream manifestOnly(BundleContext framework) throws IOException {
		Manifest manifest = new Manifest();
		Attributes main = manifest.getMainAttributes();
		main.put(Name.MANIFEST_VERSION, "1.0");
		main.put(new Name(Constants.BUNDLE_MANIFESTVERSION), "2");
		main.put(new Name(Constants.BUNDLE_SYMBOLICNAME), SYMBOLIC_NAME);
		main.put(new Name(Constants.BUNDLE_NAME), "Fennec Services RSA Proxy Host");
		main.put(new Name(Constants.BUNDLE_VERSION), framework.getBundle().getVersion().toString());
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(512);
		try (JarOutputStream jar = new JarOutputStream(bytes, manifest)) {
			// nothing but the manifest
		}
		return new ByteArrayInputStream(bytes.toByteArray());
	}
}
