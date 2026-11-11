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

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Size limit for an inbound wire body (S7).
 * <p>
 * Nothing on the DDSR wire is large. The biggest legitimate body is a
 * publish document for one provider — a few kilobytes. Reading an
 * unbounded request into memory is therefore never necessary, and doing
 * it hands an unauthenticated caller (see S2) a trivial way to exhaust
 * the broker's heap without needing any parser trick at all.
 * <p>
 * The cap is deliberately generous compared to real traffic: it is a
 * backstop against absurd input, not a business rule — for the
 * registry's own traffic. A provider that serves a contract is a
 * different matter: an operation may take a document, and how large a
 * document it is prepared to hold is that deployment's decision (#219).
 * So the limit is a parameter with this as its default: whoever serves
 * a contract states its own, and everything else keeps the backstop.
 */
public final class WireBody {

	/** 1 MiB — roughly two orders of magnitude above any real body. */
	public static final int MAX_BYTES = 1024 * 1024;

	private WireBody() {
	}

	/**
	 * Wraps a stream so that reading past {@link #MAX_BYTES} fails
	 * instead of continuing. Use this when the bytes are handed straight
	 * to a consumer (the XMI parser) and never need to be held.
	 */
	public static InputStream limited(InputStream in) {
		return limited(in, MAX_BYTES);
	}

	/**
	 * The same, with the limit the caller is prepared to hold.
	 *
	 * @param maxBytes the largest body accepted; anything not positive
	 *                 means {@link #MAX_BYTES} — there is no way to ask
	 *                 for no limit at all
	 */
	public static InputStream limited(InputStream in, long maxBytes) {
		return new LimitedInputStream(in, effective(maxBytes));
	}

	/** The limit a configured value stands for: itself, or the default when it says nothing. */
	public static long effective(long maxBytes) {
		return maxBytes > 0 ? maxBytes : MAX_BYTES;
	}

	/**
	 * Reads the whole body into memory, refusing anything larger than
	 * {@link #MAX_BYTES}. Only for callers that genuinely need the bytes
	 * more than once; prefer {@link #limited(InputStream)}.
	 */
	public static byte[] readFully(InputStream in) throws IOException {
		return readFully(in, MAX_BYTES);
	}

	/**
	 * The same, with the limit the caller is prepared to hold; anything
	 * not positive means {@link #MAX_BYTES}.
	 */
	public static byte[] readFully(InputStream in, long maxBytes) throws IOException {
		long limit = effective(maxBytes);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[8192];
		long total = 0;
		int read;
		while ((read = in.read(chunk)) != -1) {
			total += read;
			if (total > limit) {
				throw tooLarge(limit);
			}
			buffer.write(chunk, 0, read);
		}
		return buffer.toByteArray();
	}

	/**
	 * A distinct reason rather than "malformed": the body is not broken,
	 * it is too big, and a caller can act on the difference. The JAX-RS
	 * providers turn this into 413.
	 */
	static XmiCodecException tooLarge(long limit) {
		return new XmiCodecException(XmiCodecException.Reason.TOO_LARGE,
				"request body exceeds " + limit + " bytes");
	}

	/**
	 * Counts bytes handed out and fails once the budget is gone. Kept
	 * deliberately simple — it only has to stop an oversized body, not
	 * account precisely.
	 */
	private static final class LimitedInputStream extends FilterInputStream {

		private final long limit;

		private long consumed;

		LimitedInputStream(InputStream in, long limit) {
			super(in);
			this.limit = limit;
		}

		@Override
		public int read() throws IOException {
			int b = super.read();
			if (b != -1) {
				count(1);
			}
			return b;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = super.read(b, off, len);
			if (read > 0) {
				count(read);
			}
			return read;
		}

		@Override
		public long skip(long n) throws IOException {
			long skipped = super.skip(n);
			if (skipped > 0) {
				count(skipped);
			}
			return skipped;
		}

		private void count(long bytes) {
			consumed += bytes;
			if (consumed > limit) {
				throw tooLarge(limit);
			}
		}
	}
}
