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

package org.eclipse.fennec.tags.ddsr;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.invocation.BroadcastReceivers;
import org.eclipse.fennec.tags.service.TagService;

/**
 * The broker's events under one tag (#279): the registrations the tag
 * covers, and what becomes of them. Served from a copy of the broker's
 * {@code BrokerEvents} document, so a consumer hears it as it hears the
 * broker, and the distribution makes it one broadcast per tag.
 *
 * <p>A withdrawal names only its reference — the implementation is gone by
 * then — so the references shown under this tag, by its lookup or its
 * events, are remembered, and what follows for them is passed on. A registration tagged after it was
 * announced is not heard here; a consumer that looks up again finds it.
 *
 * <p>A receiver that says which consumer it is still ties its connection
 * to that consumer's session: that part is the broker's and is handed on.
 */
public final class ScopedEvents implements BroadcastReceivers {

	private final Supplier<Object> broker;

	private final TagService tags;

	private final String tag;

	/** The references shown under this tag — by a lookup or an event — and still alive. */
	private final Set<String> announced;

	ScopedEvents(Supplier<Object> broker, TagService tags, String tag, Set<String> announced) {
		this.broker = broker;
		this.tags = tags;
		this.tag = tag;
		this.announced = announced;
	}

	@SuppressWarnings("unchecked")
	public Flow.Publisher<ServiceEvent> events() throws Throwable {
		Flow.Publisher<ServiceEvent> all = (Flow.Publisher<ServiceEvent>) ScopedLookup.call(broker.get(), "events");
		return subscriber -> all.subscribe(new Flow.Subscriber<>() {

			private Flow.Subscription upstream;

			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				upstream = subscription;
				subscriber.onSubscribe(subscription);
			}

			@Override
			public void onNext(ServiceEvent event) {
				if (heard(event)) {
					subscriber.onNext(event);
				} else {
					// Not this receiver's: the demand it stood for is still open.
					upstream.request(1);
				}
			}

			@Override
			public void onError(Throwable failure) {
				subscriber.onError(failure);
			}

			@Override
			public void onComplete() {
				subscriber.onComplete();
			}
		});
	}

	private boolean heard(ServiceEvent event) {
		String id = event.getReference() == null ? null : event.getReference().getId();
		if (id == null) {
			return false;
		}
		ServiceEventType type = event.getType();
		if (type == ServiceEventType.REGISTERED || type == ServiceEventType.MODIFIED) {
			if (visible(event)) {
				announced.add(id);
				return true;
			}
			return false;
		}
		if (type == ServiceEventType.UNREGISTERING || type == ServiceEventType.RETIRED) {
			return announced.remove(id);
		}
		return announced.contains(id);
	}

	private boolean visible(ServiceEvent event) {
		if (!(event.getReference().getProvider() instanceof ServiceProvider provider)) {
			return false;
		}
		Visibility visible = Visibility.under(tags, tag);
		for (ServiceImplementation implementation : provider.getImplementations()) {
			if (visible.implementation(implementation)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void joined(String operation, Map<String, String> parameters) {
		if (broker.get() instanceof BroadcastReceivers receivers) {
			receivers.joined(operation, parameters);
		}
	}

	@Override
	public void left(String operation, Map<String, String> parameters) {
		if (broker.get() instanceof BroadcastReceivers receivers) {
			receivers.left(operation, parameters);
		}
	}
}
