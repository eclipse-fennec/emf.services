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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.Delivery;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.flavor.rest.RestStreams;
import org.eclipse.fennec.services.invocation.Flows;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/** An operation that streams is answered as Server-Sent Events (#252). */
class RestDispatcherStreamTest {

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

	/** Counts what was taken and returned, so a test can see the service held for the stream. */
	private static final class Counted implements ServiceObjects<Object> {

		final Object service;

		final AtomicInteger out = new AtomicInteger();

		Counted(Object service) {
			this.service = service;
		}

		@Override
		public Object getService() {
			out.incrementAndGet();
			return service;
		}

		@Override
		public void ungetService(Object returned) {
			out.decrementAndGet();
		}

		@Override
		public ServiceReference<Object> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	public static final class Sensor {

		final AtomicBoolean cancelled = new AtomicBoolean();

		public Flow.Publisher<Double> readings() {
			return subscriber -> {
				SubmissionPublisher<Double> values = new SubmissionPublisher<>(Runnable::run, 8);
				values.subscribe(new Flow.Subscriber<>() {

					@Override
					public void onSubscribe(Flow.Subscription subscription) {
						subscriber.onSubscribe(new Flow.Subscription() {

							@Override
							public void request(long n) {
								subscription.request(n);
							}

							@Override
							public void cancel() {
								cancelled.set(true);
								subscription.cancel();
							}
						});
					}

					@Override
					public void onNext(Double item) {
						subscriber.onNext(item);
					}

					@Override
					public void onError(Throwable throwable) {
						subscriber.onError(throwable);
					}

					@Override
					public void onComplete() {
						subscriber.onComplete();
					}
				});
				new Thread(() -> {
					for (double value : new double[] { 1.5, 2.5, 3.5 }) {
						values.submit(value);
					}
					values.close();
				}).start();
			};
		}

		public Flow.Publisher<Double> broken() {
			return Flows.failed(new IllegalStateException("the sensor is gone"));
		}
	}

	private static RestFlavor flavor(String operationName) {
		Parameter result = F.createParameter();
		result.setName("result");
		result.setType("double");
		result.setUpperBound(-1);
		result.setDelivery(Delivery.STREAM);
		ServiceOperation operation = F.createServiceOperation();
		operation.setName(operationName);
		operation.setReturnValue(result);
		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Sensor");
		contract.getOperations().add(operation);
		RestOperationFlavor operationFlavor = F.createRestOperationFlavor();
		operationFlavor.setName(operationName);
		operationFlavor.setMethod(HttpMethod.GET);
		operationFlavor.setPath("/" + operationName);
		operationFlavor.setOperation(operation);
		RestFlavor flavor = F.createRestFlavor();
		flavor.setName("sensor-rest");
		flavor.getOperationFlavors().add(operationFlavor);
		return flavor;
	}

	private static Response call(String operationName, Counted service) {
		RestDispatcher dispatcher = new RestDispatcher(flavor(operationName), () -> service, "Sensor",
				new ResourceSetObjects());
		return dispatcher.dispatch("GET", "/" + operationName, name -> List.of(), name -> null, null);
	}

	@Test
	void everyValueIsAnEventAndTheStreamSaysItIsOver() throws IOException {
		Counted service = new Counted(new Sensor());
		Response response = call("readings", service);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getMediaType().toString()).isEqualTo(RestStreams.MEDIA_TYPE);
		assertThat(service.out).as("held for as long as the stream lasts").hasValue(1);

		ByteArrayOutputStream body = new ByteArrayOutputStream();
		((StreamingOutput) response.getEntity()).write(body);
		String events = body.toString(UTF_8);

		assertThat(events.split("event: value", -1)).hasSize(4);
		assertThat(events).contains("1.5").contains("2.5").contains("3.5");
		assertThat(events.indexOf("event: end")).isGreaterThan(events.lastIndexOf("event: value"));
		assertThat(service.out).as("returned once it is over").hasValue(0);
	}

	@Test
	void aFailureIsTheLastEventAndCarriesWhy() throws IOException {
		Counted service = new Counted(new Sensor());
		Response response = call("broken", service);

		ByteArrayOutputStream body = new ByteArrayOutputStream();
		((StreamingOutput) response.getEntity()).write(body);
		String events = body.toString(UTF_8);

		assertThat(events).startsWith(": connected").contains("event: error").contains("the sensor is gone")
				.doesNotContain("event: end");
		assertThat(service.out).hasValue(0);
	}

	@Test
	void aCallerThatGoesAwayCancelsTheServicesStream() throws IOException {
		Sensor sensor = new Sensor();
		Counted service = new Counted(sensor);
		Response response = call("readings", service);

		OutputStream gone = new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				throw new IOException("connection reset");
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				throw new IOException("connection reset");
			}
		};
		((StreamingOutput) response.getEntity()).write(gone);

		assertThat(sensor.cancelled).isTrue();
		assertThat(service.out).hasValue(0);
	}
}
