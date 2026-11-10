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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import io.cloudevents.model.ce.CloudEvent;
import io.cloudevents.model.ce.CloudEventsFactory;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.stream.JsonParsingException;

/**
 * Reads and writes the two content modes of CloudEvents 1.0: structured
 * (the whole event in one JSON document) and binary (the attributes as
 * {@code ce-*} headers beside a body that is nothing but the payload).
 *
 * <p>The payload travels as bytes rather than as the envelope's
 * {@code data} feature, for two reasons that both come from this
 * registry rather than from the specification. A lifecycle event's wire
 * document is <em>multi-root</em> — the event, the reference, the
 * implementation, the provider, its interfaces — and {@code data} holds
 * one EObject. And the payload's encoding is the contract's choice
 * since #100, so an envelope that re-encoded it would quietly take that
 * choice back. What the envelope does say is which encoding was used:
 * {@code datacontenttype}, which is exactly the attribute the
 * specification has for it.
 *
 * <p>How the payload sits in the JSON document is decided by that
 * content type, the way the JSON event format lays it down (§3.1):
 * <ul>
 * <li>a JSON encoding ({@code *}{@code /json}, {@code +json}) is stored
 * <em>directly as a JSON value</em> in {@code data} — never as a string
 * holding an encoded JSON document;</li>
 * <li>any other text (XML, {@code text/}{@code *}) is a JSON string in
 * {@code data}. XMI stays readable in the message;</li>
 * <li>everything else is binary and travels as {@code data_base64}.
 * Protobuf does not pretend to be text.</li>
 * </ul>
 * Reading takes all three, and one more for a peer that is not as
 * strict: a JSON document that arrives wrapped in a string.
 */
public final class CloudEventCodec {

	private CloudEventCodec() {
	}

	/** An envelope and the payload bytes that arrived with it. */
	public record Message(CloudEvent attributes, byte[] data) {
	}

	private static final String SPECVERSION = "specversion";

	private static final String ID = "id";

	private static final String SOURCE = "source";

	private static final String TYPE = "type";

	private static final String DATACONTENTTYPE = "datacontenttype";

	private static final String DATASCHEMA = "dataschema";

	private static final String SUBJECT = "subject";

	private static final String TIME = "time";

	private static final String DATA = "data";

	private static final String DATA_BASE64 = "data_base64";

	/** The attribute names that are not extensions. */
	private static final Set<String> RESERVED = Set.of(SPECVERSION, ID, SOURCE, TYPE,
			DATACONTENTTYPE, DATASCHEMA, SUBJECT, TIME, DATA, DATA_BASE64);

