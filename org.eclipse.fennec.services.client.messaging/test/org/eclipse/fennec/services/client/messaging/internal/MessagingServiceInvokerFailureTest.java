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

package org.eclipse.fennec.services.client.messaging.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.DdsrServiceException;
import org.junit.jupiter.api.Test;

/**
 * What a failing answer is to the caller (#185): the declared error
 * whose code its Diagnostic carries, or a failure nobody announced.
 */
class MessagingServiceInvokerFailureTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static ServiceOperation chargeDeclaring(int code) {
		ServiceException declared = F.createServiceException();
		declared.setName("InsufficientFunds");
		declared.setType("payment.InsufficientFunds");
		IntProperty property = F.createIntProperty();
		property.setName("code");
		property.setValue(code);
		declared.getProperties().add(property);
		ServiceOperation charge = F.createServiceOperation();
		charge.setName("charge");
		charge.getExceptions().add(declared);
		return charge;
	}

	private static Diagnostic failing(int code, String message) {
		Diagnostic diagnostic = F.createDiagnostic();
		diagnostic.setSeverity(DiagnosticSeverity.ERROR);
		diagnostic.setCode(code);
		diagnostic.setMessage(message);
		return diagnostic;
	}

	@Test
	void aDiagnosticWithADeclaredCodeIsThatError() {
		ServiceOperation charge = chargeDeclaring(402);
		Diagnostic answered = failing(402, "only 3.50 left");

		DdsrException failure = MessagingServiceInvoker.failureOf(charge, answered, "charge");

		assertThat(failure).isInstanceOf(DdsrServiceException.class).hasMessage("only 3.50 left");
		DdsrServiceException declared = (DdsrServiceException) failure;
		assertThat(declared.declared()).isSameAs(charge.getExceptions().get(0));
		assertThat(declared.diagnostic()).isSameAs(answered);
		assertThat(declared.status()).as("a messaging broker has no status").isZero();
		assertThat(declared.isTransportFailure()).as("calling again would get the same answer").isFalse();
	}

	@Test
	void aDeclaredErrorWithoutWordsIsCalledByItsName() {
		assertThat(MessagingServiceInvoker.failureOf(chargeDeclaring(402), failing(402, null), "charge"))
				.hasMessage("InsufficientFunds");
	}

	@Test
	void anyOtherDiagnosticIsAFailureNobodyAnnounced() {
		DdsrException failure = MessagingServiceInvoker.failureOf(chargeDeclaring(402), failing(0, "boom"), "charge");

		assertThat(failure).isNotInstanceOf(DdsrServiceException.class).hasMessage("charge failed: boom");
	}
}
