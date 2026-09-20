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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.runtime.BindingDTO;
import org.eclipse.fennec.services.runtime.ClientRuntime;
import org.eclipse.fennec.services.runtime.ClientRuntimeDTO;
import org.eclipse.fennec.services.runtime.PublishedDTO;

/**
 * This runtime, as a snapshot somebody else can read (#126).
 *
 * <p>The counterpart of the broker's, and a different question: the
 * broker knows what exists, a client knows what <em>it</em> published,
 * what it is bound to, and whether it is still hearing anything. The
 * last one has no other source — a broker sees a subscription, and only
 * the consumer knows whether it is reading one.
 */
final class ClientRuntimeImpl implements ClientRuntime {

	private final ProviderImpl provider;

	private final ConsumerImpl consumer;

	private final BooleanSupplier streamConnected;

	private final Supplier<String> eventTransport;

	/** The last state this runtime was seen in, as a fingerprint. */
	private int lastFingerprint;

	/** How often that fingerprint has moved. */
	private long changes;

	ClientRuntimeImpl(ProviderImpl provider, ConsumerImpl consumer, BooleanSupplier streamConnected,
			Supplier<String> eventTransport) {
		this.provider = provider;
		this.consumer = consumer;
		this.streamConnected = streamConnected;
		this.eventTransport = eventTransport;
		this.lastFingerprint = fingerprint();
	}

	/**
	 * How often this runtime has changed, counted the way OSGi expects:
	 * a number that only ever grows.
	 *
	 * <p>What it counts is derived rather than reported, which is where
	 * this side differs from the broker's. The broker funnels every
	 * mutation through one facade, so a counter there is exact and free.
	 * A client's state changes in many places — a locator rebinds inside
	 * event handling, a stream drops on its own thread — and a counter
	 * incremented at the call sites would have to be remembered at every
	 * one of them. A missed call site is a watcher that is never told,
	 * and that failure is silent.
	 *
	 * <p>So the change is <em>detected</em>: a fingerprint of what a
	 * snapshot would say, cheap to compute over a handful of
	 * registrations and bindings, compared against the last one seen.
	 * The count moves when the fingerprint does, and a watcher gets the
	 * monotonic number the OSGi idiom promises without this side having
	 * to promise it at every call site.
	 */
	synchronized long changeCount() {
		int current = fingerprint();
		if (current != lastFingerprint) {
			lastFingerprint = current;
			changes++;
		}
		return changes;
	}

	private int fingerprint() {
		int fingerprint = Boolean.hashCode(streamConnected.getAsBoolean());
		for (RegistrationImpl registration : provider.published()) {
			ServiceReference reference = registration.reference();
			fingerprint = 31 * fingerprint + (reference == null ? 0 : reference.getId().hashCode());
		}
		for (ServiceLocatorImpl locator : consumer.tracked()) {
			ServiceReference reference = locator.reference();
			fingerprint = 31 * fingerprint + (reference == null ? 0 : reference.getId().hashCode());
			fingerprint = 31 * fingerprint + (locator.state() == null ? 0 : locator.state().hashCode());
		}
		return fingerprint;
	}

	@Override
	public ClientRuntimeDTO snapshot() {
		ClientRuntimeDTO dto = new ClientRuntimeDTO();
		dto.consumerId = consumer.consumerId();
		dto.changeCount = changeCount();
		dto.takenAt = System.currentTimeMillis();
		dto.supportedFlavors = new ArrayList<>();
		for (FlavorKind kind : consumer.supportedFlavors()) {
			dto.supportedFlavors.add(kind.getName());
		}
		dto.published = published();
		dto.bindings = bindings();
		dto.eventStreamConnected = streamConnected.getAsBoolean();
		dto.eventTransport = eventTransport.get();
		return dto;
	}

	private List<PublishedDTO> published() {
		List<PublishedDTO> all = new ArrayList<>();
		for (RegistrationImpl registration : provider.published()) {
			PublishedDTO dto = new PublishedDTO();
			ServiceImplementation implementation = registration.implementation();
			if (implementation != null) {
				dto.implementationId = implementation.getImplementationId();
				dto.version = implementation.getVersion();
				dto.contracts = new ArrayList<>();
				for (ServiceInterface contract : implementation.getServiceInterfaces()) {
					dto.contracts.add(contract.getName());
				}
			}
			ServiceReference reference = registration.reference();
			dto.referenceId = reference == null ? null : reference.getId();
			// Live means the broker is holding it, which is exactly what
			// having a reference means — a publish that was refused has
			// none, and a registration the broker retired lost it.
			dto.live = dto.referenceId != null;
			dto.failure = failureOf(registration.diagnostic());
			all.add(dto);
		}
		return all;
	}

	/**
	 * What the broker objected to, or null if it did not.
	 *
	 * <p>Only the message, not the diagnostic: a DTO that carried EMF
	 * would need EMF to read, and a watcher reporting this to a metrics
	 * backend has a string-shaped hole to put it in.
	 */
	private static String failureOf(Diagnostic diagnostic) {
		if (diagnostic == null || diagnostic.getSeverity() == DiagnosticSeverity.OK) {
			return null;
		}
		return diagnostic.getSeverity().getName() + ": " + diagnostic.getMessage();
	}

	/**
	 * Where a binding calls, as its flavor states it.
	 *
	 * <p>Not {@code endpointFor}, which answers per operation and needs
	 * a name nobody has here — what a watcher wants is the address the
	 * provider announced, which is the flavor's own.
	 */
	private static String addressOf(ServiceImplementation implementation) {
		if (implementation == null) {
			return null;
		}
		for (ServiceFlavor flavor : implementation.getFlavors()) {
			if (flavor instanceof RestFlavor rest) {
				return (rest.getHost() == null ? "" : rest.getHost())
						+ (rest.getBasePath() == null ? "" : rest.getBasePath());
			}
			if (flavor instanceof MqttFlavor mqtt && !mqtt.getBrokers().isEmpty()) {
				return mqtt.getBrokers().get(0) + " " + mqtt.getRequestTopic();
			}
		}
		return null;
	}

	private List<BindingDTO> bindings() {
		List<BindingDTO> all = new ArrayList<>();
		for (ServiceLocatorImpl locator : consumer.tracked()) {
			BindingDTO dto = new BindingDTO();
			dto.contract = locator.interfaceName();
			dto.filter = locator.filter();
			ServiceReference reference = locator.reference();
			dto.referenceId = reference == null ? null : reference.getId();
			dto.endpoint = addressOf(locator.implementation());
			dto.state = locator.state() == null ? null : locator.state().name();
			all.add(dto);
		}
		return all;
	}
}
