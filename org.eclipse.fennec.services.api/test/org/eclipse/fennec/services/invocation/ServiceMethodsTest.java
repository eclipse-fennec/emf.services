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

package org.eclipse.fennec.services.invocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

/** The method a dispatcher calls is one it may call (#266). */
class ServiceMethodsTest {

	public interface Greeter {
		String greet(String name);
	}

	/** What a provider often serves: a class nobody outside may see, implementing a public contract. */
	private static final class Hidden implements Greeter {

		@Override
		public String greet(String name) {
			return "Hello, " + name;
		}

		@SuppressWarnings("unused")
		public String whisper() {
			return "psst";
		}
	}

	@Test
	void aHiddenClassIsCalledThroughItsPublicInterface() throws Exception {
		Object service = new Hidden();
		Method method = ServiceMethods.require(service, "greet", 1);

		assertThat(method.getDeclaringClass()).isEqualTo(Greeter.class);
		assertThat(method.invoke(service, "you")).isEqualTo("Hello, you");
	}

	@Test
	void aLambdaAndAProxyAreCalledToo() throws Exception {
		Greeter lambda = name -> "Hi, " + name;
		assertThat(ServiceMethods.require(lambda, "greet", 1).invoke(lambda, "you")).isEqualTo("Hi, you");

		Object proxy = Proxy.newProxyInstance(Greeter.class.getClassLoader(), new Class<?>[] { Greeter.class },
				(self, called, args) -> "Hey, " + args[0]);
		assertThat(ServiceMethods.require(proxy, "greet", 1).invoke(proxy, "you")).isEqualTo("Hey, you");
	}

	@Test
	void aMethodOnlyTheClassHasIsTakenFromTheClassAndAMissingOneIsNamed() {
		assertThat(ServiceMethods.find(new Hidden(), "whisper", 0)).get()
				.extracting(Method::getDeclaringClass).isEqualTo(Hidden.class);
		assertThatThrownBy(() -> ServiceMethods.require(new Hidden(), "greet", 2))
				.isInstanceOf(NoSuchMethodException.class).hasMessageContaining("'greet' taking 2");
	}
}
