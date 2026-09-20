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

package org.eclipse.fennec.services.telemetry;

/**
 * One call, while it is happening (#126).
 *
 * <p>Handed out by {@link CallTracer} and closed when the call ends,
 * which is what makes try-with-resources the whole usage:
 *
 * <pre>
 * try (CallSpan span = tracer.calling("Payment/charge", carrier)) {
 *     return send(request);
 * } catch (RuntimeException failure) {
 *     span.failed(failure);
 *     throw failure;
 * }
 * </pre>
 *
 * <p>{@link #close()} does not throw, because a call site must not fail
 * a call over telemetry. That is also why it narrows
 * {@link AutoCloseable}: a caller should not have to catch anything to
 * measure something.
 */
public interface CallSpan extends AutoCloseable {

	/**
	 * What nothing records — returned by {@link CallTracer#NONE} and by
	 * a tracer that decided not to sample this call. Every method is a
	 * no-op, so a call site never has to check for null.
	 */
	CallSpan NONE = new CallSpan() {

		@Override
		public CallSpan attribute(String name, String value) {
			return this;
		}

		@Override
		public void failed(Throwable error) {
			// nothing records this
		}

		@Override
		public void close() {
			// nothing to close
		}
	};

	/**
	 * Adds a detail worth grouping by later: the contract, the provider,
	 * the reference id. Null values are dropped rather than recorded as
	 * "null".
	 */
	CallSpan attribute(String name, String value);

	/** The call did not succeed. Records the error and marks the span. */
	void failed(Throwable error);

	/** Ends the call. Never throws. */
	@Override
	void close();
}
