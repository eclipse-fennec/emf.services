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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * The properties a caller hands in survive every change-count update
 * (#188) — the part a caller cannot arrange from outside, because
 * {@link ServiceRegistration#setProperties} replaces the dictionary.
 */
class ChangeCountPublisherTest {

	interface Answer {
	}

	private final FakeRegistrations framework = new FakeRegistrations();

	private final AtomicLong changeCount = new AtomicLong(3);

	@Test
	void registersWithTheChangeCountAlone() {
		try (ChangeCountPublisher<Answer> publisher = new ChangeCountPublisher<>(framework.context(), Answer.class,
				new Answer() {
				}, changeCount::get, "test runtime")) {
			publisher.open();

			assertThat(framework.registeredAs).isEqualTo(Answer.class.getName());
			assertThat(framework.properties.get(Constants.SERVICE_CHANGECOUNT)).isEqualTo(3L);
			assertThat(framework.properties.size()).isEqualTo(1);
		}
	}

	@Test
	void extraPropertiesAreRegisteredAndKeptOnEveryUpdate() {
		Dictionary<String, Object> extra = new Hashtable<>();
		extra.put("runtime.endpoint", "http://node-a/rest");

		try (ChangeCountPublisher<Answer> publisher = new ChangeCountPublisher<>(framework.context(), Answer.class,
				new Answer() {
				}, changeCount::get, "test runtime", extra)) {
			publisher.open();
			assertThat(framework.properties.get("runtime.endpoint")).isEqualTo("http://node-a/rest");

			changeCount.set(4);
			framework.awaitChangeCount(4L);

			assertThat(framework.updates).isPositive();
			assertThat(framework.properties.get("runtime.endpoint")).isEqualTo("http://node-a/rest");
		}
	}

	@Test
	void theCallerCannotOverrideTheChangeCount() {
		Dictionary<String, Object> extra = new Hashtable<>();
		extra.put(Constants.SERVICE_CHANGECOUNT, 99L);

		try (ChangeCountPublisher<Answer> publisher = new ChangeCountPublisher<>(framework.context(), Answer.class,
				new Answer() {
				}, changeCount::get, "test runtime", extra)) {
			publisher.open();

			assertThat(framework.properties.get(Constants.SERVICE_CHANGECOUNT)).isEqualTo(3L);
		}
	}

	@Test
	void theDictionaryIsCopiedNotHeld() {
		Dictionary<String, Object> extra = new Hashtable<>();
		extra.put("runtime.name", "a");

		try (ChangeCountPublisher<Answer> publisher = new ChangeCountPublisher<>(framework.context(), Answer.class,
				new Answer() {
				}, changeCount::get, "test runtime", extra)) {
			extra.put("runtime.name", "b");
			publisher.open();

			assertThat(framework.properties.get("runtime.name")).isEqualTo("a");
		}
	}

	@Test
	void closeUnregisters() {
		ChangeCountPublisher<Answer> publisher = new ChangeCountPublisher<>(framework.context(), Answer.class,
				new Answer() {
				}, changeCount::get, "test runtime");
		publisher.open();

		publisher.close();

		assertThat(framework.unregistered).isTrue();
	}

	/**
	 * Just enough of a framework to hold one registration: the
	 * {@link BundleContext} and the {@link ServiceRegistration} are
	 * proxies that record what they were asked.
	 */
	@SuppressWarnings("unchecked")
	private static final class FakeRegistrations {

		volatile String registeredAs;

		volatile Dictionary<String, ?> properties;

		volatile int updates;

		volatile boolean unregistered;

		BundleContext context() {
			return proxy(BundleContext.class, (method, args) -> {
				if (method.equals("registerService")) {
					registeredAs = ((Class<?>) args[0]).getName();
					properties = (Dictionary<String, ?>) args[2];
					return registration();
				}
				throw new UnsupportedOperationException(method);
			});
		}

		private ServiceRegistration<?> registration() {
			return proxy(ServiceRegistration.class, (method, args) -> {
				switch (method) {
				case "setProperties" -> {
					properties = (Dictionary<String, ?>) args[0];
					updates++;
				}
				case "unregister" -> unregistered = true;
				default -> throw new UnsupportedOperationException(method);
				}
				return null;
			});
		}

		void awaitChangeCount(long expected) {
			long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
			while (!Long.valueOf(expected).equals(properties.get(Constants.SERVICE_CHANGECOUNT))) {
				assertThat(System.nanoTime()).as("change count %d published", expected).isLessThan(deadline);
				LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
			}
		}

		private interface Handler {
			Object handle(String method, Object[] args);
		}

		private static <P> P proxy(Class<P> type, Handler handler) {
			return (P) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
					(self, method, args) -> handler.handle(method.getName(), args));
		}
	}
}
