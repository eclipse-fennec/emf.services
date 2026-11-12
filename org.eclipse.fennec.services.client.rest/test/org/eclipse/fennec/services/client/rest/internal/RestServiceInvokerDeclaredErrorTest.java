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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.fennec.services.RestExceptionBinding;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.DdsrServiceException;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

/**
 * The reading side of what RestDispatcher answers (#185): a status the
 * flavor binds to a declared error is that error, and the caller is
 * told which one.
 */
class RestServiceInvokerDeclaredErrorTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static RestOperationFlavor getBinding(int status) {
		ServiceException declared = F.createServiceException();
		declared.setName("PersonNotFound");
		declared.setType("fennec.services.example.PersonNotFound");
		RestExceptionBinding binding = F.createRestExceptionBinding();
		binding.setException(declared);
		binding.setStatus(status);
		RestOperationFlavor get = F.createRestOperationFlavor();
		get.setName("get");
		get.getExceptionBindings().add(binding);
		return get;
	}

	private static Response answered(int status, String body) {
		Response response = mock(Response.class);
		when(response.getStatus()).thenReturn(status);
		when(response.hasEntity()).thenReturn(body != null);
		when(response.readEntity(String.class)).thenReturn(body);
		return response;
	}

	@Test
	void aStatusTheFlavorBindsIsTheDeclaredError() {
		RestOperationFlavor get = getBinding(404);

		assertThatThrownBy(() -> RestServiceInvoker.readResponse(answered(404, "nobody is p-17"), get))
				.isInstanceOfSatisfying(DdsrServiceException.class, declared -> {
					assertThat(declared).hasMessage("nobody is p-17");
					assertThat(declared.declared()).isSameAs(get.getExceptionBindings().get(0).getException());
					assertThat(declared.status()).isEqualTo(404);
					assertThat(declared.isTransportFailure()).isFalse();
				});
	}

	@Test
	void aDeclaredErrorWithoutABodyIsCalledByItsName() {
		assertThatThrownBy(() -> RestServiceInvoker.readResponse(answered(404, null), getBinding(404)))
				.isInstanceOf(DdsrServiceException.class).hasMessage("PersonNotFound (HTTP 404)");
	}

	@Test
	void aStatusNobodyBoundStaysAPlainFailure() {
		assertThatThrownBy(() -> RestServiceInvoker.readResponse(answered(500, "boom"), getBinding(404)))
				.isInstanceOf(DdsrException.class).isNotInstanceOf(DdsrServiceException.class)
				.hasMessage("HTTP 500: boom");
	}
}
