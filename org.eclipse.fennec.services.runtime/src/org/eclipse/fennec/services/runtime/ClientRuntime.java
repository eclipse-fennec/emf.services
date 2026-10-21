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

package org.eclipse.fennec.services.runtime;

import org.osgi.framework.Constants;

/**
 * What one runtime using the SDK holds, for anything that wants to
 * watch it.
 *
 * <p>The same contract as {@link BrokerRuntime}, down to the property:
 * bind it dynamically and the component runtime tells you when
 * {@link Constants#SERVICE_CHANGECOUNT} changes. A watcher that wants
 * both sides binds both services and does not care which runtime it is
 * in.
 */
public interface ClientRuntime {

	/** What this runtime holds now. Built per call. */
	ClientRuntimeDTO snapshot();
}
