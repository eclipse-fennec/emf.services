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

package org.eclipse.fennec.services.broker.core.internal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.DdsrBroker;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.broker.core.LookupBackend;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi DS adapter that exposes a {@link DdsrBrokerImpl} as a
 * {@link DdsrBroker} service. Thin wrapper: configuration reading
 * and an optional pluggable {@link LookupBackend} binding live here;
 * the actual logic lives in {@link DdsrBrokerImpl}, which has no
 * OSGi imports.
 *
 * <p>If no external {@code LookupBackend} is registered (e.g. the
 * Lucene module is not installed), an {@link InMemoryLookupBackend} is
 * used as the default.
 */
@Component(
		service = { DdsrBroker.class, BrokerCatalog.class, BrokerImplementations.class, BrokerLookup.class },
		configurationPid = "org.eclipse.fennec.services.broker.core",
		immediate = true)
@Designate(ocd = DdsrBrokerComponent.Config.class)
public final class DdsrBrokerComponent implements DdsrBroker {

	private static final Logger LOG = Logger.getLogger(DdsrBrokerComponent.class.getName());

	@ObjectClassDefinition(name = "DDSR Broker Core", description = "Configuration for the in-memory DDSR broker")
	public @interface Config {

		@AttributeDefinition(
				name = "Snapshot path",
				description = "File system path of the XMI snapshot. Persisted synchronously after each mutation.",
				required = false)
		String snapshot_path() default "./broker-state.xmi";
	}

	@Reference(
			cardinality = ReferenceCardinality.OPTIONAL,
			policy = ReferencePolicy.DYNAMIC,
			policyOption = ReferencePolicyOption.GREEDY)
	private volatile LookupBackend externalLookup;

	/**
	 * Event sinks as a whiteboard: every registered {@link EventSink}
	 * service gets the broker's lifecycle events. That is what keeps the
	 * transport open-ended — the SSE endpoint registers one, an MQTT
	 * bridge would register another, and neither requires a change here
	 * or in {@link DdsrBrokerImpl}.
	 */
	private final List<EventSink> eventSinks = new CopyOnWriteArrayList<>();

	/**
	 * Written by the activation thread, read by request threads —
	 * volatile provides the happens-before edge. {@link #required()}
	 * turns the activation/deactivation window into a defined failure
	 * instead of an NPE.
	 */
	private volatile DdsrBrokerImpl delegate;

	private DdsrBrokerImpl required() {
		DdsrBrokerImpl current = delegate;
		if (current == null) {
			throw new IllegalStateException("DDSR broker is not active (component starting or stopping)");
		}
		return current;
	}

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC)
	void addEventSink(EventSink sink) {
		eventSinks.add(sink);
	}

	void removeEventSink(EventSink sink) {
		eventSinks.remove(sink);
	}

	/**
	 * Fans one event out to whatever is currently registered. Passed to
	 * the broker as a stable sink so subscribers can come and go without
	 * the broker knowing.
	 * <p>
	 * Each sink is isolated: one that throws — against its contract —
	 * must not stop the others from being served.
	 */
	private void fanOut(ServiceEvent event) {
		for (EventSink sink : eventSinks) {
			try {
				sink.publish(event);
			} catch (RuntimeException sinkFailure) {
				LOG.warning("[DDSR] event sink failed, continuing: " + sinkFailure);
			}
		}
	}

	@Activate
	void activate(Config config) {
		try {
			Path snapshotPath = Paths.get(config.snapshot_path());
			LookupBackend backend = externalLookup != null ? externalLookup : new InMemoryLookupBackend();
			this.delegate = new DdsrBrokerImpl(snapshotPath, backend, this::fanOut);
			LOG.info("[DDSR] BrokerCore activated, snapshot=" + snapshotPath.toAbsolutePath());
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR] BrokerCore activation FAILED", t);
			throw t;
		}
	}

	@Deactivate
	void deactivate() {
		if (delegate != null) {
			// Best-effort final snapshot — already persisted after every
			// mutation, but defensive in case mutations happened during
			// in-flight shutdown.
			delegate.snapshot();
		}
		delegate = null;
	}

	// --- Delegation -------------------------------------------------------

	@Override
	public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return required().publishImplementation(provider, implementation);
	}

	@Override
	public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return required().withdrawImplementation(provider, implementation);
	}

	@Override
	public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
		return required().registerService(provider, implementation);
	}

	@Override
	public ServiceReference getServiceReference(String interfaceName) {
		return required().getServiceReference(interfaceName);
	}

	@Override
	public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		return required().getServiceReferences(interfaceName, filter, capability);
	}

	@Override
	public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		return required().getAllServiceReferences(interfaceName, filter, capability);
	}

	@Override
	public Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return required().addCatalogEntry(serviceInterface, requestor);
	}

	@Override
	public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return required().deprecateCatalogEntry(serviceInterface, requestor);
	}

	@Override
	public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return required().removeCatalogEntry(serviceInterface, requestor);
	}

	@Override
	public RemoteServiceRegistry getRegistry() {
		return required().getRegistry();
	}

	@Override
	public Diagnostic snapshot() {
		return required().snapshot();
	}

	@Override
	public ServiceImplementation getImplementationForReference(ServiceReference reference) {
		return required().getImplementationForReference(reference);
	}
}
