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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.ContractAddressing;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;

/**
 * Registrations nobody has looked at for a while, parked in a file
 * each (ACQUISITION.md §10).
 *
 * <p>Optional, and off by default. Cold is not the same as gone: a
 * lookup on one of a parked entry's interface names brings it back
 * through the ordinary publish path, which is why this class borrows
 * that path rather than owning a second way to register something.
 *
 * <p>That borrowing is also the one place where the broker calls into
 * itself while holding the write lock. It works because the lock is
 * reentrant and belongs to the thread, so splitting the two apart
 * changed nothing about it.
 */
final class ColdCache {

	private static final Logger LOG = Logger.getLogger(ColdCache.class.getName());

	private final BrokerState state;

	private final Announcements announcements;

	private final Retirement retirement;

	private final Republication republication;

	private final UpdatePolicies policies;

	ColdCache(BrokerState state, Announcements announcements, Retirement retirement,
			Republication republication, UpdatePolicies policies) {
		this.state = state;
		this.announcements = announcements;
		this.retirement = retirement;
		this.republication = republication;
		this.policies = policies;
	}

	/**
	 * Remembers that this registration is alive now, so the idle rule has
	 * something to measure from.
	 */
	void touch(ServiceRegistration registration) {
		registrationSince.put(registration, Instant.now());
	}

	/** Forgets a registration that is going away for good. */
	void forget(ServiceRegistration registration) {
		registrationSince.remove(registration);
	}

	/**
	 * Drops the parked twin of an identity that is publishing again, file
	 * and all.
	 */
	void dropTwin(String providerName, String implementationName, String implementationVersion) {
		ColdEntry twin = coldEntries.remove(coldKey(providerName, implementationName, implementationVersion));
		if (twin == null) {
			return;
		}
		try {
			Files.deleteIfExists(twin.file());
		} catch (IOException cleanupFailure) {
			LOG.warning("[DDSR] could not delete superseded cold file " + twin.file());
		}
	}

	/**
	 * The parked implementation that would still need this contract, or
	 * {@code null}.
	 *
	 * <p>A parked entry counts as live when a catalog entry is being
	 * removed: it is still discoverable, and it would fail to come back
	 * without its contract.
	 */
	String parkedImplementationNeeding(String addressingFingerprint) {
		for (ColdEntry entry : coldEntries.values()) {
			if (entry.addressingFingerprints().contains(addressingFingerprint)) {
				return entry.implementationId();
			}
		}
		return null;
	}

	/**
	 * Cold cache (ACQUISITION.md §10, optional policy — default off):
	 * registrations that held no lease and saw no lookup on their
	 * interfaces for the configured duration are moved out of the hot
	 * registry into one self-contained XMI file each (wire convention:
	 * provider root + full contract siblings), leaving this in-memory
	 * stub behind. Cold is NOT undiscoverable: a lookup on one of the
	 * stub's interface names lazily rehydrates the entry through the
	 * regular publish path. Keyed by provider/impl identity, guarded by
	 * the broker lock; rebuilt from the cold directory on restart.
	 */
	private final Map<String, ColdEntry> coldEntries = new LinkedHashMap<>();

	/** Last lookup instant per interface name — feeds the cold-idle rule. */
	private final Map<String, Instant> lastLookupByInterface = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * When a registration was last known active (published, rehydrated,
	 * or holding a lease at sweep time) — the other half of the
	 * cold-idle rule. Identity-keyed runtime state, pruned each sweep.
	 */
	private final Map<ServiceRegistration, Instant> registrationSince = new IdentityHashMap<>();

	/** In-memory remainder of a coldified registration. */
	private record ColdEntry(String key, List<String> interfaceNames,
			List<String> addressingFingerprints, String implementationId,
			String providerName, Path file) {
	}


