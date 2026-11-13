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

package org.eclipse.fennec.services.broker.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.fennec.services.models.ModelDocument;
import org.eclipse.fennec.services.models.ModelStore;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

/** The payload models on the broker's REST face (#256). */
class ModelsResourceTest {

	/** Keeps what it is handed, with a fingerprint of its own making; refuses what is not "ecore". */
	private static final class FakeStore implements ModelStore {
		final Map<String, ModelDocument> kept = new ConcurrentHashMap<>();

		@Override
		public ModelDocument store(byte[] ecore) {
			String text = new String(ecore, StandardCharsets.UTF_8);
			if (!text.startsWith("ecore:")) {
				throw new IllegalArgumentException("not an Ecore document");
			}
			ModelDocument document = new ModelDocument(text.substring(6), "fp1:" + text.length(), ecore, "embedded");
			kept.put(document.nsURI() + "|" + document.fingerprint(), document);
			return document;
		}

		@Override
		public Optional<ModelDocument> resolve(String nsURI, String fingerprint) {
			return kept.values().stream().filter(d -> d.nsURI().equals(nsURI)
					&& (fingerprint == null || d.fingerprint().equals(fingerprint))).findFirst();
		}
	}

	private final FakeStore store = new FakeStore();

	private ModelsResource resource() {
		ModelsResource resource = new ModelsResource();
		resource.store = store;
		resource.source = store;
		return resource;
	}

	@Test
	void aModelHandedInIsAnsweredWithTheFingerprintTheBrokerComputed() throws Exception {
		byte[] ecore = "ecore:http://example.org/readings/1.0".getBytes(StandardCharsets.UTF_8);

		Response created = resource().put(new ByteArrayInputStream(ecore));

		assertThat(created.getStatus()).isEqualTo(201);
		assertThat(created.getEntity()).isEqualTo("fp1:" + ecore.length);
		assertThat(created.getHeaderString(ModelsResource.FINGERPRINT_HEADER)).isEqualTo("fp1:" + ecore.length);
		assertThat(created.getLocation().toString()).contains("nsURI=http://example.org/readings/1.0");
	}

	@Test
	void aModelIsFetchedByNsUriAndFingerprint() throws Exception {
		byte[] ecore = "ecore:http://example.org/readings/1.0".getBytes(StandardCharsets.UTF_8);
		resource().put(new ByteArrayInputStream(ecore));

		Response found = resource().get("http://example.org/readings/1.0", "fp1:" + ecore.length);
		assertThat(found.getStatus()).isEqualTo(200);
		assertThat((byte[]) found.getEntity()).isEqualTo(ecore);

		assertThat(resource().get("http://example.org/readings/1.0", "fp1:other").getStatus()).isEqualTo(404);
		assertThat(resource().get(null, null).getStatus()).isEqualTo(400);
	}

	@Test
	void aDocumentThatIsNoModelIsABadRequest() throws Exception {
		Response refused = resource().put(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

		assertThat(refused.getStatus()).isEqualTo(400);
	}
}
