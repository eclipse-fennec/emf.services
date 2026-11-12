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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * A dispatcher is never the last user of the implementation it calls.
 *
 * <p>Taking a delayed component for one call and returning it disposes
 * of it between calls, and two calls at once race that disposal — one
 * in two hundred lookups against a broker answered 500. One use held
 * for as long as the service is the one a call would get is what keeps
 * the count from reaching zero.
 */
class HeldImplementationTest {

	@SuppressWarnings("unchecked")
	private static ServiceObjects<Object> objectsOf(ServiceReference<Object> reference) {
		ServiceObjects<Object> objects = mock(ServiceObjects.class);
		when(objects.getServiceReference()).thenReturn(reference);
		return objects;
	}

	@SuppressWarnings("unchecked")
	private static ServiceReference<Object> reference() {
		return mock(ServiceReference.class);
	}

	@Test
	void theServiceIsTakenOnceHoweverManyCallsFollow() {
		BundleContext context = mock(BundleContext.class);
		ServiceReference<Object> lookup = reference();
		when(context.getService(lookup)).thenReturn(new Object());
		HeldImplementation held = new HeldImplementation(context);
		ServiceObjects<Object> objects = objectsOf(lookup);

		assertThat(held.holding(objects)).isSameAs(objects);
		held.holding(objects);
		held.holding(objectsOf(lookup));

		verify(context, times(1)).getService(lookup);
		verify(context, never()).ungetService(any());
	}

	@Test
	void theHoldMovesWhenAnotherServiceTakesItsPlace() {
		BundleContext context = mock(BundleContext.class);
		ServiceReference<Object> first = reference();
		ServiceReference<Object> second = reference();
		when(context.getService(first)).thenReturn(new Object());
		when(context.getService(second)).thenReturn(new Object());
		HeldImplementation held = new HeldImplementation(context);

		held.holding(objectsOf(first));
		held.holding(objectsOf(second));

		InOrder order = inOrder(context);
		order.verify(context).getService(first);
		order.verify(context).getService(second);
		order.verify(context).ungetService(first);
	}

	@Test
	void nothingImplementingTheContractLetsGoOfWhatWasHeld() {
		BundleContext context = mock(BundleContext.class);
		ServiceReference<Object> lookup = reference();
		when(context.getService(lookup)).thenReturn(new Object());
		HeldImplementation held = new HeldImplementation(context);
		held.holding(objectsOf(lookup));

		assertThat(held.holding(null)).isNull();

		verify(context).ungetService(lookup);
	}

	@Test
	void releaseLetsGoOnceAndOnlyOnce() {
		BundleContext context = mock(BundleContext.class);
		ServiceReference<Object> lookup = reference();
		when(context.getService(lookup)).thenReturn(new Object());
		HeldImplementation held = new HeldImplementation(context);
		held.holding(objectsOf(lookup));

		held.release();
		held.release();

		verify(context, times(1)).ungetService(lookup);
	}

	@Test
	void aServiceThatLeftWhileBeingTakenIsTheCallsToFindOut() {
		BundleContext context = mock(BundleContext.class);
		ServiceReference<Object> gone = reference();
		when(context.getService(gone)).thenThrow(new IllegalStateException("unregistered"));
		HeldImplementation held = new HeldImplementation(context);
		ServiceObjects<Object> objects = objectsOf(gone);

		assertThat(held.holding(objects)).isSameAs(objects);
		assertThatCode(held::release).doesNotThrowAnyException();
		verify(context, never()).ungetService(any());
	}
}
