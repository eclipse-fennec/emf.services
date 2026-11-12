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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.fennec.services.examples.rsa.api.Greeter;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

/**
 * The service, registered the way the Remote Service Admin specification
 * says a service asks to be exported: {@code service.exported.interfaces}
 * and nothing else. No publisher, no flavor, no document.
 */
// The configuration PID is what lets a deployment say which transport
// this asks to be exported over without touching the code: a
// configuration property overrides a component property, so
// `service.exported.configs` is a deployment decision even though the
// default sits right here. That is how the harness exports the very
// same service over MQTT (#98).
@Component(
		service = Greeter.class,
		configurationPid = "org.eclipse.fennec.services.examples.rsa.greeter",
		property = {
				"service.exported.interfaces=*",
				"service.exported.configs=fennec.rest",
				"ddsr.provider.name=rsa-example" })
public class GreeterImpl implements Greeter {

	private static final Logger LOG = Logger.getLogger(GreeterImpl.class.getName());

	private final AtomicInteger greeted = new AtomicInteger();

	/**
	 * Says what it is asking for. Which transport a service is exported
	 * over is a deployment decision and therefore invisible in the code
	 * — and something invisible that decides whether anything happens at
	 * all is worth one line in the log.
	 */
	@Activate
	void activate(Map<String, Object> properties) {
		LOG.info("[RSA-Example] Greeter asks to be exported as "
				+ properties.get("service.exported.configs"));
	}

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

	/** How long a greeting that comes later takes. Long enough to be seen to come later. */
	private static final long LATER_MILLIS = 300;

	@Override
	public Promise<String> greetLater(String name) {
		Deferred<String> greeting = new Deferred<>();
		LOG.info("[RSA-Example] greetLater(" + name + ") — promised");
		Thread later = new Thread(() -> {
			try {
				Thread.sleep(LATER_MILLIS);
				greeted.incrementAndGet();
				greeting.resolve("Hello, " + name + ", sorry to keep you waiting!");
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				greeting.fail(interrupted);
			}
		}, "rsa-example-greet-later");
		later.setDaemon(true);
		later.start();
		return greeting.getPromise();
	}
}
