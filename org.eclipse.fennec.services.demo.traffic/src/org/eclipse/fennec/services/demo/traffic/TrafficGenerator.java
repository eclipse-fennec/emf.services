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

package org.eclipse.fennec.services.demo.traffic;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.ServiceProxyFactory;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Keeps calling a provider so there is something to look at.
 *
 * <p>A dashboard of a system at rest is a dashboard of nothing. This
 * discovers the Payment contract through the registry, calls it on a
 * timer and lets go again — the same three stages any consumer goes
 * through, only on repeat.
 *
 * <p>It is also the one piece of the demo that must not leak into a
 * deployment, which is what {@code ConfigurationPolicy.REQUIRE} is
 * for: the bundle can sit in every launch, and only the framework
 * whose configuration asks for traffic produces any.
 *
 * <p>Failures are logged and the schedule continues. A provider that
 * is not there yet, or has just gone, is the normal state of a
 * distributed demo and not a reason to stop.
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
@Designate(ocd = TrafficGenerator.Config.class)
public class TrafficGenerator {

	private static final Logger LOG = Logger.getLogger(TrafficGenerator.class.getName());

	@ObjectClassDefinition(name = "Fennec Services Demo Traffic",
			description = "Calls a provider on a timer so the observability stack has something to show.")
	public @interface Config {

		@AttributeDefinition(name = "Contract", description = "The contract to look up and call.")
		String contract() default "Payment";

		@AttributeDefinition(name = "Probe contract",
				description = "A second contract, served by the generic REST distribution, so that a trace "
						+ "has the provider's half in it as well. Empty to call only the first.")
		String probeContract() default "BindingProbe";

		@AttributeDefinition(name = "Interval (ms)", description = "How often to make a call.")
		long interval() default 2000;

		@AttributeDefinition(name = "Account", description = "The account id the calls are about.")
		String account() default "demo-account";
	}

	@Reference
	private DdsrClient client;

	@Reference
	private ServiceProxyFactory proxies;

	/**
	 * Optional, like everywhere this seam is used — but here it is what
	 * makes the picture: one span around the whole tick, so the lookup
	 * and the two calls under it read as one unit of work instead of
	 * three unrelated traces.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile CallTracer tracer;

	private ScheduledExecutorService schedule;

	private volatile Config config;

	@Activate
	void activate(Config config) {
		this.config = config;
		this.schedule = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "fennec-demo-traffic");
			thread.setDaemon(true);
			return thread;
		});
		// A first call right away, so a presenter does not wait for the
		// first tick to see anything.
		schedule.scheduleAtFixedRate(this::callOnce, 0, config.interval(), TimeUnit.MILLISECONDS);
		LOG.info("[DDSR-Demo] calling " + config.contract() + " every " + config.interval() + " ms");
	}

	@Deactivate
	void deactivate() {
		if (schedule != null) {
			schedule.shutdownNow();
			schedule = null;
		}
	}

	/**
	 * The second contract, and the one worth watching.
	 *
	 * <p>Payment is served by a hand-written JAX-RS resource, so only
	 * the calling half of it is traced. This one is served by the
	 * generic REST distribution, which continues the caller's trace —
	 * so a trace of it has the consumer, the broker lookup it needed
	 * and the provider that answered, in one tree.
	 */
	private void callProbe(Config current) {
		if (current.probeContract() == null || current.probeContract().isBlank()) {
			return;
		}
		List<ServiceLocator> probes = client.consumer().find(current.probeContract(), null);
		if (probes.isEmpty()) {
			LOG.fine(() -> "[DDSR-Demo] nobody serves " + current.probeContract());
			return;
		}
		String answer = proxies.newProxy(ProbeRemote.class, probes.get(0))
				.echo(current.account(), "EUR", "demo");
		LOG.fine(() -> "[DDSR-Demo] probe answered " + answer);
	}

	/**
	 * One round: find, call, let go.
	 *
	 * <p>Looked up per call rather than bound once, because that is what
	 * makes the demo show the registry working — a provider that is
	 * restarted mid-presentation is picked up on the next tick, and the
	 * release keeps the broker's lease count honest.
	 */
	private void callOnce() {
		Config current = config;
		CallTracer watching = tracer;
		try (CallSpan tick = watching == null ? CallSpan.NONE : watching.doing("Demo/tick")) {
			tick.attribute("fennec.demo.contract", current.contract());
			oneRound(current);
		}
	}

	private void oneRound(Config current) {
		String referenceId = null;
		try {
			List<ServiceLocator> found = client.consumer().find(current.contract(), null);
			if (found.isEmpty()) {
				LOG.fine(() -> "[DDSR-Demo] nobody serves " + current.contract() + " right now");
				return;
			}
			ServiceLocator locator = found.get(0);
			referenceId = locator.reference() == null ? null : locator.reference().getId();
			PaymentRemote payment = proxies.newProxy(PaymentRemote.class, locator);

			double balance = payment.getBalance(current.account());
			// Every third call moves money, so the dashboard shows two
			// operations rather than one repeated forever.
			if (ThreadLocalRandom.current().nextInt(3) == 0) {
				double left = payment.charge(ThreadLocalRandom.current().nextInt(1, 20), "EUR");
				LOG.info("[DDSR-Demo] charged, " + left + " left (balance was " + balance + ")");
			} else {
				LOG.fine(() -> "[DDSR-Demo] balance " + balance);
			}
			callProbe(current);
		} catch (RuntimeException callFailed) {
			// Expected while a provider is starting, restarting or gone.
			LOG.log(Level.FINE, callFailed, () -> "[DDSR-Demo] call failed");
		} finally {
			if (referenceId != null) {
				try {
					client.consumer().release(referenceId);
				} catch (RuntimeException ignored) {
					// The broker will expire the claim on its own.
				}
			}
		}
	}
}
