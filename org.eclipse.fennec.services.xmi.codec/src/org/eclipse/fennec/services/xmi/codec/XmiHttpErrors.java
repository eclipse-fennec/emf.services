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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Translates a {@link XmiCodecException} into an HTTP response.
 * <p>
 * The codec itself is transport-neutral — it has to be, or every
 * transport that reads the DDSR wire format would drag JAX-RS along.
 * This class is where the HTTP vocabulary is allowed to appear, and it
 * is shared by the message-body providers and by any resource that reads
 * a body itself, so the mapping exists exactly once.
 */
public final class XmiHttpErrors {

	private XmiHttpErrors() {
	}

	/**
	 * 413 for a body that is merely too big, 400 for everything else: the
	 * caller can act on that difference. The message stays generic —
	 * parser detail belongs in the log, not in a response (S5).
	 */
	public static WebApplicationException toHttp(XmiCodecException refusal) {
		Response.Status status = refusal.reason() == XmiCodecException.Reason.TOO_LARGE
				? Response.Status.REQUEST_ENTITY_TOO_LARGE
				: Response.Status.BAD_REQUEST;
		return new WebApplicationException(refusal.getMessage(), status);
	}
}
