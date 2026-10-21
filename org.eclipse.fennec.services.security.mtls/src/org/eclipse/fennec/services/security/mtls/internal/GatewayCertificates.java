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
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * The client certificate as a gateway that ended TLS hands it on, in a
 * request header.
 *
 * <p>Gateways disagree on the spelling, so three are read: PEM, PEM
 * URL-encoded (nginx {@code $ssl_client_escaped_cert}), and the bare
 * base64 of the DER (Traefik, without the armor lines). The header is
 * only believed from an address the node is configured to trust — from
 * anybody else it is a claim, not a certificate.
 */
final class GatewayCertificates {

	private GatewayCertificates() {
		// no instances
	}

	/**
	 * The certificate in the header value.
	 *
	 * @throws CertificateException when the value is not a certificate in
	 *         any of the three spellings
	 */
	static X509Certificate parse(String value) throws CertificateException {
		if (value == null || value.isBlank()) {
			throw new CertificateException("empty certificate header");
		}
		String text = value.strip();
		if (text.contains("%")) {
			text = percentDecoded(text);
		}
		String body = text
				.replace("-----BEGIN CERTIFICATE-----", "")
				.replace("-----END CERTIFICATE-----", "")
				.replaceAll("\\s+", "");
		byte[] der;
		try {
			der = Base64.getDecoder().decode(body);
		} catch (IllegalArgumentException e) {
			throw new CertificateException("certificate header is not base64: " + e.getMessage());
		}
		return (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(der));
	}

	/**
	 * {@code %XX} decoded and nothing else. Not URLDecoder: it reads a
	 * {@code +} as a space, and {@code +} is a base64 digit.
	 */
	static String percentDecoded(String text) throws CertificateException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '%') {
				if (i + 2 >= text.length()) {
					throw new CertificateException("truncated escape in certificate header");
				}
				int high = Character.digit(text.charAt(i + 1), 16);
				int low = Character.digit(text.charAt(i + 2), 16);
				if (high < 0 || low < 0) {
					throw new CertificateException("invalid escape in certificate header");
				}
				bytes.write(high << 4 | low);
				i += 2;
			} else {
				bytes.write(c);
			}
		}
		return bytes.toString(StandardCharsets.US_ASCII);
	}
}
