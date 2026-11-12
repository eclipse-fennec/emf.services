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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestExceptionBinding;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.xmi.codec.WireBody;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import jakarta.ws.rs.core.Response;

/**
 * What a request body becomes before it reaches the service.
 *
 * <p>The contract decides, not the transport: a parameter typed by an
 * {@code EClass} carries a model, anything else carries a value. These
 * tests go through {@code dispatch} rather than the annotated methods —
 * the annotations only translate a request into these terms, and what
 * is worth pinning is the decision underneath.
 */
class RestDispatcherBodyTest {

	private static final String NS = "http://eclipse.org/fennec/services/1.0";
	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	/** Stand-in for the emf.osgi prototype service. */
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

	/** Records what the dispatcher handed the service. */
	public static final class Probe {
		Object received;

		public String store(Object payload) {
			this.received = payload;
			return payload == null ? "nothing" : payload.getClass().getSimpleName();
		}
	}

	private static final class OneService implements ServiceObjects<Object> {
		private final Object service;

		OneService(Object service) {
			this.service = service;
		}

		@Override
		public Object getService() {
			return service;
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

	/**
	 * A contract with one operation {@code store(payload)} whose single
	 * argument travels in the body.
	 *
	 * @param modelTyped whether the parameter is typed by an EClass —
	 *        i.e. whether the body is a model or a value
	 */
	private static RestFlavor contract(boolean modelTyped) {
		Parameter payload = F.createParameter();
		payload.setName("payload");
		if (modelTyped) {
			payload.setEType(ServicesPackage.eINSTANCE.getServiceInterface());
		} else {
			payload.setType("string");
		}

		ServiceOperation store = F.createServiceOperation();
		store.setName("store");
		store.getParameters().add(payload);

		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Store");
		contract.getOperations().add(store);

		RestOperationFlavor opFlavor = F.createRestOperationFlavor();
		opFlavor.setName("store");
		opFlavor.setMethod(HttpMethod.POST);
		opFlavor.setPath("/store");
		opFlavor.setOperation(store);
		opFlavor.getProduces().add("text/plain");

		RestParameterBinding binding = F.createRestParameterBinding();
		binding.setParameter(payload);
		binding.setBinding(ParameterBinding.BODY);
		opFlavor.getParameterBindings().add(binding);

		RestFlavor flavor = F.createRestFlavor();
		flavor.setName("store-rest");
		flavor.getOperationFlavors().add(opFlavor);
		return flavor;
	}

	private final Probe probe = new Probe();

	private Response post(boolean modelTyped, String body) {
		RestDispatcher dispatcher = new RestDispatcher(contract(modelTyped),
				() -> new OneService(probe), "Store", new ResourceSetObjects());
		InputStream entity = body == null
				? null
				: new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
		return dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null, entity);
	}

	@Test
	void a_model_typed_body_arrives_as_the_model_it_is() {
		Response response = post(true, """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:ServiceInterface xmlns:services="%s" name="Payment" version="1.0.0"/>
				""".formatted(NS));

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(probe.received)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(ServiceInterface.class))
				.extracting(ServiceInterface::getName).isEqualTo("Payment");
	}

	@Test
	void a_value_typed_body_arrives_as_its_text() {
		Response response = post(false, "just a string");

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(probe.received).isEqualTo("just a string");
	}

	@Test
	void the_answer_carries_the_media_type_the_flavor_declares() {
		assertThat(post(false, "x").getMediaType().toString()).isEqualTo("text/plain");
	}

	@Test
	void a_body_that_is_not_what_the_contract_declares_is_a_400() {
		Response response = post(true, "this is not XMI at all");

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(probe.received).as("nothing may reach the service").isNull();
	}

	@Test
	void an_absent_body_is_a_missing_argument_like_any_other() {
		// The contract declares payload without optional=true, so it is
		// required — and a body is where it was to arrive. Nothing about
		// being the body makes it special here.
		Response response = post(false, null);

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(String.valueOf(response.getEntity())).contains("payload is required");
		assertThat(probe.received).as("nothing may reach the service").isNull();
	}

	@Test
	void a_failure_that_came_back_as_a_value_still_gets_its_status() {
		// The probe returns whatever it is given; here that is a failing
		// Diagnostic, and the flavor binds the error carrying code 201 to
		// a 404. The body stays the Diagnostic — this project answers
		// with one rather than with an HTTP exception.
		RestOperationFlavor opFlavor = (RestOperationFlavor) contract(false).getOperationFlavors().get(0);
		ServiceException declared = F.createServiceException();
		declared.setName("NotFound");
		declared.setType("NotFound");
		IntProperty code = F.createIntProperty();
		code.setName("code");
		code.setValue(201);
		declared.getProperties().add(code);
		RestExceptionBinding binding = F.createRestExceptionBinding();
		binding.setException(declared);
		binding.setStatus(404);
		opFlavor.getExceptionBindings().add(binding);

		Diagnostic failure = F.createDiagnostic();
		failure.setSeverity(DiagnosticSeverity.ERROR);
		failure.setCode(201);

		RestFlavor flavor = F.createRestFlavor();
		flavor.getOperationFlavors().add(opFlavor);
		RestDispatcher dispatcher = new RestDispatcher(flavor,
				() -> new OneService(new Object() {
					@SuppressWarnings("unused")
					public Object store(Object payload) {
						return failure;
					}
				}), "Store", new ResourceSetObjects());

		Response response = dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null,
				new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

		assertThat(response.getStatus()).isEqualTo(404);
		assertThat(response.getEntity()).isSameAs(failure);
		assertThat(response.getHeaderString("ce-dataschema"))
			.as("a body that is a model object says what schema it adheres to: its package")
			.isEqualTo(ServicesPackage.eNS_URI);
	}

