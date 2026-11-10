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

package org.eclipse.fennec.services.client.messaging.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.common.TlsContexts;
import org.eclipse.fennec.services.flavor.messaging.MessagingRoute;
import org.eclipse.fennec.services.invocation.Invocations;
import org.eclipse.fennec.services.messaging.LinkSettings;
import org.eclipse.fennec.services.messaging.MessagingLink;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.gecko.messaging.api.annotations.RequireMessaging;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.cloudevents.model.ce.CloudEvent;

/**
 * Calls a service over a messaging broker: the consumer half of the
 * convention whose provider half is {@code provider.messaging}.
 *
 * <p>One invoker for every flavor that travels over messaging — MQTT
 * and AMQP today. Over messaging the API is the same for both: publish
 * the call to a channel, receive the one answer that correlates with
 * it. What differs is read from the flavor as a {@link MessagingRoute}
 * and is not this class's business, with one exception it cannot hide:
 * how the answer finds its way back.
 * <ul>
 * <li>Where the protocol carries a reply address (AMQP), the
 * connection does all of it: it declares a queue only it can read,
 * names it in the request and matches the answer.</li>
 * <li>Where it does not (MQTT 3.1.1), the address travels in the
 * envelope. The reply topic is subscribed first and published to
 * second, and the first step is not a detail: a broker fast enough to
 * answer before the subscription exists would deliver the answer to
 * nobody. The subtree carries this consumer's name, so that a broker's
 * ACL can keep two consumers' answers apart.</li>
 * </ul>
 *
 * <p>Where to dial is the service's own statement — the broker its
 * flavor announces — so this invoker needs no address of its own. What
 * it is configured with is how long to wait and who this node is to a
 * broker, neither of which a flavor can say. Which protocols it can
 * speak is decided by the bindings the launch carries.
 *
 * <p>Synchronous, like the REST invoker and for the same reason: the
 * proxy the SDK hands out has Java methods that return values.
 */
@Designate(ocd = MessagingServiceInvoker.Config.class)
@Component(service = ServiceInvoker.class,
		configurationPid = "org.eclipse.fennec.services.client.messaging.invoker",
		property = { ServiceInvoker.FLAVOR_PROPERTY + "=MQTT", ServiceInvoker.FLAVOR_PROPERTY + "=AMQP" })
@RequireMessaging
public class MessagingServiceInvoker implements ServiceInvoker {

	private static final Logger LOG = Logger.getLogger(MessagingServiceInvoker.class.getName());

	@ObjectClassDefinition(name = "Fennec Services Messaging Invoker",
			description = "How this runtime calls services announced over a messaging broker (MQTT, AMQP).")
	public @interface Config {

		@AttributeDefinition(name = "Reply timeout (seconds)",
				description = "How long a call waits for its answer before failing. A transport failure, "
						+ "so the proxy rebinds and retries once — the same treatment a REST call gets "
						+ "when a provider stops answering.")
		int reply_timeout_seconds() default 10;

		@AttributeDefinition(name = "User name", required = false,
				description = "The user this node connects to brokers as. Empty: the broker's default. "
						+ "Stated here and never in a broker URL — the URL is what a flavor announces.")
		String username() default "";

		@AttributeDefinition(name = "Password", type = AttributeType.PASSWORD, required = false,
				description = "The user's password.")
		String password() default "";

		@AttributeDefinition(name = "TLS key store", required = false,
				description = "The certificate and key this node shows brokers announced with a TLS URL "
						+ "(ssl://, amqps://; PKCS12). Over MQTT its CN becomes the consumer segment of "
						+ "the reply topics — the name a broker's ACL lets read them.")
		String tls_keystore() default "";

		@AttributeDefinition(name = "TLS key store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the key store and of its key.")
		String tls_keystore_password() default "";

		@AttributeDefinition(name = "TLS trust store", required = false,
				description = "The authorities whose broker certificates this node believes (PKCS12). "
						+ "Empty: the JVM's.")
		String tls_truststore() default "";

