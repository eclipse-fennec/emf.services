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

package org.eclipse.fennec.services.client.rest.internal;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.common.TlsContexts;
import org.eclipse.fennec.services.common.TokenSupplier;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.eclipse.fennec.services.xmi.codec.XmiBundleMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.XmiBundleMessageBodyWriter;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyWriter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * Shared Jakarta REST {@link Client} for the three proxy components.
 * The {@link ClientBuilder} comes from the OSGi service registry —
 * org.eclipse.osgitech.rest.client registers it backed by Jersey, so
 * we don't have to fiddle with the thread-context classloader to make
 * the JAX-RS SPI lookup work inside OSGi.
 *
 * <p>The four shared MBR/MBW providers are registered on the client
 * as plain instances. They reuse the same classes the broker side
 * uses (DS-registered there as Jakartars Whiteboard extensions); the
 * OSGi-side DS bindings of those classes are orthogonal to the
 * instance-level registration we do here.
 */
@Component(
		service = RestTransport.class,
		configurationPid = "org.eclipse.fennec.services.client.rest",
		immediate = true)
@Designate(ocd = RestTransport.Config.class)
public final class RestTransport {

	@ObjectClassDefinition(name = "DDSR REST Client Transport",
			description = "HTTP target for the BrokerCatalog/Implementations/Lookup REST proxies")
	public @interface Config {

		@AttributeDefinition(
				name = "Broker URL",
				description = "Base URL of the broker REST endpoint, e.g. http://localhost:8887/ddsr/rest")
		String broker_url() default "http://localhost:8887/ddsr/rest";

		@AttributeDefinition(
				name = "Connect timeout (ms)",
				description = "TCP connect timeout for broker and service calls (#59). 0 = transport default.",
				required = false)
		long connect_timeout_millis() default 3000;

		@AttributeDefinition(
				name = "Read timeout (ms)",
				description = "Time to wait for a response after the request was sent (#59). A registered "
						+ "provider that no longer answers surfaces as a transport failure instead of a hang. "
						+ "0 = transport default.",
				required = false)
		long read_timeout_millis() default 10000;

		@AttributeDefinition(
				name = "Origin label",
				description = "Which system this is, for the X-DDSR-Origin header every call carries (#125): "
						+ "a deployment name such as payments-prod-eu. The other half of the origin is the "
						+ "framework UUID and is taken from the runtime. Deliberately not a host name — name "
						+ "the system, not the machine.",
				required = false)
		String origin_label() default "";

		@AttributeDefinition(name = "Broker authentication", required = false,
				description = "How this node proves itself to the broker: token (the bearer token of the "
						+ "token supplier) or certificate (the TLS key store below, for a broker in mode mtls). "
						+ "With certificate no token goes to the broker; tokens for providers are asked for "
						+ "per audience either way.")
		String broker_authentication() default TOKEN;

		@AttributeDefinition(name = "TLS key store", required = false,
				description = "The certificate and key this node shows to a server that asks for one (mode "
						+ "mtls, SECURITY.md §7d). Empty: it shows none.")
		String tls_keystore() default "";

		@AttributeDefinition(name = "TLS key store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the key store and of its key.")
		String tls_keystore_password() default "";

		@AttributeDefinition(name = "TLS key store type", required = false, description = "PKCS12 or JKS.")
		String tls_keystore_type() default "PKCS12";

		@AttributeDefinition(name = "TLS trust store", required = false,
				description = "The authorities whose server certificates this node believes. Empty: the JVM's "
						+ "own.")
		String tls_truststore() default "";

		@AttributeDefinition(name = "TLS trust store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the trust store.")
		String tls_truststore_password() default "";

		@AttributeDefinition(name = "TLS trust store type", required = false, description = "PKCS12 or JKS.")
		String tls_truststore_type() default "PKCS12";
	}

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> rsObjects;

	@Reference
	private ClientBuilder clientBuilder;

	/**
	 * Whoever is watching calls, if anyone is (#126). Optional and
	 * dynamic, like everywhere else this seam is used.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile CallTracer tracer;

	/**
	 * Where the bearer token for the broker comes from, if this node
	 * has one (SECURITY.md §4). Optional and dynamic: against a broker
	 * in mode none nothing is registered, and a supplier that arrives
	 * later is used from the next call on.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile TokenSupplier tokens;

	private static final Logger LOG = Logger.getLogger(RestTransport.class.getName());

	/** {@link Config#broker_authentication()}: the broker gets the token supplier's token. */
	static final String TOKEN = "token";

	/** {@link Config#broker_authentication()}: the broker gets the node's certificate, and no token. */
	static final String CERTIFICATE = "certificate";

	private URI baseUrl;

	/** Whether calls to the broker carry the token; not when the node proves itself by certificate. */
	private volatile boolean tokenToBroker = true;
	private Client client;
	private Config config;

	/**
	 * Who this runtime is, for every call it makes (#125). Fixed at
	 * activation: the framework half cannot change while the framework
	 * lives, and a label that changes mid-session would make the audit
	 * trail of one session read like two.
	 */
	private ClientOrigin origin;

