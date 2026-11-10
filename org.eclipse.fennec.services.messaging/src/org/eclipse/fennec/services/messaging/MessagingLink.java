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

package org.eclipse.fennec.services.messaging;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gecko.messaging.api.Message;
import org.gecko.messaging.api.MessageContext;
import org.gecko.messaging.api.MessageContextBuilder;
import org.gecko.messaging.api.MessagePublisher;
import org.gecko.messaging.api.MessageSubscription;
import org.gecko.messaging.api.MessagingConstants;
import org.gecko.messaging.api.runtime.ConnectionEvent;
import org.gecko.messaging.api.runtime.ConnectionListener;
import org.gecko.messaging.api.runtime.ConnectionState;
import org.gecko.messaging.api.runtime.MessageServiceRuntime;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.promise.Promise;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.tracker.ServiceTracker;

/**
 * One connection to one messaging broker.
 *
 * <p>Behind it is a Gecko Messaging instance. The link writes that
 * instance's factory configuration from what its owner knows, selects
 * the instance's services by name and deletes the configuration when it
 * is closed. The instance owns the connection: it reconnects with a
 * backoff of its own and restores every subscription before it says it
 * is connected again — which is what the client library used here before
 * was never reliable at.
 *
 * <p><b>Nothing here waits for the instance to appear.</b> An instance
 * is a component that Declarative Services activates on the thread that
 * delivers configuration events, and a link is usually opened on that
 * very thread — by a component being activated with its own
 * configuration. Waiting there would wait for itself. So
 * {@link #open} returns at once, {@link #subscribe} is a wish that comes
 * true when the instance does, and {@link #onConnected} is how an owner
 * learns that it has. {@link #awaitConnected} is for callers on a thread
 * of their own.
 *
 * <p>What travels is the owner's business. The instance runs without a
 * content mode of its own, so a payload reaches the wire as the bytes it
 * was handed over as, and the envelope this workspace writes stays the
 * one its other language reads.
 */
public final class MessagingLink implements AutoCloseable {

	private static final Logger LOG = Logger.getLogger(MessagingLink.class.getName());

	/** What a subscription hands its messages to. */
	public interface Receiver {

		/**
		 * @param topic   the topic the message was published to — the
		 *                concrete one, also under a wildcard filter
		 * @param payload the message body
		 */
		void received(String topic, byte[] payload);
	}

	private final BundleContext context;

	private final LinkSettings settings;

	private final String filter;

	private final Object lock = new Object();

	/** What the owner wants subscribed, whether or not it is yet. */
	private final List<Subscribed> wanted = new CopyOnWriteArrayList<>();

	private final List<Consumer<Boolean>> listeners = new CopyOnWriteArrayList<>();

	/**
	 * Listeners run here, not on the instance's event thread: the
	 * instance asks its listeners not to block, and what an owner does
	 * on a connect — subscribe, announce, read a snapshot — does.
	 */
	private final ExecutorService callbacks;

	private Configuration configuration;

	private ServiceRegistration<ConnectionListener> listenerRegistration;

	private ServiceTracker<MessageServiceRuntime, Instance> tracker;

	private volatile Instance instance;

	private boolean connected;

	private boolean everConnected;

	private volatile boolean closed;

	private MessagingLink(BundleContext context, LinkSettings settings) {
		this.context = context;
		this.settings = settings;
		this.filter = "(" + MessagingConstants.MESSAGING_NAME_PROPERTY + "=" + settings.name() + ")";
		this.callbacks = Executors.newSingleThreadExecutor(task -> {
			Thread thread = new Thread(task, "fennec-messaging-" + settings.name());
			thread.setDaemon(true);
			return thread;
		});
	}

	/**
	 * Open a link. Returns at once; the connection follows.
	 *
	 * @throws IllegalStateException    without Configuration Admin, or
	 *                                  when the configuration cannot be
	 *                                  written
	 * @throws IllegalArgumentException for a URL no binding speaks to
	 */
	public static MessagingLink open(BundleContext context, LinkSettings settings) {
		String factoryPid = settings.factoryPid();
		MessagingLink link = new MessagingLink(context, settings);
		link.start(factoryPid);
		return link;
	}

