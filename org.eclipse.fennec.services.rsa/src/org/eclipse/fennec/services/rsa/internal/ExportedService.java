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
 *
 * <p>A registration is one hand on a {@link LiveExport}; several may hold
 * the same one. Closing lets go, and the export comes down with the last
 * hand. A registration can also stand for an export that failed: then it
 * carries the exception and nothing else, which is how the specification
 * wants a failure reported when it is not the caller's fault.
 */
final class ExportedService implements ExportRegistration, ExportReference {

	private final ServiceReference<?> exported;
	private final LiveExport live;
	private final Throwable failure;
	private final Consumer<ExportedService> onUpdate;
	private final Consumer<ExportedService> onClose;
	private final AtomicBoolean open = new AtomicBoolean(true);

	private ExportedService(ServiceReference<?> exported, LiveExport live, Throwable failure,
			Consumer<ExportedService> onUpdate, Consumer<ExportedService> onClose) {
		this.exported = exported;
		this.live = live;
		this.failure = failure;
		this.onUpdate = onUpdate;
		this.onClose = onClose;
	}

	static ExportedService of(ServiceReference<?> exported, LiveExport live, Consumer<ExportedService> onUpdate,
			Consumer<ExportedService> onClose) {
		return new ExportedService(exported, live, null, onUpdate, onClose);
	}

	/** An export that did not happen, with the reason. */
	static ExportedService failed(Throwable failure) {
		return new ExportedService(null, null, failure, ignored -> {
		}, ignored -> {
		});
	}

	/** The description, whether or not the registration is still open. */
	EndpointDescription description() {
		return live == null ? null : live.description();
	}

	@Override
	public ExportReference getExportReference() {
		if (failure != null) {
			throw new IllegalStateException("this export failed", failure);
		}
		return open.get() ? this : null;
	}

	@Override
	public ServiceReference<?> getExportedService() {
		return open.get() && live != null ? exported : null;
	}

	@Override
	public EndpointDescription getExportedEndpoint() {
		return open.get() && live != null ? live.description() : null;
	}

	/**
	 * Say the endpoint anew, from the service's current properties and
	 * these overrides. The transport is untouched — the address and the
	 * contract are the endpoint's, and they did not change. Shared with
	 * every registration on the same export, because it is the same
	 * endpoint.
	 */
	@Override
	public EndpointDescription update(Map<String, ?> properties) {
		if (failure != null) {
			throw new IllegalStateException("this export failed", failure);
		}
		if (!open.get()) {
			throw new IllegalStateException("this export is closed");
		}
		EndpointDescription updated = live.update(properties);
		onUpdate.accept(this);
		return updated;
	}

	@Override
	public void close() {
		if (!open.compareAndSet(true, false)) {
			return;
		}
		try {
			if (live != null) {
				live.leave();
			}
		} finally {
			onClose.accept(this);
		}
	}

	@Override
	public Throwable getException() {
		return failure;
	}
}