	@Activate
	void activate(BundleContext context, Config config) {
		this.config = config;
		this.baseUrl = URI.create(config.broker_url());
		this.tokenToBroker = tokenToBroker(config);
		this.origin = ClientOrigin.of(config.origin_label(),
				context.getProperty(ClientOrigin.FRAMEWORK_UUID));
		this.client = build(config);
		LOG.info("[DDSR-Client] origin " + origin.token());
	}

	/**
	 * A changed configuration is taken, not died of.
	 *
	 * <p>This transport sits under every broker proxy, which sit under
	 * the client, which sits under discovery. Letting the component
	 * runtime destroy and rebuild it takes that whole column down with
	 * it — and Configuration Admin delivers the same configuration more
	 * than once while a framework starts, so it happened for no reason
	 * at all (#107). An identical configuration now changes nothing.
	 *
	 * <p>Changed timeouts do need a new client; the old one is closed
	 * once the new one is in place, which is no worse than the restart
	 * this method replaces.
	 */
	@Modified
	void modified(Config config) {
		this.baseUrl = URI.create(config.broker_url());
		this.tokenToBroker = tokenToBroker(config);
		boolean timeoutsChanged = config.connect_timeout_millis() != this.config.connect_timeout_millis()
				|| config.read_timeout_millis() != this.config.read_timeout_millis()
				|| !tlsOf(config).equals(tlsOf(this.config));
		// The runtime half stays; only the deployment's name for itself
		// is configuration. The filter reads the field, so a new label
		// takes effect without rebuilding the client.
		this.origin = ClientOrigin.of(config.origin_label(), origin.runtimeId());
		this.config = config;
		if (!timeoutsChanged) {
			return;
		}
		LOG.info("[DDSR-Client] REST transport timeouts or TLS changed — building a new client");
		Client previous = this.client;
		this.client = build(config);
		if (previous != null) {
			previous.close();
		}
	}

	private Client build(Config config) {
		if (config.connect_timeout_millis() > 0) {
			clientBuilder.connectTimeout(config.connect_timeout_millis(), TimeUnit.MILLISECONDS);
		}
		if (config.read_timeout_millis() > 0) {
			clientBuilder.readTimeout(config.read_timeout_millis(), TimeUnit.MILLISECONDS);
		}
		// One context for everything this client calls: the broker, its
		// event stream and the providers. The certificate is the node's
		// identity, not the broker's audience, so unlike the broker's
		// token it may go wherever the node calls.
		SSLContext tls = TlsContexts.of(config.tls_keystore(), config.tls_keystore_password(),
				config.tls_keystore_type(), config.tls_truststore(), config.tls_truststore_password(),
				config.tls_truststore_type());
		if (tls != null) {
			clientBuilder.sslContext(tls);
			LOG.info("[DDSR-Client] TLS: " + (config.tls_keystore().isBlank() ? "no client certificate"
					: "client certificate from " + config.tls_keystore())
					+ (config.tls_truststore().isBlank() ? ", the JVM's authorities" : ", authorities from "
							+ config.tls_truststore()));
		}
		return clientBuilder
				.register(new XmiMessageBodyReader(rsObjects))
				.register(new XmiMessageBodyWriter(rsObjects))
				.register(new XmiBundleMessageBodyReader(rsObjects))
				.register(new XmiBundleMessageBodyWriter(rsObjects))
				// #125 asks for the origin on EVERY call, and this is the
				// only place that is true by construction: a filter on the
				// shared client cannot be forgotten by a new proxy, and a
				// per-call header can. It rides on service invocations too,
				// not only on broker traffic — "which system told which
				// system what" is the question, and the answer is worth
				// nothing if it stops at the registry's door.
				.register((ClientRequestFilter) request ->
						request.getHeaders().putSingle(ClientOrigin.HEADER, origin.token()))
				.register((ClientRequestFilter) this::authorize)
				.build();
	}

	/**
	 * Whether the broker is shown the token. A node that proves itself by
	 * certificate shows none: the broker in mode mtls would ignore it, and
	 * a credential sent where it is not needed is one more place it can
	 * leak from.
	 */
	private static boolean tokenToBroker(Config config) {
		String how = config.broker_authentication() == null ? TOKEN : config.broker_authentication().strip();
		if (CERTIFICATE.equals(how)) {
			if (config.tls_keystore() == null || config.tls_keystore().isBlank()) {
				LOG.warning("[DDSR-Client] broker.authentication=certificate, but no tls.keystore — this node "
						+ "shows the broker neither a token nor a certificate");
			}
			return false;
		}
		if (!TOKEN.equals(how)) {
			LOG.warning("[DDSR-Client] broker.authentication=" + how + " is neither " + TOKEN + " nor "
					+ CERTIFICATE + " — taken as " + TOKEN);
		}
		return true;
	}

	/** What decides the TLS context, for telling a changed configuration from a repeated one. */
	private static List<String> tlsOf(Config config) {
		return List.of(config.tls_keystore(), config.tls_keystore_password(), config.tls_keystore_type(),
				config.tls_truststore(), config.tls_truststore_password(), config.tls_truststore_type());
	}

