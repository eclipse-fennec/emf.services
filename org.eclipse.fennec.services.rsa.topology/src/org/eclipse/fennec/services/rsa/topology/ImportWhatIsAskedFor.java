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

package org.eclipse.fennec.services.rsa.topology;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.rsa.spi.RsaProperties;
import org.eclipse.fennec.services.rsa.spi.ServiceDiscovery;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Imports what somebody in this framework is waiting for.
 *
 * <p>The framework already knows who wants what: every service listener
 * carries a filter, and a {@link ListenerHook} sees them all. An
 * interface that appears in one is an interface a bundle would bind to
 * if it existed — so discovery is asked for its contract, and whatever
 * turns up is imported. Nothing is imported that nobody asked for, and
 * nothing has to be configured to say who is interested. This is the
 * mechanism the specification describes, and the one Aries uses.
 *
 * <p>The contract's name is derived from the interface the way the
 * export side derives it from a service: the simple name, unless the
 * listener's filter also names {@code ddsr.contract}.
 */
@Component(service = ListenerHook.class, immediate = true, configurationPid = TopologyPolicy.PID,
		configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class ImportWhatIsAskedFor implements ListenerHook {

	private static final Logger LOG = Logger.getLogger(ImportWhatIsAskedFor.class.getName());

	/** {@code (objectClass=some.Interface)} inside a filter, however nested. */
	private static final Pattern OBJECT_CLASS = Pattern.compile("\\(" + Constants.OBJECTCLASS + "=([^)*]+)\\)");

	/** {@code (ddsr.contract=Name)} inside a filter, when a listener says so. */
	private static final Pattern CONTRACT = Pattern.compile("\\(" + RsaProperties.CONTRACT + "=([^)*]+)\\)");

	/**
	 * Interfaces that are never remote, however many listeners wait for
	 * them. Framework and specification services are this framework's
	 * own by definition, and the broker's own API is what discovery runs
	 * over — importing a proxy of it beside the client's HTTP proxy would
	 * give the SDK two of everything. The first run of this component
	 * did exactly that.
	 */
	private static final List<String> NEVER_REMOTE = List.of(
			"org.osgi.",
			"org.apache.felix.",
			"org.eclipse.fennec.services.broker.core.",
			"org.eclipse.fennec.services.client.",
			"org.eclipse.fennec.services.rsa.");

	@Reference
	private RemoteServiceAdmin rsa;

	@Reference
	private ServiceDiscovery discovery;

	/** Package-private: tests wire the two collaborators by hand. */
	ImportWhatIsAskedFor with(RemoteServiceAdmin admin, ServiceDiscovery watching) {
		this.rsa = admin;
		this.discovery = watching;
		return this;
	}

	/** One watch per interface asked for, however many listeners ask. */
	private final Map<String, Watch> watches = new ConcurrentHashMap<>();

	private volatile boolean manual;

	@Activate
	void activate(TopologyPolicy policy) {
		apply(policy);
	}

	/** See the export side: a configuration update is taken, not died of (#107). */
	@Modified
	void modified(TopologyPolicy policy) {
		apply(policy);
	}

	private void apply(TopologyPolicy policy) {
		manual = TopologyPolicy.MANUAL.equals(policy.policy());
		if (manual) {
			LOG.info("[DDSR] topology policy is manual — nothing is imported on its own");
			// Whatever is already watched stays: dropping it would
			// unregister proxies a consumer is using, and manual means
			// "start nothing new", not "take away what runs".
		}
	}

	@Override
	public void added(Collection<ListenerInfo> listeners) {
		if (manual) {
			return;
		}
		for (ListenerInfo listener : listeners) {
			if (listener.isRemoved() || listener.getFilter() == null) {
				continue;
			}
			if (listener.getBundleContext() != null && listener.getBundleContext().getBundle().getBundleId() == 0) {
				// The system bundle's listeners are the framework's own.
				continue;
			}
			Matcher interfaces = OBJECT_CLASS.matcher(listener.getFilter());
			while (interfaces.find()) {
				String interfaceName = interfaces.group(1);
				if (NEVER_REMOTE.stream().anyMatch(interfaceName::startsWith)) {
					continue;
				}
				watches.computeIfAbsent(interfaceName,
						name -> new Watch(name, contractNameOf(listener.getFilter(), name))).listeners++;
			}
		}
	}

	@Override
	public void removed(Collection<ListenerInfo> listeners) {
		for (ListenerInfo listener : listeners) {
			if (listener.getFilter() == null) {
				continue;
			}
			Matcher interfaces = OBJECT_CLASS.matcher(listener.getFilter());
			while (interfaces.find()) {
				Watch watch = watches.get(interfaces.group(1));
				if (watch != null && --watch.listeners <= 0) {
					// Nobody is waiting for it any more. What was imported
					// for them goes too — an imported service without a
					// consumer is only a proxy that will never be called.
					watches.remove(watch.interfaceName);
					watch.close();
				}
			}
		}
	}

	@Deactivate
	void deactivate() {
		watches.values().forEach(Watch::close);
		watches.clear();
	}

	private static String contractNameOf(String filter, String interfaceName) {
		Matcher stated = CONTRACT.matcher(filter);
		if (stated.find()) {
			return stated.group(1);
		}
		int lastDot = interfaceName.lastIndexOf('.');
		return lastDot < 0 ? interfaceName : interfaceName.substring(lastDot + 1);
	}

	/**
	 * Everything that is imported because one interface was asked for:
	 * the discovery watch, and one import per registration it reported.
	 */
	private final class Watch implements ServiceDiscovery.DiscoveryListener {

		final String interfaceName;
		final String contractName;
		int listeners;
		private final Map<String, ImportRegistration> imports = new ConcurrentHashMap<>();
		private final AutoCloseable subscription;

		Watch(String interfaceName, String contractName) {
			this.interfaceName = interfaceName;
			this.contractName = contractName;
			LOG.info("[DDSR] " + interfaceName + " is wanted here — watching for " + contractName);
			this.subscription = discovery.watch(contractName, this);
		}

		@Override
		public void appeared(String referenceId, ServiceImplementation implementation) {
			if (imports.containsKey(referenceId)) {
				return;
			}
			ImportRegistration imported = rsa.importService(describe(referenceId, implementation));
			if (imported != null) {
				imports.put(referenceId, imported);
			}
		}

		@Override
		public void changed(String referenceId, ServiceImplementation implementation) {
			ImportRegistration imported = imports.get(referenceId);
			if (imported != null && !imported.update(describe(referenceId, implementation))) {
				// Update is not implemented yet (#96). Re-importing here
				// would make the service flicker for its consumers; the
				// locator underneath already follows the change on its
				// own, so the proxy keeps working. Nothing to do.
				LOG.fine(() -> "[DDSR] " + referenceId + " changed; the proxy follows it through its locator");
			}
		}

		@Override
		public void gone(String referenceId) {
			ImportRegistration imported = imports.remove(referenceId);
			if (imported != null) {
				imported.close();
				LOG.info("[DDSR] " + referenceId + " is gone; its import is closed");
			}
		}

		/**
		 * The endpoint as the RSA core reads it. Built here, on the
		 * consumer side, from what discovery knows — which is the model,
		 * not the exporter's service reference. The interface name is
		 * the one that was asked for: that is the only place it exists
		 * on this side, and the reason a listener's filter is where an
		 * import starts.
		 */
		private EndpointDescription describe(String referenceId, ServiceImplementation implementation) {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put(Constants.OBJECTCLASS, new String[] { interfaceName });
			properties.put(RemoteConstants.ENDPOINT_ID, referenceId);
			properties.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[] { configTypeOf(implementation) });
			properties.put(RsaProperties.CONTRACT, contractName);
			if (implementation.getImplementationId() != null) {
				properties.put(RsaProperties.IMPLEMENTATION, implementation.getImplementationId());
			}
			return new EndpointDescription(properties);
		}

		private static String configTypeOf(ServiceImplementation implementation) {
			// The flavor kind, lower-cased under our prefix: fennec.rest,
			// fennec.mqtt. Same spelling the distribution providers use.
			return implementation.getFlavors().isEmpty() || implementation.getFlavors().get(0).getKind() == null
					? "fennec.rest"
					: "fennec." + implementation.getFlavors().get(0).getKind().getLiteral().toLowerCase();
		}

		void close() {
			imports.values().forEach(ImportRegistration::close);
			imports.clear();
			try {
				subscription.close();
			} catch (Exception failure) {
				LOG.log(Level.WARNING, "[DDSR] stopping the watch for " + contractName + " failed", failure);
			}
		}
	}

	/** For tests: what is currently watched. */
	List<String> watched() {
		return List.copyOf(watches.keySet());
	}
}
