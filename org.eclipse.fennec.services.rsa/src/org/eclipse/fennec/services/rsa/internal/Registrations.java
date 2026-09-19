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

package org.eclipse.fennec.services.rsa.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * What this admin has exported and imported, and who gets told.
 *
 * <p>The admin service itself is bundle-scoped — the specification wants
 * a bundle's registrations closed when it lets go of the service, and
 * one instance per bundle is how that is known. But
 * {@code getExportedServices()} answers for the whole implementation,
 * and an endpoint exported by two bundles is one endpoint. So the
 * bookkeeping lives here, once, and every instance reads it.
 *
 * <p>Every change is an event: to each {@link RemoteServiceAdminListener}
 * directly, and to Event Admin under
 * {@code org/osgi/service/remoteserviceadmin/<TYPE>} when there is one
 * (122.7). A listener that throws is logged and skipped — the others
 * still hear.
 */
@Component(service = Registrations.class)
public class Registrations {

	private static final Logger LOG = Logger.getLogger(Registrations.class.getName());

	private static final String TOPIC_PREFIX = "org/osgi/service/remoteserviceadmin/";

	// Listeners and Event Admin are the audience, not the setup: anyone
	// may register a listener at any time, and Event Admin is there or it
	// is not. That is what dynamic is for — and bound through methods, so
	// a change is something this component is told about rather than
	// something it reads out of a field and hopes is current.
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addListener(RemoteServiceAdminListener listener) {
		listeners.add(listener);
	}

	void removeListener(RemoteServiceAdminListener listener) {
		listeners.remove(listener);
	}

	private final List<RemoteServiceAdminListener> listeners = new CopyOnWriteArrayList<>();

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	void unsetEventAdmin(EventAdmin eventAdmin) {
		if (this.eventAdmin == eventAdmin) {
			this.eventAdmin = null;
		}
	}

	private volatile EventAdmin eventAdmin;

	private final List<ExportedService> exported = new CopyOnWriteArrayList<>();

	private final List<ImportedService> imported = new CopyOnWriteArrayList<>();

	/** The endpoints that are up, by what they were exported as. */
	private final Map<ExportKey, LiveExport> live = new LinkedHashMap<>();

	private Bundle self;

	/** What makes two exports the same endpoint. */
	record ExportKey(ServiceReference<?> reference, String exportedAs, String configType) {
	}

	@Activate
	void activate(BundleContext context) {
		self = context.getBundle();
	}

	/**
	 * The endpoint for {@code key}: the one already up, or a new one from
	 * {@code open}. Serialised, so two exports racing for the same key
	 * cannot both mount. {@code open} failing leaves nothing behind.
	 */
	LiveExport acquire(ExportKey key, Supplier<LiveExport> open) {
		synchronized (live) {
			LiveExport existing = live.get(key);
			if (existing != null) {
				existing.join();
				return existing;
			}
			LiveExport fresh = open.get();
			fresh.whenLast(() -> {
				synchronized (live) {
					live.remove(key, fresh);
				}
			});
			live.put(key, fresh);
			return fresh;
		}
	}

	Collection<ExportReference> exports() {
		return List.copyOf(exported);
	}

	Collection<ImportReference> imports() {
		return List.copyOf(imported);
	}

	void exported(ExportedService registration) {
		exported.add(registration);
		fire(new RemoteServiceAdminEvent(RemoteServiceAdminEvent.EXPORT_REGISTRATION, self, registration, null),
				registration.description());
	}

	void exportClosed(ExportedService registration) {
		if (exported.remove(registration)) {
			fire(new RemoteServiceAdminEvent(RemoteServiceAdminEvent.EXPORT_UNREGISTRATION, self, registration,
					null), registration.description());
		}
	}

	void exportUpdated(ExportedService registration) {
		fire(new RemoteServiceAdminEvent(RemoteServiceAdminEvent.EXPORT_UPDATE, self, registration, null),
				registration.description());
	}

	void importUpdated(ImportedService registration) {
		fire(new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_UPDATE, self, registration, null),
				registration.description());
	}

	void exportFailed(ExportedService registration, Throwable failure) {
		fire(new RemoteServiceAdminEvent(RemoteServiceAdminEvent.EXPORT_ERROR, self, registration, failure), null);
	}

	void imported(ImportedService registration) {
		imported.add(registration);
		fire(new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_REGISTRATION, self, registration, null),
				registration.description());
	}

	void importClosed(ImportedService registration) {
		if (imported.remove(registration)) {
			fire(new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_UNREGISTRATION, self, registration,
					null), registration.description());
		}
	}

	private void fire(RemoteServiceAdminEvent event, EndpointDescription endpoint) {
		for (RemoteServiceAdminListener listener : listeners) {
			try {
				listener.remoteAdminEvent(event);
			} catch (RuntimeException failure) {
				LOG.log(Level.WARNING, "[DDSR] a RemoteServiceAdminListener threw on " + topicOf(event.getType()),
						failure);
			}
		}
		EventAdmin admin = eventAdmin;
		if (admin != null) {
			admin.postEvent(asEvent(event, endpoint));
		}
	}

	/** The Event Admin form of an RSA event, with the properties 122.7 lists. */
	private Event asEvent(RemoteServiceAdminEvent event, EndpointDescription endpoint) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("bundle", self);
		properties.put("bundle.id", self.getBundleId());
		properties.put("bundle.symbolicname", self.getSymbolicName());
		properties.put("bundle.version", self.getVersion());
		properties.put("event", event);
		properties.put("timestamp", System.currentTimeMillis());
		Throwable cause = event.getException();
		if (cause != null) {
			properties.put("cause", cause);
			properties.put("exception", cause);
			properties.put("exception.class", cause.getClass().getName());
			if (cause.getMessage() != null) {
				properties.put("exception.message", cause.getMessage());
			}
		}
		if (endpoint != null) {
			properties.put(RemoteConstants.ENDPOINT_SERVICE_ID, endpoint.getServiceId());
			properties.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, endpoint.getFrameworkUUID());
			properties.put(RemoteConstants.ENDPOINT_ID, endpoint.getId());
			properties.put(Constants.OBJECTCLASS, endpoint.getInterfaces().toArray(String[]::new));
			properties.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS,
					endpoint.getConfigurationTypes().toArray(String[]::new));
		}
		return new Event(TOPIC_PREFIX + topicOf(event.getType()), properties);
	}

	private static String topicOf(int type) {
		return switch (type) {
		case RemoteServiceAdminEvent.IMPORT_REGISTRATION -> "IMPORT_REGISTRATION";
		case RemoteServiceAdminEvent.EXPORT_REGISTRATION -> "EXPORT_REGISTRATION";
		case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION -> "EXPORT_UNREGISTRATION";
		case RemoteServiceAdminEvent.IMPORT_UNREGISTRATION -> "IMPORT_UNREGISTRATION";
		case RemoteServiceAdminEvent.IMPORT_ERROR -> "IMPORT_ERROR";
		case RemoteServiceAdminEvent.EXPORT_ERROR -> "EXPORT_ERROR";
		case RemoteServiceAdminEvent.EXPORT_WARNING -> "EXPORT_WARNING";
		case RemoteServiceAdminEvent.IMPORT_WARNING -> "IMPORT_WARNING";
		case RemoteServiceAdminEvent.IMPORT_UPDATE -> "IMPORT_UPDATE";
		case RemoteServiceAdminEvent.EXPORT_UPDATE -> "EXPORT_UPDATE";
		default -> "UNKNOWN_" + type;
		};
	}
}
