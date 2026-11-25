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

package org.eclipse.fennec.services.broker.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
import org.eclipse.fennec.services.broker.core.BrokerApiDocument;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Self-publishes the broker's own REST API into its catalog and
 * implementation index. Eat-your-own-dogfood: the broker becomes the
 * first entry in the registry it provides.
 *
 * <p>The broker's own API documents — BrokerCatalog,
 * BrokerImplementations, BrokerLookup, BrokerEvents — live in broker.rest,
 * which serves them, and reach this publisher as {@link BrokerApiDocument}
 * services like any other document offered beside the broker. For each it
 * puts the contract in the catalog and announces the implementation that
 * serves it. Nothing about those implementations is written here: which
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
 * <p>A bundle that serves another contract beside the broker offers its
 * document the same way; it is announced like the broker's own, and
 * withdrawn when the service goes (#287).
 *
 * <p>The publisher lives in broker.core, in a private package, because in
 * mode none it announces through a publish that waives one invariant
 * (#320, {@link DdsrBrokerComponent#publishOnOpenNode}). That publish is
 * package-private and served to no other bundle and on no transport.
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
// REQUIRE: what is announced is where the broker is reachable, and the
// defaults name http://localhost:8887. Under OPTIONAL a slow start
// activated this before the configuration arrived and announced the
// broker's own contracts at the default address — on a node configured
// elsewhere, a consumer then called a different broker (seen in CI with
// the harness's mtls broker on 8898). Every launch carries this PID in
// the broker's config.json.
@Component(
		service = BrokerSelfPublisher.class,
		configurationPid = "org.eclipse.fennec.services.broker.rest",
		configurationPolicy = ConfigurationPolicy.REQUIRE,
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

	private volatile Config config;

	/** What is served beside the broker — its own four and other bundles' — as offered (#287). */
	private final Map<BrokerApiDocument, Announced> offered = new ConcurrentHashMap<>();

	/** One announcement of an offered document, so it can be withdrawn with it. */
	private record Announced(ServiceProvider provider, ServiceImplementation implementation) {
	}

	@Activate
	void activate(Config config) {
		this.config = config;
		for (BrokerApiDocument document : List.copyOf(offered.keySet())) {
			announceOffered(document);
		}
	}

	/**
	 * A contract served beside the broker — its own four from broker.rest,
	 * or another bundle's: announced with the broker's address and scheme
	 * (#287).
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addDocument(BrokerApiDocument document) {
		offered.put(document, NOT_YET);
		if (config != null) {
			announceOffered(document);
		}
	}

	void removeDocument(BrokerApiDocument document) {
		Announced announced = offered.remove(document);
		if (announced != null && announced != NOT_YET) {
			Diagnostic withdrawn = implementations.withdrawImplementation(announced.provider(), announced.implementation());
			LOG.info("[DDSR] BrokerSelfPublisher: " + announced.implementation().getName() + " withdrawn ("
					+ withdrawn.getSeverity() + ")");
		}
	}

	/** Marks an offered document that is not announced (yet). */
	private static final Announced NOT_YET = new Announced(null, null);

	private void announceOffered(BrokerApiDocument document) {
		try (InputStream in = document.document().openStream()) {
			List<EObject> roots = rootsOf(in, document.document().toString());
			Announced announced = announce(roots, URI.create(config.public_url()));
			if (announced != null && offered.containsKey(document)) {
				offered.put(document, announced);
			}
		} catch (Throwable failure) {
			LOG.log(Level.WARNING, "[DDSR] BrokerSelfPublisher: " + document.document() + " failed", failure);
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
	private synchronized Announced announce(List<EObject> roots, URI publicUrl) {
		ServiceProvider provider = (ServiceProvider) roots.get(0);
		ServiceInterface contract = (ServiceInterface) roots.get(1);
		ServiceImplementation implementation = provider.getImplementations().get(0);

		// Already announced — from a restored snapshot, say. Re-publishing
		// would add a second registration of the same thing.
		for (ServiceImplementation live : catalog.getRegistry().getImplementations()) {
			if (implementation.getName().equals(live.getName())) {
				return live.eContainer() instanceof ServiceProvider owner ? new Announced(owner, live) : null;
			}
		}

		Diagnostic added = catalog.addCatalogEntry(contract, "broker-self-publisher");
		if (added.getSeverity() == DiagnosticSeverity.ERROR
				&& added.getCode() != DdsrDiagnostics.CODE_CATALOG_ENTRY_ALREADY_EXISTS) {
			LOG.warning("[DDSR] BrokerSelfPublisher: " + contract.getName() + " refused: " + added.getMessage());
			return null;
		}
		ServiceInterface inCatalog = findInCatalog(contract.getName());
		if (inCatalog != null && inCatalog != contract) {
			// The catalog already held it — a contract outlives its
			// implementations (#271), so after a restart from a snapshot it
			// is there while the implementation is not. Point everything of
			// the provider at the live entry, not only the implementation:
			// the flavor's bindings name operations and parameters too, and
			// left on the copy just read they point at objects in no
			// resource, which fails every later snapshot of the broker.
			if (!rewire(provider, contract, inCatalog)) {
				LOG.warning("[DDSR] BrokerSelfPublisher: " + contract.getName()
						+ " differs from the contract the catalog holds — not announced");
				return null;
			}
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
		// In mode none the broker's contracts keep the roles they name in
		// every mode (sd1 does not change with the deployment), and the
		// flavor says that this node checks nothing per call. That pairing
		// is what scopesNeedAPerCallScheme refuses of any other provider;
		// for the open broker it is the documented state (SECURITY.md §7b,
		// §7c), so its own announcement is spared that one invariant (#320).
		// Only through this bundle's own broker: a BrokerImplementations of
		// another bundle gets the publish every provider gets.
		Diagnostic published = isOpen(config) && implementations instanceof DdsrBrokerComponent own
				? own.publishOnOpenNode(provider, implementation)
				: implementations.publishImplementation(provider, implementation);
		if (published.getSeverity() == DiagnosticSeverity.ERROR) {
			LOG.warning("[DDSR] self-publish of " + contract.getName() + " failed: code="
					+ published.getCode() + " " + published.getMessage());
			return null;
		}
		LOG.info("[DDSR] BrokerSelfPublisher: " + contract.getName() + " seeded and served at "
				+ flavor.getHost() + flavor.getBasePath());
		return new Announced(provider, implementation);
	}

	/**
	 * Moves every reference from {@code root}'s tree into {@code read} over
	 * to the same object in {@code live}, found by its path inside the
	 * contract.
	 *
	 * @return {@code false}, changing nothing, when an object has no
	 *         counterpart of the same kind and name — then the two are not
	 *         the same contract
	 */
	static boolean rewire(EObject root, ServiceInterface read, ServiceInterface live) {
		Map<EObject, EObject> counterparts = new HashMap<>();
		counterparts.put(read, live);
		for (Iterator<EObject> it = read.eAllContents(); it.hasNext();) {
			EObject original = it.next();
			EObject counterpart = EcoreUtil.getEObject(live, EcoreUtil.getRelativeURIFragmentPath(read, original));
			if (counterpart == null || counterpart.eClass() != original.eClass()
					|| !Objects.equals(nameOf(original), nameOf(counterpart))) {
				return false;
			}
			counterparts.put(original, counterpart);
		}
		List<EObject> tree = new ArrayList<>();
		tree.add(root);
		root.eAllContents().forEachRemaining(tree::add);
		for (EObject object : tree) {
			for (EReference reference : object.eClass().getEAllReferences()) {
				if (reference.isContainment() || reference.isContainer() || reference.isDerived()
						|| !reference.isChangeable() || !object.eIsSet(reference)) {
					continue;
				}
				if (reference.isMany()) {
					@SuppressWarnings("unchecked")
					List<EObject> values = (List<EObject>) object.eGet(reference);
					for (int i = 0; i < values.size(); i++) {
						EObject counterpart = counterparts.get(values.get(i));
						if (counterpart != null) {
							values.set(i, counterpart);
						}
					}
				} else {
					EObject counterpart = counterparts.get((EObject) object.eGet(reference));
					if (counterpart != null) {
						object.eSet(reference, counterpart);
					}
				}
			}
		}
		return true;
	}

	private static Object nameOf(EObject object) {
		EStructuralFeature name = object.eClass().getEStructuralFeature("name");
		return name == null ? null : object.eGet(name);
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
	private List<EObject> rootsOf(InputStream in, String name) throws IOException {
		List<EObject> roots = XmiCodec.readBundle(in, rsObjects).roots();
		if (roots.size() != 2 || !(roots.get(0) instanceof ServiceProvider)
				|| !(roots.get(1) instanceof ServiceInterface)) {
			throw new IOException(name + " must hold a ServiceProvider and the ServiceInterface it serves, in that order");
		}
		return roots;
	}

	/** Mode none: the deployment's explicit choice of an open broker. */
	static boolean isOpen(Config config) {
		return "none".equals(config.auth_mode() == null ? "" : config.auth_mode().strip());
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
