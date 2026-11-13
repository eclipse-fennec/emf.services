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

package org.eclipse.fennec.services.models.atlas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.eclipse.fennec.model.atlas.rest.client.api.ModelAtlasClient;
import org.eclipse.fennec.model.atlas.rest.client.api.ModelAtlasClientFactory;
import org.eclipse.fennec.model.atlas.rest.client.api.ResolvedEPackage;
import org.eclipse.fennec.model.atlas.rest.client.api.VersionMismatchException;
import org.eclipse.fennec.services.models.ModelDocument;
import org.eclipse.fennec.services.models.ModelSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.ws.rs.client.ClientBuilder;

/**
 * The Model Atlas as the source of a broker's payload models (#256).
 *
 * <p>Reads one scope at one stage, {@code release} unless configured
 * otherwise: a contract may point only at a model the atlas has released.
 * A pinned question goes to the atlas with the fingerprint as a
 * precondition, and the answer is checked here again - the fingerprint
 * of a {@link ModelDocument} is computed over the package, never taken
 * on trust, not even from the atlas.
 *
 * <p>Read-only. Models are published in the atlas, through its workflow;
 * what a provider hands to the broker goes to the broker's own store and
 * counts for the gate only once the atlas holds the same version.
 *
 * <p>A pinned answer is kept: content named by its fingerprint does not
 * change. An unpinned one is asked every time.
 *
 * <p>Registers itself, and only with {@code base.uri}: the broker's
 * configuration carries this PID with the atlas from the environment,
 * and an empty one means no atlas, without an error - the same rule as
 * the JWT verifier.
 */
@Component(name = AtlasModelSource.PID, configurationPolicy = ConfigurationPolicy.REQUIRE, service = {})
@Designate(ocd = AtlasModelSource.Config.class)
public final class AtlasModelSource implements ModelSource {

	static final String PID = "org.eclipse.fennec.services.models.atlas";

	private static final Logger LOG = Logger.getLogger(AtlasModelSource.class.getName());

	@ObjectClassDefinition(name = "Fennec Services Model Atlas source",
			description = "Reads the payload models a broker's contracts pin from a Model Atlas")
	@interface Config {

		@AttributeDefinition(description = "Base URI of the atlas REST API, e.g. http://atlas:8080/atlas/rest; empty = no atlas")
		String base_uri() default "";

		@AttributeDefinition(description = "The atlas scope the models are read from")
		String scope() default "";

		@AttributeDefinition(description = "The stage a model must have reached")
		String stage() default "release";

		@AttributeDefinition(description = "Connect timeout towards the atlas, in milliseconds")
		int connect_timeout_ms() default 5000;

		@AttributeDefinition(description = "Read timeout towards the atlas, in milliseconds")
		int read_timeout_ms() default 10000;
	}

	/** One read at the atlas: nsURI and pinned fingerprint (or null) to what the atlas answers. */
	interface AtlasRead extends BiFunction<String, String, Optional<ResolvedEPackage>> {
	}

	private final AtlasRead atlas;
	private final Function<EPackage, String> fingerprints;
	private final String origin;
	private final AutoCloseable client;
	private final ServiceRegistration<ModelSource> registration;

	/** nsURI + " " + fingerprint. */
	private final Map<String, ModelDocument> pinned = new ConcurrentHashMap<>();

	@Activate
	public AtlasModelSource(Config config, BundleContext context, @Reference ModelAtlasClientFactory clients,
			@Reference ClientBuilder http, @Reference FingerprintService fingerprints) {
		this(config.base_uri().isBlank() ? null : open(config, clients, http), config.scope(), config.stage(),
				ePackage -> fingerprints.fingerprint(ePackage), context);
		if (registration == null) {
			LOG.fine("[DDSR] no Model Atlas configured (base.uri is empty)");
		} else {
			LOG.info(() -> "[DDSR] payload models come from the Model Atlas at " + config.base_uri() + ", " + origin);
		}
	}

