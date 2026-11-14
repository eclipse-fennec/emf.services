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

package org.eclipse.fennec.services.provider.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.Delivery;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.common.DeclaredErrors;
import org.eclipse.fennec.services.flavor.messaging.MessagingRoute;
import org.eclipse.fennec.services.invocation.AsyncReturns;
import org.eclipse.fennec.services.invocation.Flows;
import org.eclipse.fennec.services.invocation.Invocations;
import org.eclipse.fennec.services.invocation.PushStreams;
import org.eclipse.fennec.services.messaging.MessagingLink;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;

import io.cloudevents.model.ce.CloudEvent;

/**
 * The provider side of a call that arrives as a message: which
 * operation, which arguments, what to answer — decided in the terms the
 * model is written in, the same way {@code RestDispatcher} decides them
 * for HTTP.
 *
 * <p>One dispatcher for every flavor that travels over messaging. What
 * a channel is called and what a call is encoded in it reads from the
 * flavor's {@link MessagingRoute}; what it does with a call is the same
 * on every protocol — and so is where the answer goes: the connection
 * hands in a {@link Reply} that knows the address and the correlation
 * the request carried, whether those travelled in properties of the
 * protocol or in the envelope.
 *
 * <p>It knows no client and no wire. What arrives is an envelope and
 * the document beside it, already read; what goes back is the same
 * pair. Subscribing, publishing and what an event looks like in bytes
 * are the connection's business, which is what lets the whole decision
 * be exercised without a broker.
 *
 * <p>A failure is an answer. A handler that throws, an argument that
 * will not decode, a method that is not there — each comes back as a
 * {@code Diagnostic}, because the alternative is a consumer waiting out
 * its timeout to learn nothing.
 */
public final class MessagingOperationDispatcher {

	private static final Logger LOG = Logger.getLogger(MessagingOperationDispatcher.class.getName());

	/** The way back to the caller of one request. */
	public interface Reply {

		void send(CloudEventCodec.Message answer) throws Exception;

	}

	private final MessagingRoute route;

	private final Supplier<Object> service;

	private final String source;

	private final ComponentServiceObjects<ResourceSet> resourceSets;

	private final CallTracer tracer;

	private final long maxBodyBytes;

	public MessagingOperationDispatcher(ServiceFlavor flavor, Supplier<Object> service, String source,
			ComponentServiceObjects<ResourceSet> resourceSets) {
		this(flavor, service, source, resourceSets, CallTracer.NONE);
	}

	/**
	 * @param tracer whoever is watching calls (#126). A dispatcher built
	 *        without one is built with {@link CallTracer#NONE}: serving a
	 *        contract over channels has never required telemetry.
	 */
	public MessagingOperationDispatcher(ServiceFlavor flavor, Supplier<Object> service, String source,
			ComponentServiceObjects<ResourceSet> resourceSets, CallTracer tracer) {
		this(flavor, service, source, resourceSets, tracer, 0);
	}

	/**
	 * @param maxBodyBytes how large the document of a call may be (#219);
	 *        zero or less leaves the codec's megabyte
	 */
	public MessagingOperationDispatcher(ServiceFlavor flavor, Supplier<Object> service, String source,
			ComponentServiceObjects<ResourceSet> resourceSets, CallTracer tracer, long maxBodyBytes) {
		this.maxBodyBytes = maxBodyBytes;
		this.tracer = tracer == null ? CallTracer.NONE : tracer;
		this.route = MessagingRoute.of(flavor);
		this.service = service;
		this.source = source;
		this.resourceSets = resourceSets;
	}

	/** The route this dispatcher answers on. */
	public MessagingRoute route() {
		return route;
	}

	/** The channels this contract answers on, one per operation. */
	public List<String> channels() {
		return route.operations().stream().map(MessagingRoute.Operation::channel).toList();
	}

	/** The QoS one of those channels is subscribed with. */
	public int qosFor(String channel) {
		MessagingRoute.Operation placed = route.onChannel(channel);
		return placed == null ? 1 : placed.qos();
	}

