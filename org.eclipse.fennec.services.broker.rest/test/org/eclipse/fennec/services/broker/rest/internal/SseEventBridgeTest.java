/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.broker.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.event;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.payment;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.provider;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.reference;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The SSE bridge (#56): flavor filtering (FR-Sync-Filtering),
 * self-contained payloads, dead-subscriber hygiene and the heartbeat.
 */
class SseEventBridgeTest {

	private final RestTestSupport.FakeLookup lookup = new RestTestSupport.FakeLookup();
	private final RestTestSupport.ResourceSets resourceSets = new RestTestSupport.ResourceSets();
	private final FakeSse sse = new FakeSse();
	private SseEventBridge bridge;

	@BeforeEach
	void setUp() {
		bridge = new SseEventBridge();
		bridge.broker = lookup;
		bridge.rsObjects = resourceSets;
		bridge.sessions = presence;
	}

	private FakeSse.Sink subscribe(FlavorKind... flavors) {
		return subscribe(null, flavors);
	}

	private FakeSse.Sink subscribe(String consumerId, FlavorKind... flavors) {
		FakeSse.Sink sink = new FakeSse.Sink();
		Set<FlavorKind> set = flavors.length == 0 ? EnumSet.noneOf(FlavorKind.class) : EnumSet.of(flavors[0], flavors);
		bridge.subscribe(sse, sink, set, consumerId);
		return sink;
	}

	/** Records what the bridge says about who is connected. */
	private final RecordingPresence presence = new RecordingPresence();

	private ServiceEvent registered(String refId, FlavorKind... flavors) {
		ServiceProvider provider = provider("payments", payment(), flavors);
		ServiceReference ref = reference(refId, provider);
		lookup.resolves(ref, provider.getImplementations().get(0));
		return event(ServiceEventType.REGISTERED, ref, null);
	}

	@Test
	void aRestOnlySubscriberDoesNotHearAboutAnMqttOnlyService() {
		FakeSse.Sink restOnly = subscribe(FlavorKind.REST);
		FakeSse.Sink mqttOnly = subscribe(FlavorKind.MQTT);
		FakeSse.Sink everything = subscribe();

		bridge.publish(registered("ref-mqtt", FlavorKind.MQTT));

		assertThat(restOnly.sent).isEmpty();
		assertThat(mqttOnly.sent).hasSize(1);
		assertThat(everything.sent).hasSize(1);
	}

	@Test
	void aServiceSpeakingBothFlavorsReachesEverySubscriber() {
		FakeSse.Sink restOnly = subscribe(FlavorKind.REST);
		FakeSse.Sink mqttOnly = subscribe(FlavorKind.MQTT);

		bridge.publish(registered("ref-both", FlavorKind.REST, FlavorKind.MQTT));

		assertThat(restOnly.sent).hasSize(1);
		assertThat(mqttOnly.sent).hasSize(1);
	}

	@Test
	void aServiceWithoutAnyFlavorIsDeliveredToEveryoneLikeTheLookupBackendDoes() {
		FakeSse.Sink restOnly = subscribe(FlavorKind.REST);

		bridge.publish(registered("ref-none"));

		assertThat(restOnly.sent).hasSize(1);
	}

	@Test
	void theFrameIsANamedXmlEventCarryingASelfContainedDocumentWithTheReason() {
		FakeSse.Sink sink = subscribe();
		ServiceProvider provider = provider("payments", payment(), FlavorKind.REST);
		ServiceReference ref = reference("ref-1", provider);
		lookup.resolves(ref, provider.getImplementations().get(0));

		bridge.publish(event(ServiceEventType.UNREGISTERING, ref, ServiceEventReasons.WITHDRAWN));

		FakeSse.Frame frame = (FakeSse.Frame) sink.sent.get(0);
		assertThat(frame.getName()).isEqualTo("ddsr-service-event");
		assertThat(frame.getMediaType().toString()).isEqualTo("application/xml");
		String payload = String.valueOf(frame.getData());
		assertThat(payload).contains("<?xml").contains("ServiceEvent").contains("type=\"UNREGISTERING\"")
				.contains("reasonCode=\"WITHDRAWN\"")
				.as("self-contained: the interface travels in the document").contains("name=\"Payment\"")
				.contains("id=\"ref-1\"")
				.as("intra-document references are positional (TS resolves paths, not ids)").contains("reference=\"/1\"")
				.doesNotContain("reference=\"ref-1\"");
		assertThat(payload.split("<services:ServiceImplementation").length - 1)
				.as("the implementation travels inside its provider, not as a second detached root")
				.isZero();
	}

	@Test
	void aWithdrawnServiceStillRoutesByFlavorThroughTheReferencesOwnProviderSubtree() {
		// After a withdraw the lookup no longer resolves the reference; the
		// broker hands over a reference whose provider subtree carries the
		// implementation (selfContainedEventReference).
		FakeSse.Sink restOnly = subscribe(FlavorKind.REST);
		FakeSse.Sink mqttOnly = subscribe(FlavorKind.MQTT);
		ServiceProvider provider = provider("payments", payment(), FlavorKind.REST);
		ServiceReference detached = reference("ref-gone", provider);
		// deliberately NOT registered with the lookup

		bridge.publish(event(ServiceEventType.UNREGISTERING, detached, ServiceEventReasons.WITHDRAWN));

		assertThat(restOnly.sent).hasSize(1);
		assertThat(mqttOnly.sent).isEmpty();
	}

	@Test
	void aSuccessorsReplacesLinkNeverLeavesTheBrokerInAnEventDocument() {
		// The successor's `replaces` points at the live predecessor, which is
		// not part of the event document. Left alone, EMF writes a file: href
		// into the payload (W1) — or fails with a dangling href.
		FakeSse.Sink sink = subscribe();
		ServiceProvider provider = provider("payments", payment(), FlavorKind.REST);
		ServiceImplementation predecessor = ServicesFactory.eINSTANCE.createServiceImplementation();
		predecessor.setName("predecessor");
		predecessor.setVersion("1.0.0");
		predecessor.setImplementationId("org.example.Impl");
		provider.getImplementations().get(0).setReplaces(predecessor);
		ServiceReference ref = reference("ref-v2", provider);
		lookup.resolves(ref, provider.getImplementations().get(0));

		bridge.publish(event(ServiceEventType.REGISTERED, ref, null));

		assertThat(sink.sent).hasSize(1);
		String payload = String.valueOf(((FakeSse.Frame) sink.sent.get(0)).getData());
		assertThat(payload).doesNotContain("replaces=").doesNotContain("href=").doesNotContain("file:");
	}

	@Test
	void aThrowingSubscriberIsDroppedAndTheOthersStillGetTheEvent() {
		FakeSse.Sink dead = subscribe();
		dead.failing = true;
		FakeSse.Sink alive = subscribe();

		bridge.publish(registered("ref-1", FlavorKind.REST));

		assertThat(alive.sent).hasSize(1);
		assertThat(bridge.subscriberCount()).isEqualTo(1);
	}

	@Test
	void aClosedSubscriberIsDroppedOnTheNextEvent() {
		FakeSse.Sink gone = subscribe();
		FakeSse.Sink alive = subscribe();
		gone.closed = true;

		bridge.publish(registered("ref-1", FlavorKind.REST));

		assertThat(gone.sent).isEmpty();
		assertThat(alive.sent).hasSize(1);
		assertThat(bridge.subscriberCount()).isEqualTo(1);
	}

	@Test
	void publishingWithoutAnySubscriberIsANoOp() {
		bridge.publish(registered("ref-1", FlavorKind.REST));
		assertThat(bridge.subscriberCount()).isZero();
	}

	@Test
	void theHeartbeatIsACommentFrameToOpenSubscribersOnly() {
		FakeSse.Sink open = subscribe();
		FakeSse.Sink closed = subscribe();
		closed.closed = true;

		bridge.sendHeartbeat();

		assertThat(open.sent).hasSize(1);
		assertThat(open.sent.get(0).getComment()).isEqualTo("keepalive");
		assertThat(open.sent.get(0).getName()).isNull();
		assertThat(closed.sent).isEmpty();
		assertThat(bridge.subscriberCount()).isEqualTo(1);
	}

	@Test
	void theResourceSetIsReleasedAfterEveryRender() {
		subscribe();
		bridge.publish(registered("ref-1", FlavorKind.REST));
		bridge.publish(registered("ref-2", FlavorKind.REST));
		assertThat(resourceSets.outstanding).isZero();
	}

	@Test
	@DisplayName("a subscriber that names itself is reported as connected")
	void namingYourselfReportsPresence() {
		subscribe("consumer-a");

		assertThat(presence.connected).containsExactly("consumer-a");
		assertThat(presence.disconnected).isEmpty();
	}

	@Test
	@DisplayName("a subscriber that stays anonymous is served and reported as nobody")
	void anonymousSubscribersAreStillServed() {
		FakeSse.Sink sink = subscribe();

		assertThat(bridge.subscriberCount()).isEqualTo(1);
		assertThat(presence.connected).containsExactly((String) null);
		assertThat(presence.disconnected).isEmpty();
	}

	@Test
	@DisplayName("a closed stream reports the consumer as gone")
	void aClosedStreamReportsTheDisconnect() {
		FakeSse.Sink sink = subscribe("consumer-a");
		sink.close();

		bridge.publish(registered("ref-1"));

		assertThat(presence.disconnected).containsExactly("consumer-a");
	}

	@Test
	@DisplayName("a consumer that still has another stream is not reported gone")
	void oneOfTwoStreamsEndingIsNotADisconnect() {
		FakeSse.Sink first = subscribe("consumer-a");
		subscribe("consumer-a");
		first.close();

		bridge.publish(registered("ref-1"));

		assertThat(presence.disconnected)
				.as("the consumer is demonstrably still there on its other stream")
				.isEmpty();
	}
}
