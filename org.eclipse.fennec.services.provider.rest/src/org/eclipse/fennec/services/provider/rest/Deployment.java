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

import java.util.Collection;

import org.osgi.service.jakartars.runtime.dto.ApplicationDTO;
import org.osgi.service.jakartars.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jakartars.runtime.dto.RuntimeDTO;

/**
 * Whether the whiteboard serves an application, as its runtimes say it
 * (#204): the one honest answer to "may this be announced yet?".
 *
 * <p>Registering an {@code Application} service only asks for a
 * deployment, which happens afterwards on the whiteboard's own thread —
 * and for a guarded application only once its authentication filter is
 * there. Until a runtime lists it, its address answers 404.
 *
 * @param state where the application stands
 * @param reason why the whiteboard refused it, or {@code null}
 */
record Deployment(State state, String reason) {

	enum State {
		/** Listed by a runtime: it answers. */
		DEPLOYED,
		/** Refused by a runtime, with the reason. */
		FAILED,
		/** Not (yet) known to any runtime. */
		PENDING
	}

	static final Deployment PENDING = new Deployment(State.PENDING, null);

	/**
	 * Where the application registered as {@code serviceId} stands across
	 * these runtimes. Deployed by any of them wins over a refusal by
	 * another: a deployment with two whiteboards may well serve an
	 * application on one that the other declines.
	 */
	static Deployment of(long serviceId, Collection<RuntimeDTO> runtimes) {
		String refusal = null;
		for (RuntimeDTO runtime : runtimes) {
			if (runtime == null) {
				continue;
			}
			for (ApplicationDTO application : runtime.applicationDTOs == null ? new ApplicationDTO[0]
					: runtime.applicationDTOs) {
				if (application.serviceId == serviceId) {
					return new Deployment(State.DEPLOYED, null);
				}
			}
			for (FailedApplicationDTO failed : runtime.failedApplicationDTOs == null ? new FailedApplicationDTO[0]
					: runtime.failedApplicationDTOs) {
				if (failed.serviceId == serviceId) {
					refusal = "failure reason " + failed.failureReason;
				}
			}
		}
		return refusal == null ? PENDING : new Deployment(State.FAILED, refusal);
	}

	boolean deployed() {
		return state == State.DEPLOYED;
	}
}
