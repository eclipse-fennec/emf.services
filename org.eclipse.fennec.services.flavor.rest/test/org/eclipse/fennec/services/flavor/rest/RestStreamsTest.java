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

package org.eclipse.fennec.services.flavor.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.fennec.services.Delivery;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/** The SSE framing of a stream over REST (#252), shared by both sides. */
class RestStreamsTest {

	private static ServiceOperation returning(Delivery delivery) {
		ServiceOperation operation = ServicesFactory.eINSTANCE.createServiceOperation();
		Parameter result = ServicesFactory.eINSTANCE.createParameter();
		result.setDelivery(delivery);
		operation.setReturnValue(result);
		return operation;
	}

	@Test
	void bothKindsOfStreamAreStreamedAndNothingElseIs() {
		assertThat(RestStreams.streams(returning(Delivery.STREAM))).isTrue();
		assertThat(RestStreams.streams(returning(Delivery.BROADCAST))).isTrue();
		assertThat(RestStreams.streams(returning(Delivery.VALUE))).isFalse();
		assertThat(RestStreams.streams(returning(Delivery.PROMISE))).isFalse();
		assertThat(RestStreams.streams(ServicesFactory.eINSTANCE.createServiceOperation())).isFalse();
	}

	@Test
	void dataTravelsOneLinePerLine() {
		String frame = RestStreams.frame(RestStreams.VALUE, "{\"a\":1}\r\n{\"b\":2}");
		assertThat(frame).isEqualTo("event: value\ndata: {\"a\":1}\ndata: {\"b\":2}\n\n");
	}

	@Test
	void anEventWithoutDataCarriesEmptyData() {
		assertThat(RestStreams.frame(RestStreams.END, null)).isEqualTo("event: end\ndata:\n\n");
	}
}
