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

package org.eclipse.fennec.services.client.mqtt.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.flavor.mqtt.MqttFlavors;
import org.eclipse.fennec.services.flavor.mqtt.MqttMessages;
import org.eclipse.fennec.services.invocation.Invocations;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.cloudevents.model.ce.CloudEvent;

/**
 * Calls a service over MQTT: the consumer half of the convention whose
 * provider half is {@code provider.mqtt}.
 *
 * <p>Subscribe the reply topic first, publish the call, wait for exactly
 * the answer that correlates with it, unsubscribe. In that order, and
 * the first step is not a detail: a broker fast enough to answer before
 * the subscription exists would deliver the answer to nobody.
 *
 * <p>Where to dial is the service's own statement — the brokers its
 * flavor announces — so this invoker needs no address of its own. What
 * it is configured with is how long to wait, and that is a property of
 * the caller rather than of the service.
 *
 * <p>Synchronous, like the REST invoker and for the same reason: the
 * proxy the SDK hands out has Java methods that return values. The
 * envelope would carry an asynchronous answer perfectly well, and the
 * day the SDK grows a promise-shaped proxy this is where it starts.
 */
@Designate(ocd = MqttServiceInvoker.Config.class)
@Component(service = ServiceInvoker.class,
		configurationPid = "org.eclipse.fennec.services.client.mqtt.invoker",
		property = ServiceInvoker.FLAVOR_PROPERTY + "=MQTT")
public class MqttServiceInvoker implements ServiceInvoker {

	private static final Logger LOG = Logger.getLogger(MqttServiceInvoker.class.getName());

	@ObjectClassDefinition(name = "Fennec Services MQTT Invoker",
			description = "How this runtime calls services announced over MQTT.")
	public @interface Config {

		@AttributeDefinition(name = "Reply timeout (seconds)",
				description = "How long a call waits for its answer before failing. A transport failure, "
						+ "so the proxy rebinds and retries once — the same treatment a REST call gets "
						+ "when a provider stops answering.")
		int reply_timeout_seconds() default 10;
	}

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	private final Map<String, MqttAsyncClient> clientsByUrl = new LinkedHashMap<>();

	/** One inbox per reply subtree, shared by every call that uses it. */
	private final Map<String, ReplyInbox> inboxes = new LinkedHashMap<>();

	private Config config;

	private String source;

	/** This runtime, as a topic level — the segment its answers arrive under. */
	private String consumerSegment;

	@Activate
	void activate(BundleContext context, Config config) {
		this.config = config;
		String uuid = context.getProperty(Constants.FRAMEWORK_UUID);
		this.source = "/consumer/" + (uuid == null ? ClientOrigin.ANONYMOUS : uuid);
		this.consumerSegment = MqttFlavors.topicSegment(uuid == null ? ClientOrigin.ANONYMOUS : uuid);
	}

	@Deactivate
	void deactivate() {
		synchronized (inboxes) {
			inboxes.clear();
		}
		synchronized (clientsByUrl) {
			for (MqttAsyncClient client : clientsByUrl.values()) {
				close(client);
			}
			clientsByUrl.clear();
		}
	}

	@Override
	public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
		if (locator == null || operationName == null) {
			throw new DdsrException("locator and operationName must not be null");
		}
		MqttFlavor flavor = mqttFlavorOf(locator);
		MqttOperationFlavor operationFlavor = MqttFlavors.operationFlavor(flavor, operationName);
		if (operationFlavor == null) {
			throw new DdsrException("operation '" + operationName + "' has no MQTT flavor on this service");
		}
		ServiceOperation operation = operationFlavor.getOperation();
		if (operation == null) {
			throw new DdsrException("the MQTT flavor for '" + operationName + "' binds no operation");
		}

		String requestTopic = MqttFlavors.requestTopic(flavor, operationFlavor);
		int qos = MqttFlavors.qos(flavor, operationFlavor);
		String consumes = MqttFlavors.consumes(operationFlavor);

