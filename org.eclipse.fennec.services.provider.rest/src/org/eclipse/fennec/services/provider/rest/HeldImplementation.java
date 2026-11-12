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

package org.eclipse.fennec.services.provider.rest;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * Keeps the service behind a contract in use between calls.
 *
 * <p>A dispatcher takes the implementation for one call and gives it
 * back, which is the right way to use a service one does not own — and
 * the wrong thing to be the only user. Most implementations are delayed
 * Declarative Services components: built when the first bundle takes
 * them, <em>disposed when the last one lets go</em>. With nobody else
 * holding it, every call builds the component and every return disposes
 * of it, and two calls at once race the disposal: the second is handed
 * nothing, or an instance on its way out, and the framework refuses to
 * take back what it no longer counts. The caller saw a 500 for a
 * request that was perfectly good — about one in two hundred under
 * load, and the reason a node that looked up twice at start sometimes
 * never imported anything.
 *
 * <p>So one use is held for as long as the implementation is the one a
 * call would get, and only moved when another service takes its place.
 * A call still takes and returns its own; it just never is the last.
 */
final class HeldImplementation {

	private final BundleContext context;

	private ServiceReference<?> held;

	HeldImplementation(BundleContext context) {
		this.context = context;
	}

	/**
	 * The same service objects, with their service now held.
	 *
	 * @param current what a call is about to use, or {@code null} when
	 *                nothing implements the contract right now
	 */
	synchronized ServiceObjects<Object> holding(ServiceObjects<Object> current) {
		ServiceReference<?> reference = current == null ? null : current.getServiceReference();
		if (reference == held || (reference != null && reference.equals(held))) {
			return current;
		}
		ServiceReference<?> previous = held;
		held = null;
		if (reference != null) {
			try {
				// A use of this bundle's own, never handed to anyone: it is
				// there to be counted.
				if (context.getService(reference) != null) {
					held = reference;
				}
			} catch (RuntimeException gone) {
				// The service left between being found and being taken. The
				// call finds that out for itself.
			}
		}
		release(previous);
		return current;
	}

	/** Lets go of the service — the dispatcher is going. */
	synchronized void release() {
		ServiceReference<?> previous = held;
		held = null;
		release(previous);
	}

	private void release(ServiceReference<?> reference) {
		if (reference == null) {
			return;
		}
		try {
			context.ungetService(reference);
		} catch (RuntimeException alreadyGone) {
			// The framework, or the service, got there first.
		}
	}
}
