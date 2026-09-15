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

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceReference;

/**
 * Handle returned by {@link DdsrProvider#publish}. Holds the
 * server-assigned {@link ServiceReference} and lets the caller
 * withdraw the implementation.
 *
 * <p>{@link #diagnostic()} surfaces the broker's publish-time
 * diagnostic — typically severity OK, but may be WARNING if any
 * referenced interface was deprecated.
 */
public interface Registration {

	ServiceReference reference();

	ServiceImplementation implementation();

	/** The broker's publish-time diagnostic. Never null. */
	Diagnostic diagnostic();

	/**
	 * Withdraw the implementation from the broker. Returns the
	 * broker's diagnostic. Idempotent at the SDK level: calling
	 * {@code withdraw()} twice on the same registration is safe and
	 * yields a {@code Diagnostic} with code {@code IMPL_NOT_PUBLISHED}
	 * on the second call.
	 */
	Diagnostic withdraw();

	/**
	 * Re-sends the implementation this registration was created with as
	 * an in-place modification (#55): change its flavors, properties,
	 * capabilities or description on the model object, then call this.
	 * The broker keeps the reference id and every consumer lease and
	 * emits {@code MODIFIED}; consumers refresh, they do not rebind. The
	 * implemented contracts must not change — that is a new publish (the
	 * broker answers {@code CODE_IMPL_CONTRACT_CHANGED}). Returns the
	 * broker's diagnostic; after a withdraw it is {@code IMPL_NOT_PUBLISHED}.
	 */
	Diagnostic update();
}