	@Test
	void a_request_no_operation_answers_is_a_404() {
		RestDispatcher dispatcher = new RestDispatcher(contract(false),
				() -> new OneService(probe), "Store", new ResourceSetObjects());

		assertThat(dispatcher.dispatch("POST", "/elsewhere", name -> List.of(), name -> null, null).getStatus())
				.isEqualTo(404);
	}

	@Test
	void no_implementation_is_a_503_naming_the_contract() {
		RestDispatcher dispatcher = new RestDispatcher(contract(false), () -> null, "Store",
				new ResourceSetObjects());

		Response response = dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null,
				new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

		assertThat(response.getStatus()).isEqualTo(503);
		assertThat(String.valueOf(response.getEntity())).contains("Store");
	}

	/** Service objects whose service comes and goes as the test says. */
	private static final class Flaky implements ServiceObjects<Object> {
		private final Object service;
		private final RuntimeException onReturn;

		Flaky(Object service, RuntimeException onReturn) {
			this.service = service;
			this.onReturn = onReturn;
		}

		@Override
		public Object getService() {
			return service;
		}

		@Override
		public void ungetService(Object returned) {
			if (onReturn != null) {
				throw onReturn;
			}
		}

		@Override
		public ServiceReference<Object> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	/**
	 * Found, and gone before it could be taken — what a delayed component
	 * being disposed of looks like to the second of two calls. That used
	 * to be a 500 with a NullPointerException for a text.
	 */
	@Test
	void an_implementation_that_left_between_lookup_and_use_is_a_503_too() {
		RestDispatcher dispatcher = new RestDispatcher(contract(false), () -> new Flaky(null, null), "Store",
				new ResourceSetObjects());

		Response response = dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null,
				new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

		assertThat(response.getStatus()).isEqualTo(503);
		assertThat(String.valueOf(response.getEntity())).contains("Store");
	}

	/**
	 * The framework refuses to take back a use it no longer counts. The
	 * call was made and answered; that refusal must not turn its answer
	 * into a failure.
	 */
	@Test
	void an_answer_stands_when_the_implementation_cannot_be_returned() {
		Object stored = new Object() {
			@SuppressWarnings("unused")
			public Object store(Object payload) {
				return "kept";
			}
		};
		RestDispatcher dispatcher = new RestDispatcher(contract(false),
				() -> new Flaky(stored, new IllegalArgumentException()), "Store", new ResourceSetObjects());

		Response response = dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null,
				new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

		assertThat(response.getStatus()).isBetween(200, 299);
	}

	/**
	 * A service that answers later (osgi.async) is waited for: an HTTP
	 * request is answered once, with what the holder ends with.
	 */
	@Test
	void a_service_that_answers_later_is_waited_for() {
		Object later = new Object() {
			@SuppressWarnings("unused")
			public CompletableFuture<Object> store(Object payload) {
				return CompletableFuture.supplyAsync(() -> "kept " + payload);
			}
		};
		RestDispatcher dispatcher = new RestDispatcher(contract(false), () -> new OneService(later), "Store",
				new ResourceSetObjects());

		Response response = dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null,
				new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

		assertThat(response.getStatus()).isBetween(200, 299);
		assertThat(String.valueOf(response.getEntity())).isEqualTo("kept x");
	}

	@Test
	void a_holder_that_fails_answers_as_a_throwing_service_does() {
		Object failing = new Object() {
			@SuppressWarnings("unused")
			public CompletableFuture<Object> store(Object payload) {
				return CompletableFuture.failedFuture(new IllegalStateException("no room"));
			}
		};
		RestDispatcher dispatcher = new RestDispatcher(contract(false), () -> new OneService(failing), "Store",
				new ResourceSetObjects());

		Response response = dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null,
				new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

		assertThat(response.getStatus()).as("an error the contract does not declare").isEqualTo(500);
		assertThat(String.valueOf(response.getEntity())).contains("no room");
	}

	// ---------------------------------------------------------- #219

	private Response post(boolean modelTyped, String body, long maxBodyBytes) {
		RestDispatcher dispatcher = new RestDispatcher(contract(modelTyped),
				() -> new OneService(probe), "Store", new ResourceSetObjects(), CallTracer.NONE, false,
				maxBodyBytes);
		return dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null,
				new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
	}

	private static String paddedInterface(int bytes) {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:ServiceInterface xmlns:services="%s" name="Payment" version="1.0.0" description="%s"/>
				""".formatted(NS, "x".repeat(bytes));
	}

	@Test
	void a_body_over_the_megabyte_is_a_413_where_nobody_says_otherwise() {
		assertThat(post(true, paddedInterface(WireBody.MAX_BYTES + 1), 0).getStatus()).isEqualTo(413);
		assertThat(probe.received).as("refused before the operation saw anything").isNull();
	}

	@Test
	void a_deployment_that_raises_the_limit_takes_the_larger_body() {
		Response response = post(true, paddedInterface(WireBody.MAX_BYTES + 1), 4L * WireBody.MAX_BYTES);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(probe.received).isInstanceOf(ServiceInterface.class);
	}

	@Test
	void a_deployment_that_lowers_the_limit_refuses_what_the_default_would_take() {
		assertThat(post(true, paddedInterface(4096), 1024).getStatus()).isEqualTo(413);
	}

	@Test
	void a_body_that_is_text_is_held_to_the_same_limit() {
		assertThat(post(false, "x".repeat(2048), 1024).getStatus()).isEqualTo(413);
		assertThat(post(false, "x".repeat(512), 1024).getStatus()).isEqualTo(200);
	}
}
