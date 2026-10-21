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
package org.eclipse.fennec.services.security.mtls.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Real certificates for the tests, issued by keytool of the running JDK:
 * the JDK has no public API to sign one, and a checked-in fixture would
 * expire under the test.
 */
final class TestCertificates {

	static final String CA_DN = "CN=Fennec Test CA,O=Fennec";

	private final Path dir;
	private final Path store;

	TestCertificates(Path dir) throws IOException, InterruptedException {
		this.dir = dir;
		this.store = dir.resolve("test.p12");
		keytool("-genkeypair", "-alias", "ca", "-dname", CA_DN, "-ext", "bc:c", "-keyalg", "EC", "-validity", "2");
		keytool("-genkeypair", "-alias", "other-ca", "-dname", "CN=Other CA,O=Elsewhere", "-ext", "bc:c",
				"-keyalg", "EC", "-validity", "2");
	}

	/** A certificate for {@code dn}, signed by the test CA (or another one). */
	X509Certificate issue(String alias, String dn, String signer) throws Exception {
		keytool("-genkeypair", "-alias", alias, "-dname", dn, "-keyalg", "EC", "-validity", "2");
		Path request = dir.resolve(alias + ".csr");
		Path signed = dir.resolve(alias + ".pem");
		keytool("-certreq", "-alias", alias, "-file", request.toString());
		keytool("-gencert", "-alias", signer, "-infile", request.toString(), "-outfile", signed.toString(), "-rfc",
				"-validity", "2");
		return parse(Files.readString(signed, StandardCharsets.US_ASCII));
	}

	X509Certificate issue(String alias, String dn) throws Exception {
		return issue(alias, dn, "ca");
	}

	static String pem(X509Certificate certificate) throws CertificateException {
		return "-----BEGIN CERTIFICATE-----\n"
				+ Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
						.encodeToString(certificate.getEncoded())
				+ "\n-----END CERTIFICATE-----\n";
	}

	private static X509Certificate parse(String pem) throws CertificateException {
		return (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
	}

	private void keytool(String... arguments) throws IOException, InterruptedException {
		List<String> command = new ArrayList<>();
		command.add(Path.of(System.getProperty("java.home"), "bin", "keytool").toString());
		command.addAll(List.of(arguments));
		command.addAll(List.of("-keystore", store.toString(), "-storetype", "PKCS12", "-storepass", "changeit",
				"-noprompt"));
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		if (process.waitFor() != 0) {
			throw new IOException("keytool " + arguments[0] + " failed: " + output);
		}
	}
}
