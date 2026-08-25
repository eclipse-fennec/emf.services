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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtension;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

/** Multi-root counterpart to {@link XmiMessageBodyWriter}. */
@Component(service = MessageBodyWriter.class)
@JakartarsExtension
@JakartarsName("ddsr-xmi-bundle-writer")
@Provider
@Produces(MediaType.APPLICATION_XML)
public class XmiBundleMessageBodyWriter implements MessageBodyWriter<XmiBundle> {

	private final ComponentServiceObjects<ResourceSet> rsObjects;

	@Activate
	public XmiBundleMessageBodyWriter(@Reference ComponentServiceObjects<ResourceSet> rsObjects) {
		this.rsObjects = rsObjects;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return XmiBundle.class.isAssignableFrom(type);
	}

	@Override
	public void writeTo(XmiBundle bundle, Class<?> type, Type genericType, Annotation[] annotations,
			MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		XmiCodec.write(entityStream, rsObjects, bundle.roots());
	}
}
