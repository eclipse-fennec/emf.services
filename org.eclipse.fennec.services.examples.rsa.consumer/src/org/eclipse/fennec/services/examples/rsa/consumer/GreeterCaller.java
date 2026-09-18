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

package org.eclipse.fennec.services.examples.rsa.consumer;

import java.util.logging.Logger;

import org.eclipse.fennec.services.examples.rsa.api.Greeter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * An ordinary consumer.
 *
 * <p>It binds a {@link Greeter} the way any component binds any service.
 * There is no Greeter in this framework — the one it gets was exported
 * from another one, found through the broker, and registered here as a
 * proxy. The reference below does not know that, which is the whole
 * point of Remote Service Admin: the consumer is written as if the
 * service were local, and it is somebody else's job to make that true.
 *
 * <p>The filter asks for the import. A {@code @Reference} is a service
 * listener underneath, and the topology manager reads the interface it
 * waits for out of that listener's filter.
 */
@Component(immediate = true)
public class GreeterCaller {

	private static final Logger LOG = Logger.getLogger(GreeterCaller.class.getName());

	@Reference
	private Greeter greeter;

	@Activate
	void activate() {
		String greeting = greeter.greet("RSA consumer");
		int soFar = greeter.greeted();
		LOG.info("[RSA-Consumer] " + greeting + " (" + soFar + " greetings so far, none of them local)");
	}
}
