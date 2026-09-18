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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

/**
 * One exported service, as Remote Service Admin hands it back.
 *
 * <p>It is both the registration the exporter holds and the reference
 * anyone else reads, because the two describe the same thing from two
 * sides and keeping them apart would only mean a second object to keep
 * in step.
 */
final class ExportedService implements ExportRegistration, ExportReference {

	private static final Logger LOG = Logger.getLogger(ExportedService.class.getName());

	private final ServiceReference<?> exported;
	private final EndpointDescription description;
	private final ExportedEndpoint endpoint;
	private final AutoCloseable announcement;
	private final Consumer<ExportedService> forget;
	private final Runnable release;
	private final AtomicBoolean open = new AtomicBoolean(true);

	ExportedService(ServiceReference<?> exported, EndpointDescription description, ExportedEndpoint endpoint,
			AutoCloseable announcement, Consumer<ExportedService> forget, Runnable release) {
		this.exported = exported;
		this.description = description;
		this.endpoint = endpoint;
		this.announcement = announcement;
		this.forget = forget;
		this.release = release;
	}

	@Override
	public ExportReference getExportReference() {
		return open.get() ? this : null;
	}

	@Override
	public ServiceReference<?> getExportedService() {
		return open.get() ? exported : null;
	}

	@Override
	public EndpointDescription getExportedEndpoint() {
		return open.get() ? description : null;
	}

	@Override
	public EndpointDescription update(Map<String, ?> properties) {
		// An update would re-announce the endpoint under changed
		// properties. Nothing here can do that half-way: saying so is
		// better than returning a description that does not match what
		// the broker holds.
		throw new UnsupportedOperationException(
				"updating an export is not implemented yet — close it and export again");
	}

	/**
	 * Withdraw first, then stop serving.
	 *
	 * <p>That order is the whole of FR-P3: a consumer that has not heard
	 * about the withdrawal yet must still find something answering at
	 * the endpoint. The other way round is a request that fails for no
	 * reason anyone can see.
	 */
	@Override
	public void close() {
		if (!open.compareAndSet(true, false)) {
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
			forget.accept(this);
			release.run();
		}
	}

	@Override
	public Throwable getException() {
		// An export that failed never becomes a registration here: the
		// failure is thrown at the caller instead. So there is never one
		// to report.
		return null;
	}
}
