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

package org.eclipse.fennec.services.provider.mqtt;

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
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.flavor.mqtt.MqttFlavors;
import org.eclipse.fennec.services.flavor.mqtt.MqttMessages;
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
 * <p>It knows no MQTT client. Subscribing and publishing are the
 * component's business; what arrives and what goes back is this class's,
 * which is what lets the whole decision be exercised without a broker.
 *
 * <p>A failure is an answer. A handler that throws, an argument that
 * will not decode, a method that is not there — each comes back as a
 * {@code Diagnostic} on the reply topic, because the alternative is a
 * consumer waiting out its timeout to learn nothing.
 */
public final class MqttOperationDispatcher {

	private static final Logger LOG = Logger.getLogger(MqttOperationDispatcher.class.getName());

	/** Where an answer goes. The component supplies the connection. */
	public interface Publisher {

		void publish(String topic, byte[] payload, int qos) throws Exception;
	}

	private final MqttFlavor flavor;

	private final Supplier<Object> service;

	private final String source;

	private final ComponentServiceObjects<ResourceSet> resourceSets;

	private final Publisher publisher;

	private final CallTracer tracer;

	/** Which operation flavor answers on which request topic. */
	private final Map<String, MqttOperationFlavor> byTopic = new LinkedHashMap<>();

	public MqttOperationDispatcher(MqttFlavor flavor, Supplier<Object> service, String source,
			ComponentServiceObjects<ResourceSet> resourceSets, Publisher publisher) {
		this(flavor, service, source, resourceSets, publisher, CallTracer.NONE);
	}

	/**
	 * @param tracer whoever is watching calls (#126). A dispatcher built
	 *        without one is built with {@link CallTracer#NONE}: serving a
	 *        contract over topics has never required telemetry.
	 */
	public MqttOperationDispatcher(MqttFlavor flavor, Supplier<Object> service, String source,
			ComponentServiceObjects<ResourceSet> resourceSets, Publisher publisher, CallTracer tracer) {
		this.tracer = tracer == null ? CallTracer.NONE : tracer;
		this.flavor = flavor;
		this.service = service;
		this.source = source;
		this.resourceSets = resourceSets;
		this.publisher = publisher;
		for (ServiceOperationFlavor candidate : flavor.getOperationFlavors()) {
			if (candidate instanceof MqttOperationFlavor operationFlavor) {
				byTopic.put(MqttFlavors.requestTopic(flavor, operationFlavor), operationFlavor);
			}
		}
	}

	/** The topics this contract answers on, one per operation. */
	public List<String> requestTopics() {
		return List.copyOf(byTopic.keySet());
	}

	/** The QoS one of those topics is subscribed with. */
	public int qosFor(String topic) {
		MqttOperationFlavor operationFlavor = byTopic.get(topic);
		return operationFlavor == null ? 1 : MqttFlavors.qos(flavor, operationFlavor);
	}

	/**
	 * One call. Never throws: a dispatcher that let a failure escape
	 * would take down the subscription that every other caller of this
	 * contract shares.
	 */
	public void onMessage(String topic, byte[] payload) {
		MqttOperationFlavor operationFlavor = byTopic.get(topic);
		if (operationFlavor == null) {
			LOG.fine(() -> "[DDSR-MQTT] nothing of this contract answers on " + topic);
			return;
		}
		CloudEvent request;
		ServiceInvocation invocation;
		try {
			var message = MqttMessages.read(payload, CloudEvents.TYPE_INVOKE);
			request = message.attributes();
			invocation = invocationOf(message.data(), request.getDatacontenttype());
		} catch (Exception unreadable) {
			// There is nothing to answer to: a message this side cannot
			// read is one whose reply address it cannot trust either.
			LOG.warning("[DDSR-MQTT] unreadable call on " + topic + " dropped: " + unreadable);
			return;
		}

		String replyTo;
		try {
			replyTo = MqttMessages.replyTopicOf(request);
		} catch (RuntimeException noAddress) {
			LOG.warning("[DDSR-MQTT] a call on " + topic + " named no reply address: " + noAddress);
			return;
		}

		// The caller's trace, continued here (#126). It travelled in the
		// envelope's extensions, which is where CloudEvents puts it and
		// the only place it could travel on MQTT 3.
		try (CallSpan span = tracer.serving(spanNameOf(operationFlavor),
				TraceCarrier.over(request.getExtensions().map()))) {
			span.attribute("rpc.system", "fennec.services")
					.attribute("server.address", topic)
					.attribute("fennec.flavor", "MQTT");
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
			answer(request, replyTo, operationFlavor, result);
		}
	}

	/**
	 * What to call this call in a trace: the contract and the operation,
	 * which is what a reader groups by — never the topic, which carries
	 * the same two facts in a shape nobody outside this project reads.
	 */
	private static String spanNameOf(MqttOperationFlavor operationFlavor) {
		ServiceOperation operation = operationFlavor.getOperation();
		if (operation == null) {
			return operationFlavor.getName();
		}
		if (operation.eContainer() instanceof ServiceInterface contract && contract.getName() != null) {
			return contract.getName() + "/" + operation.getName();
		}
		return operation.getName();
	}

	private Object invoke(MqttOperationFlavor operationFlavor, ServiceInvocation invocation)
			throws ReflectiveOperationException {
		ServiceOperation operation = operationFlavor.getOperation();
		if (operation == null) {
			throw new NoSuchMethodException("the flavor for this topic binds no operation");
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

	private void answer(CloudEvent request, String replyTo, MqttOperationFlavor operationFlavor,
			ServiceInvocationResult result) {
		String contentType = MqttFlavors.produces(operationFlavor);
		try {
			byte[] document = write(List.of(result), contentType);
			byte[] message = MqttMessages.write(MqttMessages.reply(request, source, contentType), document);
			publisher.publish(replyTo, message, MqttFlavors.qos(flavor, operationFlavor));
		} catch (Exception unanswerable) {
			// The last resort: say so in the encoding this side always
			// has. A Diagnostic is not the operation's declared result,
			// so `produces` does not describe it — and a caller that
			// learns why beats one that waits out its timer.
			LOG.log(Level.WARNING, "[DDSR-MQTT] could not answer on " + replyTo, unanswerable);
			try {
				byte[] document = write(List.of(Invocations.failure(String.valueOf(unanswerable), 0)),
						MqttFlavors.DEFAULT_CONTENT_TYPE);
				publisher.publish(replyTo, MqttMessages.write(
						MqttMessages.reply(request, source, MqttFlavors.DEFAULT_CONTENT_TYPE), document),
						MqttFlavors.qos(flavor, operationFlavor));
			} catch (Exception silence) {
				LOG.warning("[DDSR-MQTT] and could not say why either: " + silence);
			}
		}
	}

	private ServiceInvocation invocationOf(byte[] document, String contentType) throws Exception {
		XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(document), resourceSets,
				contentType == null ? MqttFlavors.DEFAULT_CONTENT_TYPE : contentType);
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