	/**
	 * The bearer token, on calls to the broker and on nothing else.
	 *
	 * <p>The same client carries service invocations to providers, and
	 * a token minted for the broker's audience must not travel there —
	 * a provider holding it could replay it against the broker. So the
	 * header goes by address: under the broker URL, or not at all.
	 * Tokens for providers are a matter of the contract's own
	 * requirement (slice 3), not of this filter.
	 */
	void authorize(ClientRequestContext request) {
		TokenSupplier supplier = tokens;
		URI broker = baseUrl;
		if (!tokenToBroker || supplier == null || broker == null || !isUnder(request.getUri(), broker)) {
			return;
		}
		request.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + supplier.token());
	}

	/** Same scheme, host and port, and a path at or below the broker's. */
	static boolean isUnder(URI target, URI broker) {
		if (target == null || !Objects.equals(target.getScheme(), broker.getScheme())
				|| !Objects.equals(target.getHost(), broker.getHost()) || target.getPort() != broker.getPort()) {
			return false;
		}
		String base = broker.getPath() == null ? "" : broker.getPath().replaceFirst("/+$", "");
		String path = target.getPath() == null ? "" : target.getPath();
		return path.equals(base) || path.startsWith(base + "/");
	}

	/**
	 * Who this runtime is, as the URI reference a CloudEvents
	 * {@code source} has to be (#101). The same identity the origin
	 * header carries — one answer to "who is calling", worn twice
	 * because the two readers are different: the header is read by this
	 * registry, the attribute by anything that reads CloudEvents.
	 */
	String originReference() {
		return "/consumer/" + (origin != null ? origin.token() : ClientOrigin.ANONYMOUS);
	}

	@Deactivate
	void deactivate() {
		if (client != null) {
			client.close();
			client = null;
		}
	}

	/**
	 * The origin token this client stamps on every request, for the
	 * places that also have to name a requestor in the body of the
	 * protocol rather than only in a header.
	 */
	String originToken() {
		return origin.token();
	}

	/**
	 * One call to the broker, traced (#126).
	 *
	 * <p>Here rather than in a client filter, and that took a try: a
	 * filter pair cannot close a span for a request that never reaches
	 * a response — a refused connection is exactly the call worth
	 * seeing, and it is the one a filter would lose. Here the span is
	 * closed by try-with-resources whatever happens.
	 *
	 * <p>Here rather than in each proxy, for the reason the origin
	 * header gives two methods up: what has to be true of every call
	 * belongs where every call passes, not where twelve call sites can
	 * each forget it.
	 *
	 * @param operation what the broker calls this, as
	 *        {@code Contract/operation} — the same name its own
	 *        dispatcher gives the serving half, so the two halves read
	 *        as one call
	 * @param request   the prepared request, before the verb
	 * @param verb      what to do with it: {@code b -> b.post(entity)}
	 */
	<T> T send(String operation, Invocation.Builder request, Function<Invocation.Builder, T> verb) {
		Map<String, String> context = new LinkedHashMap<>();
		try (CallSpan span = tracer().calling(operation, TraceCarrier.over(context))) {
			Invocation.Builder traced = request;
			for (Map.Entry<String, String> field : context.entrySet()) {
				traced = traced.header(field.getKey(), field.getValue());
			}
			span.attribute("rpc.system", "fennec.services")
					.attribute("server.address", baseUrl == null ? null : baseUrl.toString())
					.attribute("fennec.flavor", "REST")
					// Who is calling (#125). The same identity the origin
					// header carries, on the span as well, so a trace
					// answers "which system told which system what" and
					// not only "what happened".
					.attribute(ClientOrigin.ATTRIBUTE, origin == null ? null : origin.token());
			try {
				T answer = verb.apply(traced);
				if (answer instanceof Response response) {
					span.attribute("http.response.status_code", String.valueOf(response.getStatus()));
				}
				return answer;
			} catch (RuntimeException failed) {
				// Connection refused, a timeout, a body that would not
				// read: the proxies let these through as they always
				// have, and the span says what the caller will find out
				// by catching.
				span.failed(failed);
				throw failed;
			}
		}
	}

	private CallTracer tracer() {
		CallTracer bound = tracer;
		return bound == null ? CallTracer.NONE : bound;
	}

	WebTarget target() {
		return client.target(baseUrl);
	}

	/** Build a target for an absolute URL (e.g. the one a ServiceLocator
	 *  resolves from a RestFlavor.host + opPath). The configured
	 *  base URL is only relevant for proxy calls against the
	 *  ddsr-broker itself; other services may live on different hosts. */
	WebTarget targetFor(URI absoluteUrl) {
		return client.target(absoluteUrl);
	}

	/**
	 * A target from the endpoint as the flavor states it — path templates
	 * included, which {@link URI} could not carry. The caller closes them
	 * with {@code resolveTemplate}.
	 */
	WebTarget targetFor(String absoluteUrl) {
		return client.target(absoluteUrl);
	}

	URI baseUrl() {
		return baseUrl;
	}
}
