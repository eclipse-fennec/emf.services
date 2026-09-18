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

package org.eclipse.fennec.services.provider.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Which candidate is the implementation.
 *
 * <p>The distribution looks its service up by a filter that usually
 * names the contract — and Declarative Services puts the configuration's
 * properties on the service this component itself registers. So the
 * component matches its own filter, and the one candidate it must never
 * pick is itself.
 */
class ImplementationLookupTest {

	/** A candidate with just the one property this rule reads. */
	private static ServiceReference<?> candidate(Object componentId) {
		return new ServiceReference<Object>() {
			@Override
			public Object getProperty(String key) {
				return "component.id".equals(key) ? componentId : null;
			}

			@Override
			public String[] getPropertyKeys() {
				return new String[] { "component.id" };
			}

			@Override
			public Bundle getBundle() {
				return null;
			}

			@Override
			public Bundle[] getUsingBundles() {
				return new Bundle[0];
			}

			@Override
			public boolean isAssignableTo(Bundle bundle, String className) {
				return true;
			}

			@Override
			public int compareTo(Object other) {
				return 0;
			}

			@Override
			public <A> A adapt(Class<A> type) {
				return null;
			}

			@Override
			public Dictionary<String, Object> getProperties() {
				Dictionary<String, Object> properties = new Hashtable<>();
				if (componentId != null) {
					properties.put("component.id", componentId);
				}
				return properties;
			}

			@Override
			public String toString() {
				return "candidate(component.id=" + componentId + ")";
			}
		};
	}

	@Test
	void theOwnRegistrationIsSkipped() {
		ServiceReference<?> self = candidate(7L);
		ServiceReference<?> implementation = candidate(9L);

		assertThat(GenericRestDistribution.someoneElse(
				new ServiceReference<?>[] { self, implementation }, 7L))
				.isSameAs(implementation);
	}

	@Test
	void withOnlyItselfThereIsNoImplementation() {
		assertThat(GenericRestDistribution.someoneElse(new ServiceReference<?>[] { candidate(7L) }, 7L))
				.as("answering 503 is right; invoking the contract on ourselves is not")
				.isNull();
	}

	@Test
	void anyOtherCandidateIsTakenAsItComes() {
		ServiceReference<?> first = candidate(1L);

		assertThat(GenericRestDistribution.someoneElse(
				new ServiceReference<?>[] { first, candidate(2L) }, 7L))
				.isSameAs(first);
	}

	@Test
	void nothingFoundIsNotAnError() {
		assertThat(GenericRestDistribution.someoneElse(null, 7L)).isNull();
		assertThat(GenericRestDistribution.someoneElse(new ServiceReference<?>[0], 7L)).isNull();
	}
}
