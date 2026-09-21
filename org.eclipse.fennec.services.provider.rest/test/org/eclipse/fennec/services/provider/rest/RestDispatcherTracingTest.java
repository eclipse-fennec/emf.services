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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import jakarta.ws.rs.core.Response;

/**
 * The provider half of a trace (#126).
 *
 * <p>What is worth testing here is not OpenTelemetry — that has its own
 * test — but that this dispatcher hands the caller's headers to whoever
 * is watching, names the call the way a reader can group by, and does
 * all of it in the one place every dispatched call passes through.
 */
class RestDispatcherTracingTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	/** Remembers what it was asked and what it was given to read. */
	private static final class Watching implements CallTracer {

		private final List<String> served = new ArrayList<>();

		private final Map<String, String> attributes = new HashMap<>();

		private String sawTraceparent;

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
					attributes.put("error", String.valueOf(error.getMessage()));
				}

				@Override
				public void close() {
					// nothing to close in a fake
				}
			};
		}
	}

	/** The service behind the contract. */
	public static final class OneService {

		public String getBalance() {
			return "1234";
		}
	}

	@Test
	@DisplayName("the caller's context is handed to the watcher, out of the request's own headers")
	void contextReachesTheTracer() {
		Watching watching = new Watching();
		Map<String, String> headers = Map.of("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

		Response answer = dispatch(watching, headers);

		assertThat(answer.getStatus()).isEqualTo(200);
		assertThat(watching.sawTraceparent)
			.as("nothing but the headers crossed, which is what makes it one trace")
			.isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
	}

	@Test
	@DisplayName("the call is named by contract and operation, never by the path")
	void spanIsNamedByTheContract() {
		Watching watching = new Watching();

		dispatch(watching, Map.of());

		assertThat(watching.served).containsExactly("Payment/getBalance");
		assertThat(watching.attributes)
			.containsEntry("rpc.service", "Payment")
			.containsEntry("http.request.method", "GET")
			.containsEntry("fennec.flavor", "REST")
			.containsEntry("http.response.status_code", "200");
	}

	@Test
	@DisplayName("the span says who called, which is the half a trace cannot infer")
	void theCallerIsNamedOnTheSpan() {
		Watching watching = new Watching();

		dispatch(watching, Map.of(ClientOrigin.HEADER, "payment-demo/7f3a"));

		assertThat(watching.attributes)
			.as("the same identity the origin header carries (#125), so a trace answers "
					+ "which system told which system what")
			.containsEntry(ClientOrigin.ATTRIBUTE, "payment-demo/7f3a");
	}

	@Test
	@DisplayName("a caller that named nobody is anonymous, not absent")
	void anUnnamedCallerIsAnonymous() {
		Watching watching = new Watching();

		dispatch(watching, Map.of());

		assertThat(watching.attributes).containsEntry(ClientOrigin.ATTRIBUTE, ClientOrigin.ANONYMOUS);
	}

	@Test
	@DisplayName("a request that matches no operation is still served, and still named")
	void unmatchedRequestIsStillTraced() {
		Watching watching = new Watching();

		Response answer = new RestDispatcher(contract(), () -> new SingleService(new OneService()), "Payment",
				new ResourceSetObjects(), watching)
						.dispatch("GET", "/nothing-here", name -> List.of(), Map.<String, String>of()::get,
								null);

		assertThat(answer.getStatus()).isEqualTo(404);
		assertThat(watching.served)
			.as("a call nobody could route is exactly the kind a trace should show")
			.containsExactly("Payment GET");
		assertThat(watching.attributes).containsEntry("http.response.status_code", "404");
	}

	private static Response dispatch(CallTracer tracer, Map<String, String> headers) {
		RestDispatcher dispatcher = new RestDispatcher(contract(), () -> new SingleService(new OneService()), "Payment",
				new ResourceSetObjects(), tracer);
		return dispatcher.dispatch("GET", "/balance", name -> List.of(), headers::get, null);
	}

	/** A contract with one argument-free operation on GET /balance. */
	private static RestFlavor contract() {
		ServiceOperation getBalance = F.createServiceOperation();
		getBalance.setName("getBalance");

		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Payment");
		contract.getOperations().add(getBalance);

		RestOperationFlavor operationFlavor = F.createRestOperationFlavor();
		operationFlavor.setName("getBalance");
		operationFlavor.setMethod(HttpMethod.GET);
		operationFlavor.setPath("/balance");
		operationFlavor.setOperation(getBalance);
		operationFlavor.getProduces().add("text/plain");

		RestFlavor flavor = F.createRestFlavor();
		flavor.setName("payment-rest");
		flavor.getOperationFlavors().add(operationFlavor);
		return flavor;
	}

	/** The dispatcher asks for a ResourceSet only when it decodes a model. */
	private static final class ResourceSetObjects implements ComponentServiceObjects<ResourceSet> {

		@Override
		public ResourceSet getService() {
			throw new UnsupportedOperationException("not needed for these tests");
		}

		@Override
		public void ungetService(ResourceSet service) {
			// nothing was handed out
		}

		@Override
		public ServiceReference<ResourceSet> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	/** One service instance, handed out for every call. */
	private static final class SingleService implements ServiceObjects<Object> {

		private final Object service;

		SingleService(Object service) {
			this.service = service;
		}

		@Override
		public Object getService() {
			return service;
		}

		@Override
		public void ungetService(Object service) {
			// nothing to release
		}

		@Override
		public ServiceReference<Object> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}
}