	/**
	 * One call. Never throws: a dispatcher that let a failure escape
	 * would take down the subscription that every other caller of this
	 * contract shares.
	 */
	public void onRequest(String channel, CloudEventCodec.Message call, Reply reply) {
		MessagingRoute.Operation placed = route.onChannel(channel);
		if (placed == null) {
			LOG.fine(() -> "[DDSR-MSG] nothing of this contract answers on " + channel);
			return;
		}
		CloudEvent request = call.attributes();
		ServiceInvocation invocation;
		try {
			if (!CloudEvents.TYPE_INVOKE.equals(request.getType())) {
				throw new IllegalArgumentException("a '" + request.getType() + "' is not a call");
			}
			if (call.data() == null) {
				throw new IllegalArgumentException("a call without its document says nothing");
			}
			invocation = invocationOf(call.data(), request.getDatacontenttype());
		} catch (Exception unreadable) {
			// Not answered: what this side cannot read as a call is not
			// one it can say anything true about.
			LOG.warning("[DDSR-MSG] unreadable call on " + channel + " dropped: " + unreadable);
			return;
		}

		// The caller's trace, continued here (#126). It travelled in the
		// envelope's extensions, which is where CloudEvents puts it and
		// the one place every protocol here can carry it.
		try (CallSpan span = tracer.serving(spanNameOf(placed),
				TraceCarrier.over(request.getExtensions().map()))) {
			span.attribute("rpc.system", "fennec.services")
					.attribute("server.address", channel)
					.attribute("fennec.flavor", route.protocol())
					// Who called (#125). Over messaging there is no origin
					// header; the envelope's source is where that identity
					// travels, and it is the same one the REST side reads
					// off a header.
					.attribute(ClientOrigin.ATTRIBUTE,
							request.getSource() == null ? ClientOrigin.ANONYMOUS : request.getSource());
			ServiceInvocationResult result;
			try {
				Object value = invoke(placed, invocation);
				if (PushStreams.asPublisher(value) != null) {
					// A stream travels in a session (onSession), opened for an
					// operation whose contract says STREAM; one answer cannot
					// carry it.
					throw new IllegalStateException("the service answered " + spanNameOf(placed)
							+ " with a stream its contract does not declare");
				}
				if (AsyncReturns.isPending(value)) {
					// A service that answers later (osgi.async). The answer is
					// sent when it has one, from whichever thread settles it:
					// this one goes back to the subscription, so a slow call
					// no longer holds up every call behind it. The span ends
					// with the dispatch — a scope is closed on the thread it
					// was opened on, and that is this one.
					AsyncReturns.whenDone(value, (settled, failure) -> answer(request, channel, placed,
							failure != null ? thrown(placed.operation(), failure) : resultOf(settled), reply));
					return;
				}
				result = resultOf(value);
			} catch (InvocationTargetException failed) {
				span.failed(failed.getCause());
				result = thrown(placed.operation(), failed.getCause());
			} catch (Exception failed) {
				span.failed(failed);
				result = Invocations.failure(String.valueOf(failed), 0);
			}
			answer(request, channel, placed, result, reply);
		}
	}

	/**
	 * What to call this call in a trace: the contract and the operation,
	 * which is what a reader groups by — never the channel, which
	 * carries the same two facts in a shape nobody outside this project
	 * reads.
	 */
	private static String spanNameOf(MessagingRoute.Operation placed) {
		ServiceOperation operation = placed.operation();
		if (operation == null) {
			return placed.flavor().getName();
		}
		if (operation.eContainer() instanceof ServiceInterface contract && contract.getName() != null) {
			return contract.getName() + "/" + operation.getName();
		}
		return operation.getName();
	}

	/**
	 * What a returned value answers with. A failure that came back as a
	 * value rather than as a throw is still a failure: the answer carries
	 * it as its Diagnostic, code and all, and not as a value that happens
	 * to be one.
	 */
	private static ServiceInvocationResult resultOf(Object value) {
		return DeclaredErrors.isFailure(value) ? Invocations.failure((Diagnostic) value)
				: Invocations.result(value);
	}

