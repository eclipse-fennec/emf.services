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
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.MessagingFlavor;
import org.eclipse.fennec.services.MessagingOperationFlavor;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.flavor.messaging.MessagingRoute;
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
class MessagingOperationDispatcherTest {

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

	/** What the dispatcher handed back, call by call. */
	private final List<CloudEventCodec.Message> sent = new ArrayList<>();

	private final ResourceSetObjects resourceSets = new ResourceSetObjects();

	private MessagingFlavor payments() {
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

		MessagingFlavor flavor = F.createMessagingFlavor();
		flavor.setName("payments-mqtt");
		flavor.getBrokers().add("tcp://fake:1883");
		flavor.setChannel("ddsr/rpc/payments");
		MessagingOperationFlavor operationFlavor = F.createMessagingOperationFlavor();
		operationFlavor.setName("charge");
		operationFlavor.setOperation(charge);
		flavor.getOperationFlavors().add(operationFlavor);
		return flavor;
	}

	private MessagingOperationDispatcher dispatcher(MessagingFlavor flavor) {
		return new MessagingOperationDispatcher(flavor, Payments::new, "/provider/Payment", resourceSets);
	}

	private MessagingOperationDispatcher dispatcher(MessagingFlavor flavor, CallTracer tracer) {
		return new MessagingOperationDispatcher(flavor, Payments::new, "/provider/Payment", resourceSets,
				tracer);
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
	private CloudEventCodec.Message tracedCall(MessagingFlavor flavor, String replyTo, double amount, String traceparent)
			throws Exception {
		ServiceOperation operation = ((MessagingOperationFlavor) flavor.getOperationFlavors().get(0))
				.getOperation();
		ServiceInvocation invocation = Invocations.invocation(operation, Map.of("amount", amount));
		ByteArrayOutputStream document = new ByteArrayOutputStream();
		XmiCodec.write(document, resourceSets, Invocations.roots(invocation));
		CloudEvent envelope = MessagingRoute.request("/consumer/probe", "charge", "application/xml", replyTo);
		envelope.getExtensions().put("traceparent", traceparent);
		return new CloudEventCodec.Message(envelope, document.toByteArray());
	}

	@Test
	void the_callers_trace_travels_in_the_envelope() throws Exception {
		MessagingFlavor flavor = payments();
		Watching watching = new Watching();

		dispatcher(flavor, watching).onRequest("ddsr/rpc/payments/charge", tracedCall(flavor, "ddsr/rpc/payments/charge/reply/11", 12.5,
						"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"), sent::add);

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
		MessagingFlavor flavor = payments();
		Watching watching = new Watching();

		dispatcher(flavor, watching).onRequest("ddsr/rpc/payments/charge", call(flavor, "ddsr/rpc/payments/charge/reply/12", 500), sent::add);

		assertThat(watching.failure).contains("insufficient funds");
		assertThat(answerIn(sent.get(0)).getDiagnostic())
			.as("the consumer is still told, which is the answer that matters")
			.isNotNull();
	}

	/** A call, as a consumer would put it on the wire. */
	private CloudEventCodec.Message call(MessagingFlavor flavor, String replyTo, double amount) throws Exception {
		ServiceOperation operation = ((MessagingOperationFlavor) flavor.getOperationFlavors().get(0)).getOperation();
		ServiceInvocation invocation = Invocations.invocation(operation, Map.of("amount", amount));
		ByteArrayOutputStream document = new ByteArrayOutputStream();
		XmiCodec.write(document, resourceSets, Invocations.roots(invocation));
		CloudEvent envelope = MessagingRoute.request("/consumer/probe", "charge", "application/xml", replyTo);
		return new CloudEventCodec.Message(envelope, document.toByteArray());
	}

	private ServiceInvocationResult answerIn(CloudEventCodec.Message message) throws Exception {
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
	void a_call_is_answered_on_the_way_back_it_came_with() throws Exception {
		MessagingFlavor flavor = payments();
		MessagingOperationDispatcher dispatcher = dispatcher(flavor);

		assertThat(dispatcher.channels()).containsExactly("ddsr/rpc/payments/charge");
		assertThat(dispatcher.qosFor("ddsr/rpc/payments/charge")).isEqualTo(1);

		dispatcher.onRequest("ddsr/rpc/payments/charge", call(flavor, "ddsr/rpc/payments/charge/reply/7", 12.5), sent::add);

		assertThat(sent)
			.as("handed to the way back the connection supplied — where that leads is the connection's")
			.hasSize(1);
		assertThat(Invocations.valueOf(answerIn(sent.get(0)))).isEqualTo(987.5);
	}

	@Test
	void the_answer_names_the_call_it_answers() throws Exception {
		MessagingFlavor flavor = payments();
		CloudEventCodec.Message request = call(flavor, "ddsr/rpc/payments/charge/reply/9", 1);
		String requestId = request.attributes().getEventId();

		dispatcher(flavor).onRequest("ddsr/rpc/payments/charge", request, sent::add);

		CloudEvent answer = sent.get(0).attributes();
		assertThat(MessagingRoute.correlationOf(answer)).isEqualTo(requestId);
		assertThat(answer.getEventId())
			.as("an event of its own, not the request handed back")
			.isNotEqualTo(requestId);
	}

	@Test
	void a_failing_service_answers_with_a_diagnostic_rather_than_silence() throws Exception {
		MessagingFlavor flavor = payments();

		dispatcher(flavor).onRequest("ddsr/rpc/payments/charge", call(flavor, "ddsr/rpc/payments/charge/reply/3", 500), sent::add);

		ServiceInvocationResult answer = answerIn(sent.get(0));
		assertThat(answer.getDiagnostic()).isNotNull();
		assertThat(answer.getDiagnostic().getMessage()).contains("insufficient funds");
		assertThat(Invocations.valueOf(answer))
			.as("a failure has no value and must not read as one")
			.isNull();
	}

	/** What a provider throws for the contract's 'InsufficientFunds'. */
	public static final class InsufficientFundsException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public InsufficientFundsException(String message) {
			super(message);
		}
	}

	/** Raises the declared error by throwing it. */
	public static final class ThrowingPayments {

		@SuppressWarnings("unused")
		public double charge(double amount) {
			throw new InsufficientFundsException("only 3.50 left");
		}
	}

	/** Raises the declared error the other way: a failing Diagnostic as the result. */
	public static final class ReportingPayments {

		@SuppressWarnings("unused")
		public Object charge(double amount) {
			Diagnostic refused = F.createDiagnostic();
			refused.setSeverity(DiagnosticSeverity.ERROR);
			refused.setCode(402);
			refused.setMessage("only 3.50 left");
			return refused;
		}
	}

	/** The payments contract, with 'InsufficientFunds' (code 402) declared on charge. */
	private MessagingFlavor paymentsDeclaringInsufficientFunds() {
		MessagingFlavor flavor = payments();
		ServiceOperation charge = ((MessagingOperationFlavor) flavor.getOperationFlavors().get(0)).getOperation();
		ServiceException declared = F.createServiceException();
		declared.setName("InsufficientFunds");
		declared.setType("payment.InsufficientFunds");
		IntProperty code = F.createIntProperty();
		code.setName("code");
		code.setValue(402);
		declared.getProperties().add(code);
		((ServiceInterface) charge.eContainer()).getExceptions().add(declared);
		charge.getExceptions().add(declared);
		return flavor;
	}

	/**
	 * Over a messaging broker a declared error is its code and nothing
	 * else (#185): there is no status to bind it to, and the answer
	 * carries the Diagnostic anyway.
	 */
	@Test
	void a_declared_error_that_is_thrown_travels_under_its_code() throws Exception {
		MessagingFlavor flavor = paymentsDeclaringInsufficientFunds();

		new MessagingOperationDispatcher(flavor, ThrowingPayments::new, "/provider/Payment", resourceSets)
				.onRequest("ddsr/rpc/payments/charge", call(flavor, "ddsr/rpc/payments/charge/reply/4", 5), sent::add);

		Diagnostic diagnostic = answerIn(sent.get(0)).getDiagnostic();
		assertThat(diagnostic.getCode()).isEqualTo(402);
		assertThat(diagnostic.getMessage())
			.as("the provider's words, not the name of its exception class")
			.isEqualTo("only 3.50 left");
	}

	@Test
	void a_failure_returned_as_a_diagnostic_is_the_answers_diagnostic_and_not_its_value() throws Exception {
		MessagingFlavor flavor = paymentsDeclaringInsufficientFunds();

		new MessagingOperationDispatcher(flavor, ReportingPayments::new, "/provider/Payment", resourceSets)
				.onRequest("ddsr/rpc/payments/charge", call(flavor, "ddsr/rpc/payments/charge/reply/5", 5), sent::add);

		ServiceInvocationResult answer = answerIn(sent.get(0));
		assertThat(answer.getDiagnostic()).isNotNull();
		assertThat(answer.getDiagnostic().getCode()).isEqualTo(402);
		assertThat(answer.getValue())
			.as("a failure must not read as a successful call that returned a Diagnostic")
			.isNull();
	}

	@Test
	void an_error_the_contract_does_not_declare_has_no_code() throws Exception {
		MessagingFlavor flavor = paymentsDeclaringInsufficientFunds();

		dispatcher(flavor).onRequest("ddsr/rpc/payments/charge", call(flavor, "ddsr/rpc/payments/charge/reply/6", 500), sent::add);

		assertThat(answerIn(sent.get(0)).getDiagnostic().getCode())
			.as("IllegalStateException is nothing the contract announced")
			.isZero();
	}

	@Test
	void a_message_that_is_not_a_call_is_dropped() {
		MessagingFlavor flavor = payments();
		CloudEventCodec.Message foreign = new CloudEventCodec.Message(
				CloudEvents.newEnvelope("com.example.something.happened", "/elsewhere", "application/xml"),
				"<x/>".getBytes(StandardCharsets.UTF_8));
		CloudEventCodec.Message empty = new CloudEventCodec.Message(
				CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE, "/consumer/probe", "application/xml"), null);

		dispatcher(flavor).onRequest("ddsr/rpc/payments/charge", foreign, sent::add);
		dispatcher(flavor).onRequest("ddsr/rpc/payments/charge", empty, sent::add);

		assertThat(sent)
			.as("there is nothing to answer to, and a subscription must survive it")
			.isEmpty();
	}

