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

package org.eclipse.fennec.services.examples.rsa;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;

/**
 * The service, registered the way the Remote Service Admin specification
 * says a service asks to be exported: {@code service.exported.interfaces}
 * and nothing else. No publisher, no flavor, no document.
 */
@Component(
		service = Greeter.class,
		property = {
				"service.exported.interfaces=*",
				"service.exported.configs=fennec.rest",
				"ddsr.provider.name=rsa-example" })
public class GreeterImpl implements Greeter {

	private static final Logger LOG = Logger.getLogger(GreeterImpl.class.getName());

	private final AtomicInteger greeted = new AtomicInteger();

	@Override
	public String greet(String name) {
		int count = greeted.incrementAndGet();
		LOG.info("[RSA-Example] greet(" + name + ") — number " + count);
		return "Hello, " + name + "!";
	}

	@Override
	public int greeted() {
		return greeted.get();
	}
}
