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
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.gecko.messaging.api.Message;
import org.gecko.messaging.api.MessageContext;
import org.gecko.messaging.api.MessageContextBuilder;
import org.gecko.messaging.api.MessagePublisher;
import org.gecko.messaging.api.MessageSubscription;
import org.gecko.messaging.api.MessagingConstants;
import org.gecko.messaging.api.replyto.MessagingSession;
import org.gecko.messaging.api.replyto.ReplyToPublisher;
import org.gecko.messaging.api.replyto.ReplyToSessionHandler;
import org.gecko.messaging.api.util.Flows;
import org.gecko.messaging.api.replyto.ReplyToSubscriptionConstants;
import org.gecko.messaging.api.replyto.SessionEndedException;
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
 * <p>What travels is a CloudEvent, and the instance writes it. The link
 * is handed an envelope — the CloudEvents model — and the payload
 * beside it, and the instance, running in structured mode, puts both on
 * the wire as one JSON document and reads them back from one. The
 * owner decides what the event says; how an event looks on a wire is
 * the messaging project's, the same on every protocol it binds.
 */
public final class MessagingLink implements AutoCloseable {

	private static final Logger LOG = Logger.getLogger(MessagingLink.class.getName());

	/** What a subscription hands its messages to. */
	public interface Receiver {

		/**
		 * @param topic   the topic the message was published to — the
		 *                concrete one, also under a wildcard filter
		 * @param message the envelope and the payload it carried
		 */
		void received(String topic, CloudEventCodec.Message message);
	}

	/** What a subscription that answers hands its requests to. */
	public interface RequestReceiver {

		/**
		 * @param topic   the topic the request was published to
		 * @param request the envelope and the payload it carried
		 * @param reply   where the answer goes — the address the request
		 *                itself named, with its correlation
		 */
		void received(String topic, CloudEventCodec.Message request, Reply reply);
	}

	/** The way back to whoever sent a request. */
	public interface Reply {

		/**
		 * Answer, and wait for the broker to have it.
		 *
		 * @throws Exception when the request named no reply address, or
		 *                   the answer could not be handed over
		 */
		void send(CloudEventCodec.Message answer) throws Exception;

	}

