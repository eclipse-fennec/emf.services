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

package org.eclipse.fennec.services.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.fennec.services.Delivery;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/**
 * A stream is a statement of the contract; a transport that does not
 * carry one says so at the start, with the slots named.
 */
class ContractsStreamsTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static Parameter slot(String name, Delivery delivery) {
		Parameter slot = F.createParameter();
		slot.setName(name);
		slot.setType("double");
		slot.setDelivery(delivery);
		if (delivery == Delivery.STREAM) {
			slot.setUpperBound(-1);
		}
		return slot;
	}

	private static ServiceInterface sensor(Delivery returned, Delivery handedOver) {
		ServiceInterface sensor = F.createServiceInterface();
		sensor.setName("Sensor");
		ServiceOperation readings = F.createServiceOperation();
		readings.setName("readings");
		readings.setReturnValue(slot("result", returned));
		readings.getParameters().add(slot("samples", handedOver));
		sensor.getOperations().add(readings);
		return sensor;
	}

	@Test
	void streamSlotsAreNamedByOperationAndSlot() {
		assertThat(Contracts.streamSlots(sensor(Delivery.STREAM, Delivery.STREAM)))
				.containsExactly("readings.samples", "readings.return");
		assertThat(Contracts.streamSlots(sensor(Delivery.VALUE, Delivery.VALUE))).isEmpty();
		assertThat(Contracts.streamSlots(null)).isEmpty();
	}

	@Test
	void aPromiseIsNothingATransportHasToCarry() {
		ServiceInterface promised = sensor(Delivery.PROMISE, Delivery.VALUE);

		assertThat(Contracts.streamSlots(promised)).isEmpty();
		assertThatCode(() -> Contracts.requireNoStreams(promised, "a test")).doesNotThrowAnyException();
	}

	@Test
	void aTransportThatCarriesNoStreamsRefusesAtTheStartAndNamesTheSlots() {
		assertThatThrownBy(() -> Contracts.requireNoStreams(sensor(Delivery.STREAM, Delivery.VALUE),
				"the generic REST distribution"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Sensor")
				.hasMessageContaining("readings.return")
				.hasMessageContaining("the generic REST distribution");
	}
}
