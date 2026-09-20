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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.body;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.bytes;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.diagnostic;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.xmi.codec.WireBody;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** PUT/GET/DELETE /consumers/{id} (#56): body shape, path-owns-identity, error mapping. */
class ConsumersResourceTest {

	private static final class FakeSessions implements BrokerSessions {
		ConsumerSession putSession;
		List<String> putIds;
		String deleted;
		SessionSnapshot snapshot;
		Diagnostic answer = diagnostic(DiagnosticSeverity.OK, 0, "ok");

		/** What the bridge reported; this resource never touches these. */
		final List<String> connected = new ArrayList<>();
		final List<String> disconnected = new ArrayList<>();

		@Override
		public void consumerConnected(String consumerId) {
			connected.add(consumerId);
		}

		@Override
		public void consumerDisconnected(String consumerId) {
			disconnected.add(consumerId);
		}

		@Override
		public Diagnostic putSession(ConsumerSession session, Collection<String> acquiredReferenceIds) {
			putSession = session;
			putIds = new ArrayList<>(acquiredReferenceIds);
			return answer;
		}

		@Override
		public Diagnostic deleteSession(String consumerId) {
			deleted = consumerId;
			return answer;
		}

		@Override
		public Optional<SessionSnapshot> getSession(String consumerId) {
			return Optional.ofNullable(snapshot);
		}

		@Override
		public int expireSessions(Instant cutoff) {
			return 0;
		}

		@Override
		public int sessionCount() {
			return 0;
		}
	}

	private final FakeSessions sessions = new FakeSessions();
	private final RestTestSupport.ResourceSets resourceSets = new RestTestSupport.ResourceSets();
	private ConsumersResource resource;

	@BeforeEach
	void setUp() {
		resource = new ConsumersResource();
		resource.sessions = sessions;
		resource.rsObjects = resourceSets;
	}

	private static ConsumerSession session(String id) {
		ConsumerSession s = ServicesFactory.eINSTANCE.createConsumerSession();
		s.setConsumerId(id);
		return s;
	}

	private static ServiceReference stub(String id) {
		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId(id);
		return ref;
	}

	@Test
	void putForwardsTheSessionAndTheReferenceIdStubs() throws IOException {
		Response r = resource.put("c1", body(resourceSets, session("c1"), stub("ref-a"), stub("ref-b")));

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(sessions.putSession.getConsumerId()).isEqualTo("c1");
		assertThat(sessions.putIds).containsExactly("ref-a", "ref-b");
		assertThat(r.getEntity()).isInstanceOf(Diagnostic.class);
	}

	@Test
	void aBodyWithoutConsumerIdTakesTheIdFromThePath() throws IOException {
		resource.put("c1", body(resourceSets, session(null)));
		assertThat(sessions.putSession.getConsumerId()).isEqualTo("c1");
	}

	@Test
	void aBodyIdContradictingThePathIsRejected() throws IOException {
		Response r = resource.put("c1", body(resourceSets, session("someone-else")));
		assertThat(r.getStatus()).isEqualTo(400);
		assertThat(sessions.putSession).isNull();
	}

	@Test
	void aBodyWithoutASessionRootIsRejected() throws IOException {
		Response r = resource.put("c1", body(resourceSets, stub("ref-a")));
		assertThat(r.getStatus()).isEqualTo(400);
	}

	@Test
	void anEmptyAcquisitionListIsAPureHeartbeat() throws IOException {
		resource.put("c1", body(resourceSets, session("c1")));
		assertThat(sessions.putIds).isEmpty();
	}

	@Test
	void aMalformedBodyIs400AndAnOversizedOneIs413() {
		assertThatThrownBy(() -> resource.put("c1", bytes("<not xml")))
				.isInstanceOf(WebApplicationException.class)
				.extracting(e -> ((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400);
		byte[] huge = new byte[WireBody.MAX_BYTES + 1];
		java.util.Arrays.fill(huge, (byte) ' ');
		assertThatThrownBy(() -> resource.put("c1", new java.io.ByteArrayInputStream(huge)))
				.isInstanceOf(WebApplicationException.class)
				.extracting(e -> ((WebApplicationException) e).getResponse().getStatus()).isEqualTo(413);
	}

	@Test
	void getRendersTheSessionWithItsAcquisitionsAsIdStubs() {
		sessions.snapshot = new BrokerSessions.SessionSnapshot(session("c1"), List.of("ref-a", "ref-b"));

		Response r = resource.get("c1");

		assertThat(r.getStatus()).isEqualTo(200);
		XmiBundle bundle = (XmiBundle) r.getEntity();
		assertThat(bundle.roots().get(0)).isInstanceOf(ConsumerSession.class);
		assertThat(bundle.roots().stream().filter(ServiceReference.class::isInstance)
				.map(root -> ((ServiceReference) root).getId())).containsExactly("ref-a", "ref-b");
	}

	@Test
	void getOfAnUnknownSessionIs404() {
		assertThat(resource.get("nobody").getStatus()).isEqualTo(404);
	}

	@Test
	void deleteMapsTheDiagnosticToTheHttpStatus() {
		assertThat(resource.delete("c1").getStatus()).isEqualTo(200);
		assertThat(sessions.deleted).isEqualTo("c1");

		sessions.answer = diagnostic(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_SESSION_INVALID, "blank");
		assertThat(resource.delete(" ").getStatus()).isEqualTo(400);
	}

	@Test
	void theResourceSetIsReleasedAfterParsing() throws IOException {
		resource.put("c1", body(resourceSets, session("c1")));
		assertThat(resourceSets.outstanding).isZero();
	}
}
