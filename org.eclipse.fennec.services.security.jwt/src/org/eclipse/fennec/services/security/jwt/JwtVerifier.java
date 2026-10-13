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
package org.eclipse.fennec.services.security.jwt;

/**
 * A credential in, a principal out — or the reason it was refused.
 *
 * <p>The one seam of this bundle. A bearer token is what it verifies
 * today; a client certificate (SECURITY.md §5) would be a second
 * implementation behind the same seam, and the filter would not know.
 */
public interface JwtVerifier {

	/**
	 * Verifies a compact JWT and says who it stands for.
	 *
	 * @param compactJwt the token as it arrived, without the
	 *                   {@code Bearer} prefix
	 * @return the verified principal
	 * @throws TokenRejectedException with the reason, never a runtime
	 *                                exception: a bad token is an
	 *                                expected input, not a failure
	 */
	VerifiedPrincipal verify(String compactJwt) throws TokenRejectedException;

	/** Why a token was not accepted. The message is for the log, never for the response body. */
	class TokenRejectedException extends Exception {
		private static final long serialVersionUID = 1L;

		public TokenRejectedException(String message, Throwable cause) {
			super(message, cause);
		}

		public TokenRejectedException(String message) {
			super(message);
		}
	}
}
