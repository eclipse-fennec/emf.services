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

package org.eclipse.fennec.services.cloudevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.cloudevents.model.ce.CloudEvent;

/**
 * The envelope is a cross-language contract, so what these tests pin is
 * the wire and not the API: the member names of the JSON, which payload
 * ends up base64 and which does not, and the one header that binary
 * mode must NOT write.
 */
class CloudEventCodecTest {

	private static final String XMI = "<?xml version=\"1.0\"?><services:ServiceEvent/>";

	private CloudEvent envelope(String datacontenttype) {
		CloudEvent envelope = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE,
				"/consumer/probe", datacontenttype);
		envelope.setSubject("ref-1");
		envelope.setTime(new Date(1_700_000_000_000L));
		return envelope;
	}

	@Test
	void a_textual_payload_travels_as_a_json_string() {
		byte[] message = CloudEventCodec.writeStructured(envelope("application/xml"),
				XMI.getBytes(StandardCharsets.UTF_8));

		String json = new String(message, StandardCharsets.UTF_8);
		assertThat(json)
			.contains("\"specversion\":\"1.0\"")
			.contains("\"type\":\"org.eclipse.fennec.services.invoke\"")
			.contains("\"datacontenttype\":\"application/xml\"")
			.contains("\"subject\":\"ref-1\"")
			.contains("\"time\":\"2023-11-14T22:13:20Z\"")
			.contains("\"data\":")
			.doesNotContain("data_base64");

		CloudEventCodec.Message read = CloudEventCodec.readStructured(message);
		assertThat(new String(read.data(), StandardCharsets.UTF_8)).isEqualTo(XMI);
		assertThat(read.attributes().getSubject()).isEqualTo("ref-1");
		assertThat(read.attributes().getTime()).isEqualTo(new Date(1_700_000_000_000L));
	}

	@Test
	void a_binary_payload_travels_as_base64() {
		byte[] protobuf = { 0x0a, 0x03, (byte) 0xff, 0x00, 0x7f };

		byte[] message = CloudEventCodec.writeStructured(envelope("application/x-protobuf"), protobuf);

		assertThat(new String(message, StandardCharsets.UTF_8))
			.contains("\"data_base64\":\"CgP/AH8=\"");
		assertThat(CloudEventCodec.readStructured(message).data()).isEqualTo(protobuf);
	}

	@Test
	void a_json_payload_is_stored_directly_as_a_json_value() {
		String document = "{\"name\": \"Zürich\", \"values\": [1, true, null]}";

		byte[] message = CloudEventCodec.writeStructured(envelope("application/json"),
				document.getBytes(StandardCharsets.UTF_8));

		assertThat(new String(message, StandardCharsets.UTF_8))
			.as("the format forbids a JSON document wrapped in a string (JSON event format §3.1.1)")
			.contains("\"data\":{\"name\":\"Zürich\",\"values\":[1,true,null]}")
			.doesNotContain("data_base64");
		assertThat(new String(CloudEventCodec.readStructured(message).data(), StandardCharsets.UTF_8))
			.as("what comes back is the same document, whatever its white space was")
			.isEqualTo("{\"name\":\"Zürich\",\"values\":[1,true,null]}");
	}

	@Test
	void a_structured_suffix_counts_as_json_too() {
		byte[] message = CloudEventCodec.writeStructured(envelope("application/vnd.acme+json; charset=utf-8"),
				"[1,2]".getBytes(StandardCharsets.UTF_8));

		assertThat(new String(message, StandardCharsets.UTF_8)).contains("\"data\":[1,2]");
	}

	@Test
	void data_declared_json_that_is_not_json_is_not_sent() {
		assertThatThrownBy(() -> CloudEventCodec.writeStructured(envelope("application/json"),
				"<not json/>".getBytes(StandardCharsets.UTF_8)))
			.isInstanceOf(CloudEventException.class)
			.hasMessageContaining("declared JSON");
	}

	@Test
	void a_json_document_a_peer_wrapped_in_a_string_is_still_read() {
		// What this codec wrote itself before it followed the format, and
		// what a peer that has not caught up still writes.
		String wrapped = "{\"specversion\":\"1.0\",\"id\":\"1\",\"source\":\"/s\",\"type\":\"t\","
				+ "\"datacontenttype\":\"application/json\",\"data\":\"{\\\"a\\\": 1}\"}";

		assertThat(new String(CloudEventCodec.readStructured(wrapped.getBytes(StandardCharsets.UTF_8)).data(),
				StandardCharsets.UTF_8)).isEqualTo("{\"a\": 1}");
	}

	@Test
	void a_plain_string_under_a_json_content_type_stays_a_json_string() {
		String plain = "{\"specversion\":\"1.0\",\"id\":\"1\",\"source\":\"/s\",\"type\":\"t\","
				+ "\"datacontenttype\":\"application/json\",\"data\":\"just a string\"}";

		assertThat(new String(CloudEventCodec.readStructured(plain.getBytes(StandardCharsets.UTF_8)).data(),
				StandardCharsets.UTF_8))
			.as("a string is a JSON string value; it is not parsed as a document (§3.1.2)")
			.isEqualTo("\"just a string\"");
	}

	@Test
	void xml_that_another_implementation_sent_as_base64_is_read_as_the_same_document() {
		// Gecko Messaging treats every payload that is not text/* as
		// binary. Both forms are valid; a reader has to take both.
		String base64 = Base64.getEncoder().encodeToString(XMI.getBytes(StandardCharsets.UTF_8));
		String message = "{\"specversion\":\"1.0\",\"id\":\"1\",\"source\":\"/s\",\"type\":\"t\","
				+ "\"datacontenttype\":\"application/xml\",\"data_base64\":\"" + base64 + "\"}";

		assertThat(new String(CloudEventCodec.readStructured(message.getBytes(StandardCharsets.UTF_8)).data(),
				StandardCharsets.UTF_8)).isEqualTo(XMI);
	}

	@Test
	void the_schema_is_the_namespace_of_the_payloads_package() {
		CloudEvent envelope = CloudEvents.describing(envelope("application/xml"),
				ServicesFactory.eINSTANCE.createServiceInvocation());

		assertThat(envelope.getDataschema())
			.as("an absolute URI, so the package and not a class in it — a fragment is not allowed there")
			.isEqualTo(ServicesPackage.eNS_URI)
			.doesNotContain("#");
		assertThat(new String(CloudEventCodec.writeStructured(envelope, XMI.getBytes(StandardCharsets.UTF_8)),
				StandardCharsets.UTF_8))
			.contains("\"dataschema\":\"" + ServicesPackage.eNS_URI + "\"");
		assertThat(CloudEvents.describing(envelope(null), null).getDataschema()).isNull();
	}

	@Test
	void an_event_without_a_payload_carries_neither_data_member() {
		byte[] message = CloudEventCodec.writeStructured(envelope(null), null);

		String json = new String(message, StandardCharsets.UTF_8);
		assertThat(json).doesNotContain("\"data\"").doesNotContain("data_base64")
			.doesNotContain("datacontenttype");
		assertThat(CloudEventCodec.readStructured(message).data()).isNull();
	}

	@Test
	void an_extension_survives_being_read_and_written_again() {
		CloudEvent envelope = envelope("application/xml");
		envelope.getExtensions().put(CloudEvents.EXTENSION_REPLY_TO, "ddsr/rpc/Payment/charge/reply/7");
		envelope.getExtensions().put("traceparent", "00-abc-def-01");

		CloudEventCodec.Message read = CloudEventCodec.readStructured(
				CloudEventCodec.writeStructured(envelope, null));

		assertThat(read.attributes().getExtensions().map())
			.containsEntry(CloudEvents.EXTENSION_REPLY_TO, "ddsr/rpc/Payment/charge/reply/7")
			.containsEntry("traceparent", "00-abc-def-01");
	}

	@Test
	void a_document_that_is_not_a_cloud_event_is_refused() {
		assertThatThrownBy(() -> CloudEventCodec.readStructured("{\"id\":\"1\"}".getBytes(StandardCharsets.UTF_8)))
			.isInstanceOf(CloudEventException.class)
			.hasMessageContaining("specversion");

		assertThatThrownBy(() -> CloudEventCodec.readStructured("not json".getBytes(StandardCharsets.UTF_8)))
			.isInstanceOf(CloudEventException.class);
	}

	@Test
	void an_envelope_missing_a_required_attribute_is_not_written() {
		CloudEvent envelope = envelope("application/xml");
		envelope.setSource(null);

		assertThatThrownBy(() -> CloudEventCodec.writeStructured(envelope, null))
			.isInstanceOf(CloudEventException.class)
			.hasMessageContaining("source");
	}

	@Test
	void binary_mode_leaves_the_content_type_to_the_message() {
		CloudEvent envelope = envelope("application/x-protobuf");
		envelope.getExtensions().put(CloudEvents.EXTENSION_CORRELATION_ID, "req-1");

		Map<String, String> headers = CloudEventCodec.toHeaders(envelope);

		assertThat(headers)
			.containsEntry("ce-specversion", "1.0")
			.containsEntry("ce-type", CloudEvents.TYPE_INVOKE)
			.containsEntry("ce-source", "/consumer/probe")
			.containsEntry("ce-subject", "ref-1")
			.containsEntry("ce-correlationid", "req-1")
			.containsKey("ce-id")
			.doesNotContainKey("ce-datacontenttype");
	}

	@Test
	void headers_are_read_back_case_insensitively_and_the_content_type_becomes_the_encoding() {
		CloudEvent read = CloudEventCodec.fromHeaders(Map.of(
				"CE-SpecVersion", "1.0",
				"Ce-Id", "e-1",
				"ce-source", "/broker",
				"ce-Type", CloudEvents.TYPE_INVOKE,
				"ce-correlationid", "req-1",
				"Content-Length", "17"), "application/xml");

		assertThat(read.getEventId()).isEqualTo("e-1");
		assertThat(read.getType()).isEqualTo(CloudEvents.TYPE_INVOKE);
		assertThat(read.getDatacontenttype()).isEqualTo("application/xml");
		assertThat(read.getExtensions().map())
			.containsEntry(CloudEvents.EXTENSION_CORRELATION_ID, "req-1")
			.doesNotContainKey("content-length");
	}

	@Test
	void a_message_without_an_envelope_reads_as_none() {
		assertThat(CloudEventCodec.fromHeaders(Map.of("Content-Type", "application/xml"), "application/xml"))
			.isNull();
	}

	@ParameterizedTest
	@EnumSource(ServiceEventType.class)
	void every_lifecycle_type_has_a_name_that_reads_back(ServiceEventType type) {
		assertThat(CloudEvents.eventTypeOf(CloudEvents.typeOf(type))).isEqualTo(type);
	}

	@Test
	void a_foreign_type_is_not_a_lifecycle_event() {
		assertThat(CloudEvents.eventTypeOf("com.example.something.happened")).isNull();
		assertThat(CloudEvents.eventTypeOf(CloudEvents.TYPE_INVOKE)).isNull();
		assertThat(CloudEvents.eventTypeOf(null)).isNull();
	}
}
