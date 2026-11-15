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

package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A many-valued model answer is one document: the values in order, then
 * their context (#302). The reader takes every root of the return type.
 */
class RestServiceInvokerManyValuesTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static RestOperationFlavor returning(int upperBound) {
		Parameter result = F.createParameter();
		result.setName("result");
		result.setEType(ServicesPackage.eINSTANCE.getServiceProvider());
		result.setUpperBound(upperBound);
		ServiceOperation operation = F.createServiceOperation();
		operation.setName("providers");
		operation.setReturnValue(result);
		RestOperationFlavor flavor = F.createRestOperationFlavor();
		flavor.setName("providers");
		flavor.setOperation(operation);
		return flavor;
	}

	private static Response answered(XmiBundle document) {
		Response response = mock(Response.class);
		when(response.getStatus()).thenReturn(200);
		when(response.hasEntity()).thenReturn(true);
		when(response.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);
		when(response.readEntity(XmiBundle.class)).thenReturn(document);
		return response;
	}

	@Test
	void everyRootOfTheReturnTypeIsAValue() {
		ServiceProvider first = provider("first");
		ServiceProvider second = provider("second");
		ServiceInterface context = F.createServiceInterface();

		Object read = RestServiceInvoker.readResponse(answered(new XmiBundle(first, second, context)), returning(-1));

		assertThat(read).asList().containsExactly(first, second);
	}

	@Test
	void aSingleValueIsStillTheFirstRoot() {
		ServiceProvider first = provider("first");
		ServiceInterface context = F.createServiceInterface();

		Object read = RestServiceInvoker.readResponse(answered(new XmiBundle(first, context)), returning(1));

		assertThat(read).isSameAs(first);
	}

	@Test
	void oneValueOfAManyValuedAnswerIsAListOfOne() {
		ServiceProvider only = provider("only");

		assertThat(RestServiceInvoker.readResponse(answered(new XmiBundle(only)), returning(-1)))
				.isEqualTo(List.of(only));
	}

	private static ServiceProvider provider(String name) {
		ServiceProvider provider = F.createServiceProvider();
		provider.setName(name);
		return provider;
	}
}
