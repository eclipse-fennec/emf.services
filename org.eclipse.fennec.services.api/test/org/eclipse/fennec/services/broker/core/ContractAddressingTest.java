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
package org.eclipse.fennec.services.broker.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.UpdatePolicy;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.junit.jupiter.api.Test;

/**
 * The catalog address {@code (name, sd1-normalized)} must survive every
 * lifecycle mutation of an entry (ACQUISITION.md §11.2, #47).
 */
class ContractAddressingTest {

	private static ServiceInterface payment() {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName("Payment");
		si.setVersion("1.0.0");
		return si;
	}

	@Test
	void anActiveEntryIsAddressedByItsRawSd1() {
		ServiceInterface si = payment();
		assertThat(ContractAddressing.fingerprint(si))
				.isEqualTo(ServiceDescriptionFingerprint.fingerprint(si));
	}

	@Test
	void deprecationDoesNotMoveTheAddressAlthoughItMovesTheRawSd1() {
		ServiceInterface active = payment();
		ServiceInterface deprecated = payment();
		deprecated.setStatus(CatalogStatus.DEPRECATED);
		deprecated.setDeprecationReason("superseded");
		deprecated.setReplacedBy(payment());

		assertThat(ServiceDescriptionFingerprint.fingerprint(deprecated))
				.as("status= is part of the frozen sd1 grammar")
				.isNotEqualTo(ServiceDescriptionFingerprint.fingerprint(active));
		assertThat(ContractAddressing.fingerprint(deprecated))
				.as("the address neutralizes the lifecycle metadata")
				.isEqualTo(ContractAddressing.fingerprint(active));
		assertThat(ContractAddressing.matches(deprecated, ServiceDescriptionFingerprint.fingerprint(active)))
				.as("a stub generated from the ACTIVE contract still addresses the entry")
				.isTrue();
	}

	@Test
	void updatePolicyNeedsNoNeutralizationBecauseSd1NeverSawIt() {
		ServiceInterface plain = payment();
		ServiceInterface withPolicy = payment();
		withPolicy.setUpdatePolicy(UpdatePolicy.HARD_CUTOVER);

		assertThat(ServiceDescriptionFingerprint.fingerprint(withPolicy))
				.isEqualTo(ServiceDescriptionFingerprint.fingerprint(plain));
		assertThat(ContractAddressing.fingerprint(withPolicy))
				.isEqualTo(ContractAddressing.fingerprint(plain));
	}

	@Test
	void nullAndForeignFingerprintsDoNotMatch() {
		assertThat(ContractAddressing.fingerprint(null)).isNull();
		assertThat(ContractAddressing.matches(null, "sd1:0")).isFalse();
		assertThat(ContractAddressing.matches(payment(), null)).isFalse();
		assertThat(ContractAddressing.matches(payment(), "sd1:0000")).isFalse();
	}
}
