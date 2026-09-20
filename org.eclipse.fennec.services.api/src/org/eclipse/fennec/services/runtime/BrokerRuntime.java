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
 * What the broker currently holds, for anything that wants to watch it.
 *
 * <p>The same shape OSGi uses for this: {@code ServiceComponentRuntime}
 * answers with DTOs, and nothing that asks it needs to know how the
 * component runtime works inside. Telemetry, a console, a health check
 * and a test all want the same thing, and none of them should have to
 * reach into the broker to get it.
 *
 * <p><strong>How to be told about change.</strong> The service carries
 * {@link Constants#SERVICE_CHANGECOUNT}, and its value changes whenever
 * the snapshot would differ. A consumer binds it dynamically and gets a
 * {@code modified} callback:
 *
 * <pre>
 * &#64;Reference(policy = DYNAMIC, cardinality = OPTIONAL)
 * void setRuntime(BrokerRuntime runtime, Map&lt;String, Object&gt; properties) { … }
 * void modifiedRuntime(BrokerRuntime runtime, Map&lt;String, Object&gt; properties) { … }
 * void unsetRuntime(BrokerRuntime runtime) { … }
 * </pre>
 *
 * <p>That is the whole contract — no listener to register, no lifecycle
 * to know. It is also why the change count is a service property and
 * not only a field: a property change is something the component
 * runtime already delivers.
 *
 * <p>The property is the standard one rather than one of ours. OSGi
 * already has a name for "this service's answer has changed", every
 * whiteboard runtime uses it, and a watcher that knows the idiom needs
 * to be told nothing about us to use it.
 */
public interface BrokerRuntime {

	/**
	 * What the broker holds now.
	 *
	 * <p>Built per call. A snapshot handed out and kept would be a
	 * snapshot someone else could change, and a reader that got an
	 * older one would have no way to tell.
	 */
	BrokerRuntimeDTO snapshot();
}
