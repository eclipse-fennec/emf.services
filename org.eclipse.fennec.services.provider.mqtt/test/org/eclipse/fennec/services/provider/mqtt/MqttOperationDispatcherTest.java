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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.flavor.mqtt.MqttMessages;
import org.eclipse.fennec.services.invocation.Invocations;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.cloudevents.model.ce.CloudEvent;

/**
 * What the provider side decides, without a broker anywhere near it:
 * the dispatcher takes its publisher and its service as seams, so a
 * call is a byte array in and a byte array out.
 */
class MqttOperationDispatcherTest {

	private static final String NS = ServicesPackage.eNS_URI;

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static final class ResourceSetObjects implements ComponentServiceObjects<ResourceSet> {

		@Override
		public ResourceSet getService() {
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			rs.getPackageRegistry().put(NS, ServicesPackage.eINSTANCE);
			return rs;
		}

		@Override
		public void ungetService(ResourceSet service) {
			// nothing to release in a test
		}

		@Override
		public ServiceReference<ResourceSet> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	/** The service behind the contract. */
	public static final class Payments {

		@SuppressWarnings("unused")
		public double charge(double amount) {
			if (amount > 100) {
				throw new IllegalStateException("insufficient funds");
			}
			return 1000 - amount;
		}
	}

	private record Published(String topic, byte[] payload, int qos) {
	}

	private final List<Published> sent = new ArrayList<>();

	private final ResourceSetObjects resourceSets = new ResourceSetObjects();

	private MqttFlavor payments() {
		Parameter amount = F.createParameter();
		amount.setName("amount");
		amount.setType("double");
		amount.setIndex(0);
		ServiceOperation charge = F.createServiceOperation();
		charge.setName("charge");
		charge.getParameters().add(amount);
		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Payment");
		contract.getOperations().add(charge);

		MqttFlavor flavor = F.createMqttFlavor();
		flavor.setName("payments-mqtt");
		flavor.getBrokers().add("tcp://fake:1883");
		flavor.setRequestTopic("ddsr/rpc/payments");
		MqttOperationFlavor operationFlavor = F.createMqttOperationFlavor();
		operationFlavor.setName("charge");
		operationFlavor.setOperation(charge);
		flavor.getOperationFlavors().add(operationFlavor);
		return flavor;
	}

	private MqttOperationDispatcher dispatcher(MqttFlavor flavor) {
		return new MqttOperationDispatcher(flavor, Payments::new, "/provider/Payment", resourceSets,
				(topic, payload, qos) -> sent.add(new Published(topic, payload, qos)));
	}

	private MqttOperationDispatcher dispatcher(MqttFlavor flavor, CallTracer tracer) {
		return new MqttOperationDispatcher(flavor, Payments::new, "/provider/Payment", resourceSets,
				(topic, payload, qos) -> sent.add(new Published(topic, payload, qos)), tracer);
	}

	/**
	 * A tracer that remembers what it was told. Enough to answer the one
	 * question this side owns: does the caller's context reach it.
	 */
	private static final class Watching implements CallTracer {

		private final List<String> served = new ArrayList<>();

		private String sawTraceparent;

		private String failure;

		private final Map<String, String> attributes = new LinkedHashMap<>();

		@Override
		public CallSpan calling(String operation, TraceCarrier outbound) {
			throw new AssertionError("a dispatcher serves, it does not call");
		}

		@Override
		public CallSpan serving(String operation, TraceCarrier inbound) {
			served.add(operation);
			sawTraceparent = inbound.get("traceparent");
			return new CallSpan() {

				@Override
				public CallSpan attribute(String name, String value) {
					attributes.put(name, value);
					return this;
				}

				@Override
				public void failed(Throwable error) {
					failure = String.valueOf(error.getMessage());
				}

				@Override
				public void close() {
					// nothing to close in a fake
				}
			};
		}
	}

	/** The same call, with a caller's trace context in the envelope. */
	private byte[] tracedCall(MqttFlavor flavor, String replyTo, double amount, String traceparent)
			throws Exception {
		ServiceOperation operation = ((MqttOperationFlavor) flavor.getOperationFlavors().get(0))
				.getOperation();
		ServiceInvocation invocation = Invocations.invocation(operation, Map.of("amount", amount));
		ByteArrayOutputStream document = new ByteArrayOutputStream();
		XmiCodec.write(document, resourceSets, Invocations.roots(invocation));
		CloudEvent envelope = MqttMessages.request("/consumer/probe", "charge", replyTo, "application/xml");
		envelope.getExtensions().put("traceparent", traceparent);
		return MqttMessages.write(envelope, document.toByteArray());
	}

	@Test
	void the_callers_trace_travels_in_the_envelope() throws Exception {
		MqttFlavor flavor = payments();
		Watching watching = new Watching();

		dispatcher(flavor, watching).onMessage("ddsr/rpc/payments/charge",
				tracedCall(flavor, "ddsr/rpc/payments/charge/reply/11", 12.5,
						"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"));

		assertThat(watching.served)
			.as("named by contract and operation, which is what a reader groups by")
			.containsExactly("Payment/charge");
		assertThat(watching.sawTraceparent)
			.as("MQTT 3 has no user properties, so the extensions are the only place it can ride")
			.isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
		assertThat(sent)
			.as("and the call was served as it always was")
			.hasSize(1);
		assertThat(watching.attributes)
			.as("who called (#125) — over MQTT the envelope's source is where that travels")
			.containsEntry(ClientOrigin.ATTRIBUTE, "/consumer/probe");
	}

	@Test
	void a_failing_call_is_marked_on_the_span_as_well_as_answered() throws Exception {
		MqttFlavor flavor = payments();
		Watching watching = new Watching();

		dispatcher(flavor, watching).onMessage("ddsr/rpc/payments/charge",
				call(flavor, "ddsr/rpc/payments/charge/reply/12", 500));

		assertThat(watching.failure).contains("insufficient funds");
		assertThat(answerIn(sent.get(0)).getDiagnostic())
			.as("the consumer is still told, which is the answer that matters")
			.isNotNull();
	}

	/** A call, as a consumer would put it on the wire. */
	private byte[] call(MqttFlavor flavor, String replyTo, double amount) throws Exception {
		ServiceOperation operation = ((MqttOperationFlavor) flavor.getOperationFlavors().get(0)).getOperation();
		ServiceInvocation invocation = Invocations.invocation(operation, Map.of("amount", amount));
		ByteArrayOutputStream document = new ByteArrayOutputStream();
		XmiCodec.write(document, resourceSets, Invocations.roots(invocation));
		CloudEvent envelope = MqttMessages.request("/consumer/probe", "charge", replyTo, "application/xml");
		return MqttMessages.write(envelope, document.toByteArray());
	}

	private ServiceInvocationResult answerIn(Published published) throws Exception {
		CloudEventCodec.Message message = CloudEventCodec.readStructured(published.payload());
		assertThat(message.attributes().getType()).isEqualTo(CloudEvents.TYPE_INVOKE_REPLY);
		XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(message.data()), resourceSets);
		for (EObject root : bundle.roots()) {
			if (root instanceof ServiceInvocationResult result) {
				return result;
			}
		}
		throw new AssertionError("the answer carried no ServiceInvocationResult");
	}

