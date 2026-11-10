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

package org.eclipse.fennec.services.provider.amqp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.AmqpFlavor;
import org.eclipse.fennec.services.AmqpOperationFlavor;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.flavor.amqp.AmqpFlavors;
import org.eclipse.fennec.services.flavor.amqp.AmqpMessages;
import org.eclipse.fennec.services.invocation.Invocations;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;

import io.cloudevents.model.ce.CloudEvent;

/**
 * Turns one AMQP message into one call and one answer — what
 * {@code MqttOperationDispatcher} is on its wire.
 *
 * <p>It knows no AMQP client. What arrives is a routing key and a
 * payload; where the answer goes is a {@link Reply} the connection
 * hands in, already bound to the address and the correlation the
 * request carried. That is the whole difference to the MQTT dispatcher,
 * which reads both out of the envelope.
 *
 * <p>A failure is an answer. A handler that throws, an argument that
 * will not decode, a method that is not there — each comes back as a
 * {@code Diagnostic}, because the alternative is a consumer waiting out
 * its timeout to learn nothing.
 */
public final class AmqpOperationDispatcher {

	private static final Logger LOG = Logger.getLogger(AmqpOperationDispatcher.class.getName());

	/** The way back to the caller of one request. */
	public interface Reply {

		void send(byte[] payload) throws Exception;
	}

	private final Supplier<Object> service;

	private final String source;

	private final ComponentServiceObjects<ResourceSet> resourceSets;

	private final CallTracer tracer;

	/** Which operation flavor answers under which routing key. */
	private final Map<String, AmqpOperationFlavor> byKey = new LinkedHashMap<>();

	public AmqpOperationDispatcher(AmqpFlavor flavor, Supplier<Object> service, String source,
			ComponentServiceObjects<ResourceSet> resourceSets, CallTracer tracer) {
		this.tracer = tracer == null ? CallTracer.NONE : tracer;
		this.service = service;
		this.source = source;
		this.resourceSets = resourceSets;
		for (ServiceOperationFlavor candidate : flavor.getOperationFlavors()) {
			if (candidate instanceof AmqpOperationFlavor operationFlavor) {
				byKey.put(AmqpFlavors.routingKey(flavor, operationFlavor), operationFlavor);
			}
		}
	}

	/** The routing keys this contract answers under, one per operation. */
	public List<String> routingKeys() {
		return List.copyOf(byKey.keySet());
	}

	/**
	 * One call. Never throws: a dispatcher that let a failure escape
	 * would take down the binding every other caller of this operation
	 * shares.
	 */
	public void onRequest(String routingKey, byte[] payload, Reply reply) {
		AmqpOperationFlavor operationFlavor = byKey.get(routingKey);
		if (operationFlavor == null) {
			LOG.fine(() -> "[DDSR-AMQP] nothing of this contract answers under " + routingKey);
			return;
		}
		CloudEvent request;
		ServiceInvocation invocation;
		try {
			var message = AmqpMessages.read(payload, CloudEvents.TYPE_INVOKE);
			request = message.attributes();
			invocation = invocationOf(message.data(), request.getDatacontenttype());
		} catch (Exception unreadable) {
			// Nothing to correlate an answer with in the envelope's terms,
			// and nothing this side understood to answer about.
			LOG.warning("[DDSR-AMQP] unreadable call under " + routingKey + " dropped: " + unreadable);
			return;
		}

		// The caller's trace, continued here (#126): it travels in the
		// envelope's extensions on every wire this project speaks.
		try (CallSpan span = tracer.serving(spanNameOf(operationFlavor),
				TraceCarrier.over(request.getExtensions().map()))) {
			span.attribute("rpc.system", "fennec.services")
					.attribute("server.address", routingKey)
					.attribute("fennec.flavor", "AMQP")
					.attribute(ClientOrigin.ATTRIBUTE,
							request.getSource() == null ? ClientOrigin.ANONYMOUS : request.getSource());
			ServiceInvocationResult result;
			try {
				result = Invocations.result(invoke(operationFlavor, invocation));
			} catch (InvocationTargetException failed) {
				span.failed(failed.getCause());
				result = Invocations.failure(String.valueOf(failed.getCause()), 0);
			} catch (Exception failed) {
				span.failed(failed);
				result = Invocations.failure(String.valueOf(failed), 0);
			}
			answer(request, routingKey, operationFlavor, result, reply);
		}
	}

	private static String spanNameOf(AmqpOperationFlavor operationFlavor) {
		ServiceOperation operation = operationFlavor.getOperation();
		if (operation == null) {
			return operationFlavor.getName();
		}
		if (operation.eContainer() instanceof ServiceInterface contract && contract.getName() != null) {
			return contract.getName() + "/" + operation.getName();
		}
		return operation.getName();
	}

	private Object invoke(AmqpOperationFlavor operationFlavor, ServiceInvocation invocation)
			throws ReflectiveOperationException {
		ServiceOperation operation = operationFlavor.getOperation();
		if (operation == null) {
			throw new NoSuchMethodException("the flavor for this routing key binds no operation");
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

	private void answer(CloudEvent request, String routingKey, AmqpOperationFlavor operationFlavor,
			ServiceInvocationResult result, Reply reply) {
		String contentType = AmqpFlavors.produces(operationFlavor);
		try {
			byte[] document = write(List.of(result), contentType);
			reply.send(AmqpMessages.write(AmqpMessages.reply(request, source, contentType), document));
		} catch (Exception unanswerable) {
			// The last resort: say so in the encoding this side always
			// has. A Diagnostic is not the operation's declared result,
			// so `produces` does not describe it — and a caller that
			// learns why beats one that waits out its timer.
			LOG.log(Level.WARNING, "[DDSR-AMQP] could not answer a call under " + routingKey, unanswerable);
			try {
				byte[] document = write(List.of(Invocations.failure(String.valueOf(unanswerable), 0)),
						AmqpFlavors.DEFAULT_CONTENT_TYPE);
				reply.send(AmqpMessages.write(
						AmqpMessages.reply(request, source, AmqpFlavors.DEFAULT_CONTENT_TYPE), document));
			} catch (Exception silence) {
				LOG.warning("[DDSR-AMQP] and could not say why either: " + silence);
			}
		}
	}

	private ServiceInvocation invocationOf(byte[] document, String contentType) throws Exception {
		XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(document), resourceSets,
				contentType == null ? AmqpFlavors.DEFAULT_CONTENT_TYPE : contentType);
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