	private AtlasModelSource(ModelAtlasClient client, String scope, String stage,
			Function<EPackage, String> fingerprints, BundleContext context) {
		this(client == null ? (nsURI, fingerprint) -> Optional.empty()
				: (nsURI, fingerprint) -> client.ePackages().resolveAtStage(nsURI, scope, stage, fingerprint),
				fingerprints, scope, stage, client, client == null ? null : context);
	}

	AtlasModelSource(AtlasRead atlas, Function<EPackage, String> fingerprints, String scope, String stage,
			AutoCloseable client) {
		this(atlas, fingerprints, scope, stage, client, null);
	}

	private AtlasModelSource(AtlasRead atlas, Function<EPackage, String> fingerprints, String scope, String stage,
			AutoCloseable client, BundleContext context) {
		this.atlas = atlas;
		this.fingerprints = fingerprints;
		this.origin = "atlas " + scope + "/" + stage;
		this.client = client;
		this.registration = context == null ? null
				: context.registerService(ModelSource.class, this,
						FrameworkUtil.asDictionary(Map.of(ModelSource.PROPERTY, "atlas")));
	}

	private static ModelAtlasClient open(Config config, ModelAtlasClientFactory clients, ClientBuilder http) {
		return AtlasClients.open(clients, http, config.base_uri(), config.scope(), config.connect_timeout_ms(),
				config.read_timeout_ms());
	}

	@Deactivate
	void deactivate() {
		if (registration != null) {
			registration.unregister();
		}
		if (client == null) {
			return;
		}
		try {
			client.close();
		} catch (Exception closing) {
			LOG.log(Level.FINE, "[DDSR] closing the Model Atlas client", closing);
		}
	}

	@Override
	public Optional<ModelDocument> resolve(String nsURI, String fingerprint) {
		if (nsURI == null || nsURI.isBlank()) {
			return Optional.empty();
		}
		if (fingerprint != null) {
			ModelDocument known = pinned.get(nsURI + " " + fingerprint);
			if (known != null) {
				return Optional.of(known);
			}
		}
		Optional<ResolvedEPackage> answer;
		try {
			answer = atlas.apply(nsURI, fingerprint);
		} catch (VersionMismatchException otherVersion) {
			LOG.fine(() -> "[DDSR] the atlas holds " + nsURI + " in another version than " + fingerprint);
			return Optional.empty();
		} catch (RuntimeException unreachable) {
			LOG.log(Level.WARNING, "[DDSR] the Model Atlas could not answer for " + nsURI + ": "
					+ unreachable.getMessage(), unreachable);
			return Optional.empty();
		}
		if (answer.isEmpty() || answer.get().getEPackage() == null) {
			return Optional.empty();
		}
		EPackage ePackage = answer.get().getEPackage();
		String computed = fingerprints.apply(ePackage);
		if (computed == null || computed.isBlank()) {
			LOG.warning(() -> "[DDSR] no fingerprint could be computed for " + nsURI + " from the atlas");
			return Optional.empty();
		}
		if (fingerprint != null && !fingerprint.equals(computed)) {
			LOG.warning(() -> "[DDSR] the atlas answered " + nsURI + " at " + fingerprint + " with " + computed);
			return Optional.empty();
		}
		ModelDocument document;
		try {
			document = new ModelDocument(nsURI, computed, ecoreOf(ePackage), origin
					+ (answer.get().getVersion() == null ? "" : " version " + answer.get().getVersion()));
		} catch (IOException unwritable) {
			LOG.log(Level.WARNING, "[DDSR] the payload model " + nsURI + " from the atlas could not be written",
					unwritable);
			return Optional.empty();
		}
		pinned.putIfAbsent(nsURI + " " + computed, document);
		return Optional.of(document);
	}

	/**
	 * The Ecore document of a package, written as a copy under its nsURI:
	 * the references inside it become fragments of that one document, and
	 * whoever reads it computes the fingerprint computed here.
	 */
	static byte[] ecoreOf(EPackage ePackage) throws IOException {
		Resource resource = new EcoreResourceFactoryImpl()
				.createResource(URI.createURI(ePackage.getNsURI()));
		resource.getContents().add(EcoreUtil.copy(ePackage));
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resource.save(bytes, Map.of());
		return bytes.toByteArray();
	}
}