		@AttributeDefinition(name = "TLS trust store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the trust store.")
		String tls_truststore_password() default "";
	}

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	/**
	 * Whoever is watching calls, if anyone is (#126). Optional and
	 * dynamic: a deployment without telemetry installs nothing and this
	 * invoker notices nothing.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile CallTracer tracer;

	private CallTracer tracer() {
		CallTracer bound = tracer;
		return bound == null ? CallTracer.NONE : bound;
	}

	/** One link per broker (and exchange), opened by the first call that goes there. */
	private final Map<String, MessagingLink> links = new LinkedHashMap<>();

	/**
	 * One inbox per link and reply subtree, shared by every call that
	 * uses it — only where the way back is in the envelope.
	 */
	private final Map<String, ReplyInbox> inboxes = new LinkedHashMap<>();

	private BundleContext context;

	private Config config;

	private String source;

	/** This runtime's name — the level its answers arrive under, where they arrive on a tree. */
	private String consumer;

	@Activate
	void activate(BundleContext context, Config config) {
		this.context = context;
		this.config = config;
		String uuid = context.getProperty(Constants.FRAMEWORK_UUID);
		this.source = "/consumer/" + (uuid == null ? ClientOrigin.ANONYMOUS : uuid);
		// A node with a certificate is that certificate's CN: its answers
		// arrive under that name, and a broker that takes the CN as the
		// user can let only it read them (SECURITY.md §7d, §8). Without
		// one, the runtime's own id keeps two runtimes' answers apart.
		String name = TlsContexts.commonName(config.tls_keystore(), config.tls_keystore_password(), "PKCS12");
		this.consumer = name != null ? name : uuid == null ? ClientOrigin.ANONYMOUS : uuid;
	}

	@Deactivate
	void deactivate() {
		synchronized (inboxes) {
			inboxes.clear();
		}
		synchronized (links) {
			for (MessagingLink link : links.values()) {
				link.close();
			}
			links.clear();
		}
	}

	@Override
	public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
		if (locator == null || operationName == null) {
			throw new DdsrException("locator and operationName must not be null");
		}
		MessagingRoute route = MessagingRoute.of(locator.implementation());
		if (route == null) {
			throw new DdsrException("this service does not expose a flavor that travels over messaging");
		}
		MessagingRoute.Operation placed = route.operation(operationName);
		if (placed == null) {
			throw new DdsrException("operation '" + operationName + "' has no " + route.protocol()
					+ " flavor on this service");
		}
		ServiceOperation operation = placed.operation();
		if (operation == null) {
			throw new DdsrException("the " + route.protocol() + " flavor for '" + operationName
					+ "' binds no operation");
		}

		CloudEvent request;
		byte[] document;
		String replyTopic = null;
		try {
			ServiceInvocation invocation = Invocations.invocation(operation,
					args == null ? Map.of() : args);
			document = write(Invocations.roots(invocation), placed.consumes());
			// The envelope says what it carries twice over: the encoding
			// (datacontenttype) and the schema the document adheres to
			// (dataschema) — what a reader needs to pick the resource for
			// it before it has opened it.
			request = CloudEvents.describing(
					MessagingRoute.request(source, operation.getName(), placed.consumes(), null), invocation);
			if (!route.repliesNatively()) {
				// The reply topic has to be known before the call exists,
				// because the call carries it — so the envelope is built
				// first and its own id names the topic it answers on.
				replyTopic = route.replyTopic(placed, consumer, request.getEventId());
				request.getExtensions().put(CloudEvents.EXTENSION_REPLY_TO, replyTopic);
			}
		} catch (Exception unwritable) {
			throw new DdsrException("could not write the call to " + operationName + ": "
					+ unwritable.getMessage(), unwritable);
		}

