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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.ContractAddressing;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.exception.CatalogEntryAmbiguous;
import org.eclipse.fennec.services.broker.core.exception.CatalogEntryNotFound;

/**
 * The contracts the broker knows, and what a publish is measured
 * against.
 *
 * <p>Two jobs that look separate and are not. Governance adds,
 * deprecates and removes entries. Resolution takes the stub contracts a
 * publisher sends in its wire body and rewires them onto the live
 * entries, so that a lookup by interface name can find the result. The
 * second is why a publish cannot simply be told about the catalog: it
 * hands its implementation over to be corrected.
 *
 * <p>An entry is addressed by name, and by fingerprint when a name is
 * not enough. Nothing here compares versions: a fingerprint is
 * identity, a version communicates intent (ACQUISITION.md §11.3).
 */
final class CatalogStore {

	private static final Logger LOG =
			Logger.getLogger(CatalogStore.class.getName());

	private final BrokerState state;

	private final ColdCache cold;

	CatalogStore(BrokerState state, ColdCache cold) {
		this.state = state;
		this.cold = cold;
	}

	Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		if (serviceInterface == null || serviceInterface.getName() == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND,
					"serviceInterface and serviceInterface.name must not be null");
		}
		state.writeLock().lock();
		try {
			// (name, sd1) key (§11.2): identical content is an idempotent
			// no-op; a different contract under the same name COEXISTS as
			// its own entry — consumers address the contract they speak
			// via the fingerprint, never by name alone.
			String sd1 = ContractAddressing.fingerprint(serviceInterface);
			List<ServiceInterface> sameName = findCatalogEntriesByName(serviceInterface.getName());
			for (ServiceInterface entry : sameName) {
				if (sd1.equals(ContractAddressing.fingerprint(entry))) {
					return DdsrDiagnostics.ok("catalog entry already present (identical content), fingerprint="
							+ sd1);
				}
			}
			state.registry().getCatalog().add(serviceInterface);
			Diagnostic d = state.persist();
			if (DdsrDiagnostics.isError(d)) {
				// Roll back so a failed save does not leave the in-memory
				// catalog ahead of the snapshot — see the note on
				// rollbackOnPersistFailure.
				state.registry().getCatalog().remove(serviceInterface);
				return d;
			}
			// Hand the broker's fingerprint of the accepted entry back to
			// the publisher, so producer and broker can compare views
			// without another round trip (DECISIONS_PARITY D6).
			return DdsrDiagnostics.ok("catalog entry added, fingerprint=" + sd1
					+ (sameName.isEmpty() ? ""
							: " (coexists with " + sameName.size()
							+ " other contract(s) named '" + serviceInterface.getName() + "')"));
		} finally {
			state.writeLock().unlock();
		}
	}
	Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return deprecateCatalogEntry(serviceInterface, serviceInterface == null ? null
				: serviceInterface.getDeprecationReason(),
				serviceInterface == null ? null : serviceInterface.getReplacedBy(), requestor);
	}

	private Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String reason,
			ServiceInterface replacedBy, String requestor) {
		if (serviceInterface == null || serviceInterface.getName() == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND,
					"serviceInterface and serviceInterface.name must not be null");
		}
		state.writeLock().lock();
		try {
			CatalogResolution resolution = resolveCatalogEntry(serviceInterface,
					serviceInterface.getName(), DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND);
			if (resolution.refusal() != null) {
				return resolution.refusal();
			}
			ServiceInterface inCatalog = resolution.entry();
			CatalogStatus previousStatus = inCatalog.getStatus();
			String previousReason = inCatalog.getDeprecationReason();
			ServiceInterface previousReplacedBy = inCatalog.getReplacedBy();

			inCatalog.setStatus(CatalogStatus.DEPRECATED);
			if (reason != null) {
				inCatalog.setDeprecationReason(reason);
			}
			if (replacedBy != null) {
				inCatalog.setReplacedBy(replacedBy);
			}

			Diagnostic d = state.persist();
			if (DdsrDiagnostics.isError(d)) {
				inCatalog.setStatus(previousStatus);
				inCatalog.setDeprecationReason(previousReason);
				inCatalog.setReplacedBy(previousReplacedBy);
			}
			return d;
		} finally {
			state.writeLock().unlock();
		}
	}
	Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		if (serviceInterface == null || serviceInterface.getName() == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND,
					"serviceInterface and serviceInterface.name must not be null");
		}
		state.writeLock().lock();
		try {
			CatalogResolution resolution = resolveCatalogEntry(serviceInterface,
					serviceInterface.getName(), DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND);
			if (resolution.refusal() != null) {
				return resolution.refusal();
			}
			ServiceInterface inCatalog = resolution.entry();
			// Strict-Reject: any live implementation that references this
			// interface blocks the removal (REQUIREMENTS FR-Catalog-Removal-StrictReject).
			// Identity, not name: the publish path rewires every live impl
			// onto its catalog entry, and with (name, sd1) coexistence a
			// same-named SIBLING contract must not block this removal.
			for (ServiceImplementation impl : state.registry().getImplementations()) {
				for (ServiceInterface si : impl.getServiceInterfaces()) {
					if (si == inCatalog) {
						return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS,
								"cannot remove catalog entry '" + inCatalog.getName()
										+ "': implementation '"
										+ impl.getName() + "' still publishes it");
					}
				}
			}
			// Cold entries count as live for strict-reject: they are still
			// discoverable and would fail to rehydrate without their contract.
			String parked = cold.parkedImplementationNeeding(ContractAddressing.fingerprint(inCatalog));
			if (parked != null) {
				return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS,
						"cannot remove catalog entry '" + inCatalog.getName()
								+ "': cold implementation '" + parked + "' still references it");
			}
			// Keep the position so a rollback restores the catalog exactly,
			// not just its contents — cross-refs into the catalog use
			// positional URI fragments (//@catalog.N), so order matters.
			int previousIndex = state.registry().getCatalog().indexOf(inCatalog);
			state.registry().getCatalog().remove(inCatalog);
			Diagnostic d = state.persist();
			if (DdsrDiagnostics.isError(d)) {
				state.registry().getCatalog().add(previousIndex, inCatalog);
			}
			return d;
		} finally {
			state.writeLock().unlock();
		}
	}
	/** Soft-deprecate the contract a name and fingerprint address. */
	Diagnostic deprecateCatalogEntry(String name, String fingerprint, ServiceInterface governance,
			String requestor) {
		// One write section for resolving and applying, and the governance
		// fields travel as values rather than being written onto the live
		// entry first. Before, the caller's reason and replacedBy were set
		// on the resolved entry with no lock held at all, and a save that
		// then failed left them there — the rollback below only knew about
		// the fields the inner call had changed.
		return state.write(() -> deprecateCatalogEntry(governanceTarget(name, fingerprint),
				governance == null ? null : governance.getDeprecationReason(),
				governance == null ? null : governance.getReplacedBy(),
				requestor));
	}
	/** Remove the contract a name and fingerprint address. */
	Diagnostic removeCatalogEntry(String name, String fingerprint, String requestor) {
		// One write section: resolving under the read lock and removing
		// under the write lock left a gap in which the entry that was
		// resolved could already be gone.
		return state.write(() -> removeCatalogEntry(governanceTarget(name, fingerprint), requestor));
	}
	/**
	 * One contract by name.
	 *
	 * <p>Several contracts may share a name — they are told apart by
	 * content fingerprint (ACQUISITION.md §11.2) — so a bare name is
	 * only answered while it addresses one.
	 */
	ServiceInterface getCatalogEntry(String name, String fingerprint) {
		state.readLock().lock();
		try {
			List<ServiceInterface> named = entriesNamed(name);
			if (fingerprint != null && !fingerprint.isBlank()) {
				for (ServiceInterface candidate : named) {
					if (ContractAddressing.matches(candidate, fingerprint)) {
						return candidate;
					}
				}
				throw new CatalogEntryNotFound(
						"no catalog entry named '" + name + "' with fingerprint " + fingerprint);
			}
			if (named.isEmpty()) {
				throw new CatalogEntryNotFound("no catalog entry named '" + name + "'");
			}
			if (named.size() > 1) {
				throw new CatalogEntryAmbiguous("interface name '" + name + "' names " + named.size()
						+ " coexisting contracts — address one via its fingerprint; available: "
						+ named.stream().map(ContractAddressing::fingerprint).collect(Collectors.joining(", ")));
			}
			return named.get(0);
		} finally {
			state.readLock().unlock();
		}
	}
	/**
	 * What a governance call acts on: with a fingerprint the entry
	 * itself, so content addressing hits it even where several contracts
	 * share the name; without one a name-only stub, which leaves the
	 * unambiguity rule to the operation that is about to run.
	 */
	ServiceInterface governanceTarget(String name, String fingerprint) {
		if (fingerprint == null || fingerprint.isBlank()) {
			ServiceInterface stub = ServicesFactory.eINSTANCE.createServiceInterface();
			stub.setName(name);
			return stub;
		}
		return getCatalogEntry(name, fingerprint);
	}
	List<ServiceInterface> entriesNamed(String name) {
		List<ServiceInterface> named = new ArrayList<>();
		for (ServiceInterface entry : state.registry().getCatalog()) {
			if (entry.getName() != null && entry.getName().equals(name)) {
				named.add(entry);
			}
		}
		return named;
	}
	List<ServiceInterface> findCatalogEntriesByName(String name) {
		List<ServiceInterface> entries = new ArrayList<>();
		if (name == null) {
			return entries;
		}
		for (ServiceInterface si : state.registry().getCatalog()) {
			if (name.equals(si.getName())) {
				entries.add(si);
			}
		}
		return entries;
	}
	/**
	 * Contract addressing (ACQUISITION.md §11.2): the catalog key is
	 * {@code (name, sd1)} — same-named entries with different contracts
	 * coexist. An incoming ServiceInterface that carries content
	 * (operations or exceptions) addresses its entry EXACTLY by that
	 * content: its sd1 must match one of the same-named entries — a miss
	 * is contract drift, not a lookup fallback. A stub (name-only
	 * sibling, catalog-URL proxy, or bodyless REST call) resolves by
	 * name alone and requires the name to be unambiguous.
	 *
	 * @param notFoundCode the code for "no entry under this name" —
	 *                     differs between the publish path
	 *                     ({@code CODE_IMPL_INTERFACE_NOT_IN_CATALOG})
	 *                     and catalog governance
	 *                     ({@code CODE_CATALOG_ENTRY_NOT_FOUND})
	 */
	CatalogResolution resolveCatalogEntry(ServiceInterface incoming, String name, int notFoundCode) {
		List<ServiceInterface> entries = findCatalogEntriesByName(name);
		if (entries.isEmpty()) {
			return CatalogResolution.refuse(DdsrDiagnostics.error(notFoundCode,
					"service interface '" + name + "' is not in the catalog"));
		}
		boolean carriesContent = !((InternalEObject) incoming).eIsProxy()
				&& (!incoming.getOperations().isEmpty() || !incoming.getExceptions().isEmpty());
		if (carriesContent) {
			String sd1 = ContractAddressing.fingerprint(incoming);
			for (ServiceInterface entry : entries) {
				if (sd1.equals(ContractAddressing.fingerprint(entry))) {
					return CatalogResolution.of(entry);
				}
			}
			return CatalogResolution.refuse(DdsrDiagnostics.error(notFoundCode,
					"contract drift: '" + name + "' is in the catalog ("
					+ entries.size() + " contract(s)), but none matches the submitted content ("
					+ sd1 + ")"));
		}
		if (entries.size() > 1) {
			return CatalogResolution.refuse(DdsrDiagnostics.error(
					DdsrDiagnostics.CODE_CATALOG_ENTRY_AMBIGUOUS,
					"interface name '" + name + "' names " + entries.size()
					+ " coexisting catalog contracts — address the contract by content "
					+ "(full ServiceInterface) or by its sd1 fingerprint"));
		}
		return CatalogResolution.of(entries.get(0));
	}
	/** Resolution outcome: exactly one of entry / refusal is set. */
	record CatalogResolution(ServiceInterface entry, Diagnostic refusal) {
		static CatalogResolution of(ServiceInterface entry) {
			return new CatalogResolution(entry, null);
		}
		static CatalogResolution refuse(Diagnostic refusal) {
			return new CatalogResolution(null, refusal);
		}
	}
	/**
	 * Extract the catalog-entry name from a ServiceInterface ref.
	 * Handles both:
	 * <ul>
	 *   <li>resolved SI (sibling root in the wire) → {@code si.getName()}</li>
	 *   <li>EMF proxy with URI like {@code .../catalog/Payment} →
	 *       last path segment</li>
	 * </ul>
	 */
	static String catalogNameOf(ServiceInterface si) {
		if (si == null) {
			return null;
		}
		if (!((InternalEObject) si).eIsProxy()) {
			return si.getName();
		}
		URI proxyUri =
				((InternalEObject) si).eProxyURI();
		if (proxyUri == null) {
			return null;
		}
		// Last path segment of e.g. http://broker/ddsr/rest/catalog/Payment
		int segments = proxyUri.segmentCount();
		if (segments == 0) {
			return null;
		}
		return proxyUri.segment(segments - 1);
	}
	/**
	 * Shared by publish and modify: the incoming impl.serviceInterfaces
	 * may be stub SIs (just name+version, or an href to the catalog URL)
	 * to keep the wire body self-contained. Match them against the live
	 * catalog and rewire the impl ref to the real catalog entry, so
	 * subsequent lookups by interface name can match. Refuses unknown or
	 * unidentifiable interfaces; reports deprecated ones as a note.
	 */
	ContractResolution resolveContracts(ServiceImplementation implementation) {
		StringBuilder deprecationNote = new StringBuilder();
		List<ServiceInterface> sis = implementation.getServiceInterfaces();
		for (int i = 0; i < sis.size(); i++) {
			ServiceInterface si = sis.get(i);
			String name = catalogNameOf(si);
			if (name == null || name.isBlank()) {
				return new ContractResolution(DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG,
						"impl.serviceInterfaces[" + i + "] has no identifiable name — "
						+ "send it as <serviceInterfaces href=\"<broker>/catalog/{name}\"/> "
						+ "or as a sibling root with name+version set"), null);
			}
			CatalogResolution resolution = resolveCatalogEntry(si, name,
					DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG);
			if (resolution.refusal() != null) {
				return new ContractResolution(resolution.refusal(), null);
			}
			ServiceInterface inCatalog = resolution.entry();
			if (inCatalog.getStatus() == CatalogStatus.DEPRECATED) {
				if (deprecationNote.length() > 0) {
					deprecationNote.append("; ");
				}
				deprecationNote.append(name);
				if (inCatalog.getDeprecationReason() != null) {
					deprecationNote.append(" (").append(inCatalog.getDeprecationReason()).append(")");
				}
			}
			if (inCatalog != si) {
				sis.set(i, inCatalog);
				rewireContractReferences(implementation, si, inCatalog);
			}
		}
		return new ContractResolution(null, deprecationNote.length() == 0 ? null : deprecationNote.toString());
	}
	static record ContractResolution(Diagnostic refusal, String deprecationNote) {
	}
	/**
	 * Re-point everything in the implementation that referenced the
	 * published copy of a contract at the catalog entry that replaced it.
	 *
	 * <p>A publish body carries the contract along, so the broker sees two
	 * objects for one contract: the copy in the payload and the entry in
	 * its catalog. {@link #resolveContracts} keeps the catalog entry —
	 * anything else would let a publisher redefine a contract by
	 * announcing an implementation of it. But a flavor does not only
	 * reference the contract, it references single parameters and
	 * exceptions <em>inside</em> it, and those references would still
	 * point into the payload copy, which is now attached to nothing.
	 * Saving the registry then fails with "not contained in a resource".
	 *
	 * <p>The correspondence is the containment path: the two contracts are
	 * the same contract, and a catalog entry is only accepted when its
	 * fingerprint says so, which makes the path from the contract to a
	 * parameter the same on both sides.
	 *
	 * <p>This stayed invisible while the TypeScript client omitted
	 * single-valued cross-references from what it published (#79) — there
	 * simply was no reference into the contract to rewire.
	 */
	static void rewireContractReferences(ServiceImplementation implementation,
			ServiceInterface published, ServiceInterface inCatalog) {
		List<EObject> elements = new ArrayList<>();
		elements.add(implementation);
		implementation.eAllContents().forEachRemaining(elements::add);
		for (EObject element : elements) {
			for (EReference reference : element.eClass().getEAllReferences()) {
				if (reference.isContainment() || reference.isDerived() || !element.eIsSet(reference)) {
					continue;
				}
				if (reference.isMany()) {
					@SuppressWarnings("unchecked")
					List<EObject> values = (List<EObject>) element.eGet(reference);
					for (int i = 0; i < values.size(); i++) {
						EObject replacement = counterpart(values.get(i), published, inCatalog);
						if (replacement != null) {
							values.set(i, replacement);
						}
					}
				} else {
					EObject replacement = counterpart((EObject) element.eGet(reference), published, inCatalog);
					if (replacement != null) {
						element.eSet(reference, replacement);
					}
				}
			}
		}
	}
	/**
	 * The object at the same place inside the catalog entry, or
	 * {@code null} when the reference does not point into the published
	 * copy at all and is to be left alone.
	 */
	static EObject counterpart(EObject target, ServiceInterface published, ServiceInterface inCatalog) {
		if (target == null || target.eIsProxy() || !EcoreUtil.isAncestor(published, target)) {
			return null;
		}
		if (target == published) {
			return inCatalog;
		}
		String path = EcoreUtil.getRelativeURIFragmentPath(published, target);
		EObject counterpart = EcoreUtil.getEObject(inCatalog, path);
		if (counterpart == null) {
			LOG.warning("[DDSR] catalog entry " + inCatalog.getName() + " has nothing at " + path
					+ " — a reference of the published implementation is dropped");
		}
		return counterpart;
	}
}
