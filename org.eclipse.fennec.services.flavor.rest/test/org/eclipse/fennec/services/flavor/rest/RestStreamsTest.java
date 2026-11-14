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

import static java.nio.charset.StandardCharsets.UTF_8;
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
	void textTravelsAsItIsOneDataLinePerLine() {
		String frame = RestStreams.frame(RestStreams.VALUE, "<a>\r\n<b/>\n</a>".getBytes(UTF_8), "application/xml");
		assertThat(frame).isEqualTo("event: value\ndata: <a>\ndata: <b/>\ndata: </a>\n\n");
		assertThat(RestStreams.document("<a>\n<b/>\n</a>", "application/xml")).isEqualTo("<a>\n<b/>\n</a>".getBytes(UTF_8));
	}

	@Test
	void whatIsTextIsSaidByTheMediaType() {
		assertThat(RestStreams.isText("application/xml")).isTrue();
		assertThat(RestStreams.isText("application/json; charset=utf-8")).isTrue();
		assertThat(RestStreams.isText("application/vnd.example+json")).isTrue();
		assertThat(RestStreams.isText("text/plain")).isTrue();
		assertThat(RestStreams.isText("application/x-protobuf")).isFalse();
	}

	@Test
	void anEventWithoutDocumentCarriesEmptyData() {
		assertThat(RestStreams.frame(RestStreams.END, null, "application/xml")).isEqualTo("event: end\ndata:\n\n");
	}
}
