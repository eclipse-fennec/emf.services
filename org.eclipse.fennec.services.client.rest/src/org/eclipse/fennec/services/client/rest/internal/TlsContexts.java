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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * The TLS side of a node's client (SECURITY.md §7d): the certificate it
 * shows (a key store) and the authorities it believes (a trust store).
 *
 * <p>Both are optional and independent. Without a trust store the JVM's
 * own authorities decide, which is right for a broker with a public
 * certificate; without a key store the node shows none, which is right
 * against a broker that only needs server TLS. Neither set means no
 * context at all, and the client stays exactly what it was.
 */
final class TlsContexts {

	private TlsContexts() {
		// no instances
	}

	/**
	 * The context for these stores, or {@code null} when neither is named.
	 *
	 * @throws IllegalStateException when a named store cannot be read —
	 *         a node configured to prove itself must not quietly call
	 *         without its certificate
	 */
	static SSLContext of(String keyStore, String keyStorePassword, String keyStoreType, String trustStore,
			String trustStorePassword, String trustStoreType) {
		boolean keys = present(keyStore);
		boolean trust = present(trustStore);
		if (!keys && !trust) {
			return null;
		}
		try {
			KeyManager[] keyManagers = null;
			if (keys) {
				KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				factory.init(load(keyStore, keyStorePassword, keyStoreType), chars(keyStorePassword));
				keyManagers = factory.getKeyManagers();
			}
			TrustManager[] trustManagers = null;
			if (trust) {
				TrustManagerFactory factory = TrustManagerFactory
						.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				factory.init(load(trustStore, trustStorePassword, trustStoreType));
				trustManagers = factory.getTrustManagers();
			}
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(keyManagers, trustManagers, null);
			return context;
		} catch (GeneralSecurityException | IOException e) {
			throw new IllegalStateException("cannot build the TLS context from "
					+ (keys ? "key store " + keyStore : "") + (keys && trust ? " and " : "")
					+ (trust ? "trust store " + trustStore : "") + ": " + e.getMessage(), e);
		}
	}

	private static KeyStore load(String path, String password, String type)
			throws GeneralSecurityException, IOException {
		KeyStore store = KeyStore.getInstance(present(type) ? type.strip() : "PKCS12");
		try (InputStream in = Files.newInputStream(Path.of(path.strip()))) {
			store.load(in, chars(password));
		}
		return store;
	}

	private static boolean present(String value) {
		return value != null && !value.isBlank();
	}

	private static char[] chars(String password) {
		return password == null ? new char[0] : password.toCharArray();
	}
}