	@Test
	void a_service_that_is_not_there_is_an_answer_too() throws Exception {
		MessagingFlavor flavor = payments();
		MessagingOperationDispatcher dispatcher = new MessagingOperationDispatcher(flavor, () -> null,
				"/provider/Payment", resourceSets);

		dispatcher.onRequest("ddsr/rpc/payments/charge", call(flavor, "r/1", 1), sent::add);

		assertThat(answerIn(sent.get(0)).getDiagnostic().getMessage())
			.contains("no service implements this contract");
	}

	@Test
	void a_way_back_that_fails_does_not_escape() throws Exception {
		MessagingFlavor flavor = payments();

		// The subscription is shared by every caller of the operation:
		// one answer that cannot be sent must not end it.
		dispatcher(flavor).onRequest("ddsr/rpc/payments/charge", call(flavor, "r/1", 1), answer -> {
			throw new IllegalStateException("the request named no reply address");
		});
	}

	@Test
	void an_amqp_flavor_is_dispatched_by_its_routing_key_and_nothing_else_differs() throws Exception {
		MessagingFlavor mqtt = payments();
		ServiceOperation charge = ((MessagingOperationFlavor) mqtt.getOperationFlavors().get(0)).getOperation();
		MessagingFlavor flavor = F.createMessagingFlavor();
		flavor.setName("payments-amqp");
		flavor.setKind(FlavorKind.AMQP);
		flavor.getBrokers().add("amqp://fake:5672");
		flavor.setChannel("ddsr.rpc.payments");
		MessagingOperationFlavor operationFlavor = F.createMessagingOperationFlavor();
		operationFlavor.setName("charge");
		operationFlavor.setOperation(charge);
		flavor.getOperationFlavors().add(operationFlavor);
		MessagingOperationDispatcher dispatcher = new MessagingOperationDispatcher(flavor, Payments::new,
				"/provider/Payment", resourceSets);

		assertThat(dispatcher.channels()).containsExactly("ddsr.rpc.payments.charge");

		dispatcher.onRequest("ddsr.rpc.payments.charge", call(mqtt, null, 10.0), sent::add);

		assertThat(Invocations.valueOf(answerIn(sent.get(0)))).isEqualTo(990.0);
		assertThat(sent.get(0).attributes().getDataschema())
			.as("the answer says what schema its document adheres to")
			.isEqualTo(ServicesPackage.eNS_URI);
	}

	@Test
	void a_topic_this_contract_does_not_answer_on_is_ignored() throws Exception {
		MessagingFlavor flavor = payments();

		dispatcher(flavor).onRequest("ddsr/rpc/payments/refund", call(flavor, "ddsr/rpc/payments/charge/reply/1", 1), sent::add);

		assertThat(sent).isEmpty();
	}
}
