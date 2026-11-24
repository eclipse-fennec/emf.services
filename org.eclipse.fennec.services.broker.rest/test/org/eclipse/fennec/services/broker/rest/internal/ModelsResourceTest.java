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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.fennec.services.models.ModelDocument;
import org.eclipse.fennec.services.models.ModelStore;
import org.eclipse.fennec.services.xmi.codec.WireBody;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.WebApplicationException;
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
	void aModelAboveTheBackstopIsRefusedWith413WhenNothingIsConfigured() {
		byte[] large = model(WireBody.MAX_BYTES + 1);

		assertThatThrownBy(() -> resource().put(new ByteArrayInputStream(large)))
				.isInstanceOfSatisfying(WebApplicationException.class,
						refused -> assertThat(refused.getResponse().getStatus()).isEqualTo(413));
		assertThat(store.kept).isEmpty();
	}

	@Test
	void zeroInTheConfigurationKeepsTheBackstop() {
		ModelsResource resource = resource();
		resource.activate(limit(0));

		assertThatThrownBy(() -> resource.put(new ByteArrayInputStream(model(WireBody.MAX_BYTES + 1))))
				.isInstanceOfSatisfying(WebApplicationException.class,
						refused -> assertThat(refused.getResponse().getStatus()).isEqualTo(413));
	}

	@Test
	void theConfiguredLimitLetsALargerModelIn() throws Exception {
		int limit = 3 * WireBody.MAX_BYTES;
		ModelsResource resource = resource();
		resource.activate(limit(limit));

		Response created = resource.put(new ByteArrayInputStream(model(limit)));
		assertThat(created.getStatus()).as("exactly at the limit").isEqualTo(201);

		assertThatThrownBy(() -> resource.put(new ByteArrayInputStream(model(limit + 1))))
				.as("one byte above it")
				.isInstanceOfSatisfying(WebApplicationException.class,
						refused -> assertThat(refused.getResponse().getStatus()).isEqualTo(413));
	}

	/** A synthetic model the fake store accepts, padded to exactly {@code size} bytes. */
	private static byte[] model(int size) {
		byte[] head = "ecore:http://example.org/large/1.0#".getBytes(StandardCharsets.UTF_8);
		byte[] body = Arrays.copyOf(head, size);
		Arrays.fill(body, head.length, size, (byte) 'x');
		return body;
	}

	private static ModelsResource.Config limit(long maxBytes) {
		return new ModelsResource.Config() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return ModelsResource.Config.class;
			}

			@Override
			public long models_max_bytes() {
				return maxBytes;
			}
		};
	}

	@Test
	void aDocumentThatIsNoModelIsABadRequest() throws Exception {
		Response refused = resource().put(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

		assertThat(refused.getStatus()).isEqualTo(400);
	}
}
