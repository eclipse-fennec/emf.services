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

package org.eclipse.fennec.services.provider.messaging;

import java.util.concurrent.Flow;

import org.gecko.messaging.api.util.Flows;
import org.osgi.util.pushstream.PushStream;

/**
 * What a service returned for an operation that streams, as a publisher —
 * a {@link Flow.Publisher} as it is, a {@link PushStream} through the
 * messaging library's own adapter ({@link Flows#toPublisher}).
 */
final class ServedStreams {

	private ServedStreams() {
	}

	/** The publisher, or {@code null} when what was returned is no stream. */
	@SuppressWarnings("unchecked")
	static Flow.Publisher<Object> publisherOf(Object returned) {
		if (returned instanceof Flow.Publisher<?> publisher) {
			return (Flow.Publisher<Object>) publisher;
		}
		if (returned instanceof PushStream<?> stream) {
			return Flows.toPublisher((PushStream<Object>) stream);
		}
		return null;
	}
}
