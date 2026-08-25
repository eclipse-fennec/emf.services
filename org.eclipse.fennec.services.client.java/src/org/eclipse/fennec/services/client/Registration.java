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
}
