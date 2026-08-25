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

package org.eclipse.fennec.services.xmi.codec;

/**
 * A wire document was refused.
 * <p>
 * Transport-neutral on purpose. The codec used to throw
 * {@code jakarta.ws.rs.WebApplicationException}, which meant every
 * bundle that wanted to read or write the DDSR wire format had to drag
 * JAX-RS along — including an MQTT transport, where an HTTP status code
 * means nothing. The JAX-RS providers in this bundle translate this
 * exception into the right response; other transports translate it into
 * whatever their protocol uses.
 * <p>
 * The message is deliberately generic. Parser detail can carry local
 * paths or the URI a rejected entity pointed at, so it belongs in the
 * log, not in anything a caller might hand back (S5).
 */
public class XmiCodecException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Why the document was refused. Transports map these to their own
	 * vocabulary — the JAX-RS providers here turn {@link #TOO_LARGE} into
	 * 413 and everything else into 400.
	 */
	public enum Reason {

		/** Not parseable, or carrying something we refuse (DTD, entities). */
		MALFORMED,

		/** Parsed, but contained no root at all. */
		EMPTY,

		/** Parsed, but had more roots than the caller can accept. */
		UNEXPECTED_ROOTS,

		/** Exceeded {@link WireBody#MAX_BYTES}. */
		TOO_LARGE
	}

	private final Reason reason;

	public XmiCodecException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason reason() {
		return reason;
	}
}
