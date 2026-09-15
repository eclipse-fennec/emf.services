/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.broker.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.broker.core.EventSink;

/**
 * Test double for the broker's {@link EventSink}: records every event in
 * order. Shared by the lifecycle tests (#54) instead of one private copy
 * per class.
 */
public final class RecordingEventSink implements EventSink {

	public final List<ServiceEvent> received = new ArrayList<>();

	@Override
	public void publish(ServiceEvent event) {
		received.add(event);
	}

	public List<ServiceEventType> types() {
		return received.stream().map(ServiceEvent::getType).toList();
	}

	public List<String> reasons() {
		return received.stream().map(ServiceEvent::getReasonCode).toList();
	}

	/** {@code TYPE} or {@code TYPE/REASON} per event — the row language of the lifecycle matrix. */
	public List<String> signatures() {
		return received.stream().map(RecordingEventSink::signature).toList();
	}

	public static String signature(ServiceEvent event) {
		return event.getReasonCode() == null
				? event.getType().getLiteral()
				: event.getType().getLiteral() + "/" + event.getReasonCode();
	}

	public void clear() {
		received.clear();
	}
}