	private void start(String factoryPid) {
		// Listening before the instance exists: it tells a listener the
		// state it is in when it finds it, so nothing is missed.
		Hashtable<String, Object> target = new Hashtable<>();
		target.put(MessagingConstants.MESSAGING_TARGET_PROPERTY, filter);
		listenerRegistration = context.registerService(ConnectionListener.class, this::connectionChanged,
				target);
		try {
			tracker = new ServiceTracker<>(context,
					context.createFilter("(&(objectClass=" + MessageServiceRuntime.class.getName() + ")"
							+ filter + ")"),
					null) {

				@Override
				public Instance addingService(ServiceReference<MessageServiceRuntime> reference) {
					return bind();
				}

				@Override
				public void removedService(ServiceReference<MessageServiceRuntime> reference,
						Instance gone) {
					unbind(gone);
				}
			};
		} catch (InvalidSyntaxException unnameable) {
			listenerRegistration.unregister();
			throw new IllegalArgumentException("'" + settings.name() + "' cannot name a messaging instance",
					unnameable);
		}
		tracker.open();

		ServiceReference<ConfigurationAdmin> admin = context.getServiceReference(ConfigurationAdmin.class);
		if (admin == null) {
			close();
			throw new IllegalStateException("a messaging link needs Configuration Admin, and there is none");
		}
		try {
			ConfigurationAdmin configurations = context.getService(admin);
			configuration = configurations.getFactoryConfiguration(factoryPid, settings.name(), "?");
			configuration.update(settings.configuration());
		} catch (IOException | RuntimeException unwritable) {
			close();
			throw new IllegalStateException("could not configure the messaging instance for " + settings,
					unwritable);
		} finally {
			context.ungetService(admin);
		}
	}

	/**
	 * Subscribe to a topic filter.
	 *
	 * <p>With the link connected, the subscription is in place on the
	 * broker when this returns. Without, it is made with the next
	 * connect. Either way it survives every connection loss after that.
	 *
	 * @return closing it ends the subscription. Idempotent.
	 * @throws IllegalStateException when the broker refuses it
	 */
	public AutoCloseable subscribe(String topicFilter, int qos, Receiver receiver) {
		Subscribed subscribed = new Subscribed(topicFilter, qos, receiver);
		wanted.add(subscribed);
		Instance current = instance;
		if (current != null) {
			try {
				subscribed.on(current);
			} catch (RuntimeException refused) {
				wanted.remove(subscribed);
				throw refused;
			}
		}
		return () -> {
			wanted.remove(subscribed);
			subscribed.off();
		};
	}

	/**
	 * Publish, and wait for the broker to have it.
	 *
	 * @param timeoutMillis how long to wait for the hand-over; zero or
	 *                      less does not wait, and then only a failure
	 *                      that is already known is reported
	 * @throws Exception when the link is not connected, the hand-over
	 *                   failed or did not happen in time
	 */
	public void publish(String topic, byte[] payload, int qos, long timeoutMillis) throws Exception {
		Instance current = instance;
		if (current == null) {
			throw new IllegalStateException("no connection to " + settings.url() + " yet");
		}
		Promise<Void> handedOver = current.publisher.publish(builder -> builder.channel(topic)
				.extensionEntry(MessagingConstants.EXTENSION_QOS, qos)
				.content(ByteBuffer.wrap(payload))
				.buildMessage());
		if (timeoutMillis > 0) {
			CountDownLatch settled = new CountDownLatch(1);
			handedOver.onResolve(settled::countDown);
			if (!settled.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
				throw new TimeoutException("the broker at " + settings.url() + " did not take the message on "
						+ topic + " within " + timeoutMillis + " ms");
			}
		}
		if (handedOver.isDone()) {
			rethrow(handedOver.getFailure());
		}
	}

	/**
	 * Be told about every connect: the first, and each one after a
	 * loss. Told at once when the link is connected already.
	 *
	 * <p>The argument says whether it is a reconnect — messages
	 * published while the link was away did not arrive, and whoever
	 * depends on them reads again.
	 *
	 * @return closing it stops the telling
	 */
	public AutoCloseable onConnected(Consumer<Boolean> listener) {
		boolean now;
		synchronized (lock) {
			listeners.add(listener);
			now = connected;
		}
		if (now) {
			tell(List.of(listener), false);
		}
		return () -> listeners.remove(listener);
	}

