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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.Delivery;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.DdsrServiceException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.common.DeclaredErrors;
import org.eclipse.fennec.services.common.TlsContexts;
import org.eclipse.fennec.services.flavor.messaging.MessagingRoute;
import org.eclipse.fennec.services.invocation.Flows;
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
 * <p>One invoker for every flavor that travels over messaging,
 * whichever protocol its broker speaks. Over messaging the API is the
 * same: publish the call to a channel, receive the one answer that
 * correlates with it. Which channel, which broker, what encoding — all
 * of that is read from the flavor as a {@link MessagingRoute} and is
 * not this class's business. One thing the flavor says does change
 * what happens here: where the answer goes.
 * <ul>
 * <li>The flavor names a place for answers: this consumer listens on a
 * subtree of its own name there, and each call names its own channel
 * in it. The subtree is subscribed first and the call published
 * second, and the first step is not a detail — a broker fast enough to
 * answer before the subscription exists would deliver the answer to
 * nobody. That the subtree carries the consumer's name is what lets a
 * broker's ACL keep two consumers' answers apart.</li>
 * <li>The flavor names none: the connection provides a reply channel
 * only it reads, names it in the request and matches the answer.</li>
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

		@AttributeDefinition(name = "Connect timeout (seconds)", required = false,
				description = "How long a call waits for the connection to the messaging broker before "
						+ "its answer is waited for. The first call to a broker opens that connection, and "
						+ "creating it and connecting can take longer than an answer may (#286); it is "
						+ "waited for on its own, so the reply timeout stays the time an answer may take.")
		int connect_timeout_seconds() default 30;

		@AttributeDefinition(name = "MQTT version", required = false,
				description = "Which MQTT this connection speaks to an MQTT broker: 3 (MQTT 3.1.1, the "
						+ "default) or 5. A broker carries messages between the two, so nodes need not "
						+ "agree; 5 adds the broker's flow control (receiveMaximum) on this connection. "
						+ "Needs the MQTT 5 binding in the launch. Means nothing with an amqp:// URL.")
		int mqtt_version() default 3;

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

		@AttributeDefinition(name = "Largest answer", required = false,
				description = "How many bytes the document of an answer may have. Zero leaves the default "
						+ "of one megabyte per message; a larger value also raises what this node's "
						+ "connections accept and send. The messaging broker has a limit of its own.")
		long max_body_bytes() default 0;

		@AttributeDefinition(name = "Broadcast buffer", required = false,
				description = "How many values of a stream every receiver shares (#252) this node holds "
						+ "before the contract's overflow policy decides: wait, drop the oldest, or fail. "
						+ "Zero or less leaves the messaging instance's own size.")
		int broadcast_buffer() default 256;
	}

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	/**
	 * Whoever is watching calls, if anyone is (#126). Optional and
	 * dynamic: a deployment without telemetry installs nothing and this
	 * invoker notices nothing.
	 */
	private final AtomicReference<CallTracer> tracer = new AtomicReference<>();

	@Reference(name = "tracer", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	void setTracer(CallTracer tracer) {
		this.tracer.set(tracer);
	}

	void unsetTracer(CallTracer tracer) {
		// Only the one that is bound: a replacement is set before the one
		// it replaces is unset.
		this.tracer.compareAndSet(tracer, null);
	}

	private CallTracer tracer() {
		CallTracer bound = tracer.get();
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

	/** One call, written and addressed: what both shapes of a call start from. */
	private record Prepared(MessagingRoute route, MessagingRoute.Operation placed, ServiceOperation operation,
			CloudEvent request, byte[] document, String replyTopic) {
	}

	private Prepared prepare(ServiceLocator locator, String operationName, Map<String, Object> args) {
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
			if (placed.namesReplyChannel()) {
				// The reply channel has to be known before the call exists,
				// because the call carries it — so the envelope is built
				// first and its own id names the channel it answers on.
				replyTopic = route.replyChannel(placed, consumer, request.getEventId());
				request.getExtensions().put(CloudEvents.EXTENSION_REPLY_TO, replyTopic);
			}
		} catch (Exception unwritable) {
			throw new DdsrException("could not write the call to " + operationName + ": "
					+ unwritable.getMessage(), unwritable);
		}
		return new Prepared(route, placed, operation, request, document, replyTopic);
	}

	@Override
	public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
		Prepared prepared = prepare(locator, operationName, args);
		MessagingRoute route = prepared.route();
		MessagingRoute.Operation placed = prepared.placed();
		ServiceOperation operation = prepared.operation();
		CloudEvent request = prepared.request();
		byte[] document = prepared.document();
		String replyTopic = prepared.replyTopic();

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
				CloudEventCodec.Message call = new CloudEventCodec.Message(request, document);
				CloudEventCodec.Message answer = placed.namesReplyChannel()
						? viaInbox(link, route, placed, call, replyTopic, timeout)
						: link.request(placed.channel(), call, placed.qos(), timeout);
				return valueOf(answer, request, operation, operationName);
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

	/**
	 * The same call without a thread waiting for it (osgi.async).
	 *
	 * <p>Over a messaging broker a call is two messages, and the second
	 * is delivered: nothing has to wait for it. The connection is told
	 * when it is there, the request is published, and the stage
	 * completes on the thread that brings the answer. {@code blocking}
	 * is never used — that is the point.
	 *
	 * <p>The span covers handing the call over, not the answer: a scope
	 * is closed on the thread it was opened on. What travels in the
	 * envelope is the trace context of that span all the same, so the
	 * serving side continues the caller's trace.
	 */
	@Override
	public CompletionStage<Object> invokeAsync(ServiceLocator locator, String operationName,
			Map<String, Object> args, Executor blocking) {
		Prepared prepared;
		try {
			prepared = prepare(locator, operationName, args);
		} catch (RuntimeException unpreparable) {
			return CompletableFuture.failedFuture(unpreparable);
		}
		MessagingRoute.Operation placed = prepared.placed();
		CloudEvent request = prepared.request();
		long timeout = TimeUnit.SECONDS.toMillis(config.reply_timeout_seconds());
		try (CallSpan span = tracer().calling(spanNameOf(prepared.operation(), operationName),
				TraceCarrier.over(request.getExtensions().map()))) {
			span.attribute("rpc.system", "fennec.services")
					.attribute("rpc.method", operationName)
					.attribute("server.address", placed.channel())
					.attribute("fennec.flavor", prepared.route().protocol())
					.attribute(ClientOrigin.ATTRIBUTE, source);
		}
		CloudEventCodec.Message call = new CloudEventCodec.Message(request, prepared.document());
		return linkLater(prepared.route())
				.thenCompose(link -> placed.namesReplyChannel()
						? viaInboxLater(link, prepared, call, timeout)
						: link.requestLater(placed.channel(), call, placed.qos(), timeout))
				.handle((answer, failure) -> {
					if (failure != null) {
						throw failed(unwrapped(failure), operationName, prepared);
					}
					try {
						return valueOf(answer, request, prepared.operation(), operationName);
					} catch (DdsrException alreadyOurs) {
						throw alreadyOurs;
					} catch (Exception unreadable) {
						throw failed(unreadable, operationName, prepared);
					}
				});
	}

	/**
	 * A call answered with a stream ({@code Delivery.STREAM}), in a duplex
	 * session of the messaging library.
	 *
	 * <p>The session is opened when the stream has its subscriber, and
	 * each of its messages is one value. A message holding a Diagnostic
	 * is the failure the stream ended with — a declared error is
	 * recognised by its code as in a single answer. A subscriber that
	 * cancels closes the session, and the provider's stream stops; one
	 * that goes away without a word is ended by the provider when the
	 * session's lease runs out. What is not taken yet waits in the
	 * session's buffer, and the broker's window holds back the rest.
	 */
	/** A session of the messaging library carries a stream, over MQTT and AMQP alike. */
	@Override
	public boolean carriesStreams() {
		return true;
	}

	@Override
	public Flow.Publisher<Object> invokeStream(ServiceLocator locator, String operationName,
			Map<String, Object> args) {
		return subscriber -> {
			Prepared prepared;
			try {
				prepared = prepare(locator, operationName, args);
			} catch (RuntimeException unpreparable) {
				Flows.<Object>failed(unpreparable).subscribe(subscriber);
				return;
			}
			MessagingRoute.Operation placed = prepared.placed();
			if (prepared.operation().getReturnValue() != null
					&& prepared.operation().getReturnValue().getDelivery() == Delivery.BROADCAST) {
				receive(prepared, operationName, subscriber);
				return;
			}
			CloudEvent request = prepared.request();
			long timeout = TimeUnit.SECONDS.toMillis(config.reply_timeout_seconds());
			try (CallSpan span = tracer().calling(spanNameOf(prepared.operation(), operationName),
					TraceCarrier.over(request.getExtensions().map()))) {
				span.attribute("rpc.system", "fennec.services")
						.attribute("rpc.method", operationName)
						.attribute("server.address", placed.channel())
						.attribute("fennec.flavor", prepared.route().protocol())
						.attribute(ClientOrigin.ATTRIBUTE, source);
			}
			if (placed.namesReplyChannel()) {
				// The session's own channel in this consumer's subtree, a
				// level below the answers to calls answered once.
				request.getExtensions().put(CloudEvents.EXTENSION_REPLY_TO,
						prepared.route().sessionReplyChannel(placed, consumer, request.getEventId()));
			}
			CloudEventCodec.Message call = new CloudEventCodec.Message(request, prepared.document());
			linkLater(prepared.route()).whenComplete((link, unreachable) -> {
				Flow.Publisher<CloudEventCodec.Message> answers = unreachable != null
						? Flows.failed(unwrapped(unreachable))
						: link.openSession(placed.channel(), call, placed.qos(),
								prepared.route().sessionChannelPrefix(placed), timeout);
				Flow.Publisher<Object> values = Flows.map(answers, answer -> {
					try {
						return valueOf(answer, request, prepared.operation(), operationName);
					} catch (RuntimeException alreadyTelling) {
						throw alreadyTelling;
					} catch (Exception unreadable) {
						throw failed(unreadable, operationName, prepared);
					}
				});
				Flows.mapFailure(values, failure -> failed(unwrapped(failure), operationName, prepared))
						.subscribe(subscriber);
			});
		};
	}

	/**
	 * A stream every receiver shares (#252): nothing is sent, the channel
	 * of the operation is subscribed to through a buffer of this node,
	 * whose policy is the contract's — what this receiver may lose.
	 */
	private void receive(Prepared prepared, String operationName, Flow.Subscriber<? super Object> subscriber) {
		MessagingRoute.Operation placed = prepared.placed();
		ServiceOperation operation = prepared.operation();
		String channel = placed.channel();
		String describe = "the broadcast of " + operationName + " on " + channel;
		MessagingLink.Buffer buffer = new MessagingLink.Buffer(config.broadcast_buffer(),
				operation.getReturnValue().getOverflow().getName());
		BroadcastReceiver receiver = new BroadcastReceiver(subscriber, message -> {
			if (message.data() == null) {
				throw new DdsrException("a value of " + describe + " carried no document");
			}
			XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(message.data()), resourceSets,
					message.attributes().getDatacontenttype(), config.max_body_bytes());
			for (EObject root : bundle.roots()) {
				if (root instanceof ServiceInvocationResult result) {
					if (result.getDiagnostic() != null) {
						throw failureOf(operation, result.getDiagnostic(), operationName);
					}
					return Invocations.valueOf(result);
				}
			}
			throw new DdsrException("a value of " + describe + " carried no result");
		}, describe);
		linkLater(prepared.route()).whenComplete((link, unreachable) -> receiver.start(opening -> {
			if (unreachable != null) {
				throw failed(unwrapped(unreachable), operationName, prepared);
			}
			return link.subscribe(channel, placed.qos(), buffer, (topic, message) -> opening.received(message),
					opening::overflowed);
		}));
	}

	/** What a failure on the way is to the caller — the same words {@link #invoke} throws. */
	private DdsrException failed(Throwable failure, String operationName, Prepared prepared) {
		if (failure instanceof DdsrException alreadyOurs) {
			return alreadyOurs;
		}
		if (failure instanceof TimeoutException) {
			return DdsrException.transport("no answer to " + operationName + " on "
					+ prepared.placed().channel() + " within " + config.reply_timeout_seconds() + "s", failure);
		}
		return DdsrException.transport("calling " + operationName + " over " + prepared.route().protocol()
				+ " failed: " + failure.getMessage(), failure);
	}

	private static Throwable unwrapped(Throwable failure) {
		Throwable current = failure;
		while (current instanceof CompletionException && current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}

	/** The flavor names a place for answers: be told in this consumer's inbox there. */
	private CompletionStage<CloudEventCodec.Message> viaInboxLater(MessagingLink link, Prepared prepared,
			CloudEventCodec.Message call, long timeout) {
		MessagingRoute.Operation placed = prepared.placed();
		String id = prepared.request().getEventId();
		ReplyInbox inbox;
		try {
			inbox = inboxFor(link, prepared.route().replySubtree(placed, consumer),
					prepared.route().replyFilter(placed, consumer), placed.qos());
		} catch (RuntimeException unsubscribable) {
			return CompletableFuture.failedFuture(unsubscribable);
		}
		CompletableFuture<CloudEventCodec.Message> answer = inbox.expect(id);
		// Never retained: a call is a transition. A request the broker
		// did not take has no answer to wait for.
		link.publishLater(placed.channel(), call, placed.qos()).whenComplete((sent, unsent) -> {
			if (unsent != null) {
				answer.completeExceptionally(unsent);
			}
		});
		// The subscription stays: it is this consumer's inbox, not this
		// call's. What goes is the expectation of an answer.
		return answer.orTimeout(timeout, TimeUnit.MILLISECONDS).whenComplete((arrived, failure) -> inbox.forget(id));
	}

	/**
	 * The link to the broker this service announces, told when it is
	 * connected — {@link #linkFor} without the waiting.
	 */
	private CompletionStage<MessagingLink> linkLater(MessagingRoute route) {
		String url = route.broker();
		if (url == null) {
			return CompletableFuture.failedFuture(new DdsrException("the " + route.protocol()
					+ " flavor of this service announces no broker to call it on"));
		}
		String key = linkKey(route);
		MessagingLink link;
		try {
			synchronized (links) {
				link = links.get(key);
				if (link == null) {
					link = open(route);
					links.put(key, link);
				}
			}
		} catch (RuntimeException unreachable) {
			return CompletableFuture.failedFuture(DdsrException.transport("cannot reach the broker at " + url
					+ ": " + unreachable.getMessage(), unreachable));
		}
		MessagingLink opened = link;
		return opened.whenConnected(config.connect_timeout_seconds(), TimeUnit.SECONDS).handle((ready, failure) -> {
			if (failure == null) {
				return opened;
			}
			// A broker that was never there: its link is taken away again,
			// as the waiting shape does.
			if (!opened.hasBeenConnected()) {
				synchronized (links) {
					links.remove(key, opened);
				}
				opened.close();
			}
			throw DdsrException.transport("cannot reach the broker at " + url + " within "
					+ config.connect_timeout_seconds() + "s", null);
		});
	}

	private static String linkKey(MessagingRoute route) {
		return route.broker() + " " + (route.exchange() == null ? "" : route.exchange());
	}

	private MessagingLink open(MessagingRoute route) {
		String id = UUID.randomUUID().toString().substring(0, 8);
		return MessagingLink.open(context,
				new LinkSettings("fennec.services.invoker." + id, route.broker(), "ddsr-consumer-" + id,
						config.tls_keystore(), config.tls_keystore_password(),
						config.tls_truststore(), config.tls_truststore_password())
						.onExchange(route.exchange()).as(config.username(), config.password())
						.carrying(config.max_body_bytes()).speaking(config.mqtt_version()));
	}

	/** The flavor names a place for answers: wait in this consumer's inbox there for the one that correlates. */
	private CloudEventCodec.Message viaInbox(MessagingLink link, MessagingRoute route,
			MessagingRoute.Operation placed, CloudEventCodec.Message call, String replyTopic, long timeout)
			throws Exception {
		CloudEvent request = call.attributes();
		ReplyInbox inbox = inboxFor(link, route.replySubtree(placed, consumer),
				route.replyFilter(placed, consumer), placed.qos());
		inbox.expect(request.getEventId());
		try {
			// Never retained: a call is a transition.
			link.publish(placed.channel(), call, placed.qos(), timeout);
			CloudEventCodec.Message answer = inbox.await(request.getEventId(), timeout, TimeUnit.MILLISECONDS);
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
	private ReplyInbox inboxFor(MessagingLink link, String subtree, String filter, int qos) {
		synchronized (inboxes) {
			// Per broker, because a subtree of the same name on another
			// broker is another subscription.
			String key = link.url() + " " + subtree;
			ReplyInbox inbox = inboxes.get(key);
			if (inbox != null) {
				return inbox;
			}
			ReplyInbox fresh = new ReplyInbox(subtree, filter);
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

	/**
	 * What a failing answer is to the caller.
	 *
	 * <p>A messaging broker has no status to bind an error to, and needs
	 * none: the answer carries the Diagnostic, and its code is the
	 * declared error it is ({@link DeclaredErrors}). Without a declared
	 * error of that code it is a failure nobody announced.
	 */
	static DdsrException failureOf(ServiceOperation operation, Diagnostic diagnostic, String operationName) {
		Optional<ServiceException> declared = DeclaredErrors.byCode(operation, diagnostic.getCode());
		if (declared.isPresent()) {
			return new DdsrServiceException(diagnostic.getMessage() == null
					? declared.get().getName() : diagnostic.getMessage(), declared.get(), diagnostic);
		}
		return new DdsrException(operationName + " failed: " + diagnostic.getMessage());
	}

	/** What the answer says, or why there is none. */
	private Object valueOf(CloudEventCodec.Message message, CloudEvent request, ServiceOperation operation,
			String operationName) throws Exception {
		if (!CloudEvents.TYPE_INVOKE_REPLY.equals(message.attributes().getType())) {
			throw new DdsrException("the answer to " + operationName + " is a '" + message.attributes().getType()
					+ "' and not an answer");
		}
		if (message.data() == null) {
			throw new DdsrException("the answer to " + operationName + " carried no document");
		}
		String correlation = MessagingRoute.correlationOf(message.attributes());
		if (correlation != null && !correlation.equals(request.getEventId())) {
			// Matched by the inbox or by the protocol, so this should not
			// happen — which is exactly why it is worth saying when it does.
			LOG.warning("[DDSR-MSG] the answer to " + request.getEventId() + " says it correlates with "
					+ correlation);
		}
		XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(message.data()), resourceSets,
				message.attributes().getDatacontenttype(), config.max_body_bytes());
		for (EObject root : bundle.roots()) {
			if (root instanceof ServiceInvocationResult result) {
				if (result.getDiagnostic() != null) {
					throw failureOf(operation, result.getDiagnostic(), operationName);
				}
				return Invocations.valueOf(result);
			}
		}
		throw new DdsrException("the answer to " + operationName + " carried no result");
	}

	/**
	 * The link to the broker this service announces, connected.
	 *
	 * <p>Waited for with the connect timeout, not the reply timeout: the
	 * first call pays for creating the link and connecting it, and that
	 * may take longer than an answer may (#286).
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
		String key = linkKey(route);
		synchronized (links) {
			MessagingLink link = links.get(key);
			try {
				if (link == null) {
					link = open(route);
					links.put(key, link);
				}
				if (link.awaitConnected(config.connect_timeout_seconds(), TimeUnit.SECONDS)) {
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
					+ config.connect_timeout_seconds() + "s", null);
		}
	}

	private byte[] write(List<EObject> roots, String contentType) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, resourceSets, contentType, roots);
		return out.toByteArray();
	}
}
