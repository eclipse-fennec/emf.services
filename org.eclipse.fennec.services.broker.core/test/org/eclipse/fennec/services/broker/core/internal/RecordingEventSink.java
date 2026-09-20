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
import java.util.Collections;
import java.util.List;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.broker.core.EventSink;

/**
 * Test double for the broker's {@link EventSink}: records every event in
 * order. Shared by the lifecycle tests (#54) instead of one private copy
 * per class.
 *
 * <p>Since #124 the broker hands events to a delivery queue instead of
 * calling the sink itself, so "the call returned" and "the sink was
 * told" are two moments. Every accessor here crosses that gap first, by
 * asking the broker whether its queue is empty — counted, not slept on,
 * because a barrier that waits "long enough" is how a test passes for
 * the wrong reason. A sink with no broker attached behaves as before,
 * which is what the direct unit tests of this class want.
 */
public final class RecordingEventSink implements EventSink {

	/** Written on the delivery thread, read by the test thread. */
	private final List<ServiceEvent> events = Collections.synchronizedList(new ArrayList<>());

	private volatile Runnable barrier = () -> {
	};

	/** Waits for this broker's queue before every read. Returns this, for setUp chains. */
	public RecordingEventSink deliveredBy(DdsrBrokerImpl broker) {
		this.barrier = broker::awaitEventsDelivered;
		return this;
	}

	/** Everything the sink has been told, once the broker's queue is empty. */
	public List<ServiceEvent> received() {
		barrier.run();
		synchronized (events) {
			return List.copyOf(events);
		}
	}

	@Override
	public void publish(ServiceEvent event) {
		events.add(event);
	}

	public List<ServiceEventType> types() {
		return received().stream().map(ServiceEvent::getType).toList();
	}

	public List<String> reasons() {
		return received().stream().map(ServiceEvent::getReasonCode).toList();
	}

	/** {@code TYPE} or {@code TYPE/REASON} per event — the row language of the lifecycle matrix. */
	public List<String> signatures() {
		return received().stream().map(RecordingEventSink::signature).toList();
	}

	public static String signature(ServiceEvent event) {
		return event.getReasonCode() == null
				? event.getType().getLiteral()
				: event.getType().getLiteral() + "/" + event.getReasonCode();
	}

	public void clear() {
		barrier.run();
		events.clear();
	}
}
