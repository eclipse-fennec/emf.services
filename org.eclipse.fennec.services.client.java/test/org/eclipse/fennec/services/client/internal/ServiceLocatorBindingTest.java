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

package org.eclipse.fennec.services.client.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.TrackedServiceLocator.State;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a locator is bound to, and that the parts of it belong together
 * (#124).
 *
 * <p>The reference, its implementation and the state used to be three
 * separate volatile fields written by two methods that do not exclude
 * each other — {@code rebind} on a caller's thread, {@code onEvent} on
 * the event stream's. They are one value now, so a reader can only ever
 * see a pair that really existed.
 *
 * <p><b>That part is by construction and is not demonstrated here.</b>
 * The old defect needed an interleaving between two field writes, which
 * no single-threaded test can produce and no multi-threaded one can
 * produce reliably; a test that started two threads and hoped would
 * pass for the wrong reason. What is checked here is everything that
 * IS checkable: that each transition replaces the whole binding, and
 * that the pair a caller reads always comes from one registration.
 */
class ServiceLocatorBindingTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	@Test
	@DisplayName("a MODIFIED replaces reference and implementation together")
	void modifiedReplacesBothHalves() {
		ServiceLocatorImpl locator = locator("ref-1", "payments-1");

		locator.onEvent(modified("ref-2", "payments-2"), false);

		assertThat(locator.reference().getId()).isEqualTo("ref-2");
		assertThat(locator.implementation().getName())
				.as("the implementation must come from the same event as the reference")
				.isEqualTo("payments-2");
		assertThat(locator.state()).isEqualTo(State.LIVE);
	}

	@Test
	@DisplayName("a MODIFIED the locator cannot read leaves the old pair intact")
	void anUnreadableModifiedKeepsThePreviousPair() {
		ServiceLocatorImpl locator = locator("ref-1", "payments-1");

		// No provider subtree: the event says something changed but not
		// what. The locator may not take half of it.
		ServiceReference bare = F.createServiceReference();
		bare.setId("ref-2");
		ServiceEvent event = F.createServiceEvent();
		event.setType(ServiceEventType.MODIFIED);
		event.setReference(bare);
		locator.onEvent(event, false);

		assertThat(locator.state()).isEqualTo(State.MODIFIED);
		assertThat(locator.reference().getId())
				.as("nothing was replaced, because not everything could be")
				.isEqualTo("ref-1");
		assertThat(locator.implementation().getName()).isEqualTo("payments-1");
	}

	@Test
	@DisplayName("a withdrawal changes only what it knows: the state, not the pair")
	void anUnregisteringOnlyChangesTheState() {
		ServiceLocatorImpl locator = locator("ref-1", "payments-1");

		ServiceEvent event = F.createServiceEvent();
		event.setType(ServiceEventType.UNREGISTERING);
		event.setReference(F.createServiceReference());
		locator.onEvent(event, false);

		assertThat(locator.state()).isEqualTo(State.REBIND);
		assertThat(locator.reference().getId())
				.as("a locator still names what it was bound to until it rebinds")
				.isEqualTo("ref-1");
	}

	@Test
	@DisplayName("a rebind replaces the whole binding with what it resolved")
	void aRebindReplacesTheWholeBinding() {
		ServiceReference fresh = reference("ref-9");
		ServiceImplementation freshImpl = implementation("payments-9", fresh);
		ServiceLocatorImpl locator = new ServiceLocatorImpl(reference("ref-1"),
				implementation("payments-1", reference("ref-1")),
				(name, filter) -> List.of(new ServiceLocatorImpl.Resolved(fresh, freshImpl)),
				"Payment", null);

		assertThat(locator.rebind(true)).isTrue();

		assertThat(locator.reference().getId()).isEqualTo("ref-9");
		assertThat(locator.implementation().getName()).isEqualTo("payments-9");
		assertThat(locator.state()).isEqualTo(State.LIVE);
	}

	@Test
	@DisplayName("a rebind that finds nothing leaves the binding exactly as it was")
	void aFailedRebindChangesNothing() {
		ServiceLocatorImpl locator = new ServiceLocatorImpl(reference("ref-1"),
				implementation("payments-1", reference("ref-1")),
				(name, filter) -> List.of(),
				"Payment", null);
		ServiceEvent gone = F.createServiceEvent();
		gone.setType(ServiceEventType.UNREGISTERING);
		gone.setReference(F.createServiceReference());
		locator.onEvent(gone, false);

		assertThat(locator.rebind(true)).isFalse();

		assertThat(locator.reference().getId()).isEqualTo("ref-1");
		assertThat(locator.state())
				.as("still asking to rebind, so the next use tries again")
				.isEqualTo(State.REBIND);
	}

	// ------------------------------------------------------------------
	// Fixtures
	// ------------------------------------------------------------------

	private static ServiceLocatorImpl locator(String refId, String implName) {
		ServiceReference ref = reference(refId);
		return new ServiceLocatorImpl(ref, implementation(implName, ref));
	}

	private static ServiceReference reference(String id) {
		ServiceReference ref = F.createServiceReference();
		ref.setId(id);
		return ref;
	}

	private static ServiceImplementation implementation(String name, ServiceReference ref) {
		ServiceImplementation impl = F.createServiceImplementation();
		impl.setName(name);
		RestFlavor flavor = F.createRestFlavor();
		flavor.setHost("http://localhost:9090");
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		ServiceProvider provider = F.createServiceProvider();
		provider.setName("provider-" + name);
		provider.getImplementations().add(impl);
		ref.setProvider(provider);
		return impl;
	}

	/** A MODIFIED as the broker sends it: self-contained, one implementation. */
	private static ServiceEvent modified(String refId, String implName) {
		ServiceReference ref = reference(refId);
		implementation(implName, ref);
		ServiceEvent event = F.createServiceEvent();
		event.setType(ServiceEventType.MODIFIED);
		event.setReference(ref);
		return event;
	}
}