	/**
	 * One message in structured mode: the envelope and the payload in a
	 * single JSON document, for transports that carry nothing but
	 * messages.
	 *
	 * @param envelope the attributes; {@code datacontenttype} decides how
	 *                 the payload is carried
	 * @param data     the encoded payload, or {@code null} for an event
	 *                 that has none
	 */
	public static byte[] writeStructured(CloudEvent envelope, byte[] data) {
		var json = Json.createObjectBuilder();
		json.add(SPECVERSION, require(envelope.getSpecVersion(), SPECVERSION));
		json.add(ID, require(envelope.getEventId(), ID));
		json.add(SOURCE, require(envelope.getSource(), SOURCE));
		json.add(TYPE, require(envelope.getType(), TYPE));
		addIfSet(json, DATACONTENTTYPE, envelope.getDatacontenttype());
		addIfSet(json, DATASCHEMA, envelope.getDataschema());
		addIfSet(json, SUBJECT, envelope.getSubject());
		if (envelope.getTime() != null) {
			json.add(TIME, DateTimeFormatter.ISO_INSTANT.format(envelope.getTime().toInstant()));
		}
		for (Map.Entry<String, String> extension : envelope.getExtensions()) {
			String name = extension.getKey();
			if (name != null && !RESERVED.contains(name) && extension.getValue() != null) {
				json.add(name, extension.getValue());
			}
		}
		if (data != null) {
			if (isJson(envelope.getDatacontenttype())) {
				// Directly as a JSON value: a document wrapped in a string
				// is what the format forbids, and what a reader that
				// follows it would hand on as a string literal.
				json.add(DATA, jsonValue(data));
			} else if (isTextual(envelope.getDatacontenttype())) {
				json.add(DATA, new String(data, StandardCharsets.UTF_8));
			} else {
				json.add(DATA_BASE64, Base64.getEncoder().encodeToString(data));
			}
		}
		StringWriter out = new StringWriter();
		try (JsonWriter writer = Json.createWriter(out)) {
			writer.writeObject(json.build());
		}
		return out.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * The inverse of {@link #writeStructured}. Every attribute that is
	 * not one of the specification's own becomes an extension, so an
	 * extension this registry does not know survives being read and
	 * written again.
	 *
	 * @throws CloudEventException when the document is not JSON, or not a
	 *                             CloudEvent
	 */
	public static Message readStructured(byte[] payload) {
		JsonObject json;
		try (JsonReader reader = Json.createReader(new ByteArrayInputStream(payload))) {
			json = reader.readObject();
		} catch (JsonParsingException | IllegalStateException | ClassCastException malformed) {
			throw new CloudEventException("not a JSON object: " + malformed.getMessage(), malformed);
		}
		CloudEvent envelope = CloudEventsFactory.eINSTANCE.createCloudEvent();
		envelope.setSpecVersion(string(json, SPECVERSION, true));
		envelope.setEventId(string(json, ID, true));
		envelope.setSource(string(json, SOURCE, true));
		envelope.setType(string(json, TYPE, true));
		envelope.setDatacontenttype(string(json, DATACONTENTTYPE, false));
		envelope.setDataschema(string(json, DATASCHEMA, false));
		envelope.setSubject(string(json, SUBJECT, false));
		String time = string(json, TIME, false);
		if (time != null) {
			envelope.setTime(parseTime(time));
		}
		for (Map.Entry<String, JsonValue> member : json.entrySet()) {
			if (!RESERVED.contains(member.getKey()) && member.getValue() instanceof JsonString text) {
				envelope.getExtensions().put(member.getKey(), text.getString());
			}
		}
		byte[] data = null;
		JsonValue carried = json.get(DATA);
		if (json.get(DATA_BASE64) instanceof JsonString encoded) {
			try {
				data = Base64.getDecoder().decode(encoded.getString());
			} catch (IllegalArgumentException notBase64) {
				throw new CloudEventException("data_base64 is not base64", notBase64);
			}
		} else if (carried instanceof JsonString text) {
			data = isJson(envelope.getDatacontenttype()) ? jsonString(text)
					: text.getString().getBytes(StandardCharsets.UTF_8);
		} else if (carried != null && carried.getValueType() != JsonValue.ValueType.NULL) {
			// A JSON value stored directly: its text is the document.
			data = carried.toString().getBytes(StandardCharsets.UTF_8);
		}
		return new Message(envelope, data);
	}

	/**
	 * The payload of a JSON encoding, as the value it is.
	 *
	 * @throws CloudEventException when the bytes are not JSON — an event
	 *                             that says its data is JSON and carries
	 *                             something else must not be sent
	 */
	private static JsonValue jsonValue(byte[] data) {
		try (JsonReader reader = Json.createReader(new ByteArrayInputStream(data))) {
			return reader.readValue();
		} catch (JsonParsingException | IllegalStateException notJson) {
			throw new CloudEventException("the data is declared JSON and is not: " + notJson.getMessage(),
					notJson);
		}
	}

	/**
	 * A string under a JSON content type. By the format it is a JSON
	 * string value and nothing more. A peer that wrapped a whole
	 * document in it — which this codec did itself before it followed
	 * the format — is still read: a string that parses as an object or
	 * an array is taken as that document, anything else as the string
	 * it is.
	 */
	private static byte[] jsonString(JsonString text) {
		String content = text.getString().strip();
		if (content.startsWith("{") || content.startsWith("[")) {
			try (JsonReader reader = Json.createReader(new StringReader(content))) {
				reader.readValue();
				return content.getBytes(StandardCharsets.UTF_8);
			} catch (JsonParsingException | IllegalStateException justAString) {
				// Falls through: it only looked like a document.
			}
		}
		return text.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * The attributes as HTTP headers, for binary mode — everything but
	 * {@code datacontenttype}, which in binary mode <em>is</em> the
	 * message's own {@code Content-Type} and must not be duplicated.
	 *
	 * <p>The body is then the payload, byte for byte, in the encoding
	 * its contract declares. That is what makes the envelope additive
	 * over HTTP: what this registry already sends stays exactly as it
	 * is and gains a few headers.
	 */
	public static Map<String, String> toHeaders(CloudEvent envelope) {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put(CloudEvents.HEADER_PREFIX + SPECVERSION, require(envelope.getSpecVersion(), SPECVERSION));
		headers.put(CloudEvents.HEADER_PREFIX + ID, require(envelope.getEventId(), ID));
		headers.put(CloudEvents.HEADER_PREFIX + SOURCE, require(envelope.getSource(), SOURCE));
		headers.put(CloudEvents.HEADER_PREFIX + TYPE, require(envelope.getType(), TYPE));
		if (envelope.getDataschema() != null) {
			headers.put(CloudEvents.HEADER_PREFIX + DATASCHEMA, envelope.getDataschema());
		}
		if (envelope.getSubject() != null) {
			headers.put(CloudEvents.HEADER_PREFIX + SUBJECT, envelope.getSubject());
		}
		if (envelope.getTime() != null) {
			headers.put(CloudEvents.HEADER_PREFIX + TIME,
					DateTimeFormatter.ISO_INSTANT.format(envelope.getTime().toInstant()));
		}
		for (Map.Entry<String, String> extension : envelope.getExtensions()) {
			if (extension.getKey() != null && !RESERVED.contains(extension.getKey())
					&& extension.getValue() != null) {
				headers.put(CloudEvents.HEADER_PREFIX + extension.getKey(), extension.getValue());
			}
		}
		return headers;
	}

	/**
	 * The envelope a request or response carries in its headers, or
	 * {@code null} when it carries none.
	 *
	 * <p>Null is a normal answer, not a failure: binary mode is additive,
	 * so a peer that has not gained the envelope yet sends a message
	 * that is still perfectly readable — the body never depended on the
	 * headers. Header names are matched case-insensitively, as HTTP
	 * requires.
	 *
	 * @param headers     all headers of the message, single-valued
	 * @param contentType the message's {@code Content-Type}, which is
	 *                    the envelope's {@code datacontenttype}
	 */
	public static CloudEvent fromHeaders(Map<String, String> headers, String contentType) {
		Map<String, String> lower = new LinkedHashMap<>();
		for (Map.Entry<String, String> header : headers.entrySet()) {
			if (header.getKey() != null && header.getValue() != null) {
				lower.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
			}
		}
		if (!lower.containsKey(CloudEvents.HEADER_PREFIX + ID)) {
			return null;
		}
		CloudEvent envelope = CloudEventsFactory.eINSTANCE.createCloudEvent();
		envelope.setSpecVersion(lower.getOrDefault(CloudEvents.HEADER_PREFIX + SPECVERSION,
				CloudEvents.SPEC_VERSION));
		envelope.setEventId(lower.get(CloudEvents.HEADER_PREFIX + ID));
		envelope.setSource(lower.get(CloudEvents.HEADER_PREFIX + SOURCE));
		envelope.setType(lower.get(CloudEvents.HEADER_PREFIX + TYPE));
		envelope.setDataschema(lower.get(CloudEvents.HEADER_PREFIX + DATASCHEMA));
		envelope.setSubject(lower.get(CloudEvents.HEADER_PREFIX + SUBJECT));
		String time = lower.get(CloudEvents.HEADER_PREFIX + TIME);
		if (time != null) {
			envelope.setTime(parseTime(time));
		}
		if (contentType != null && !contentType.isBlank()) {
			envelope.setDatacontenttype(contentType);
		}
		for (Map.Entry<String, String> header : lower.entrySet()) {
			if (!header.getKey().startsWith(CloudEvents.HEADER_PREFIX)) {
				continue;
			}
			String name = header.getKey().substring(CloudEvents.HEADER_PREFIX.length());
			if (!RESERVED.contains(name)) {
				envelope.getExtensions().put(name, header.getValue());
			}
		}
		return envelope;
	}

	/** The extension attributes this registry names itself. */
	private static final Set<String> KNOWN_EXTENSIONS = Set.of(
			CloudEvents.EXTENSION_CORRELATION_ID, CloudEvents.EXTENSION_REPLY_TO);

	/**
	 * The same as {@link #fromHeaders(Map, String)} for a caller that can
	 * only ask for headers by name — a JAX-RS dispatcher, say, which is
	 * handed a lookup rather than the map.
	 *
	 * <p>The difference is what cannot be done: a lookup cannot be
	 * enumerated, so an extension this registry does not name is invisible
	 * here. The specification's own attributes and
	 * {@link #KNOWN_EXTENSIONS} are read; anything else a peer sent is
	 * neither rejected nor preserved, which is the honest price of the
	 * narrower interface.
	 */
	public static CloudEvent fromHeaders(UnaryOperator<String> headerLookup, String contentType) {
		String id = headerLookup.apply(CloudEvents.HEADER_PREFIX + ID);
		if (id == null || id.isBlank()) {
			return null;
		}
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put(CloudEvents.HEADER_PREFIX + ID, id);
		for (String name : List.of(SPECVERSION, SOURCE, TYPE, DATASCHEMA, SUBJECT, TIME)) {
			String value = headerLookup.apply(CloudEvents.HEADER_PREFIX + name);
			if (value != null) {
				headers.put(CloudEvents.HEADER_PREFIX + name, value);
			}
		}
		for (String extension : KNOWN_EXTENSIONS) {
			String value = headerLookup.apply(CloudEvents.HEADER_PREFIX + extension);
			if (value != null) {
				headers.put(CloudEvents.HEADER_PREFIX + extension, value);
			}
		}
		return fromHeaders(headers, contentType);
	}

	/**
	 * Whether a content type declares JSON-formatted data: a media
	 * subtype {@code json} or one ending in {@code +json}, parameters
	 * aside. No content type at all means JSON too — that is the
	 * format's own default.
	 */
	static boolean isJson(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return true;
		}
		String type = contentType.toLowerCase(Locale.ROOT);
		int parameters = type.indexOf(';');
		if (parameters >= 0) {
			type = type.substring(0, parameters).trim();
		}
		return type.endsWith("/json") || type.endsWith("+json");
	}

	/**
	 * Whether a payload in this encoding is text rather than binary.
	 * Text is text: XML, JSON and anything under {@code text/}; a
	 * structured suffix counts as its structure.
	 *
	 * <p>No content type means the payload is JSON by the specification's
	 * own default, which is textual too.
	 */
	static boolean isTextual(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return true;
		}
		String type = contentType.toLowerCase(Locale.ROOT);
		int parameters = type.indexOf(';');
		if (parameters >= 0) {
			type = type.substring(0, parameters).trim();
		}
		return type.startsWith("text/") || type.endsWith("/json") || type.endsWith("+json")
				|| type.endsWith("/xml") || type.endsWith("+xml");
	}

	private static Date parseTime(String rfc3339) {
		try {
			return Date.from(Instant.parse(rfc3339));
		} catch (DateTimeParseException notATime) {
			throw new CloudEventException("time is not RFC 3339: " + rfc3339, notATime);
		}
	}

	private static String string(JsonObject json, String name, boolean required) {
		JsonValue value = json.get(name);
		if (value instanceof JsonString text) {
			return text.getString();
		}
		if (required) {
			throw new CloudEventException("required attribute '" + name + "' is missing or not a string");
		}
		return null;
	}

	private static String require(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new CloudEventException("required attribute '" + name + "' is not set");
		}
		return value;
	}

	private static void addIfSet(JsonObjectBuilder json, String name, String value) {
		if (value != null && !value.isBlank()) {
			json.add(name, value);
		}
	}
}
