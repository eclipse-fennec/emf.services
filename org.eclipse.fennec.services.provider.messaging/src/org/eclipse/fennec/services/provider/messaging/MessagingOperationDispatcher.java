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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.flavor.messaging.MessagingRoute;
import org.eclipse.fennec.services.invocation.Invocations;
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
 * on every protocol. The one thing that is not is where the answer
 * goes, and the caller of this class says which it is by the method it
 * calls:
 * <ul>
 * <li>{@link #onMessage} — the way back is in the envelope (MQTT
 * 3.1.1): the call names a reply topic, and the answer is published
 * there;</li>
 * <li>{@link #onRequest} — the way back is the protocol's (AMQP): the
 * connection hands in a {@link Reply} that already knows the address
 * and the correlation.</li>
 * </ul>
 *
 * <p>It knows no client. Subscribing and publishing are the
 * component's business; what arrives and what goes back is this
 * class's, which is what lets the whole decision be exercised without a
 * broker.
 *
 * <p>A failure is an answer. A handler that throws, an argument that
 * will not decode, a method that is not there — each comes back as a
 * {@code Diagnostic}, because the alternative is a consumer waiting out
 * its timeout to learn nothing.
 */
public final class MessagingOperationDispatcher {

	private static final Logger LOG = Logger.getLogger(MessagingOperationDispatcher.class.getName());

	/** Where an answer goes when the call named a topic. The component supplies the connection. */
	public interface Publisher {

		void publish(String topic, byte[] payload, int qos) throws Exception;
	}

	/** The way back to the caller of one request, when the protocol carries it. */
	public interface Reply {

		void send(byte[] payload) throws Exception;
	}

	private final MessagingRoute route;

	private final Supplier<Object> service;

	private final String source;

	private final ComponentServiceObjects<ResourceSet> resourceSets;

	private final Publisher publisher;

	private final CallTracer tracer;

	public MessagingOperationDispatcher(ServiceFlavor flavor, Supplier<Object> service, String source,
			ComponentServiceObjects<ResourceSet> resourceSets, Publisher publisher) {
		this(flavor, service, source, resourceSets, publisher, CallTracer.NONE);
	}

	/**
	 * @param publisher where answers to calls that name a topic go; may
	 *        be null for a flavor whose protocol carries the reply
	 *        address
	 * @param tracer whoever is watching calls (#126). A dispatcher built
	 *        without one is built with {@link CallTracer#NONE}: serving a
	 *        contract over channels has never required telemetry.
	 */
	public MessagingOperationDispatcher(ServiceFlavor flavor, Supplier<Object> service, String source,
			ComponentServiceObjects<ResourceSet> resourceSets, Publisher publisher, CallTracer tracer) {
		this.tracer = tracer == null ? CallTracer.NONE : tracer;
		this.route = MessagingRoute.of(flavor);
		this.service = service;
		this.source = source;
		this.resourceSets = resourceSets;
		this.publisher = publisher;
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
	 * One call whose way back is in its envelope. Never throws: a
	 * dispatcher that let a failure escape would take down the
	 * subscription that every other caller of this contract shares.
	 */
	public void onMessage(String channel, byte[] payload) {
		dispatch(channel, payload, null);
	}

	/**
	 * One call whose way back the protocol carries. Never throws, for
	 * the same reason.
	 */
	public void onRequest(String channel, byte[] payload, Reply reply) {
		dispatch(channel, payload, reply);
	}

	private void dispatch(String channel, byte[] payload, Reply handedIn) {
		MessagingRoute.Operation placed = route.onChannel(channel);
		if (placed == null) {
			LOG.fine(() -> "[DDSR-MSG] nothing of this contract answers on " + channel);
			return;
		}
		CloudEvent request;
		ServiceInvocation invocation;
		try {
			var message = MessagingRoute.read(payload, CloudEvents.TYPE_INVOKE);
			request = message.attributes();
			invocation = invocationOf(message.data(), request.getDatacontenttype());
		} catch (Exception unreadable) {
			// There is nothing to answer to: a message this side cannot
			// read is one whose reply address it cannot trust either.
			LOG.warning("[DDSR-MSG] unreadable call on " + channel + " dropped: " + unreadable);
			return;
		}

		Reply reply = handedIn;
		if (reply == null) {
			String replyTo;
			try {
				replyTo = MessagingRoute.replyAddressOf(request);
			} catch (RuntimeException noAddress) {
				LOG.warning("[DDSR-MSG] a call on " + channel + " named no reply address: " + noAddress);
				return;
			}
			reply = answer -> publisher.publish(replyTo, answer, placed.qos());
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
				result = Invocations.result(invoke(placed, invocation));
			} catch (InvocationTargetException failed) {
				span.failed(failed.getCause());
				result = Invocations.failure(String.valueOf(failed.getCause()), 0);
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
			reply.send(MessagingRoute.write(MessagingRoute.reply(request, source, contentType), document));
		} catch (Exception unanswerable) {
			// The last resort: say so in the encoding this side always
			// has. A Diagnostic is not the operation's declared result,
			// so `produces` does not describe it — and a caller that
			// learns why beats one that waits out its timer.
			LOG.log(Level.WARNING, "[DDSR-MSG] could not answer a call on " + channel, unanswerable);
			try {
				byte[] document = write(List.of(Invocations.failure(String.valueOf(unanswerable), 0)),
						MessagingRoute.DEFAULT_CONTENT_TYPE);
				reply.send(MessagingRoute.write(
						MessagingRoute.reply(request, source, MessagingRoute.DEFAULT_CONTENT_TYPE), document));
			} catch (Exception silence) {
				LOG.warning("[DDSR-MSG] and could not say why either: " + silence);
			}
		}
	}

	private ServiceInvocation invocationOf(byte[] document, String contentType) throws Exception {
		XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(document), resourceSets,
				contentType == null ? MessagingRoute.DEFAULT_CONTENT_TYPE : contentType);
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
