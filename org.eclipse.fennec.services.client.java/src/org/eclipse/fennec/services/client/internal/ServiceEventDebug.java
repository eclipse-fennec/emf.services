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

package org.eclipse.fennec.services.client.internal;

import java.util.logging.Logger;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.ServiceEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Smoke check for the event stream (A1): registers a listener for
 * {@code Payment} and prints what arrives.
 * <p>
 * Sibling of {@link ClientRoundtripDebug} and equally throwaway — it
 * exists so the end-to-end path is visible in a launch log. Delete it
 * once the stream is no longer interesting.
 */
@Component(immediate = true)
public final class ServiceEventDebug {

	private static final Logger LOG = Logger.getLogger(ServiceEventDebug.class.getName());

	private static final String INTERFACE = "Payment";

	@Reference
	private DdsrClient client;

	private AutoCloseable subscription;

	@Activate
	void activate() {
		subscription = client.consumer().addServiceListener(INTERFACE, null, this::onEvent);
		LOG.info("[DDSR-Client] ServiceEventDebug listening for " + INTERFACE);
	}

	private void onEvent(ServiceEvent event) {
		String provider = event.getReference() != null && event.getReference().getProvider() != null
				? event.getReference().getProvider().getName()
				: "(provider not in document)";
		LOG.info("[DDSR-Client] EVENT " + event.getType()
				+ " ref=" + (event.getReference() != null ? event.getReference().getId() : "?")
				+ " provider=" + provider
				+ " at=" + event.getTimestamp());
	}

	@Deactivate
	void deactivate() {
		if (subscription != null) {
			try {
				subscription.close();
			} catch (Exception ignored) {
				// Shutting down.
			}
			subscription = null;
		}
	}
}
