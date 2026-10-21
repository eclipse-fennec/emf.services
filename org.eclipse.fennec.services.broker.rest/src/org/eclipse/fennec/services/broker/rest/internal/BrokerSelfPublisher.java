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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.ConnectionScheme;
import org.eclipse.fennec.services.MutualTlsScheme;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.SecurityScheme;
import org.eclipse.fennec.services.SecuritySchemeKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Self-publishes the broker's own REST API into its catalog and
 * implementation index. Eat-your-own-dogfood: the broker becomes the
 * first entry in the registry it provides.
 *
 * <p>At {@code @Activate} it reads the three API documents from the
 * bundle — BrokerCatalog, BrokerImplementations, BrokerLookup — puts each
 * contract in the catalog and announces the implementation that serves
 * it. Nothing about those implementations is written here: which
 * operations, which paths, where every argument travels and which error
 * is which status all live in the documents, and the same documents are
 * what the generic REST distribution answers requests by. That is the
 * whole point of #76 — the broker used to describe its API three times
 * (documents, Java interfaces, resource classes) and the three had drifted.
 *
 * <p>Only the deployment fact is added: {@code public.url} says where
 * this instance is reachable, and the modelled base path is appended to
 * it, so a consumer gets a self-contained flavor.
 *
 * <p>Three announcements, one per contract — each is its own
 * implementation with its own base path, which is also how the generic
 * distribution mounts them.
 *
 * <p>After activation a consumer can:
 * <pre>
 * GET /references?interface=BrokerCatalog&amp;flavors=REST
 * </pre>
 * and discover the broker's own REST surface.
 */
@Component(
		service = BrokerSelfPublisher.class,
		configurationPid = "org.eclipse.fennec.services.broker.rest",
		immediate = true)
@Designate(ocd = BrokerSelfPublisher.Config.class)
public final class BrokerSelfPublisher {

	private static final Logger LOG = Logger.getLogger(BrokerSelfPublisher.class.getName());

	@ObjectClassDefinition(
			name = "DDSR Broker Self-Publisher",
			description = "Self-publication of the broker's REST endpoints into its own catalog")
	public @interface Config {

		@AttributeDefinition(
				name = "Public URL",
				description = "External URL where the broker's REST endpoint is reachable. "
						+ "The authority part (scheme://host:port) becomes RestFlavor.host; "
						+ "the path becomes RestFlavor.basePath. Consumers use this to build "
						+ "operation URLs without any client-side fallback config.")
		String public_url() default "http://localhost:8887/ddsr/rest";

		@AttributeDefinition(name = "Authentication mode",
				description = "The broker's mode, from the same placeholder as its applications: bearer, mtls or none.")
		String auth_mode() default "bearer";

		@AttributeDefinition(name = "Token issuer", description = "The issuer the broker's verifier accepts.")
		String issuer() default "";

		@AttributeDefinition(name = "Token audiences", description = "The audiences a token for the broker carries.")
		String[] audiences() default {};

		@AttributeDefinition(name = "JWKS URI", description = "Where the issuer's signing keys are.")
		String jwks_uri() default "";

		@AttributeDefinition(name = "Scope claim", description = "Where the verifier reads roles and scopes from.")
		String scope_claim() default "scope";

		@AttributeDefinition(name = "Trusted certificate issuers",
				description = "Mode mtls: the DNs of the authorities whose client certificates the broker "
						+ "accepts, as its flavors announce them. Empty: the trust store decides.")
		String[] trusted_issuers() default {};
	}

	@Reference
	private BrokerCatalog catalog;

	@Reference
	private BrokerImplementations implementations;

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> rsObjects;

	private Config config;

	@Activate
	void activate(Config config) {
		this.config = config;
		for (String document : List.of("broker-catalog-api.xmi", "broker-implementations-api.xmi",
				"broker-lookup-api.xmi")) {
			try {
				announce(document, URI.create(config.public_url()));
			} catch (Throwable failure) {
				LOG.log(Level.WARNING, "[DDSR] BrokerSelfPublisher: " + document + " failed", failure);
			}
		}
	}

