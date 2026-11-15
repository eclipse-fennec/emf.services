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

package org.eclipse.fennec.services.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.gecko.messaging.api.runtime.ConnectionEvent;
import org.gecko.messaging.api.runtime.ConnectionListener;
import org.gecko.messaging.api.runtime.ConnectionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * A link whose instance gives up on its broker says so, once, and is dead
 * from then on (#297).
 */
class MessagingLinkGivenUpTest {

	private final BundleContext context = mock(BundleContext.class);
	private ConnectionListener instance;
	private MessagingLink link;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void open() throws InvalidSyntaxException, IOException {
		when(context.createFilter(anyString())).thenReturn(mock(Filter.class));
		ServiceReference<ConfigurationAdmin> admin = mock(ServiceReference.class);
		when(context.getServiceReference(ConfigurationAdmin.class)).thenReturn(admin);
		ConfigurationAdmin configurations = mock(ConfigurationAdmin.class);
		when(context.getService(admin)).thenReturn(configurations);
		when(configurations.getFactoryConfiguration(anyString(), anyString(), eq("?")))
				.thenReturn(mock(Configuration.class));
		ArgumentCaptor<ConnectionListener> listener = ArgumentCaptor.forClass(ConnectionListener.class);
		when(context.registerService(eq(ConnectionListener.class), listener.capture(), any()))
				.thenReturn(mock(ServiceRegistration.class));

		link = MessagingLink.open(context, LinkSettings.of("test", "tcp://localhost:1883", "test-id"));
		instance = listener.getValue();
		verify(context).registerService(eq(ConnectionListener.class), any(ConnectionListener.class), any());
	}

	private void state(ConnectionState state, Throwable cause) {
		instance.connectionChanged(new ConnectionEvent("test", null, state, false, false, 0, cause));
	}

	@Test
	void givingUpIsToldOnceWithWhy() throws Exception {
		List<Throwable> told = new CopyOnWriteArrayList<>();
		link.onGivenUp(told::add);
		state(ConnectionState.CONNECTED, null);
		assertThat(link.state()).isEqualTo("connected");

		ConnectException refused = new ConnectException("refused");
		state(ConnectionState.CLOSED, refused);
		state(ConnectionState.CLOSED, refused);

		waitFor(() -> !told.isEmpty());
		Thread.sleep(50);
		assertThat(told).containsExactly(refused);
		assertThat(link.hasGivenUp()).isTrue();
		assertThat(link.isConnected()).isFalse();
		assertThat(link.state()).isEqualTo("gave-up");
	}

	@Test
	void aLinkThatGaveUpConnectsNoMoreAndWaitsForNothing() throws Exception {
		state(ConnectionState.CLOSED, new ConnectException("refused"));
		state(ConnectionState.CONNECTED, null);

		assertThat(link.isConnected()).as("dead: a late event does not revive it").isFalse();
		long started = System.nanoTime();
		assertThat(link.awaitConnected(5, TimeUnit.SECONDS)).isFalse();
		assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started))
				.as("nobody waits for a link that gave up").isLessThan(1000);
	}

	@Test
	void anOwnerThatAsksLateIsToldAtOnce() throws Exception {
		state(ConnectionState.CLOSED, new ConnectException("refused"));
		List<Throwable> told = new CopyOnWriteArrayList<>();

		link.onGivenUp(told::add);

		waitFor(() -> !told.isEmpty());
		assertThat(told).hasSize(1);
	}

	@Test
	void aLossIsNotGivingUp() {
		state(ConnectionState.CONNECTED, null);
		state(ConnectionState.DISCONNECTED, new ConnectException("lost"));

		assertThat(link.hasGivenUp()).isFalse();
		assertThat(link.state()).isEqualTo("reconnecting");
	}

	private static void waitFor(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5_000;
		while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
			Thread.sleep(10);
		}
	}
}
