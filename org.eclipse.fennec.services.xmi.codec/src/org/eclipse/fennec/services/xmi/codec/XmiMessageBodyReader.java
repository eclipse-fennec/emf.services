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

package org.eclipse.fennec.services.xmi.codec;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtension;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

/**
 * Reads XMI request/response bodies into typed {@link EObject}
 * parameters / return values. The DS adapter on this class auto-
 * registers it with the Jakartars Whiteboard on the server side;
 * clients can also instantiate this manually with a CSO and register
 * it on a {@code jakarta.ws.rs.client.Client} via
 * {@code Client.register(...)}.
 */
@Component(service = MessageBodyReader.class)
@JakartarsExtension
@JakartarsName("ddsr-xmi-reader")
@Provider
// XML by default and whatever else a deployment registered an encoding
// for (#100). Widened rather than duplicated per media type: which
// encodings exist is a property of the ResourceSet, not of this class,
// and isReadable is where that question gets asked.
@Consumes({ MediaType.APPLICATION_XML, MediaType.WILDCARD })
public class XmiMessageBodyReader implements MessageBodyReader<EObject> {

	private final ComponentServiceObjects<ResourceSet> rsObjects;

	@Activate
	public XmiMessageBodyReader(@Reference(target = "(emf.name=services)") ComponentServiceObjects<ResourceSet> rsObjects) {
		this.rsObjects = rsObjects;
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return EObject.class.isAssignableFrom(type) && XmiCodec.canDecode(rsObjects, contentTypeOf(mediaType));
	}

	@Override
	public EObject readFrom(Class<EObject> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		EObject root;
		try {
			root = XmiCodec.read(entityStream, rsObjects, contentTypeOf(mediaType));
		} catch (XmiCodecException refusal) {
			throw XmiHttpErrors.toHttp(refusal);
		}
		if (!type.isInstance(root)) {
			throw new WebApplicationException(
					"expected " + type.getSimpleName() + " but body contained " + root.eClass().getName(),
					Response.Status.BAD_REQUEST);
		}
		return root;
	}

	/** The media type without its parameters, or {@code null} for XMI. */
	static String contentTypeOf(MediaType mediaType) {
		return mediaType == null ? null : mediaType.getType() + "/" + mediaType.getSubtype();
	}
}