	/**
	 * Seed one of the broker's own contracts and announce the
	 * implementation that serves it.
	 *
	 * <p>Everything about that implementation — which operations, which
	 * paths, where every argument travels, which error is which status —
	 * is in the document. Nothing is restated here, which is the point:
	 * the same document is what the generic distribution serves the API
	 * by, so what the broker announces and what it answers cannot drift
	 * apart (#76). Only the deployment fact is added: where this instance
	 * is reachable.
	 */
	private void announce(String documentName, URI publicUrl) throws IOException {
		List<EObject> roots = readBundle(documentName);
		ServiceProvider provider = (ServiceProvider) roots.get(0);
		ServiceInterface contract = (ServiceInterface) roots.get(1);
		ServiceImplementation implementation = provider.getImplementations().get(0);

		// Already announced — from a restored snapshot, say. Re-publishing
		// would add a second registration of the same thing.
		for (ServiceImplementation live : catalog.getRegistry().getImplementations()) {
			if (implementation.getName().equals(live.getName())) {
				return;
			}
		}

		Diagnostic added = catalog.addCatalogEntry(contract, "broker-self-publisher");
		if (added.getSeverity() == DiagnosticSeverity.ERROR
				&& added.getCode() != DdsrDiagnostics.CODE_CATALOG_ENTRY_ALREADY_EXISTS) {
			LOG.warning("[DDSR] BrokerSelfPublisher: " + contract.getName() + " refused: " + added.getMessage());
			return;
		}
		ServiceInterface inCatalog = findInCatalog(contract.getName());
		if (inCatalog != null && inCatalog != contract) {
			// The catalog already held it; reference the live entry so the
			// publish does not carry a second copy of the same contract.
			implementation.getServiceInterfaces().clear();
			implementation.getServiceInterfaces().add(inCatalog);
		}

		RestFlavor flavor = (RestFlavor) implementation.getFlavors().get(0);
		String mountPath = flavor.getBasePath() != null ? flavor.getBasePath() : "";
		flavor.setHost(publicUrl.getScheme() + "://" + publicUrl.getAuthority());
		flavor.setBasePath((publicUrl.getPath() != null ? publicUrl.getPath() : "") + mountPath);
		// The other deployment fact: how a caller proves itself here. The
		// documents say who may call (the role matrix, SECURITY.md §3);
		// only the instance knows which issuer it trusts.
		SecurityScheme scheme = schemeOf(config);
		if (scheme != null) {
			flavor.getSecuritySchemes().add(scheme);
		}

		provider.setSymbolicName("org.eclipse.fennec.services.broker");
		Diagnostic published = implementations.publishImplementation(provider, implementation);
		if (published.getSeverity() == DiagnosticSeverity.ERROR) {
			LOG.warning("[DDSR] self-publish of " + contract.getName() + " failed: code="
					+ published.getCode() + " " + published.getMessage());
			return;
		}
		LOG.info("[DDSR] BrokerSelfPublisher: " + contract.getName() + " seeded and served at "
				+ flavor.getHost() + flavor.getBasePath());
	}

	private ServiceInterface findInCatalog(String name) {
		for (ServiceInterface si : catalog.getRegistry().getCatalog()) {
			if (name.equals(si.getName())) {
				return si;
			}
		}
		return null;
	}

	/**
	 * The two roots of one API document: the provider that serves the
	 * contract, and the contract itself. One document on purpose — every
	 * reference from the flavor into the contract is then intra-document
	 * and resolves wherever the file is read.
	 */
	private List<EObject> readBundle(String resourceName) throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
			if (in == null) {
				throw new IOException("bundle resource not found: " + resourceName);
			}
			List<EObject> roots = XmiCodec.readBundle(in, rsObjects).roots();
			if (roots.size() != 2 || !(roots.get(0) instanceof ServiceProvider)
					|| !(roots.get(1) instanceof ServiceInterface)) {
				throw new IOException(resourceName
						+ " must hold a ServiceProvider and the ServiceInterface it serves, in that order");
			}
			return roots;
		}
	}

	/**
	 * The scheme the broker's own flavors name. In mode bearer, the
	 * issuer its verifier accepts. In mode none, a connection scheme —
	 * which is the model's way of saying that this node checks nothing
	 * per call, and exactly what an open broker is; the contracts and
	 * their requirements stay the same in every mode, so does their sd1.
	 * In mode mtls, a client-certificate scheme naming the authorities the
	 * broker trusts.
	 * Bearer without an issuer names nothing: such a broker serves
	 * nothing either, and announcing a scheme nobody can satisfy would be
	 * the one lie here.
	 */
	static SecurityScheme schemeOf(Config config) {
		String mode = config.auth_mode() == null ? "" : config.auth_mode().strip();
		if ("none".equals(mode)) {
			ConnectionScheme open = ServicesFactory.eINSTANCE.createConnectionScheme();
			open.setName("none");
			open.setKind(SecuritySchemeKind.CONNECTION);
			return open;
		}
		if ("mtls".equals(mode)) {
			MutualTlsScheme certificates = ServicesFactory.eINSTANCE.createMutualTlsScheme();
			certificates.setName("mtls");
			certificates.setKind(SecuritySchemeKind.MUTUAL_TLS);
			for (String issuer : config.trusted_issuers()) {
				if (issuer != null && !issuer.isBlank()) {
					certificates.getTrustedIssuers().add(issuer.strip());
				}
			}
			return certificates;
		}
		if (config.issuer() == null || config.issuer().isBlank()) {
			return null;
		}
		BearerTokenScheme bearer = ServicesFactory.eINSTANCE.createBearerTokenScheme();
		bearer.setName("bearer");
		bearer.setKind(SecuritySchemeKind.BEARER_TOKEN);
		bearer.setIssuer(config.issuer().strip());
		for (String audience : config.audiences()) {
			if (audience != null && !audience.isBlank()) {
				bearer.getAudiences().add(audience.strip());
			}
		}
		bearer.setJwksUri(config.jwks_uri() == null || config.jwks_uri().isBlank() ? null : config.jwks_uri());
		bearer.setScopeClaim(config.scope_claim());
		return bearer;
	}
}
