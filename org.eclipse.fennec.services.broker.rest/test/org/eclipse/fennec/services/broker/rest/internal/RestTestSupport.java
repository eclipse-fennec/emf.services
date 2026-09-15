/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.broker.rest.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttQos;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;

/** Shared fakes and fixtures of the REST layer tests (#56). Plain JUnit, no OSGi runtime. */
final class RestTestSupport {

	private RestTestSupport() {
	}

	/** Stand-in for the emf.osgi prototype ResourceSet service. */
	static final class ResourceSets implements ComponentServiceObjects<ResourceSet> {
		int outstanding;

		@Override
		public ResourceSet getService() {
			outstanding++;
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			rs.getPackageRegistry().put(ServicesPackage.eNS_URI, ServicesPackage.eINSTANCE);
			return rs;
		}

		@Override
		public void ungetService(ResourceSet service) {
			outstanding--;
		}

		@Override
		public org.osgi.framework.ServiceReference<ResourceSet> getServiceReference() {
			throw new UnsupportedOperationException("not needed");
		}
	}

	/** A lookup that resolves references to implementations from a map and records queries. */
	static final class FakeLookup implements BrokerLookup {
		final Map<String, ServiceImplementation> implByReferenceId = new LinkedHashMap<>();
		final List<ServiceReference> results = new ArrayList<>();
		String lastInterface;
		String lastFilter;
		ConsumerCapability lastCapability;

		void resolves(ServiceReference reference, ServiceImplementation implementation) {
			implByReferenceId.put(reference.getId(), implementation);
		}

		@Override
		public ServiceReference getServiceReference(String interfaceName) {
			return results.isEmpty() ? null : results.get(0);
		}

		@Override
		public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
				ConsumerCapability capability) {
			lastInterface = interfaceName;
			lastFilter = filter;
			lastCapability = capability;
			return new ArrayList<>(results);
		}

		@Override
		public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
				ConsumerCapability capability) {
			return getServiceReferences(interfaceName, filter, capability);
		}

		@Override
		public ServiceImplementation getImplementationForReference(ServiceReference reference) {
			return reference == null ? null : implByReferenceId.get(reference.getId());
		}
	}

	// ------------------------------------------------------------------
	// Model fixtures
	// ------------------------------------------------------------------

	static ServiceInterface payment() {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName("Payment");
		si.setVersion("1.0.0");
		ServiceOperation charge = ServicesFactory.eINSTANCE.createServiceOperation();
		charge.setName("charge");
		si.getOperations().add(charge);
		return si;
	}

	/** Provider with one implementation of the interface, speaking the given flavors (none = no flavor). */
	static ServiceProvider provider(String name, ServiceInterface si, FlavorKind... flavors) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(name);
		provider.setVersion("1.0.0");
		provider.setSymbolicName("org.example." + name);
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(name + "-impl");
		impl.setVersion("1.0.0");
		impl.setImplementationId("org.example.Impl");
		if (si != null) {
			impl.getServiceInterfaces().add(si);
		}
		for (FlavorKind kind : flavors) {
			ServiceFlavor flavor;
			if (kind == FlavorKind.MQTT) {
				MqttFlavor mqtt = ServicesFactory.eINSTANCE.createMqttFlavor();
				mqtt.getBrokers().add("tcp://localhost:1883");
				mqtt.setRequestTopic("payments/req");
				mqtt.setDefaultQos(MqttQos.AT_LEAST_ONCE);
				flavor = mqtt;
			} else {
				RestFlavor rest = ServicesFactory.eINSTANCE.createRestFlavor();
				rest.setHost("http://localhost:9091");
				rest.setBasePath("/payments");
				flavor = rest;
			}
			flavor.setName(kind.getName().toLowerCase());
			flavor.setKind(kind);
			impl.getFlavors().add(flavor);
		}
		provider.getImplementations().add(impl);
		return provider;
	}

	/** A live-looking reference/registration pair for the provider's sole implementation. */
	static ServiceReference reference(String id, ServiceProvider provider) {
		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId(id);
		ref.setProvider(provider);
		ServiceRegistration reg = ServicesFactory.eINSTANCE.createServiceRegistration();
		reg.setProvider(provider);
		reg.setImplementation(provider.getImplementations().get(0));
		reg.setReference(ref);
		ref.setRegistration(reg);
		return ref;
	}

	static ServiceEvent event(ServiceEventType type, ServiceReference reference, String reason) {
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(type);
		event.setReference(reference);
		event.setReasonCode(reason);
		return event;
	}

	static Diagnostic diagnostic(DiagnosticSeverity severity, int code, String message) {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(severity);
		d.setCode(code);
		d.setMessage(message);
		return d;
	}

	// ------------------------------------------------------------------
	// Wire helpers
	// ------------------------------------------------------------------

	/** Serializes the roots the way a client does and hands them back as a request body. */
	static InputStream body(ComponentServiceObjects<ResourceSet> rsObjects, EObject... roots) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, rsObjects, Arrays.asList(roots));
		return new ByteArrayInputStream(out.toByteArray());
	}

	static String xml(ComponentServiceObjects<ResourceSet> rsObjects, XmiBundle bundle) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, rsObjects, bundle.roots());
		return out.toString(StandardCharsets.UTF_8);
	}

	static InputStream bytes(String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}
}