	/**
	 * Moves every registration cold that has been idle since before the
	 * cutoff: no lease at sweep time (a lease at sweep time restarts its
	 * idle clock), published/rehydrated before the cutoff, and no lookup
	 * on any of its interface names since the cutoff. A coldified
	 * registration announces {@code UNREGISTERING} (its reference id
	 * becomes invalid — the OSGi lifecycle promise holds), but stays
	 * discoverable through its stub: the next lookup rehydrates it via
	 * the regular publish path, announcing {@code REGISTERED} with a
	 * fresh reference. Returns the number of registrations moved.
	 */
	int coldifyIdle(Instant cutoff) {
		if (cutoff == null) {
			return 0;
		}
		state.writeLock().lock();
		try {
			Instant now = Instant.now();
			registrationSince.keySet().retainAll(new java.util.HashSet<>(state.registrations()));
			int moved = 0;
			for (ServiceRegistration reg : new ArrayList<>(state.registrations())) {
				if (reg.isUnregistered() || reg.getImplementation() == null || reg.getProvider() == null) {
					continue;
				}
				if (policies.isPartyOfSupersession(reg)) {
					// The policy sweep owns the predecessor's fate, and the
					// successor must stay hot for consumers migrating to it.
					continue;
				}
				Instant since = registrationSince.computeIfAbsent(reg, r -> now);
				if (!reg.getUsingSessions().isEmpty()) {
					registrationSince.put(reg, now);
					continue;
				}
				if (!since.isBefore(cutoff) || anyInterfaceLookedUpSince(reg.getImplementation(), cutoff)) {
					continue;
				}
				if (moveCold(reg)) {
					moved++;
				}
			}
			return moved;
		} finally {
			state.writeLock().unlock();
		}
	}
	/** Number of cold entries currently parked on disk. */
	int coldCount() {
		state.readLock().lock();
		try {
			return coldEntries.size();
		} finally {
			state.readLock().unlock();
		}
	}
	boolean anyInterfaceLookedUpSince(ServiceImplementation impl, Instant cutoff) {
		for (ServiceInterface si : impl.getServiceInterfaces()) {
			Instant last = lastLookupByInterface.get(si.getName());
			if (last != null && !last.isBefore(cutoff)) {
				return true;
			}
		}
		return false;
	}
	/** Serialize provider stub + impl + full contracts, retire the hot entry, keep the stub. */
	boolean moveCold(ServiceRegistration reg) {
		ServiceImplementation impl = reg.getImplementation();
		ServiceProvider provider = reg.getProvider();
		String key = coldKey(provider.getName(), impl.getName(), impl.getVersion());

		List<String> names = new ArrayList<>();
		List<String> fingerprints = new ArrayList<>();
		EcoreUtil.Copier copier = new EcoreUtil.Copier();
		ServiceImplementation implCopy = (ServiceImplementation) copier.copy(impl);
		List<ServiceInterface> contractCopies = new ArrayList<>();
		for (ServiceInterface si : impl.getServiceInterfaces()) {
			contractCopies.add((ServiceInterface) copier.copy(si));
			names.add(si.getName());
			fingerprints.add(ContractAddressing.fingerprint(si));
		}
		copier.copyReferences();

		ServiceProvider providerStub = ServicesFactory.eINSTANCE.createServiceProvider();
		providerStub.setName(provider.getName());
		providerStub.setVersion(provider.getVersion());
		providerStub.getImplementations().add(implCopy);

		Path file = coldDir().resolve(coldFileName(key));
		try {
			Files.createDirectories(coldDir());
			Resource coldResource = new XMIResourceImpl(URI.createFileURI(file.toAbsolutePath().toString()));
			coldResource.getContents().add(providerStub);
			coldResource.getContents().addAll(contractCopies);
			Map<Object, Object> opts = new HashMap<>();
			opts.put(org.eclipse.emf.ecore.xmi.XMIResource.OPTION_ENCODING, "UTF-8");
			coldResource.save(opts);
		} catch (IOException writeFailure) {
			LOG.warning("[DDSR] cold-cache write failed for " + key + ", entry stays hot: " + writeFailure);
			return false;
		}

		// Event material before the detach (like withdraw and the policy
		// retire): after retireImplementation the lookup no longer resolves
		// the implementation, and a bare reference would send the event to
		// MQTT's _unknown topic / SSE's deliver-to-all fallback (#54 matrix).
		ServiceReference eventReference = Announcements.selfContained(provider, impl, reg.getReference());
		ServiceReference retiredRef = retirement.retire(provider, impl);
		registrationSince.remove(reg);
		coldEntries.put(key, new ColdEntry(key, names, fingerprints,
				impl.getImplementationId(), provider.getName(), file));
		Diagnostic d = state.persist();
		if (DdsrDiagnostics.isError(d)) {
			// Degraded but recoverable: the entry is discoverable through
			// its stub, and rehydration republishes it.
			LOG.warning("[DDSR] persist after coldify failed for " + key + ": " + d.getMessage());
		}
		announcements.emit(ServiceEventType.UNREGISTERING, eventReference != null ? eventReference : retiredRef,
				ServiceEventReasons.COLDIFIED);
		LOG.fine(() -> "[DDSR] coldified " + key + " -> " + file);
		return true;
	}
	/**
	 * Rehydrate every cold entry serving the given interface name —
	 * called on the lookup path BEFORE the index query, so "cold" is
	 * never "undiscoverable". Runs the regular publish path (catalog
	 * validation, decoration, REGISTERED event); an entry that no longer
	 * publishes cleanly (e.g. its contract left the catalog) is dropped
	 * from the stub index with a warning — the file stays for forensics.
	 */
	/** Lookup-path hook: record interface activity, then wake matching cold entries. */
	void touchAndRehydrate(String interfaceName) {
		if (interfaceName != null) {
			lastLookupByInterface.put(interfaceName, Instant.now());
		}
		rehydrateColdFor(interfaceName);
	}
	void rehydrateColdFor(String interfaceName) {
		if (interfaceName == null || coldEntries.isEmpty()) {
			return;
		}
		state.writeLock().lock();
		try {
			for (ColdEntry entry : new ArrayList<>(coldEntries.values())) {
				if (!entry.interfaceNames().contains(interfaceName)) {
					continue;
				}
				coldEntries.remove(entry.key());
				Diagnostic d = rehydrate(entry);
				if (DdsrDiagnostics.isError(d)) {
					LOG.warning("[DDSR] cold entry " + entry.key() + " failed to rehydrate and was dropped: "
							+ d.getMessage());
				} else {
					try {
						Files.deleteIfExists(entry.file());
					} catch (IOException cleanupFailure) {
						LOG.warning("[DDSR] rehydrated but could not delete cold file " + entry.file());
					}
				}
			}
		} finally {
			state.writeLock().unlock();
		}
	}
	Diagnostic rehydrate(ColdEntry entry) {
		ResourceSet coldSet = new ResourceSetImpl();
		coldSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("xmi", new XMIResourceFactoryImpl());
		try {
			Resource coldResource = coldSet.getResource(
					URI.createFileURI(entry.file().toAbsolutePath().toString()), true);
			for (EObject root : coldResource.getContents()) {
				if (root instanceof ServiceProvider provider && !provider.getImplementations().isEmpty()) {
					return republication.republish(provider, provider.getImplementations().get(0));
				}
			}
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_PERSISTENCE_FAILED,
					"cold file carries no provider root: " + entry.file());
		} catch (RuntimeException loadFailure) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_PERSISTENCE_FAILED,
					"cold file unreadable: " + entry.file() + " — " + loadFailure);
		}
	}
	/** Rebuild the stub index from the cold directory (broker restart). */
	void loadColdStubs() {
		Path dir = coldDir();
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (var files = Files.list(dir)) {
			files.filter(f -> f.getFileName().toString().endsWith(".xmi")).forEach(this::loadColdStub);
		} catch (IOException scanFailure) {
			LOG.warning("[DDSR] cold directory unreadable, cold entries stay parked: " + scanFailure);
		}
	}
	void loadColdStub(Path file) {
		ResourceSet coldSet = new ResourceSetImpl();
		coldSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("xmi", new XMIResourceFactoryImpl());
		try {
			Resource coldResource = coldSet.getResource(
					URI.createFileURI(file.toAbsolutePath().toString()), true);
			for (EObject root : coldResource.getContents()) {
				if (!(root instanceof ServiceProvider provider) || provider.getImplementations().isEmpty()) {
					continue;
				}
				ServiceImplementation impl = provider.getImplementations().get(0);
				List<String> names = new ArrayList<>();
				List<String> fingerprints = new ArrayList<>();
				for (ServiceInterface si : impl.getServiceInterfaces()) {
					names.add(si.getName());
					fingerprints.add(ContractAddressing.fingerprint(si));
				}
				String key = coldKey(provider.getName(), impl.getName(), impl.getVersion());
				coldEntries.put(key, new ColdEntry(key, names, fingerprints,
						impl.getImplementationId(), provider.getName(), file));
				return;
			}
			LOG.warning("[DDSR] cold file without provider root ignored: " + file);
		} catch (RuntimeException parseFailure) {
			LOG.warning("[DDSR] unreadable cold file ignored: " + file + " — " + parseFailure);
		}
	}
	Path coldDir() {
		Path parent = state.snapshotPath().toAbsolutePath().getParent();
		return parent.resolve(state.snapshotPath().getFileName() + ".cold");
	}
	static String coldKey(String providerName, String implName, String implVersion) {
		return providerName + "|" + implName + "|" + (implVersion == null ? "" : implVersion);
	}
	static String coldFileName(String key) {
		try {
			byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
					.digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (int i = 0; i < 12; i++) {
				hex.append(String.format("%02x", digest[i]));
			}
			return hex + ".xmi";
		} catch (java.security.NoSuchAlgorithmException impossible) {
			throw new IllegalStateException("JVM without SHA-256", impossible);
		}
	}
}