	/**
	 * Wait for the connection. Not for a thread that delivers
	 * configuration events — see the class comment.
	 *
	 * @return whether the link is connected
	 */
	public boolean awaitConnected(long timeout, TimeUnit unit) throws InterruptedException {
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		synchronized (lock) {
			while (!connected && !closed) {
				long remaining = deadline - System.nanoTime();
				if (remaining <= 0) {
					return false;
				}
				TimeUnit.NANOSECONDS.timedWait(lock, remaining);
			}
			return connected;
		}
	}

	/** Whether the link is connected right now. */
	public boolean isConnected() {
		synchronized (lock) {
			return connected;
		}
	}

	/**
	 * Whether the link has been connected at all — what tells a broker
	 * that is away for a moment from one that was never there.
	 */
	public boolean hasBeenConnected() {
		synchronized (lock) {
			return everConnected;
		}
	}

	/** Where this link goes. */
	public String url() {
		return settings.url();
	}

	/** End the subscriptions, take the instance away. Idempotent. */
	@Override
	public void close() {
		synchronized (lock) {
			if (closed) {
				return;
			}
			closed = true;
			connected = false;
			lock.notifyAll();
		}
		listeners.clear();
		for (Subscribed subscribed : wanted) {
			subscribed.off();
		}
		wanted.clear();
		if (listenerRegistration != null) {
			try {
				listenerRegistration.unregister();
			} catch (IllegalStateException alreadyGone) {
				// The framework got there first.
			}
		}
		if (tracker != null) {
			tracker.close();
		}
		if (configuration != null) {
			try {
				configuration.delete();
			} catch (IOException | IllegalStateException alreadyGone) {
				// Deleted by someone else, or Configuration Admin is going
				// down with the framework and takes it along.
			}
		}
		callbacks.shutdown();
	}

	private void connectionChanged(ConnectionEvent event) {
		boolean now = event.state() == ConnectionState.CONNECTED;
		boolean before;
		synchronized (lock) {
			if (closed) {
				return;
			}
			before = connected;
			connected = now;
			everConnected |= now;
			lock.notifyAll();
		}
		if (now && !before) {
			LOG.info("[DDSR-MSG] " + settings + (event.reconnect() ? " connected again" : " connected"));
			tell(listeners, event.reconnect());
		} else if (!now && before) {
			LOG.warning("[DDSR-MSG] " + settings + " lost its connection"
					+ (event.cause() == null ? "" : ": " + event.cause()));
		} else if (event.state() == ConnectionState.CLOSED && event.cause() != null) {
			LOG.warning("[DDSR-MSG] " + settings + " gave up connecting: " + event.cause());
		}
	}

	private void tell(Collection<Consumer<Boolean>> whom, boolean reconnect) {
		for (Consumer<Boolean> listener : whom) {
			try {
				callbacks.execute(() -> {
					try {
						listener.accept(reconnect);
					} catch (RuntimeException failed) {
						LOG.log(Level.WARNING, "[DDSR-MSG] a listener of " + settings + " failed", failed);
					}
				});
			} catch (RejectedExecutionException closing) {
				return;
			}
		}
	}

	/** The instance is there: take its services and make the wishes true. */
	private Instance bind() {
		Instance found;
		try {
			found = Instance.of(context, filter);
		} catch (RuntimeException incomplete) {
			LOG.log(Level.WARNING, "[DDSR-MSG] the messaging instance of " + settings + " is incomplete",
					incomplete);
			return null;
		}
		instance = found;
		for (Subscribed subscribed : wanted) {
			try {
				subscribed.on(found);
			} catch (RuntimeException refused) {
				LOG.log(Level.WARNING, "[DDSR-MSG] could not subscribe " + subscribed.topicFilter + " on "
						+ settings, refused);
			}
		}
		return found;
	}

	private void unbind(Instance gone) {
		if (instance == gone) {
			instance = null;
		}
		synchronized (lock) {
			connected = false;
		}
		// The instance closed its streams when it stopped. What was
		// subscribed is wanted still, and a next instance gets it.
		for (Subscribed subscribed : wanted) {
			subscribed.forget(gone);
		}
		gone.release(context);
	}

