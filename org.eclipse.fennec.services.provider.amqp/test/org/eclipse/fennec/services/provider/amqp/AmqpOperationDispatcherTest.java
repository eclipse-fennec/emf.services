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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.AmqpFlavor;
import org.eclipse.fennec.services.AmqpOperationFlavor;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.flavor.amqp.AmqpMessages;
import org.eclipse.fennec.services.invocation.Invocations;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.cloudevents.model.ce.CloudEvent;

/**
 * What the provider side decides over AMQP, without a broker: a call is
 * a routing key and a byte array in, and a byte array on the way back
 * the connection handed in.
 */
class AmqpOperationDispatcherTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static final class ResourceSetObjects implements ComponentServiceObjects<ResourceSet> {

		@Override
		public ResourceSet getService() {
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			rs.getPackageRegistry().put(ServicesPackage.eNS_URI, ServicesPackage.eINSTANCE);
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

	private final List<byte[]> answers = new ArrayList<>();

	private final ResourceSetObjects resourceSets = new ResourceSetObjects();

	private ServiceOperation charge;

	private AmqpFlavor payments() {
		Parameter amount = F.createParameter();
		amount.setName("amount");
		amount.setType("double");
		amount.setIndex(0);
		charge = F.createServiceOperation();
		charge.setName("charge");
		charge.getParameters().add(amount);
		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Payment");
		contract.getOperations().add(charge);

		AmqpFlavor flavor = F.createAmqpFlavor();
		flavor.setName("payments-amqp");
		flavor.getBrokers().add("amqp://fake:5672");
		flavor.setRequestRoutingKey("ddsr.rpc.payments");
		AmqpOperationFlavor operationFlavor = F.createAmqpOperationFlavor();
		operationFlavor.setName("charge");
		operationFlavor.setOperation(charge);
		flavor.getOperationFlavors().add(operationFlavor);
		return flavor;
	}

	private AmqpOperationDispatcher dispatcher(AmqpFlavor flavor) {
		return new AmqpOperationDispatcher(flavor, Payments::new, "/provider/Payment", resourceSets,
				CallTracer.NONE);
	}

	private byte[] call(double amount) throws Exception {
		CloudEvent request = AmqpMessages.request("/consumer/test", "charge", "application/xml");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, resourceSets, "application/xml",
				Invocations.roots(Invocations.invocation(charge, Map.of("amount", amount))));
		lastRequest = request;
		return AmqpMessages.write(request, out.toByteArray());
	}

	private CloudEvent lastRequest;

	private ServiceInvocationResult resultOf(byte[] answer) throws Exception {
		CloudEventCodec.Message message = AmqpMessages.read(answer, CloudEvents.TYPE_INVOKE_REPLY);
		assertThat(message.attributes().getExtensions().get(CloudEvents.EXTENSION_CORRELATION_ID))
				.as("the envelope names its call too, for whoever reads it off the wire")
				.isEqualTo(lastRequest.getEventId());
		XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(message.data()), resourceSets,
				message.attributes().getDatacontenttype());
		for (EObject root : bundle.roots()) {
			if (root instanceof ServiceInvocationResult result) {
				return result;
			}
		}
		throw new AssertionError("no result in the answer");
	}

	@Test
	void itAnswersUnderOneKeyPerOperation() {
		assertThat(dispatcher(payments()).routingKeys()).containsExactly("ddsr.rpc.payments.charge");
	}

	@Test
	void aCallIsInvokedAndAnsweredOnTheWayBackItCameWith() throws Exception {
		AmqpOperationDispatcher dispatcher = dispatcher(payments());

		dispatcher.onRequest("ddsr.rpc.payments.charge", call(10.0), answers::add);

		assertThat(answers).hasSize(1);
		ServiceInvocationResult result = resultOf(answers.get(0));
		assertThat(result.getDiagnostic()).isNull();
		assertThat(Invocations.valueOf(result)).isEqualTo(990.0);
	}

	@Test
	void aServiceThatThrowsIsAnAnswerNotASilence() throws Exception {
		AmqpOperationDispatcher dispatcher = dispatcher(payments());

		dispatcher.onRequest("ddsr.rpc.payments.charge", call(500.0), answers::add);

		assertThat(answers).hasSize(1);
		assertThat(resultOf(answers.get(0)).getDiagnostic().getMessage()).contains("insufficient funds");
	}

	@Test
	void aServiceThatIsNotThereIsAnAnswerToo() throws Exception {
		AmqpOperationDispatcher dispatcher = new AmqpOperationDispatcher(payments(), () -> null,
				"/provider/Payment", resourceSets, CallTracer.NONE);

		dispatcher.onRequest("ddsr.rpc.payments.charge", call(10.0), answers::add);

		assertThat(resultOf(answers.get(0)).getDiagnostic().getMessage())
				.contains("no service implements this contract");
	}

	@Test
	void whatCannotBeReadIsDroppedWithoutAnAnswer() {
		AmqpOperationDispatcher dispatcher = dispatcher(payments());

		dispatcher.onRequest("ddsr.rpc.payments.charge", "not an envelope".getBytes(), answers::add);

		assertThat(answers).isEmpty();
	}

	@Test
	void aKeyOfAnotherContractIsNotThisDispatchersBusiness() throws Exception {
		AmqpOperationDispatcher dispatcher = dispatcher(payments());

		dispatcher.onRequest("ddsr.rpc.other.charge", call(10.0), answers::add);

		assertThat(answers).isEmpty();
	}

	@Test
	void aWayBackThatFailsDoesNotEscape() throws Exception {
		AmqpOperationDispatcher dispatcher = dispatcher(payments());

		// The binding is shared by every caller of the operation: one
		// answer that cannot be sent must not end it.
		dispatcher.onRequest("ddsr.rpc.payments.charge", call(10.0), payload -> {
			throw new IllegalStateException("the request named no reply address");
		});
	}
}
