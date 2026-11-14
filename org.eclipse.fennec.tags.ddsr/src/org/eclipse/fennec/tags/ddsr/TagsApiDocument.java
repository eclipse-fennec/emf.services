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

import java.net.URL;

import org.eclipse.fennec.services.broker.core.BrokerApiDocument;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Offers the {@code Tags} contract to the broker for announcement (#287):
 * the broker puts it into its catalog and announces it with its own
 * address and security scheme, so a lookup for {@code Tags} finds it —
 * over REST, and over Remote Service Admin.
 */
@Component(service = BrokerApiDocument.class)
public final class TagsApiDocument implements BrokerApiDocument {

	private final URL document;

	@Activate
	public TagsApiDocument(BundleContext context) {
		this.document = context.getBundle().getEntry("tags-api.xmi");
	}

	@Override
	public URL document() {
		return document;
	}
}
