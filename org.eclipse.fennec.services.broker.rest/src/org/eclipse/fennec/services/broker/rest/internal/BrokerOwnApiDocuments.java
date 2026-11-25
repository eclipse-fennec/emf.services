/********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 AND CC0-1.0
 *
 * This file was generated with the assistance of an AI tool
 * (Anthropic Claude) and reviewed by a project committer.
 *
 * Contributors:
 *   Data In Motion Consulting - initial implementation
 ********************************************************************/

package org.eclipse.fennec.services.broker.rest.internal;

import java.net.URL;

import org.eclipse.fennec.services.broker.core.BrokerApiDocument;
import org.osgi.service.component.annotations.Component;

/**
 * The broker's own four API documents, offered for announcement the way
 * every document beside the broker is (#287, #320).
 *
 * <p>The documents live here because this bundle serves them: the generic
 * REST distribution answers requests by the same files. The publisher
 * that announces them is in broker.core, so each is offered as a
 * {@link BrokerApiDocument}, one service per document, and withdrawn with
 * this bundle.
 */
final class BrokerOwnApiDocuments {

	private BrokerOwnApiDocuments() {
	}

	/** A document packaged at the root of this bundle ({@code -includeresource}). */
	private abstract static class Packaged implements BrokerApiDocument {

		private final String name;

		Packaged(String name) {
			this.name = name;
		}

		@Override
		public URL document() {
			return Packaged.class.getClassLoader().getResource(name);
		}
	}

	/** {@code BrokerCatalog}. */
	@Component(service = BrokerApiDocument.class)
	public static final class Catalog extends Packaged {
		public Catalog() {
			super("broker-catalog-api.xmi");
		}
	}

	/** {@code BrokerImplementations}. */
	@Component(service = BrokerApiDocument.class)
	public static final class Implementations extends Packaged {
		public Implementations() {
			super("broker-implementations-api.xmi");
		}
	}

	/** {@code BrokerLookup}. */
	@Component(service = BrokerApiDocument.class)
	public static final class Lookup extends Packaged {
		public Lookup() {
			super("broker-lookup-api.xmi");
		}
	}

	/** {@code BrokerEvents}. */
	@Component(service = BrokerApiDocument.class)
	public static final class Events extends Packaged {
		public Events() {
			super("broker-events-api.xmi");
		}
	}
}
