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
package org.eclipse.fennec.services.provider.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.osgi.service.jakartars.runtime.dto.ApplicationDTO;
import org.osgi.service.jakartars.runtime.dto.DTOConstants;
import org.osgi.service.jakartars.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jakartars.runtime.dto.RuntimeDTO;

/**
 * Served first, then told (#204): the generic distribution announces
 * only what a whiteboard lists as deployed.
 */
class DeploymentTest {

	private static final long SELF = 42;

	@Test
	void nothingKnownIsPending() {
		assertThat(Deployment.of(SELF, List.of())).isEqualTo(Deployment.PENDING);
		assertThat(Deployment.of(SELF, List.of(runtime(deployed(7)), runtime())).deployed()).isFalse();
	}

	@Test
	void listedByTheWhiteboardIsDeployed() {
		assertThat(Deployment.of(SELF, List.of(runtime(deployed(7), deployed(SELF)))).deployed()).isTrue();
	}

	@Test
	void aRefusalSaysWhy() {
		Deployment refused = Deployment.of(SELF, List.of(runtime(failed(SELF,
				DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE))));

		assertThat(refused.state()).isEqualTo(Deployment.State.FAILED);
		assertThat(refused.reason()).contains(String.valueOf(DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE));
	}

	@Test
	void deployedOnOneWhiteboardWinsOverARefusalByAnother() {
		RuntimeDTO refusing = runtime();
		refusing.failedApplicationDTOs = new FailedApplicationDTO[] {
				failed(SELF, DTOConstants.FAILURE_REASON_DUPLICATE_NAME) };

		assertThat(Deployment.of(SELF, List.of(refusing, runtime(deployed(SELF)))).deployed()).isTrue();
	}

	private static RuntimeDTO runtime(ApplicationDTO... applications) {
		RuntimeDTO runtime = new RuntimeDTO();
		runtime.applicationDTOs = applications;
		runtime.failedApplicationDTOs = new FailedApplicationDTO[0];
		return runtime;
	}

	private static RuntimeDTO runtime(FailedApplicationDTO failed) {
		RuntimeDTO runtime = runtime();
		runtime.failedApplicationDTOs = new FailedApplicationDTO[] { failed };
		return runtime;
	}

	private static ApplicationDTO deployed(long serviceId) {
		ApplicationDTO application = new ApplicationDTO();
		application.serviceId = serviceId;
		return application;
	}

	private static FailedApplicationDTO failed(long serviceId, int reason) {
		FailedApplicationDTO failed = new FailedApplicationDTO();
		failed.serviceId = serviceId;
		failed.failureReason = reason;
		return failed;
	}
}
