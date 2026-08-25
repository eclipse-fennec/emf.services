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

package org.eclipse.fennec.services.client;

import org.eclipse.fennec.services.ServiceEvent;

/**
 * Callback for service lifecycle events, the consumer-side counterpart
 * of the broker's {@code EventSink}.
 * <p>
 * The model declares a {@code ServiceListener} with the same
 * {@code serviceChanged} method, and that is the conceptual reference —
 * but it extends {@code EObject}, so implementing it would force
 * application code to implement all of EMF's reflective API. This is
 * the plain interface application code actually implements; the model
 * type stays what it is meant to be, a description.
 * <p>
 * Called on the delivery thread of whatever transport is in use, so a
 * listener should return quickly and must not throw — an exception is
 * logged and swallowed rather than propagated back into the stream,
 * because one listener may not break delivery for the others.
 */
@FunctionalInterface
public interface DdsrServiceListener {

	/**
	 * A service this listener subscribed to changed state.
	 *
	 * @param event the lifecycle event; {@code event.getType()} says what
	 *              happened, {@code event.getReference()} identifies the
	 *              service
	 */
	void serviceChanged(ServiceEvent event);
}
