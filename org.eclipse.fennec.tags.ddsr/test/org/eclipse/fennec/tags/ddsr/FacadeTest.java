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


package org.eclipse.fennec.tags.ddsr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclEngineImpl;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclSettingDelegateFactory;
import org.eclipse.fennec.m2x.ocl.parser.OclParserSupport;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.tags.TagRegistry;
import org.eclipse.fennec.tags.TagsFactory;
import org.eclipse.fennec.tags.TagsPackage;
import org.eclipse.fennec.tags.service.TagStore;
import org.eclipse.fennec.tags.service.TagSubjects;
import org.eclipse.fennec.tags.service.core.TagServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The facade's pieces (#279): what a tag lets a caller see, the lookup and
 * the events narrowed to it, and the broker's own contracts answered at
 * the facade's address.
 */
class FacadeTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	@BeforeAll
	static void registerOcl() {
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.put(
				"http://www.eclipse.org/fennec/m2x/ocl/1.0",
				new OclSettingDelegateFactory(new OclEngineImpl(new OclParserSupport())));
		((EStructuralFeature.Internal) TagsPackage.Literals.TAG__PATH).setSettingDelegate(null);
	}

	/** The broker's lookup adapter, as the facade calls it: by name and arity. */
	public static final class FakeLookup {
		final List<ServiceProvider> providers = new ArrayList<>();

		public LocalServiceRegistry getServiceReferences(String interfaceName, String filter, String flavors,
				String consumerId, String fingerprint) {
			LocalServiceRegistry answer = F.createLocalServiceRegistry();
			for (ServiceProvider provider : providers) {
				ServiceProvider copy = EcoreUtil.copy(provider);
				ServiceReference reference = F.createServiceReference();
				reference.setId("ref-" + provider.getName());
				reference.setProvider(copy);
				answer.getProviders().add(copy);
				answer.getReferences().add(reference);
			}
			return answer;
		}
	}

	/** The broker's events adapter: hands out a publisher the test drives. */
	public static final class FakeEvents {
		Flow.Subscriber<? super ServiceEvent> subscriber;

		public Flow.Publisher<ServiceEvent> events() {
			return s -> {
				subscriber = s;
				s.onSubscribe(new Flow.Subscription() {
					@Override
					public void request(long n) {
					}

					@Override
					public void cancel() {
					}
				});
			};
		}
	}

	private TagServiceImpl tags;

	@BeforeEach
	void setUp() {
		tags = new TagServiceImpl(new TagStore() {
			@Override
			public TagRegistry load() {
				return TagsFactory.eINSTANCE.createTagRegistry();
			}

			@Override
			public void save(TagRegistry registry) {
			}
		});
		tags.addSubjects(new TagSubjects() {
			@Override
			public Set<String> kinds() {
				return Set.of("contract", "implementation");
			}

			@Override
			public Collection<Subject> subjects(String kind) {
				return "implementation".equals(kind)
						? List.of(new Subject(kind, "card/Payment", Map.of(), List.of()),
								new Subject(kind, "paypal/Payment", Map.of(), List.of()))
						: List.of(new Subject(kind, "Payment", Map.of(), List.of()));
			}
		});
		tags.putTag(null, "payment", null, null, false);
		tags.putTag("payment", "card", null, null, false);
		tags.putTag(null, "other", null, null, false);
		tags.assign("payment/card", "implementation", "card/Payment");
	}

	@Test
	void aTagShowsWhatItsImplementationsOrContractsCarry() {
		Visibility card = Visibility.under(tags, "payment");

		assertThat(card.implementation("card", "Payment")).isTrue();
		assertThat(card.implementation("paypal", "Payment")).isFalse();
		assertThat(card.contract("BrokerLookup")).as("the broker's own consumer contracts are always there").isTrue();

		tags.assign("payment", "contract", "Payment");
		assertThat(Visibility.under(tags, "payment").implementation("paypal", "Payment"))
				.as("a tagged contract shows every implementation of it").isTrue();
	}

	@Test
	void theLookupKeepsOnlyWhatTheTagCovers() throws Throwable {
		FakeLookup broker = new FakeLookup();
		broker.providers.add(provider("card", "Payment"));
		broker.providers.add(provider("paypal", "Payment"));
		Set<String> shown = ConcurrentHashMap.newKeySet();

		LocalServiceRegistry answer = new ScopedLookup(() -> broker, tags, "payment", shown)
				.getServiceReferences("Payment", null, null, null, null);

		assertThat(answer.getReferences()).extracting(ServiceReference::getId).containsExactly("ref-card");
		assertThat(answer.getProviders()).extracting(ServiceProvider::getName).containsExactly("card");
		assertThat(shown).containsExactly("ref-card");
		assertThat(new ScopedLookup(() -> broker, tags, "other", shown)
				.getServiceReferences("Payment", null, null, null, null).getReferences()).isEmpty();
	}

	@Test
	void theBrokersOwnContractsAreAnsweredAtTheFacade() throws Throwable {
		FakeLookup broker = new FakeLookup();
		ServiceProvider self = provider("ddsr-broker", "BrokerLookup");
		RestFlavor flavor = F.createRestFlavor();
		flavor.setBasePath("/ddsr/rest/references");
		self.getImplementations().get(0).getFlavors().add(flavor);
		broker.providers.add(self);

		LocalServiceRegistry answer = new ScopedLookup(() -> broker, tags, "payment/card", ConcurrentHashMap.newKeySet())
				.getServiceReferences("BrokerLookup", null, null, null, null);

		assertThat(((RestFlavor) answer.getProviders().get(0).getImplementations().get(0).getFlavors().get(0))
				.getBasePath()).isEqualTo("/ddsr/rest/scoped/payment/card/references");
	}

	@Test
	void theEventsPassWhatTheTagCoversAndWhatBecomesOfIt() throws Throwable {
		FakeEvents broker = new FakeEvents();
		Set<String> shown = ConcurrentHashMap.newKeySet();
		shown.add("ref-looked-up");
		List<ServiceEvent> heard = new ArrayList<>();
		new ScopedEvents(() -> broker, tags, "payment", shown).events().subscribe(new Flow.Subscriber<>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
			}

			@Override
			public void onNext(ServiceEvent item) {
				heard.add(item);
			}

			@Override
			public void onError(Throwable throwable) {
			}

			@Override
			public void onComplete() {
			}
		});

		broker.subscriber.onNext(event(ServiceEventType.REGISTERED, "ref-card", provider("card", "Payment")));
		broker.subscriber.onNext(event(ServiceEventType.REGISTERED, "ref-paypal", provider("paypal", "Payment")));
		broker.subscriber.onNext(event(ServiceEventType.UNREGISTERING, "ref-paypal", null));
		broker.subscriber.onNext(event(ServiceEventType.UNREGISTERING, "ref-card", null));
		broker.subscriber.onNext(event(ServiceEventType.UNREGISTERING, "ref-looked-up", null));

		assertThat(heard).extracting(e -> e.getType() + " " + e.getReference().getId()).containsExactly(
				"REGISTERED ref-card", "UNREGISTERING ref-card", "UNREGISTERING ref-looked-up");
	}

	@Test
	void theFacadeAddressPutsTheTagBeforeTheMountPoint() {
		assertThat(ScopedLookup.scoped("/ddsr/rest/events", "iot/sensors")).isEqualTo("/ddsr/rest/scoped/iot/sensors/events");
	}

	// ------------------------------------------------------------------

	private static ServiceProvider provider(String name, String contract) {
		ServiceInterface served = F.createServiceInterface();
		served.setName(contract);
		ServiceImplementation implementation = F.createServiceImplementation();
		implementation.setName(name + "-impl");
		implementation.getServiceInterfaces().add(served);
		ServiceProvider provider = F.createServiceProvider();
		provider.setName(name);
		provider.getImplementations().add(implementation);
		return provider;
	}

	private static ServiceEvent event(ServiceEventType type, String id, ServiceProvider provider) {
		ServiceReference reference = F.createServiceReference();
		reference.setId(id);
		reference.setProvider(provider);
		ServiceEvent event = F.createServiceEvent();
		event.setType(type);
		event.setReference(reference);
		return event;
	}
}