	/**
	 * What a thrown failure answers with. An error the contract declares
	 * travels under its code — over a messaging broker that code is all
	 * a declared error is, and the caller recognises it by it
	 * ({@link DeclaredErrors}). Anything else has no code: the provider
	 * failed, and saying more would be inventing it.
	 */
	private static ServiceInvocationResult thrown(ServiceOperation operation, Throwable failure) {
		Optional<ServiceException> declared = DeclaredErrors.raisedBy(operation, failure);
		if (declared.isEmpty()) {
			return Invocations.failure(String.valueOf(failure), 0);
		}
		String message = failure.getMessage() == null ? declared.get().getName() : failure.getMessage();
		return Invocations.failure(message, DeclaredErrors.codeOf(declared.get()).orElse(0));
	}

	private Object invoke(MessagingRoute.Operation placed, ServiceInvocation invocation)
			throws ReflectiveOperationException {
		ServiceOperation operation = placed.operation();
		if (operation == null) {
			throw new NoSuchMethodException("the flavor for this channel binds no operation");
		}
		Object target = service.get();
		if (target == null) {
			throw new IllegalStateException("no service implements this contract here");
		}
		Map<String, Object> arguments = Invocations.argumentsOf(invocation);
		List<Object> values = new ArrayList<>();
		for (Parameter parameter : operation.getParameters()) {
			values.add(arguments.get(parameter.getName()));
		}
		for (Method method : target.getClass().getMethods()) {
			if (method.getName().equals(operation.getName()) && method.getParameterCount() == values.size()) {
				return method.invoke(target, values.toArray());
			}
		}
		throw new NoSuchMethodException("the service behind this contract, a "
				+ target.getClass().getName() + ", has no method '" + operation.getName()
				+ "' taking " + values.size() + " argument(s)");
	}

	private void answer(CloudEvent request, String channel, MessagingRoute.Operation placed,
			ServiceInvocationResult result, Reply reply) {
		String contentType = placed.produces();
		try {
			byte[] document = write(List.of(result), contentType);
			reply.send(new CloudEventCodec.Message(
					CloudEvents.describing(MessagingRoute.reply(request, source, contentType), result),
					document));
		} catch (Exception unanswerable) {
			// The last resort: say so in the encoding this side always
			// has. A Diagnostic is not the operation's declared result,
			// so `produces` does not describe it — and a caller that
			// learns why beats one that waits out its timer.
			LOG.log(Level.WARNING, "[DDSR-MSG] could not answer a call on " + channel, unanswerable);
			try {
				byte[] document = write(List.of(Invocations.failure(String.valueOf(unanswerable), 0)),
						MessagingRoute.DEFAULT_CONTENT_TYPE);
				reply.send(new CloudEventCodec.Message(
						MessagingRoute.reply(request, source, MessagingRoute.DEFAULT_CONTENT_TYPE), document));
			} catch (Exception silence) {
				LOG.warning("[DDSR-MSG] and could not say why either: " + silence);
			}
		}
	}

	/** Where the provider listens for the messages of the sessions opened on this channel. */
	public String sessionChannelPrefix(String channel) {
		MessagingRoute.Operation placed = route.onChannel(channel);
		if (placed == null) {
			throw new IllegalArgumentException("nothing of this contract answers on " + channel);
		}
		return route.sessionChannelPrefix(placed);
	}

	/** Whether the operation on this channel answers with a stream, and is served in sessions. */
	public boolean streams(String channel) {
		MessagingRoute.Operation placed = route.onChannel(channel);
		return placed != null && placed.operation() != null && placed.operation().getReturnValue() != null
				&& placed.operation().getReturnValue().getDelivery() == Delivery.STREAM;
	}

	/** Whether the operation on this channel is a stream shared by every receiver (#252). */
	public boolean broadcasts(String channel) {
		MessagingRoute.Operation placed = route.onChannel(channel);
		return placed != null && placed.operation() != null && placed.operation().getReturnValue() != null
				&& placed.operation().getReturnValue().getDelivery() == Delivery.BROADCAST;
	}

