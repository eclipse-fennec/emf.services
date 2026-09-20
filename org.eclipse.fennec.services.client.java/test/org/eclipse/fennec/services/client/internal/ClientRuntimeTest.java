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

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.runtime.ClientRuntimeDTO;
import org.eclipse.fennec.services.runtime.PublishedDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a runtime using the SDK says about itself (#126).
 *
 * <p>Two things are worth pinning: that it reports what it published
 * and what it is bound to, and that its changeCount is derived from that
 * state rather than counted — because a counter here would have to be
 * remembered at every call site, and a forgotten one is a watcher that
 * is never told.
 */
class ClientRuntimeTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	/** A broker that takes every publish and hands back a reference. */
	private static final class AcceptingBroker implements BrokerImplementations, BrokerLookup {

		private int published;

		/** What the broker says about a publish; OK unless a test says otherwise. */
		private DiagnosticSeverity severity = DiagnosticSeverity.OK;

		private String message;

		@Override
		public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			published++;
			Diagnostic answer = F.createDiagnostic();
			answer.setSeverity(severity);
			answer.setMessage(message);
			return answer;
		}

		@Override
		public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			Diagnostic ok = F.createDiagnostic();
			ok.setSeverity(DiagnosticSeverity.OK);
			return ok;
		}

		@Override
		public Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			return publishImplementation(provider, implementation);
		}

		@Override
		public Diagnostic heartbeat(String referenceId, long intervalSeconds) {
			Diagnostic ok = F.createDiagnostic();
			ok.setSeverity(DiagnosticSeverity.OK);
			return ok;
		}

		@Override
		public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ServiceReference getServiceReference(String interfaceName) {
			return references(interfaceName).stream().findFirst().orElse(null);
		}

		@Override
		public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
				ConsumerCapability capability) {
			return references(interfaceName);
		}

		@Override
		public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
				ConsumerCapability capability) {
			return references(interfaceName);
		}

		@Override
		public ServiceImplementation getImplementationForReference(ServiceReference reference) {
			return reference.getProvider() == null || reference.getProvider().getImplementations().isEmpty()
					? null
					: reference.getProvider().getImplementations().get(0);
		}

		private List<ServiceReference> references(String interfaceName) {
			if (published == 0) {
				return List.of();
			}
			ServiceProvider provider = provider(interfaceName);
			ServiceReference reference = F.createServiceReference();
			reference.setId("ref-" + interfaceName);
			reference.setProvider(provider);
			return List.of(reference);
		}
	}

	private static ServiceInterface contract(String name) {
		ServiceInterface contract = F.createServiceInterface();
		contract.setName(name);
		contract.setVersion("1.0.0");
		return contract;
	}

	private static ServiceProvider provider(String contractName) {
		ServiceImplementation implementation = F.createServiceImplementation();
		implementation.setName("impl-" + contractName);
		implementation.setVersion("1.0.0");
		implementation.setImplementationId("node:" + contractName + ":1.0.0");
		implementation.getServiceInterfaces().add(contract(contractName));
		RestFlavor flavor = F.createRestFlavor();
		flavor.setName("rest");
		flavor.setKind(FlavorKind.REST);
		flavor.setHost("http://localhost:9091");
		flavor.setBasePath("/payments");
		implementation.getFlavors().add(flavor);
		ServiceProvider provider = F.createServiceProvider();
		provider.setName("node");
		provider.setVersion("1.0.0");
		provider.getImplementations().add(implementation);
		return provider;
	}

	private DdsrClientImpl clientOf(AcceptingBroker broker) {
		return new DdsrClientImpl(broker, broker, List.of(FlavorKind.REST), "consumer-1", null, false,
				() -> "rest");
	}

	@Test
	@DisplayName("a runtime that has done nothing says so, with its identity")
	void emptyRuntime() {
		ClientRuntimeDTO snapshot = clientOf(new AcceptingBroker()).runtimeSnapshot();

		assertThat(snapshot.consumerId).isEqualTo("consumer-1");
		assertThat(snapshot.supportedFlavors).containsExactly(FlavorKind.REST.getName());
		assertThat(snapshot.eventTransport)
			.as("which transport it listens over, as the source itself says")
			.isEqualTo("rest");
		assertThat(snapshot.published).isEmpty();
		assertThat(snapshot.bindings).isEmpty();
		assertThat(snapshot.eventStreamConnected).isFalse();
	}

	@Test
	@DisplayName("what this runtime published is reported, with what the broker made of it")
	void publishedIsReported() {
		AcceptingBroker broker = new AcceptingBroker();
		DdsrClientImpl client = clientOf(broker);
		ServiceProvider provider = provider("Payment");

		client.provider().publish(provider, provider.getImplementations().get(0));

		ClientRuntimeDTO snapshot = client.runtimeSnapshot();
		assertThat(snapshot.published).hasSize(1);
		assertThat(snapshot.published.get(0).implementationId).isEqualTo("node:Payment:1.0.0");
		assertThat(snapshot.published.get(0).contracts).containsExactly("Payment");
	}

	@Test
	@DisplayName("a binding is reported with the address its flavor announced")
	void bindingsAreReported() {
		AcceptingBroker broker = new AcceptingBroker();
		DdsrClientImpl client = clientOf(broker);
		ServiceProvider provider = provider("Payment");
		client.provider().publish(provider, provider.getImplementations().get(0));

		client.consumer().find("Payment", null);

		ClientRuntimeDTO snapshot = client.runtimeSnapshot();
		assertThat(snapshot.bindings).hasSize(1);
		assertThat(snapshot.bindings.get(0).contract).isEqualTo("Payment");
		assertThat(snapshot.bindings.get(0).referenceId).isEqualTo("ref-Payment");
		assertThat(snapshot.bindings.get(0).endpoint)
			.as("where it calls, which is the flavor's own statement and not an operation's")
			.isEqualTo("http://localhost:9091/payments");
		assertThat(snapshot.bindings.get(0).state).isEqualTo("LIVE");
	}

	@Test
	@DisplayName("a publish the broker grumbled about says so, and says why")
	void publishFailureIsReported() {
		AcceptingBroker broker = new AcceptingBroker();
		broker.severity = DiagnosticSeverity.WARNING;
		broker.message = "contract is deprecated";
		DdsrClientImpl client = clientOf(broker);
		ServiceProvider provider = provider("Payment");

		client.provider().publish(provider, provider.getImplementations().get(0));

		PublishedDTO published = client.runtimeSnapshot().published.get(0);
		assertThat(published.live)
			.as("a warning is not a refusal — the broker took it")
			.isTrue();
		assertThat(published.failure).isEqualTo("WARNING: contract is deprecated");
	}

	@Test
	@DisplayName("the change count follows the state, without anything having to report it")
	void changeCountFollowsTheState() {
		AcceptingBroker broker = new AcceptingBroker();
		DdsrClientImpl client = clientOf(broker);
		long empty = client.runtimeChangeCount();

		assertThat(client.runtimeChangeCount())
			.as("asking twice about an unchanged runtime gives the same answer")
			.isEqualTo(empty);

		ServiceProvider provider = provider("Payment");
		client.provider().publish(provider, provider.getImplementations().get(0));
		long afterPublish = client.runtimeChangeCount();
		assertThat(afterPublish)
			.as("and it only ever grows, which is what the OSGi idiom promises")
			.isGreaterThan(empty);

		client.consumer().find("Payment", null);
		assertThat(client.runtimeChangeCount())
			.as("a binding nobody announced still changes what this runtime holds")
			.isGreaterThan(afterPublish);
	}
}