	/** How long an answer may take to reach the broker. */
	private static final long REPLY_PUBLISH_TIMEOUT_MILLIS = 30_000;

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
		return want(new Subscribed(topicFilter, qos, (instance, message) -> {
			CloudEventCodec.Message carried = Envelopes.fromMessage(message);
			if (carried != null) {
				receiver.received(message.getContext().getChannel(), carried);
			}
		}));
	}

	/**
	 * The buffer a subscription holds what it has not handed on yet in,
	 * and what happens when it is full: the Gecko Messaging
	 * per-subscription buffer ({@code bufferSize}, {@code queuePolicy}).
	 *
	 * @param size   the messages it holds at most; zero or less for the
	 *               instance's own size
	 * @param policy {@code BLOCK}, {@code DISCARD_OLDEST} or {@code FAIL}
	 */
	public record Buffer(int size, String policy) {
	}

	/**
	 * Subscribe to a topic filter through a buffer of its own (#252): what
	 * the receiver has not taken yet waits there, and when it is full the
	 * policy decides — wait, drop the oldest, or end the subscription.
	 * The receiver may hold up delivery as long as it likes; that is what
	 * fills the buffer.
	 *
	 * <p>A subscription that ends on its own — {@code FAIL} on a full
	 * buffer — is not made again: {@code ended} is told why, once.
	 *
	 * @return closing it ends the subscription. Idempotent.
	 * @throws IllegalStateException when the broker refuses it
	 */
	public AutoCloseable subscribe(String topicFilter, int qos, Buffer buffer, Receiver receiver,
			Consumer<Throwable> ended) {
		Subscribed subscribed = new Subscribed(topicFilter, qos, (instance, message) -> {
			CloudEventCodec.Message carried = Envelopes.fromMessage(message);
			if (carried != null) {
				receiver.received(message.getContext().getChannel(), carried);
			}
		});
		subscribed.buffer = buffer;
		subscribed.onEnded = failure -> {
			wanted.remove(subscribed);
			ended.accept(failure);
		};
		return want(subscribed);
	}

	/**
	 * Subscribe to requests: messages that say where their answer goes.
	 *
	 * <p>The receiver is handed a way back that already knows the reply
	 * address and the correlation, wherever the request carried them —
	 * in properties of the protocol where it has them, in the envelope
	 * where it has not. That difference ends here.
	 *
	 * @return closing it ends the subscription. Idempotent.
	 * @throws IllegalStateException when the broker refuses it
	 */
	public AutoCloseable serve(String topicFilter, int qos, RequestReceiver receiver) {
		return want(new Subscribed(topicFilter, qos, (instance, message) -> {
			CloudEventCodec.Message carried = Envelopes.fromMessage(message);
			if (carried == null) {
				return;
			}
			MessageContext request = message.getContext();
			receiver.received(request.getChannel(), carried, answer -> {
				if (request.getReplyToChannel() == null) {
					throw new IllegalStateException("the request on " + request.getChannel()
							+ " named no reply address");
				}
				// The correlation the requester waits for: the one its
				// request carried, when it carried one. A requester that
				// correlates by the request's own id sent none, and then
				// the answer's envelope — which names that id — says it.
				await(instance.publisher.publish(builder -> Envelopes.toMessage(builder,
						request.getReplyToChannel(), answer, qos, request.getCorrelationId())),
						REPLY_PUBLISH_TIMEOUT_MILLIS, request.getReplyToChannel());
			});
		}));
	}

	/**
	 * Publish a request and wait for its answer.
	 *
	 * <p>For a call whose flavor names no place for answers. The
	 * instance subscribes a reply channel of its own before the request
	 * goes out, names it in the request and takes the one answer that
	 * correlates — by the id of the request's envelope, which is what
	 * this project correlates by on every wire.
	 *
	 * @return the answer
	 * @throws TimeoutException              when no answer came in time
	 * @throws UnsupportedOperationException when the instance offers no
	 *                                       request/reply
	 * @throws Exception                     when the link is not
	 *                                       connected or the request
	 *                                       could not be handed over
	 */
	public CloudEventCodec.Message request(String topic, CloudEventCodec.Message message, int qos,
			long timeoutMillis) throws Exception {
		Instance current = instance;
		if (current == null) {
			throw new IllegalStateException("no connection to " + settings.address() + " yet");
		}
		if (current.requests == null) {
			throw new UnsupportedOperationException("the connection to " + settings.address()
					+ " offers no request and reply");
		}
		MessageContextBuilder builder = current.builders.getService();
		Message request;
		try {
			builder.extensionEntry(MessagingConstants.EXTENSION_REPLY_TIMEOUT, timeoutMillis);
			request = Envelopes.toMessage(builder, topic, message, qos, message.attributes().getEventId());
		} finally {
			current.builders.ungetService(builder);
		}
		Promise<Message> answered = current.requests.publishWithReply(request);
		// A little longer than the instance waits itself, so that its
		// timeout — which names the reason — is the one that is reported.
		await(answered, timeoutMillis + 1_000, topic);
		CloudEventCodec.Message answer = Envelopes.fromMessage(answered.getValue());
		if (answer == null) {
			throw new IllegalStateException("the answer on " + topic + " is no CloudEvent");
		}
		return answer;
	}

	/**
	 * A request whose answer is told, not waited for.
	 *
	 * <p>The same request as {@link #request}, for a caller that has
	 * something better to do with its thread: the stage completes on a
	 * thread of the messaging instance when the answer is there, and
	 * fails the way {@code request} throws — with a
	 * {@link TimeoutException} when the instance waited in vain.
	 */
	public CompletionStage<CloudEventCodec.Message> requestLater(String topic, CloudEventCodec.Message message,
			int qos, long timeoutMillis) {
		Instance current = instance;
		if (current == null) {
			return CompletableFuture
					.failedFuture(new IllegalStateException("no connection to " + settings.address() + " yet"));
		}
		if (current.requests == null) {
			return CompletableFuture.failedFuture(new UnsupportedOperationException(
					"the connection to " + settings.address() + " offers no request and reply"));
		}
		try {
			MessageContextBuilder builder = current.builders.getService();
			Message request;
			try {
				builder.extensionEntry(MessagingConstants.EXTENSION_REPLY_TIMEOUT, timeoutMillis);
				request = Envelopes.toMessage(builder, topic, message, qos, message.attributes().getEventId());
			} finally {
				current.builders.ungetService(builder);
			}
			return stageOf(current.requests.publishWithReply(request)).thenApply(answered -> {
				CloudEventCodec.Message answer = Envelopes.fromMessage(answered);
				if (answer == null) {
					throw new IllegalStateException("the answer on " + topic + " is no CloudEvent");
				}
				return answer;
			});
		} catch (RuntimeException unsendable) {
			return CompletableFuture.failedFuture(unsendable);
		}
	}

	/**
	 * How a session's messages end with a reason of the application: the
	 * failure a {@link SessionReceiver}'s publisher ends with to send one
	 * last message - a Diagnostic, say - and end the session with the
	 * reason (org.gecko.messaging#32). The requester receives the message,
	 * then the session fails with the reason. Any other failure reaches
	 * the requester only as the library's fixed reason.
	 */
	public static final class SessionEnd extends RuntimeException {

		private static final long serialVersionUID = 1L;

		private final transient CloudEventCodec.Message last;

		private final String reason;

		/**
		 * @param last   the message the requester receives last
		 * @param reason short and without internals, e.g. a code
		 */
		public SessionEnd(CloudEventCodec.Message last, String reason) {
			super("the session ended: " + reason);
			this.last = last;
			this.reason = reason;
		}

		public CloudEventCodec.Message last() {
			return last;
		}

		public String reason() {
			return reason;
		}
	}

	/** What a session handler hands the call that opened a session to. */
	public interface SessionReceiver {

		/**
		 * @param channel the channel the session was opened on
		 * @param call    the envelope and payload of the request that
		 *                opened it
		 * @return the messages to send to the requester, asked for one at
		 *         a time; the session ends when they end, and they are
		 *         cancelled when the session ends first — the requester
		 *         closed it, or went silent for longer than its lease
		 */
		Flow.Publisher<CloudEventCodec.Message> opened(String channel, CloudEventCodec.Message call);
	}

	/**
	 * A request that opens a duplex session of the messaging library,
	 * and the messages the responder sends in it.
	 *
	 * <p>Nothing is published before the publisher has a subscriber. The
	 * session is opened then, and the subscriber is told once the
	 * responder accepted it. Its messages are taken as the subscriber
	 * asks. The publisher completes when the responder ends the session
	 * and fails when the session could not be opened, its lease expired
	 * or the responder failed. A subscriber that cancels closes the
	 * session, and the responder is told — that is how a stream the
	 * caller no longer wants stops at its source.
	 *
	 * <p>The session listens on the reply channel the call names
	 * ({@code replyto} in its envelope) — where the flavor names a reply
	 * tree, the consumer's subtree there, which a broker's ACL holds to
	 * the consumer as for any answer. A call that names none gets a
	 * channel the library generates.
	 *
	 * <p>The responder names the channel this side then writes to; the
	 * library accepts a generated one, and one below
	 * {@code sessionChannelPrefix} - where a provider of this project
	 * listens - for this session (org.gecko.messaging#36). Anything else
	 * ends the session before a message is sent there.
	 *
	 * @param sessionChannelPrefix where the responder's session channels
	 *                             lie, or null for generated ones only
	 * @param timeoutMillis        how long the responder may take to accept
	 */
	public Flow.Publisher<CloudEventCodec.Message> openSession(String topic, CloudEventCodec.Message message,
			int qos, String sessionChannelPrefix, long timeoutMillis) {
		return subscriber -> {
			Promise<MessagingSession> opening;
			try {
				Instance current = instance;
				if (current == null) {
					throw new IllegalStateException("no connection to " + settings.address() + " yet");
				}
				if (current.requests == null) {
					throw new UnsupportedOperationException("the connection to " + settings.address()
							+ " offers no request and reply");
				}
				MessageContextBuilder builder = current.builders.getService();
				Message request;
				try {
					builder.extensionEntry(MessagingConstants.EXTENSION_REPLY_TIMEOUT, timeoutMillis);
					if (sessionChannelPrefix != null) {
						// The responder's channel is the prefix, a "/" and an id.
						builder.extensionEntry(MessagingConstants.EXTENSION_SESSION_CHANNEL_ALLOWED,
								sessionChannelPrefix + "/");
					}
					request = Envelopes.toMessage(builder, topic, message, qos, message.attributes().getEventId());
				} finally {
					current.builders.ungetService(builder);
				}
				opening = current.requests.openSession(request);
			} catch (RuntimeException unopenable) {
				MessagingLink.<CloudEventCodec.Message>failed(unopenable).subscribe(subscriber);
				return;
			}
			opening.onFailure(unopened -> MessagingLink.<CloudEventCodec.Message>failed(unopened).subscribe(subscriber));
			opening.onSuccess(session -> {
				Flow.Publisher<CloudEventCodec.Message> received = Flows.toPublisher(
						session.receive().map(Envelopes::fromMessage).filter(answer -> answer != null));
				received.subscribe(new Flow.Subscriber<CloudEventCodec.Message>() {
					@Override
					public void onSubscribe(Flow.Subscription subscription) {
						subscriber.onSubscribe(new Flow.Subscription() {
							@Override
							public void request(long n) {
								subscription.request(n);
							}

							@Override
							public void cancel() {
								subscription.cancel();
								session.close();
							}
						});
					}

					@Override
					public void onNext(CloudEventCodec.Message answer) {
						subscriber.onNext(answer);
					}

					@Override
					public void onError(Throwable failure) {
						session.close();
						subscriber.onError(failure);
					}

					@Override
					public void onComplete() {
						subscriber.onComplete();
					}
				});
			});
		};
	}

	/**
	 * Answer the sessions opened on a channel: what a stream a service
	 * returns travels in.
	 *
	 * <p>A handler of the messaging library's whiteboard, bound to this
	 * link's instance. Each session listens for the requester's messages
	 * on a channel below {@code sessionChannelPrefix}, so a broker's ACL
	 * can grant the requester that tree. The library accepts each
	 * session, keeps it alive
	 * with its lease, and sends what the receiver returns once the
	 * requester is ready. Whatever the receiver returns is asked for one
	 * message at a time, within the instance's send window.
	 *
	 * @return closing it stops answering, and ends the sessions open now.
	 *         Idempotent.
	 */
	public AutoCloseable serveSessions(String channel, String sessionChannelPrefix, int qos,
			SessionReceiver receiver) {
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(ReplyToSubscriptionConstants.REPLY_TO_SUBSCRIPTION_CHANNEL_PROPERTY, channel);
		properties.put(ReplyToSubscriptionConstants.REPLY_TO_SUBSCRIPTION_SESSION_CHANNEL_PREFIX_PROPERTY,
				sessionChannelPrefix);
		properties.put(MessagingConstants.MESSAGING_TARGET_PROPERTY, filter);
		ReplyToSessionHandler handler = (openRequest, messages, responseBuilders) -> {
			// Nothing comes from the requester yet: a stream argument is not
			// carried (#248). Connected, as the library asks, and dropped.
			messages.forEach(ignored -> {
				// no stream arguments yet
			});
			CloudEventCodec.Message call = Envelopes.fromMessage(openRequest);
			String reply = openRequest.getContext().getReplyToChannel();
			String session = openRequest.getContext().getCorrelationId();
			if (call == null) {
				LOG.warning("[DDSR-MSG] a session on " + channel + " was opened with no CloudEvent; ended");
				return Flows.fromPublisher(MessagingLink.<Message>failed(
						new IllegalArgumentException("a session opened with no CloudEvent")));
			}
			// The application's end, in the library's words: its last message
			// and its reason travel, nothing else of the failure.
			Flow.Publisher<CloudEventCodec.Message> answers = endedAs(receiver.opened(channel, call),
					ended -> new SessionEndedException(ended.reason(),
							Envelopes.toMessage(responseBuilders.get(), reply, ended.last(), qos, session)));
			return Flows.fromPublisher(answers)
					.map(answer -> Envelopes.toMessage(responseBuilders.get(), reply, answer, qos, session));
		};
		ServiceRegistration<ReplyToSessionHandler> registration = context.registerService(ReplyToSessionHandler.class,
				handler, properties);
		AtomicBoolean closedOnce = new AtomicBoolean();
		return () -> {
			if (closedOnce.compareAndSet(false, true)) {
				try {
					registration.unregister();
				} catch (IllegalStateException alreadyGone) {
					// unregistered with the bundle
				}
			}
		};
	}

	/** The same publisher, a {@link SessionEnd} it fails with told as the library's end. */
	private static <T> Flow.Publisher<T> endedAs(Flow.Publisher<T> source,
			Function<SessionEnd, ? extends Throwable> told) {
		return subscriber -> source.subscribe(new Flow.Subscriber<T>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscriber.onSubscribe(subscription);
			}

			@Override
			public void onNext(T element) {
				subscriber.onNext(element);
			}

			@Override
			public void onError(Throwable failure) {
				Throwable other = failure;
				if (failure instanceof SessionEnd ended) {
					try {
						other = told.apply(ended);
					} catch (RuntimeException unsayable) {
						LOG.log(Level.FINE, "[DDSR-MSG] the end of a session could not be written", unsayable);
					}
				}
				subscriber.onError(other);
			}

			@Override
			public void onComplete() {
				subscriber.onComplete();
			}
		});
	}

	/** A publisher that fails at once, for a stream that could not be started. */
	private static <T> Flow.Publisher<T> failed(Throwable failure) {
		return subscriber -> {
			subscriber.onSubscribe(new Flow.Subscription() {
				@Override
				public void request(long n) {
					// nothing will come
				}

				@Override
				public void cancel() {
					// nothing to stop
				}
			});
			subscriber.onError(failure);
		};
	}

	/**
	 * Publish without waiting: the stage completes when the broker has
	 * the message, and fails when the hand-over did.
	 */
	public CompletionStage<Void> publishLater(String topic, CloudEventCodec.Message message, int qos) {
		Instance current = instance;
		if (current == null) {
			return CompletableFuture
					.failedFuture(new IllegalStateException("no connection to " + settings.address() + " yet"));
		}
		try {
			return stageOf(current.publisher
					.publish(builder -> Envelopes.toMessage(builder, topic, message, qos, null)));
		} catch (RuntimeException unsendable) {
			return CompletableFuture.failedFuture(unsendable);
		}
	}

	/**
	 * Be told when the link is connected, once — at once when it is
	 * already. The way to wait for a connection without a thread: what
	 * {@link #awaitConnected} is for a caller that may block.
	 *
	 * @return completes when connected, fails with a
	 *         {@link TimeoutException} when that took too long
	 */
	public CompletionStage<Void> whenConnected(long timeout, TimeUnit unit) {
		CompletableFuture<Void> ready = new CompletableFuture<>();
		AutoCloseable telling = onConnected(again -> ready.complete(null));
		ready.orTimeout(timeout, unit).whenComplete((connected, failure) -> {
			try {
				telling.close();
			} catch (Exception ignored) {
				// Closing a listener registration does not fail.
			}
		});
		return ready;
	}

	/** A promise of the instance as a stage, failed with the promise's own failure. */
	private static <T> CompletableFuture<T> stageOf(Promise<T> promise) {
		CompletableFuture<T> stage = new CompletableFuture<>();
		promise.onResolve(() -> {
			try {
				Throwable failure = promise.getFailure();
				if (failure != null) {
					stage.completeExceptionally(failure);
				} else {
					stage.complete(promise.getValue());
				}
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				stage.completeExceptionally(interrupted);
			} catch (InvocationTargetException unreadable) {
				stage.completeExceptionally(unreadable.getCause());
			}
		});
		return stage;
	}

	private AutoCloseable want(Subscribed subscribed) {
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
	 * @param message       the envelope and its payload; a payload of
	 *                      null is an event that carries none
	 * @param timeoutMillis how long to wait for the hand-over; zero or
	 *                      less does not wait, and then only a failure
	 *                      that is already known is reported
	 * @throws Exception when the link is not connected, the hand-over
	 *                   failed or did not happen in time
	 */
	public void publish(String topic, CloudEventCodec.Message message, int qos, long timeoutMillis)
			throws Exception {
		Instance current = instance;
		if (current == null) {
			throw new IllegalStateException("no connection to " + settings.address() + " yet");
		}
		Promise<Void> handedOver = current.publisher
				.publish(builder -> Envelopes.toMessage(builder, topic, message, qos, null));
		if (timeoutMillis > 0) {
			await(handedOver, timeoutMillis, topic);
		} else if (handedOver.isDone()) {
			rethrow(handedOver.getFailure());
		}
	}

	/** Wait for a promise of the instance, and fail the way it failed. */
	private void await(Promise<?> settling, long timeoutMillis, String topic) throws Exception {
		CountDownLatch settled = new CountDownLatch(1);
		settling.onResolve(settled::countDown);
		if (!settled.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException("the broker at " + settings.address() + " did not settle " + topic
					+ " within " + timeoutMillis + " ms");
		}
		rethrow(settling.getFailure());
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
		return settings.address();
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
			ServiceObjects<MessageContextBuilder> builders, ServiceReference<ReplyToPublisher> requestsReference,
			ReplyToPublisher requests) {

		static Instance of(BundleContext context, String filter) {
			try {
				ServiceReference<MessagePublisher> publisher = only(
						context.getServiceReferences(MessagePublisher.class, filter), "publisher");
				ServiceReference<MessageSubscription> subscription = only(
						context.getServiceReferences(MessageSubscription.class, filter), "subscription");
				ServiceReference<MessageContextBuilder> builder = only(
						context.getServiceReferences(MessageContextBuilder.class, filter), "context builder");
				// Only where the protocol has a reply address of its own —
				// the instance offers the service exactly then.
				Collection<ServiceReference<ReplyToPublisher>> requests = context
						.getServiceReferences(ReplyToPublisher.class, filter);
				ServiceReference<ReplyToPublisher> request = requests.isEmpty() ? null
						: requests.iterator().next();
				return new Instance(publisher, context.getService(publisher), subscription,
						context.getService(subscription), context.getServiceObjects(builder), request,
						request == null ? null : context.getService(request));
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
			return contextFor(topicFilter, qos, null);
		}

		MessageContext contextFor(String topicFilter, int qos, Buffer buffer) {
			MessageContextBuilder builder = builders.getService();
			try {
				builder.channel(topicFilter).extensionEntry(MessagingConstants.EXTENSION_QOS, qos);
				if (buffer != null) {
					if (buffer.size() > 0) {
						builder.extensionEntry(MessagingConstants.EXTENSION_BUFFER_SIZE, buffer.size());
					}
					if (buffer.policy() != null) {
						builder.extensionEntry(MessagingConstants.EXTENSION_QUEUE_POLICY, buffer.policy());
					}
				}
				return builder.buildContext();
			} finally {
				builders.ungetService(builder);
			}
		}

		void release(BundleContext context) {
			try {
				context.ungetService(publisherReference);
				context.ungetService(subscriptionReference);
				if (requestsReference != null) {
					context.ungetService(requestsReference);
				}
			} catch (IllegalStateException bundleStopping) {
				// Released with the bundle.
			}
		}
	}

	/** What a subscription does with a message of the instance it is on. */
	private interface Handler {
		void handle(Instance from, Message message);
	}

	/** One wish: a topic filter, and where its messages go. */
	private final class Subscribed {

		private final String topicFilter;

		private final int qos;

		private final Handler handler;

		private Instance on;

		private PushStream<Message> stream;

		private boolean ended;

		/** A buffer of its own, or null for the instance's. */
		Buffer buffer;

		/** Told when the subscription ends on its own; null for one that is made again. */
		Consumer<Throwable> onEnded;

		Subscribed(String topicFilter, int qos, Handler handler) {
			this.topicFilter = topicFilter;
			this.qos = qos;
			this.handler = handler;
		}

		/** Subscribe on this instance, once. */
		synchronized void on(Instance target) {
			if (ended || on == target) {
				return;
			}
			PushStream<Message> opened = target.subscription.subscribe(target.contextFor(topicFilter, qos, buffer));
			Promise<Void> running = opened.forEach(message -> deliver(target, message));
			if (running.isDone()) {
				Throwable refused;
				try {
					refused = running.getFailure();
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					refused = interrupted;
				}
				if (refused != null) {
					throw new IllegalStateException("the broker at " + settings.address() + " refused "
							+ topicFilter, refused);
				}
			}
			running.onFailure(failed -> {
				Consumer<Throwable> tell;
				synchronized (this) {
					tell = onEnded;
					if (tell != null) {
						// Ended on its own — a full buffer that fails. Not
						// made again: whoever subscribed decides what next.
						ended = true;
						stream = null;
						on = null;
						onEnded = null;
					}
				}
				if (tell != null) {
					tell.accept(failed);
				} else {
					LOG.warning("[DDSR-MSG] the subscription to " + topicFilter + " on " + settings + " ended: "
							+ failed);
				}
			});
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
		private void deliver(Instance from, Message message) {
			try {
				handler.handle(from, message);
			} catch (RuntimeException failed) {
				LOG.log(Level.WARNING, "[DDSR-MSG] a message on " + topicFilter + " could not be handled",
						failed);
			}
		}
	}
}