		MessagingLink link = linkFor(route);
		long timeout = TimeUnit.SECONDS.toMillis(config.reply_timeout_seconds());
		// The trace context travels in the envelope's extensions, which is
		// where CloudEvents puts it and the one place every protocol here
		// can carry it (#126). Written before the message is serialised
		// and after the span exists, because what travels must name THIS
		// call. `map()` is a view on the EMap, so writing through it
		// writes the extension.
		try (CallSpan span = tracer().calling(spanNameOf(operation, operationName),
				TraceCarrier.over(request.getExtensions().map()))) {
			span.attribute("rpc.system", "fennec.services")
					.attribute("rpc.method", operationName)
					.attribute("server.address", placed.channel())
					.attribute("fennec.flavor", route.protocol())
					// Who is calling (#125): the same identity the envelope's
					// source carries, which is where it travels on messaging.
					.attribute(ClientOrigin.ATTRIBUTE, source);
			try {
				byte[] answer = route.repliesNatively()
						? link.request(placed.channel(), MessagingRoute.write(request, document), placed.qos(),
								timeout)
						: viaInbox(link, route, placed, request, document, replyTopic, timeout);
				return valueOf(answer, request, operationName);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				DdsrException failed = DdsrException.transport("waiting for the answer to "
						+ operationName + " was interrupted", interrupted);
				span.failed(failed);
				throw failed;
			} catch (TimeoutException silent) {
				DdsrException failed = DdsrException.transport("no answer to " + operationName + " on "
						+ placed.channel() + " within " + config.reply_timeout_seconds() + "s", silent);
				span.failed(failed);
				throw failed;
			} catch (DdsrException alreadyOurs) {
				span.failed(alreadyOurs);
				throw alreadyOurs;
			} catch (Exception failed) {
				DdsrException transport = DdsrException.transport("calling " + operationName
						+ " over " + route.protocol() + " failed: " + failed.getMessage(), failed);
				span.failed(transport);
				throw transport;
			}
		}
	}

	/** The way back in the envelope: wait in this consumer's inbox for the answer that correlates. */
	private byte[] viaInbox(MessagingLink link, MessagingRoute route, MessagingRoute.Operation placed,
			CloudEvent request, byte[] document, String replyTopic, long timeout) throws Exception {
		ReplyInbox inbox = inboxFor(link, route.replySubtree(placed, consumer), placed.qos());
		inbox.expect(request.getEventId());
		try {
			// Never retained: a call is a transition.
			link.publish(placed.channel(), MessagingRoute.write(request, document), placed.qos(), timeout);
			byte[] answer = inbox.await(request.getEventId(), timeout, TimeUnit.MILLISECONDS);
			if (answer == null) {
				throw new TimeoutException("nothing arrived on " + replyTopic);
			}
			return answer;
		} finally {
			// The subscription stays: it is this consumer's inbox, not this
			// call's. What goes is the expectation of an answer.
			inbox.forget(request.getEventId());
		}
	}

	/**
	 * What to call this call in a trace: the contract and the operation,
	 * so a reader can group by it. The subject on the envelope is the
	 * operation alone, because that is what the wire has always said and
	 * renaming it would be a wire change for a trace's sake.
	 */
	private static String spanNameOf(ServiceOperation operation, String operationName) {
		if (operation.eContainer() instanceof ServiceInterface contract && contract.getName() != null) {
			return contract.getName() + "/" + operation.getName();
		}
		return operationName;
	}

	/**
	 * The inbox for one reply subtree, subscribed once.
	 *
	 * <p>Once rather than per call, because the subtree is this
	 * consumer's and the correlation in the envelope is what tells the
	 * calls apart. Several calls in flight then cost several entries in
	 * a map instead of several subscribe/unsubscribe round trips.
	 */
	private ReplyInbox inboxFor(MessagingLink link, String subtree, int qos) {
		synchronized (inboxes) {
			// Per broker, because a subtree of the same name on another
			// broker is another subscription.
			String key = link.url() + " " + subtree;
			ReplyInbox inbox = inboxes.get(key);
			if (inbox != null) {
				return inbox;
			}
			ReplyInbox fresh = new ReplyInbox(subtree);
			try {
				// In place when this returns, because the link is connected
				// — and restored by the link after every loss, so the inbox
				// outlives the connection it was opened on.
				link.subscribe(fresh.filter(), qos, (topic, received) -> fresh.deliver(received));
			} catch (RuntimeException unsubscribable) {
				throw DdsrException.transport("could not listen for answers on " + fresh.filter()
						+ ": " + unsubscribable.getMessage(), unsubscribable);
			}
			inboxes.put(key, fresh);
			LOG.info("[DDSR-MSG] listening for answers on " + fresh.filter());
			return fresh;
		}
	}

	/** What the answer says, or why there is none. */
	private Object valueOf(byte[] answer, CloudEvent request, String operationName) throws Exception {
		CloudEventCodec.Message message = MessagingRoute.read(answer, CloudEvents.TYPE_INVOKE_REPLY);
		String correlation = MessagingRoute.correlationOf(message.attributes());
		if (correlation != null && !correlation.equals(request.getEventId())) {
			// Matched by the inbox or by the protocol, so this should not
			// happen — which is exactly why it is worth saying when it does.
			LOG.warning("[DDSR-MSG] the answer to " + request.getEventId() + " says it correlates with "
					+ correlation);
		}
		XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(message.data()), resourceSets,
				message.attributes().getDatacontenttype());
		for (EObject root : bundle.roots()) {
			if (root instanceof ServiceInvocationResult result) {
				if (result.getDiagnostic() != null) {
					throw new DdsrException(operationName + " failed: "
							+ result.getDiagnostic().getMessage());
				}
				return Invocations.valueOf(result);
			}
		}
		throw new DdsrException("the answer to " + operationName + " carried no result");
	}

	/**
	 * The link to the broker this service announces, connected.
	 *
	 * <p>Opened by the first call and kept: the link reconnects on its
	 * own, so a broker that is away for a moment costs the calls made
	 * meanwhile and nothing after. A broker that was never there is a
	 * different matter — its link is taken away again, because an
	 * announcement naming a dead address must not leave a connection
	 * attempt behind for every such address a registry ever listed.
	 */
	private MessagingLink linkFor(MessagingRoute route) {
		String url = route.broker();
		if (url == null) {
			throw new DdsrException("the " + route.protocol()
					+ " flavor of this service announces no broker to call it on");
		}
		String key = url + " " + (route.exchange() == null ? "" : route.exchange());
		synchronized (links) {
			MessagingLink link = links.get(key);
			try {
				if (link == null) {
					String id = UUID.randomUUID().toString().substring(0, 8);
					link = MessagingLink.open(context,
							new LinkSettings("fennec.services.invoker." + id, url, "ddsr-consumer-" + id,
									config.tls_keystore(), config.tls_keystore_password(),
									config.tls_truststore(), config.tls_truststore_password())
									.onExchange(route.exchange()).as(config.username(), config.password()));
					links.put(key, link);
				}
				if (link.awaitConnected(config.reply_timeout_seconds(), TimeUnit.SECONDS)) {
					return link;
				}
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw DdsrException.transport("connecting to the broker at " + url + " was interrupted",
						interrupted);
			} catch (RuntimeException unreachable) {
				throw DdsrException.transport("cannot reach the broker at " + url + ": "
						+ unreachable.getMessage(), unreachable);
			}
			if (!link.hasBeenConnected()) {
				links.remove(key);
				link.close();
			}
			throw DdsrException.transport("cannot reach the broker at " + url + " within "
					+ config.reply_timeout_seconds() + "s", null);
		}
	}

	private byte[] write(List<EObject> roots, String contentType) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, resourceSets, contentType, roots);
		return out.toByteArray();
	}
}
