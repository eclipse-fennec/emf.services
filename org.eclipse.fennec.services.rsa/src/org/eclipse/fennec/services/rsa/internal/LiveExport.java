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

package org.eclipse.fennec.services.rsa.internal;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * A service that is being served and announced right now — once,
 * however many registrations point at it.
 *
 * <p>Exporting the same service twice under the same interface and
 * configuration type must not mount it twice: two applications on one
 * path is a conflict, and two announcements of one identity is the
 * broker seeing a restart that did not happen. So the second export
 * joins the first, and the endpoint stays up until the last registration
 * lets go. The specification allows sharing and no longer requires it;
 * here it is the only thing that works.
 *
 * <p>The description is the first export's. A later export with other
 * overrides gets the same endpoint — it is the same endpoint.
 */
final class LiveExport {

	private static final Logger LOG = Logger.getLogger(LiveExport.class.getName());

	private final ExportedEndpoint endpoint;
	private final AutoCloseable announcement;
	private final Runnable release;
	private final Function<Map<String, ?>, EndpointDescription> describe;
	private final AtomicInteger users = new AtomicInteger(1);
	private volatile EndpointDescription description;
	private volatile Runnable onLast = () -> {
	};

	/**
	 * @param describe the description for a set of overrides — the same
	 *        derivation the export was made with, so that an update says
	 *        exactly what a fresh export would say
	 */
	LiveExport(EndpointDescription description, ExportedEndpoint endpoint, AutoCloseable announcement,
			Runnable release, Function<Map<String, ?>, EndpointDescription> describe) {
		this.description = description;
		this.endpoint = endpoint;
		this.announcement = announcement;
		this.release = release;
		this.describe = describe;
	}

	EndpointDescription description() {
		return description;
	}

	/**
	 * Describe the endpoint again, from the service's current properties
	 * and {@code overrides} — or the last overrides when {@code null}, as
	 * the specification reads it. The endpoint itself does not move: what
	 * changes is what is said about it.
	 */
	synchronized EndpointDescription update(Map<String, ?> overrides) {
		description = describe.apply(overrides);
		return description;
	}

	/** What to do once the last registration has let go. */
	void whenLast(Runnable action) {
		this.onLast = action;
	}

	void join() {
		users.incrementAndGet();
	}

	/**
	 * One registration less. The last one takes the endpoint down —
	 * withdraw first, then stop serving.
	 *
	 * <p>That order is the whole of FR-P3: a consumer that has not heard
	 * about the withdrawal yet must still find something answering at the
	 * endpoint. The other way round is a request that fails for no reason
	 * anyone can see.
	 */
	void leave() {
		if (users.decrementAndGet() > 0) {
			return;
		}
		try {
			announcement.close();
		} catch (Exception failure) {
			LOG.log(Level.WARNING, "[DDSR] withdrawing " + description.getId() + " failed", failure);
		}
		try {
			endpoint.close();
		} finally {
			try {
				release.run();
			} finally {
				onLast.run();
			}
		}
	}
}
