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

package org.eclipse.fennec.services.broker.core.internal;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;

/**
 * Putting a service back into the live registry, as the cold cache
 * needs it.
 *
 * <p>Bringing a parked registration back is not a special path: it is
 * an ordinary publish of what was parked, which is what keeps the cold
 * cache from having a second, subtly different way of registering
 * something.
 *
 * <p>The call is made with the write lock already held, and the lock is
 * reentrant, so the publish underneath takes it again on the same
 * thread and gets it. That was true when both lived in one class and it
 * is still true now that they do not: reentrancy belongs to the thread,
 * not to the object.
 */
interface Republication {

	Diagnostic republish(ServiceProvider provider, ServiceImplementation implementation);
}
