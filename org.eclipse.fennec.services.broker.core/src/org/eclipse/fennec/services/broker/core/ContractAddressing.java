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
package org.eclipse.fennec.services.broker.core;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;

/**
 * Catalog contract addressing (ACQUISITION.md §11.2): the catalog key
 * is {@code (name, sd1)}, with the sd1 computed over a copy whose
 * lifecycle metadata is neutralized (status → default, deprecationReason
 * and replacedBy cleared). Deprecating an entry must not move its
 * address — a publisher whose stubs were generated from the ACTIVE
 * contract still addresses the same entry afterwards. The raw sd1
 * (whose frozen grammar includes {@code status=}) remains what
 * references are decorated with and what consumers compare.
 */
public final class ContractAddressing {

	private ContractAddressing() {
	}

	/** The addressing fingerprint of the given contract, or {@code null} for null. */
	public static String fingerprint(ServiceInterface si) {
		if (si == null) {
			return null;
		}
		if (si.getStatus() == CatalogStatus.ACTIVE
				&& si.getDeprecationReason() == null && si.getReplacedBy() == null) {
			return ServiceDescriptionFingerprint.fingerprint(si);
		}
		ServiceInterface normalized = EcoreUtil.copy(si);
		normalized.setStatus(CatalogStatus.ACTIVE);
		normalized.setDeprecationReason(null);
		normalized.setReplacedBy(null);
		return ServiceDescriptionFingerprint.fingerprint(normalized);
	}

	/**
	 * Whether the given requested fingerprint addresses the given
	 * catalog entry — matches the addressing fingerprint and, for
	 * convenience, the raw sd1 (they differ only on deprecated entries).
	 */
	public static boolean matches(ServiceInterface entry, String requestedFingerprint) {
		if (entry == null || requestedFingerprint == null) {
			return false;
		}
		return requestedFingerprint.equals(fingerprint(entry))
				|| requestedFingerprint.equals(ServiceDescriptionFingerprint.fingerprint(entry));
	}
}