	/**
	 * The stream every receiver of the operation on this channel shares
	 * (#252): the service is called once, without arguments, and every
	 * value becomes a message to publish on the channel — a
	 * {@code ServiceInvocationResult} under
	 * {@link CloudEvents#TYPE_BROADCAST}. A failure, thrown by the call or
	 * ending the stream, ends it with a {@link BroadcastFailed} carrying
	 * the last message, the one holding the Diagnostic; when it completes,
	 * {@link #broadcastEnd} is what to send.
	 *
	 * <p>Never throws, for the reason {@link #onRequest} does not.
	 */
	public Flow.Publisher<CloudEventCodec.Message> broadcast(String channel) {
		MessagingRoute.Operation placed = route.onChannel(channel);
		if (placed == null || placed.operation() == null) {
			return Flows.failed(new IllegalArgumentException("nothing of this contract broadcasts on " + channel));
		}
		ServiceOperation operation = placed.operation();
		Flow.Publisher<Object> values;
		try {
			Object target = service.get();
			if (target == null) {
				throw new IllegalStateException("no service implements this contract here");
			}
			Method method = null;
			for (Method candidate : target.getClass().getMethods()) {
				if (candidate.getName().equals(operation.getName()) && candidate.getParameterCount() == 0) {
					method = candidate;
				}
			}
			if (method == null) {
				throw new NoSuchMethodException("the service behind this contract, a " + target.getClass().getName()
						+ ", has no method '" + operation.getName() + "' without arguments");
			}
			values = PushStreams.asPublisher(method.invoke(target));
			if (values == null) {
				throw new IllegalStateException("the service answered " + spanNameOf(placed)
						+ " with no stream, although its contract says it broadcasts");
			}
		} catch (InvocationTargetException failed) {
			values = Flows.failed(failed.getCause());
		} catch (Exception failed) {
			values = Flows.failed(failed);
		}
		String contentType = placed.produces();
		Flow.Publisher<CloudEventCodec.Message> messages = Flows.map(values, value -> {
			try {
				return broadcastMessage(resultOf(value), contentType);
			} catch (Exception unwritable) {
				throw new IllegalStateException("a value of the broadcast could not be written: "
						+ unwritable.getMessage(), unwritable);
			}
		});
		Flow.Publisher<CloudEventCodec.Message> failing = Flows.mapFailure(messages, failure -> {
			LOG.log(Level.WARNING, "[DDSR-MSG] the broadcast on " + channel + " ended with a failure", failure);
			try {
				return new BroadcastFailed(
						broadcastMessage(thrown(operation, failure), MessagingRoute.DEFAULT_CONTENT_TYPE));
			} catch (Exception unsayable) {
				return failure;
			}
		});
		return failing;
	}

	/** The message that says a broadcast is over, sent when its stream completes. */
	public CloudEventCodec.Message broadcastEnd() {
		return new CloudEventCodec.Message(CloudEvents.newEnvelope(CloudEvents.TYPE_BROADCAST_END, source, null),
				null);
	}

	/**
	 * The last message of a broadcast that failed, standing in for the
	 * failure: a publisher ends with it, and the one who publishes sends it.
	 */
	public static final class BroadcastFailed extends RuntimeException {

		private static final long serialVersionUID = 1L;

		private final transient CloudEventCodec.Message last;

		BroadcastFailed(CloudEventCodec.Message last) {
			super("the broadcast failed", null, false, false);
			this.last = last;
		}

		public CloudEventCodec.Message last() {
			return last;
		}
	}

	private CloudEventCodec.Message broadcastMessage(ServiceInvocationResult result, String contentType)
			throws Exception {
		return new CloudEventCodec.Message(
				CloudEvents.describing(CloudEvents.newEnvelope(CloudEvents.TYPE_BROADCAST, source, contentType), result),
				write(List.of(result), contentType));
	}

