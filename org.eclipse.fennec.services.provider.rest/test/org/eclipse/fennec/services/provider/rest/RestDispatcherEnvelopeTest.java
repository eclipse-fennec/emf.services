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

package org.eclipse.fennec.services.provider.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.cloudevents.model.ce.CloudEvent;
import jakarta.ws.rs.core.Response;

/**
 * Binary mode on the provider side (#101): the attributes arrive as
 * {@code ce-*} headers, the answer is the second event of the pair, and
 * the body is not touched by any of it.
 *
 * <p>That last part is what makes the envelope additive over HTTP, so
 * it is asserted rather than assumed: a request with no envelope at all
 * must be answered exactly as before.
 */
class RestDispatcherEnvelopeTest {

	private static final String NS = "http://eclipse.org/fennec/services/1.0";

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
	public static final class Balance {

		@SuppressWarnings("unused")
		public String getBalance() {
			return "1234";
		}
	}

	private static final class OneService implements ServiceObjects<Object> {

		@Override
		public Object getService() {
			return new Balance();
		}

		@Override
		public void ungetService(Object service) {
			// nothing to release in a test
		}

		@Override
		public ServiceReference<Object> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	/** A contract with one argument-free operation on GET /balance. */
	private static RestFlavor contract() {
		ServiceOperation getBalance = F.createServiceOperation();
		getBalance.setName("getBalance");

		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Payment");
		contract.getOperations().add(getBalance);

		RestOperationFlavor opFlavor = F.createRestOperationFlavor();
		opFlavor.setName("getBalance");
		opFlavor.setMethod(HttpMethod.GET);
		opFlavor.setPath("/balance");
		opFlavor.setOperation(getBalance);
		opFlavor.getProduces().add("text/plain");

		RestFlavor flavor = F.createRestFlavor();
		flavor.setName("payment-rest");
		flavor.getOperationFlavors().add(opFlavor);
		return flavor;
	}

	private Response get(Map<String, String> headers) {
		RestDispatcher dispatcher = new RestDispatcher(contract(), OneService::new, "Payment",
				new ResourceSetObjects());
		return dispatcher.dispatch("GET", "/balance", name -> List.of(), headers::get, null);
	}

	private static Map<String, String> requestEnvelope(String id) {
		CloudEvent request = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE, "/consumer/probe", null);
		request.setEventId(id);
		request.setSubject("Payment/getBalance");
		return new HashMap<>(CloudEventCodec.toHeaders(request));
	}

	@Test
	void the_answer_is_the_second_event_of_the_pair() {
		Response answer = get(requestEnvelope("req-1"));

		assertThat(answer.getStatus()).isEqualTo(200);
		assertThat(answer.getEntity()).isEqualTo("1234");
		assertThat(answer.getHeaderString("ce-type")).isEqualTo(CloudEvents.TYPE_INVOKE_REPLY);
		assertThat(answer.getHeaderString("ce-correlationid"))
				.as("the reply names the call it answers")
				.isEqualTo("req-1");
		assertThat(answer.getHeaderString("ce-id"))
				.as("and is an event of its own, not the request again")
				.isNotEqualTo("req-1");
		assertThat(answer.getHeaderString("ce-source")).isEqualTo("/provider/Payment");
		assertThat(answer.getHeaderString("ce-subject")).isEqualTo("Payment/getBalance");
	}

	@Test
	void a_request_without_an_envelope_is_answered_exactly_as_before() {
		Response answer = get(Map.of());

		assertThat(answer.getStatus()).isEqualTo(200);
		assertThat(answer.getEntity())
				.as("the body never depended on the headers, which is what made this additive")
				.isEqualTo("1234");
		assertThat(answer.getHeaderString("ce-type")).isEqualTo(CloudEvents.TYPE_INVOKE_REPLY);
		assertThat(answer.getHeaderString("ce-correlationid"))
				.as("there is no call to correlate with")
				.isNull();
	}

	@Test
	void a_failing_call_answers_with_an_envelope_too() {
		Response answer = get(requestEnvelope("req-2"));
		Response missing = new RestDispatcher(contract(), () -> null, "Payment", new ResourceSetObjects())
				.dispatch("GET", "/balance", name -> List.of(), requestEnvelope("req-3")::get, null);

		assertThat(answer.getHeaderString("ce-correlationid")).isEqualTo("req-2");
		assertThat(missing.getStatus()).isEqualTo(503);
		assertThat(missing.getHeaderString("ce-correlationid"))
				.as("a 503 is an answer, and an answer says which call it answers")
				.isEqualTo("req-3");
	}
}