		CloudEvent request;
		byte[] message;
		String replyTopic;
		try {
			ServiceInvocation invocation = Invocations.invocation(operation,
					args == null ? Map.of() : args);
			byte[] document = write(Invocations.roots(invocation), consumes);
			// The reply topic has to be known before the call exists,
			// because the call carries it — so the envelope is built
			// first and its own id names the topic it answers on.
			request = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE, source, consumes);
			// Under this consumer's own subtree, not merely under an
			// unguessable id: a broker can be told who may read what only
			// if there is a name to write the rule against.
			replyTopic = MqttFlavors.replyTopic(flavor, operationFlavor, consumerSegment,
					request.getEventId());
			request.setSubject(operation.getName());
			request.getExtensions().put(CloudEvents.EXTENSION_REPLY_TO, replyTopic);
			message = MqttMessages.write(request, document);
		} catch (Exception unwritable) {
			throw new DdsrException("could not write the call to " + operationName + ": "
					+ unwritable.getMessage(), unwritable);
		}

		MqttAsyncClient client = clientFor(flavor);
		String subtree = MqttFlavors.replySubtree(flavor, operationFlavor, consumerSegment);
		ReplyInbox inbox = inboxFor(client, subtree, qos);
		inbox.expect(request.getEventId());
		try {
			MqttMessage published = new MqttMessage(message);
			published.setQos(qos);
			published.setRetained(MqttFlavors.retained());
			client.publish(requestTopic, published).waitForCompletion();

			byte[] answer = inbox.await(request.getEventId(), config.reply_timeout_seconds(),
					TimeUnit.SECONDS);
			if (answer == null) {
				throw DdsrException.transport("no answer to " + operationName + " on " + replyTopic
						+ " within " + config.reply_timeout_seconds() + "s", null);
			}
			return valueOf(answer, request, operationName);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw DdsrException.transport("waiting for the answer to " + operationName
					+ " was interrupted", interrupted);
		} catch (DdsrException alreadyOurs) {
			throw alreadyOurs;
		} catch (Exception failed) {
			throw DdsrException.transport("calling " + operationName + " over MQTT failed: "
					+ failed.getMessage(), failed);
		} finally {
			// The subscription stays: it is this consumer's inbox, not
			// this call's. What goes is the expectation of an answer.
			inbox.forget(request.getEventId());
		}
	}

	/**
	 * The inbox for one reply subtree, subscribed once.
	 *
	 * <p>Once rather than per call, because the subtree is this
	 * consumer's and the correlation in the envelope is what tells the
	 * calls apart. Several calls in flight then cost several entries in
	 * a map instead of several subscribe/unsubscribe round trips — and
	 * an answer that arrives while a caller is still setting up is
	 * waited for rather than missed.
	 */
	private ReplyInbox inboxFor(MqttAsyncClient client, String subtree, int qos) {
		synchronized (inboxes) {
			ReplyInbox inbox = inboxes.get(subtree);
			if (inbox != null) {
				return inbox;
			}
			ReplyInbox fresh = new ReplyInbox(subtree);
			try {
				client.subscribe(fresh.filter(), qos,
						(IMqttMessageListener) (topic, received) -> fresh.deliver(received.getPayload()))
						.waitForCompletion();
			} catch (Exception unsubscribable) {
				throw DdsrException.transport("could not listen for answers on " + fresh.filter()
						+ ": " + unsubscribable.getMessage(), unsubscribable);
			}
			inboxes.put(subtree, fresh);
			LOG.info("[DDSR-MQTT] listening for answers on " + fresh.filter());
			return fresh;
		}
	}

	/** What the answer says, or why there is none. */
	private Object valueOf(byte[] answer, CloudEvent request, String operationName) throws Exception {
		CloudEventCodec.Message message = MqttMessages.read(answer, CloudEvents.TYPE_INVOKE_REPLY);
		String correlation = MqttMessages.correlationOf(message.attributes());
		if (correlation != null && !correlation.equals(request.getEventId())) {
			// One topic per request, so this should not happen — which is
			// exactly why it is worth saying when it does.
			LOG.warning("[DDSR-MQTT] the answer on the reply topic of " + request.getEventId()
					+ " correlates with " + correlation);
		}
		XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(message.data()), resourceSets,
				message.attributes().getDatacontenttype());
		for (EObject root : bundle.roots()) {
			if (root instanceof ServiceInvocationResult result) {
				if (result.getDiagnostic() != null) {
					throw new DdsrException(operationName + " failed: "
							+ result.getDiagnostic().getMessage());
				}
				return Invocations.valueOf(result);
			}
		}
		throw new DdsrException("the answer to " + operationName + " carried no result");
	}

	private MqttFlavor mqttFlavorOf(ServiceLocator locator) {
		ServiceImplementation implementation = locator.implementation();
		if (implementation != null) {
			for (ServiceFlavor flavor : implementation.getFlavors()) {
				if (flavor instanceof MqttFlavor mqtt) {
					return mqtt;
				}
			}
		}
		throw new DdsrException("this service does not expose an MQTT flavor");
	}

	private MqttAsyncClient clientFor(MqttFlavor flavor) {
		if (flavor.getBrokers().isEmpty()) {
			throw new DdsrException("the MQTT flavor of this service announces no broker to call it on");
		}
		String url = flavor.getBrokers().get(0);
		synchronized (clientsByUrl) {
			MqttAsyncClient client = clientsByUrl.get(url);
			if (client != null && client.isConnected()) {
				return client;
			}
			try {
				MqttAsyncClient fresh = new MqttAsyncClient(url,
						"ddsr-consumer-" + UUID.randomUUID().toString().substring(0, 8),
						new MemoryPersistence());
				MqttConnectOptions options = new MqttConnectOptions();
				options.setCleanSession(true);
				options.setAutomaticReconnect(true);
				fresh.connect(options).waitForCompletion();
				clientsByUrl.put(url, fresh);
				return fresh;
			} catch (Exception unreachable) {
				throw DdsrException.transport("cannot reach the MQTT broker at " + url + ": "
						+ unreachable.getMessage(), unreachable);
			}
		}
	}

	private byte[] write(List<EObject> roots, String contentType) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, resourceSets, contentType, roots);
		return out.toByteArray();
	}

	private static void close(MqttAsyncClient client) {
		try {
			client.disconnectForcibly(500L, 500L);
		} catch (Exception shuttingDown) {
			// Nothing to do about it.
		}
		try {
			client.close();
		} catch (Exception shuttingDown) {
			// Same.
		}
	}
}
