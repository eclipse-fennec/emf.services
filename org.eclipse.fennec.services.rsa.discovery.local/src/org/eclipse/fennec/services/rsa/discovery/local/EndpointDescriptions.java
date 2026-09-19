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

package org.eclipse.fennec.services.rsa.discovery.local;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Endpoint descriptions as 122.6.2 writes them down.
 *
 * <p>The format is a property list with types spelled out, because an
 * endpoint's properties are matched by LDAP filters and a filter
 * compares typed values: {@code endpoint.service.id} has to come back a
 * {@code Long}, not the text of one.
 *
 * <p>Parsing is separate from the extender that uses it so that it can
 * be tested against the exact documents the specification's own tests
 * produce — this is a wire format like any other, and a reader that is
 * nearly right is a service nobody finds.
 *
 * <p>The parser is told to trust nothing: no doctype, no external
 * entities, secure processing. A description file travels in a bundle
 * from somewhere, and the same reasoning applies as to a body off the
 * network.
 */
public final class EndpointDescriptions {

	static final String NAMESPACE = "http://www.osgi.org/xmlns/rsa/v1.0.0";

	private EndpointDescriptions() {
	}

	/** Every description in one document. */
	public static List<EndpointDescription> read(InputStream document) throws IOException {
		Element root = parse(document);
		List<EndpointDescription> endpoints = new ArrayList<>();
		for (Element description : childrenNamed(root, "endpoint-description")) {
			Map<String, Object> properties = new LinkedHashMap<>();
			for (Element property : childrenNamed(description, "property")) {
				String name = property.getAttribute("name");
				if (name == null || name.isEmpty()) {
					continue;
				}
				Object value = valueOf(property);
				if (value != null) {
					properties.put(name, value);
				}
			}
			if (!properties.isEmpty()) {
				endpoints.add(new EndpointDescription(properties));
			}
		}
		return endpoints;
	}

	private static Element parse(InputStream document) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver((publicId, systemId) -> {
				throw new SAXException("this document may not fetch " + systemId);
			});
			return builder.parse(document).getDocumentElement();
		} catch (ParserConfigurationException | SAXException malformed) {
			throw new IOException("not an endpoint description document: " + malformed.getMessage(), malformed);
		}
	}

	/**
	 * One property's value: a scalar said in the attribute, a piece of
	 * XML kept as text, or several values in an array, list or set.
	 */
	private static Object valueOf(Element property) {
		String type = property.getAttribute("value-type");
		if (type == null || type.isEmpty()) {
			type = "String";
		}
		List<Element> array = childrenNamed(property, "array");
		if (!array.isEmpty()) {
			return asArray(values(array.get(0)), type);
		}
		List<Element> list = childrenNamed(property, "list");
		if (!list.isEmpty()) {
			return scalars(values(list.get(0)), type);
		}
		List<Element> set = childrenNamed(property, "set");
		if (!set.isEmpty()) {
			return new LinkedHashSet<>(scalars(values(set.get(0)), type));
		}
		List<Element> xml = childrenNamed(property, "xml");
		if (!xml.isEmpty()) {
			// Kept as the text it is: the property is a document, and
			// re-serialising it would change it.
			return textOf(xml.get(0)).trim();
		}
		if (property.hasAttribute("value")) {
			return scalar(property.getAttribute("value"), type);
		}
		return null;
	}

	private static List<String> values(Element container) {
		List<String> values = new ArrayList<>();
		for (Element value : childrenNamed(container, "value")) {
			values.add(textOf(value).trim());
		}
		return values;
	}

	private static List<Object> scalars(List<String> values, String type) {
		List<Object> scalars = new ArrayList<>(values.size());
		for (String value : values) {
			scalars.add(scalar(value, type));
		}
		return scalars;
	}

	private static Object asArray(List<String> values, String type) {
		List<Object> scalars = scalars(values, type);
		Object[] array = switch (type) {
		case "Long" -> new Long[values.size()];
		case "Double" -> new Double[values.size()];
		case "Float" -> new Float[values.size()];
		case "Integer" -> new Integer[values.size()];
		case "Byte" -> new Byte[values.size()];
		case "Character" -> new Character[values.size()];
		case "Boolean" -> new Boolean[values.size()];
		case "Short" -> new Short[values.size()];
		default -> new String[values.size()];
		};
		return scalars.toArray(array);
	}

	private static Object scalar(String value, String type) {
		try {
			return switch (type) {
			case "Long" -> Long.valueOf(value);
			case "Double" -> Double.valueOf(value);
			case "Float" -> Float.valueOf(value);
			case "Integer" -> Integer.valueOf(value);
			case "Byte" -> Byte.valueOf(value);
			case "Character" -> value.isEmpty() ? null : Character.valueOf(value.charAt(0));
			case "Boolean" -> Boolean.valueOf(value);
			case "Short" -> Short.valueOf(value);
			default -> value;
			};
		} catch (NumberFormatException notANumber) {
			// The document says a type its value does not have. Keeping
			// the text is more useful than dropping the property: a
			// filter can still match it, and the mistake stays visible.
			return value;
		}
	}

	private static List<Element> childrenNamed(Element parent, String name) {
		List<Element> children = new ArrayList<>();
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element element && name.equals(element.getLocalName() == null
					? element.getNodeName()
					: element.getLocalName())) {
				children.add(element);
			}
		}
		return children;
	}

	private static String textOf(Element element) {
		StringBuilder text = new StringBuilder();
		NodeList nodes = element.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
				text.append(node.getNodeValue());
			} else if (node instanceof Element nested) {
				// An <xml> property keeps its markup.
				text.append(markupOf(nested));
			}
		}
		return text.toString();
	}

	private static String markupOf(Element element) {
		StringBuilder markup = new StringBuilder("<").append(element.getNodeName());
		for (int i = 0; i < element.getAttributes().getLength(); i++) {
			Node attribute = element.getAttributes().item(i);
			markup.append(' ').append(attribute.getNodeName()).append("=\"")
					.append(attribute.getNodeValue()).append('"');
		}
		markup.append('>').append(textOf(element)).append("</").append(element.getNodeName()).append('>');
		return markup.toString();
	}
}