	private static void rethrow(Throwable failure) throws Exception {
		if (failure instanceof Exception exception) {
			throw exception;
		}
		if (failure instanceof Error error) {
			throw error;
		}
		if (failure != null) {
			throw new IllegalStateException(failure);
		}
	}

	/** The services of one messaging instance. */
	private record Instance(ServiceReference<MessagePublisher> publisherReference, MessagePublisher publisher,
			ServiceReference<MessageSubscription> subscriptionReference, MessageSubscription subscription,
			ServiceObjects<MessageContextBuilder> builders) {

		static Instance of(BundleContext context, String filter) {
			try {
				ServiceReference<MessagePublisher> publisher = only(
						context.getServiceReferences(MessagePublisher.class, filter), "publisher");
				ServiceReference<MessageSubscription> subscription = only(
						context.getServiceReferences(MessageSubscription.class, filter), "subscription");
				ServiceReference<MessageContextBuilder> builder = only(
						context.getServiceReferences(MessageContextBuilder.class, filter), "context builder");
				return new Instance(publisher, context.getService(publisher), subscription,
						context.getService(subscription), context.getServiceObjects(builder));
			} catch (InvalidSyntaxException impossible) {
				throw new IllegalStateException(impossible);
			}
		}

		private static <S> ServiceReference<S> only(Collection<ServiceReference<S>> found, String what) {
			if (found.isEmpty()) {
				throw new IllegalStateException("no " + what);
			}
			return found.iterator().next();
		}

		MessageContext contextFor(String topicFilter, int qos) {
			MessageContextBuilder builder = builders.getService();
			try {
				return builder.channel(topicFilter)
						.extensionEntry(MessagingConstants.EXTENSION_QOS, qos)
						.buildContext();
			} finally {
				builders.ungetService(builder);
			}
		}

		void release(BundleContext context) {
			try {
				context.ungetService(publisherReference);
				context.ungetService(subscriptionReference);
			} catch (IllegalStateException bundleStopping) {
				// Released with the bundle.
			}
		}
	}

	/** One wish: a topic filter, and where its messages go. */
	private final class Subscribed {

		private final String topicFilter;

		private final int qos;

		private final Receiver receiver;

		private Instance on;

		private PushStream<Message> stream;

		private boolean ended;

		Subscribed(String topicFilter, int qos, Receiver receiver) {
			this.topicFilter = topicFilter;
			this.qos = qos;
			this.receiver = receiver;
		}

		/** Subscribe on this instance, once. */
		synchronized void on(Instance target) {
			if (ended || on == target) {
				return;
			}
			PushStream<Message> opened = target.subscription.subscribe(target.contextFor(topicFilter, qos));
			Promise<Void> running = opened.forEach(this::deliver);
			if (running.isDone()) {
				Throwable refused;
				try {
					refused = running.getFailure();
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					refused = interrupted;
				}
				if (refused != null) {
					throw new IllegalStateException("the broker at " + settings.url() + " refused "
							+ topicFilter, refused);
				}
			}
			running.onFailure(failed -> LOG.warning("[DDSR-MSG] the subscription to " + topicFilter
					+ " on " + settings + " ended: " + failed));
			on = target;
			stream = opened;
		}

		synchronized void forget(Instance gone) {
			if (on == gone) {
				on = null;
				stream = null;
			}
		}

		synchronized void off() {
			ended = true;
			if (stream != null) {
				try {
					stream.close();
				} catch (RuntimeException alreadyClosed) {
					// Closed with its instance.
				}
				stream = null;
			}
			on = null;
		}

		/**
		 * Never throws: a consumer that fails ends its stream, and one
		 * message that cannot be handled must not take the subscription
		 * down with it.
		 */
		private void deliver(Message message) {
			try {
				ByteBuffer content = message.payload();
				byte[] payload = new byte[content.remaining()];
				content.get(payload);
				receiver.received(message.getContext().getChannel(), payload);
			} catch (RuntimeException failed) {
				LOG.log(Level.WARNING, "[DDSR-MSG] a message on " + topicFilter + " could not be handled",
						failed);
			}
		}
	}
}
