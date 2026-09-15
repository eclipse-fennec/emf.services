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
package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringProperty;
import org.junit.jupiter.api.Test;

/** The query-parameter side of GET /references (#56); the TS twin is {@code broker-http.test.ts}. */
class LookupHttpProxyTest {

	@Test
	void flavorsTravelAsCsvInDeclaredOrder() {
		assertThat(LookupHttpProxy.flavorCsv(List.of(FlavorKind.MQTT, FlavorKind.REST))).isEqualTo("MQTT,REST");
		assertThat(LookupHttpProxy.flavorCsv(List.of())).isEmpty();
	}

	@Test
	void theContractFingerprintTravelsAsTheFingerprintParameter() {
		ConsumerCapability cap = ServicesFactory.eINSTANCE.createConsumerCapability();
		StringProperty fp = ServicesFactory.eINSTANCE.createStringProperty();
		fp.setName("ddsr.fingerprint");
		fp.setValue("sd1:abc");
		cap.getProperties().add(fp);

		assertThat(LookupHttpProxy.requestedFingerprint(cap)).isEqualTo("sd1:abc");
	}

	@Test
	void noCapabilityBlankOrForeignPropertiesMeanNoFingerprint() {
		assertThat(LookupHttpProxy.requestedFingerprint(null)).isNull();

		ConsumerCapability cap = ServicesFactory.eINSTANCE.createConsumerCapability();
		StringProperty blank = ServicesFactory.eINSTANCE.createStringProperty();
		blank.setName("ddsr.fingerprint");
		blank.setValue("  ");
		IntProperty other = ServicesFactory.eINSTANCE.createIntProperty();
		other.setName("ddsr.fingerprint");
		other.setValue(7);
		cap.getProperties().add(blank);
		cap.getProperties().add(other);

		assertThat(LookupHttpProxy.requestedFingerprint(cap)).as("only a non-blank StringProperty counts").isNull();
	}
}
