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

package org.eclipse.fennec.services.itest.mqtt;

import java.util.logging.Logger;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.client.DdsrClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Podman scenario D's eyes: a listener for {@code Payment} that logs every
 * event the client hears — over MQTT in this launch — so the harness can
 * read the {@code EVENT REGISTERED} and {@code EVENT UNREGISTERING} lines.
 * <p>
 * It used to live in the SDK and started in every framework with a client
 * (#268); a harness bundle is where it belongs.
 */
@Component(immediate = true)
public final class PaymentEventLog {

	private static final Logger LOG = Logger.getLogger(PaymentEventLog.class.getName());

	private static final String INTERFACE = "Payment";

	@Reference
	private DdsrClient client;

	private AutoCloseable subscription;

	@Activate
	void activate() {
		subscription = client.consumer().addServiceListener(INTERFACE, null, this::onEvent);
		LOG.info("[DDSR-Client] PaymentEventLog listening for " + INTERFACE);
	}

	private void onEvent(ServiceEvent event) {
		String provider = event.getReference() != null && event.getReference().getProvider() != null
				? event.getReference().getProvider().getName()
				: "(provider not in document)";
		LOG.info("[DDSR-Client] EVENT " + event.getType()
				+ " ref=" + (event.getReference() != null ? event.getReference().getId() : "?")
				+ " provider=" + provider
				+ (event.getReasonCode() != null ? " reason=" + event.getReasonCode() : "")
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