	/**
	 * A session opened for an operation that answers with a stream
	 * ({@code Delivery.STREAM}): the messages to send in it.
	 *
	 * <p>One message per value, each a {@code ServiceInvocationResult} in
	 * the encoding the flavor declares, asked for when the connection has
	 * taken the one before. A failure — thrown by the call or ending the
	 * stream — ends the stream with a {@link MessagingLink.SessionEnd}: one
	 * last message holding the Diagnostic a single answer would have
	 * carried, a declared error under its code, and the reason the session
	 * ends with ({@code declared:<code>} or {@code failed}). When the
	 * session ends first —
	 * the caller closed it or went silent for its lease — the stream is
	 * cancelled, and with it the service's.
	 *
	 * <p>Never throws, for the reason {@link #onRequest} does not.
	 */
	public Flow.Publisher<CloudEventCodec.Message> onSession(String channel, CloudEventCodec.Message call) {
		CloudEvent request = call.attributes();
		MessagingRoute.Operation placed = route.onChannel(channel);
		ServiceInvocation invocation;
		try {
			if (placed == null) {
				throw new IllegalArgumentException("nothing of this contract answers on " + channel);
			}
			if (!CloudEvents.TYPE_INVOKE.equals(request.getType())) {
				throw new IllegalArgumentException("a '" + request.getType() + "' is not a call");
			}
			if (call.data() == null) {
				throw new IllegalArgumentException("a call without its document says nothing");
			}
			invocation = invocationOf(call.data(), request.getDatacontenttype());
		} catch (Exception unreadable) {
			LOG.warning("[DDSR-MSG] unreadable call opening a session on " + channel + ": " + unreadable);
			return Flows.failed(unreadable);
		}
		ServiceOperation operation = placed.operation();
		Flow.Publisher<Object> values;
		try (CallSpan span = tracer.serving(spanNameOf(placed), TraceCarrier.over(request.getExtensions().map()))) {
			span.attribute("rpc.system", "fennec.services")
					.attribute("server.address", channel)
					.attribute("fennec.flavor", route.protocol())
					.attribute(ClientOrigin.ATTRIBUTE,
							request.getSource() == null ? ClientOrigin.ANONYMOUS : request.getSource());
			try {
				Object value = invoke(placed, invocation);
				values = PushStreams.asPublisher(value);
				if (values == null) {
					throw new IllegalStateException("the service answered " + spanNameOf(placed)
							+ " with no stream, although its contract says it streams");
				}
			} catch (InvocationTargetException failed) {
				span.failed(failed.getCause());
				values = Flows.failed(failed.getCause());
			} catch (Exception failed) {
				span.failed(failed);
				values = Flows.failed(failed);
			}
		}
		String contentType = placed.produces();
		Flow.Publisher<CloudEventCodec.Message> answers = Flows.map(values, value -> {
			try {
				return message(request, resultOf(value), contentType);
			} catch (Exception unwritable) {
				throw new IllegalStateException("a value of the stream could not be written: " + unwritable.getMessage(),
						unwritable);
			}
		});
		return Flows.mapFailure(answers, failure -> {
			LOG.log(Level.FINE, "[DDSR-MSG] the stream answering a call on " + channel + " ended with a failure",
					failure);
			ServiceInvocationResult result = thrown(operation, failure);
			int code = result.getDiagnostic().getCode();
			try {
				// The Diagnostic as the session's last message, and the reason
				// that ends it: which declared error, or that it failed.
				return new MessagingLink.SessionEnd(message(request, result, MessagingRoute.DEFAULT_CONTENT_TYPE),
						code != 0 ? "declared:" + code : "failed");
			} catch (Exception unsayable) {
				return failure;
			}
		});
	}

	/** One answer: the result in the encoding given, the envelope saying what it is. */
	private CloudEventCodec.Message message(CloudEvent request, ServiceInvocationResult result, String contentType)
			throws Exception {
		return new CloudEventCodec.Message(
				CloudEvents.describing(MessagingRoute.reply(request, source, contentType), result),
				write(List.of(result), contentType));
	}

	private ServiceInvocation invocationOf(byte[] document, String contentType) throws Exception {
		XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(document), resourceSets,
				contentType == null ? MessagingRoute.DEFAULT_CONTENT_TYPE : contentType, maxBodyBytes);
		for (EObject root : bundle.roots()) {
			if (root instanceof ServiceInvocation invocation) {
				return invocation;
			}
		}
		throw new IllegalArgumentException("the message carries no ServiceInvocation");
	}

	private byte[] write(List<EObject> roots, String contentType) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, resourceSets, contentType, roots);
		return out.toByteArray();
	}
}
