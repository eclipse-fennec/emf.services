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

import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.xmi.codec.jakartars.XmiBundleMessageBodyWriter;
import org.eclipse.fennec.services.xmi.codec.jakartars.XmiMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.jakartars.XmiMessageBodyWriter;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationBase;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;

import jakarta.ws.rs.core.Application;

/**
 * The application the tag facade is served in: under {@code /scoped},
 * beside the broker's own, in the broker's authentication mode — the
 * configuration states it twice, as {@code fennec.auth} and as the
 * extension selector requiring the filter of that mode, so a bearer
 * facade on a node without a verifier is not deployed rather than
 * deployed open.
 *
 * <p>An application of its own rather than the broker's default one, for
 * one property: Jersey holds back the first 8 KiB of an answer, and the
 * facade's event stream would then sit in the server (#252) — the
 * generic distribution's applications say {@code contentLength.buffer=0},
 * and so does this one.
 */
@Component(service = Application.class, configurationPid = "org.eclipse.fennec.tags.ddsr.facade",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@JakartarsApplicationBase("/" + TagFacade.SCOPED)
@JakartarsName(TagFacadeApplication.NAME)
public class TagFacadeApplication extends Application {

	static final String NAME = "fennec-tags-facade";

	private final ComponentServiceObjects<ResourceSet> resourceSets;

	@Activate
	public TagFacadeApplication(
			@Reference(target = "(emf.name=services)") ComponentServiceObjects<ResourceSet> resourceSets) {
		this.resourceSets = resourceSets;
	}

	/**
	 * The XMI providers, as every application serving a document brings
	 * its own: the whiteboard's are not asked for an application that
	 * names providers of its own, and the facade answers models and
	 * multi-root documents like the broker.
	 */
	@Override
	public Set<Object> getSingletons() {
		return Set.of(new XmiMessageBodyReader(resourceSets), new XmiMessageBodyWriter(resourceSets),
				new XmiBundleMessageBodyWriter(resourceSets));
	}

	@Override
	public Map<String, Object> getProperties() {
		return Map.of("jersey.config.server.contentLength.buffer", 0);
	}
}