	@Test
	void a_call_is_answered_on_the_topic_it_named() throws Exception {
		MqttFlavor flavor = payments();
		MqttOperationDispatcher dispatcher = dispatcher(flavor);

		assertThat(dispatcher.requestTopics()).containsExactly("ddsr/rpc/payments/charge");

		dispatcher.onMessage("ddsr/rpc/payments/charge",
				call(flavor, "ddsr/rpc/payments/charge/reply/7", 12.5));

		assertThat(sent).hasSize(1);
		assertThat(sent.get(0).topic()).isEqualTo("ddsr/rpc/payments/charge/reply/7");
		assertThat(Invocations.valueOf(answerIn(sent.get(0)))).isEqualTo(987.5);
	}

	@Test
	void the_answer_names_the_call_it_answers() throws Exception {
		MqttFlavor flavor = payments();
		byte[] request = call(flavor, "ddsr/rpc/payments/charge/reply/9", 1);
		String requestId = CloudEventCodec.readStructured(request).attributes().getEventId();

		dispatcher(flavor).onMessage("ddsr/rpc/payments/charge", request);

		CloudEvent answer = CloudEventCodec.readStructured(sent.get(0).payload()).attributes();
		assertThat(MqttMessages.correlationOf(answer)).isEqualTo(requestId);
		assertThat(answer.getEventId())
			.as("an event of its own, not the request handed back")
			.isNotEqualTo(requestId);
	}

	@Test
	void a_failing_service_answers_with_a_diagnostic_rather_than_silence() throws Exception {
		MqttFlavor flavor = payments();

		dispatcher(flavor).onMessage("ddsr/rpc/payments/charge",
				call(flavor, "ddsr/rpc/payments/charge/reply/3", 500));

		ServiceInvocationResult answer = answerIn(sent.get(0));
		assertThat(answer.getDiagnostic()).isNotNull();
		assertThat(answer.getDiagnostic().getMessage()).contains("insufficient funds");
		assertThat(Invocations.valueOf(answer))
			.as("a failure has no value and must not read as one")
			.isNull();
	}

	@Test
	void a_message_that_is_not_a_call_is_dropped() {
		MqttFlavor flavor = payments();
		byte[] foreign = CloudEventCodec.writeStructured(
				CloudEvents.newEnvelope("com.example.something.happened", "/elsewhere", "application/xml"),
				"<x/>".getBytes(StandardCharsets.UTF_8));

		dispatcher(flavor).onMessage("ddsr/rpc/payments/charge", foreign);
		dispatcher(flavor).onMessage("ddsr/rpc/payments/charge", "not even json".getBytes(StandardCharsets.UTF_8));

		assertThat(sent)
			.as("there is nothing to answer to, and a subscription must survive it")
			.isEmpty();
	}

	@Test
	void a_call_that_names_no_reply_address_is_dropped() throws Exception {
		MqttFlavor flavor = payments();
		ServiceOperation operation = ((MqttOperationFlavor) flavor.getOperationFlavors().get(0)).getOperation();
		ByteArrayOutputStream document = new ByteArrayOutputStream();
		XmiCodec.write(document, resourceSets,
				Invocations.roots(Invocations.invocation(operation, Map.of("amount", 1.0))));
		byte[] addressless = CloudEventCodec.writeStructured(
				CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE, "/consumer/probe", "application/xml"),
				document.toByteArray());

		dispatcher(flavor).onMessage("ddsr/rpc/payments/charge", addressless);

		assertThat(sent).isEmpty();
	}

	@Test
	void a_topic_this_contract_does_not_answer_on_is_ignored() throws Exception {
		MqttFlavor flavor = payments();

		dispatcher(flavor).onMessage("ddsr/rpc/payments/refund",
				call(flavor, "ddsr/rpc/payments/charge/reply/1", 1));

		assertThat(sent).isEmpty();
	}
}
