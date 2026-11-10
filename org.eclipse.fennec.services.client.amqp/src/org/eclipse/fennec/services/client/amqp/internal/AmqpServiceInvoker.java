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

package org.eclipse.fennec.services.client.amqp.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.AmqpFlavor;
import org.eclipse.fennec.services.AmqpOperationFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.flavor.amqp.AmqpFlavors;
import org.eclipse.fennec.services.flavor.amqp.AmqpMessages;
import org.eclipse.fennec.services.invocation.Invocations;
import org.eclipse.fennec.services.messaging.LinkSettings;
import org.eclipse.fennec.services.messaging.MessagingLink;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.gecko.messaging.api.annotations.RequireMessaging;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.cloudevents.model.ce.CloudEvent;

/**
 * Calls a service over AMQP: the consumer half of the convention whose
 * provider half is {@code provider.amqp}.
 *
 * <p>Publish the call under the operation's routing key, wait for the
 * one answer that correlates with it. Where the answer goes is not in
 * this class and not in the flavor: the connection declares a queue
 * only it can read, names it in the request's reply-to and matches the
 * answer by the protocol's correlation id — before the request leaves,
 * so an answer cannot arrive ahead of the place it goes to.
 *
 * <p>Where to dial is the service's own statement, the brokers and the
 * exchange its flavor announces. What this invoker is configured with
 * is how long to wait and who this node is to a broker, neither of
 * which a flavor can say.
 *
 * <p>Synchronous, like the REST and MQTT invokers and for the same
 * reason: the proxy the SDK hands out has Java methods that return
 * values.
 */
@Designate(ocd = AmqpServiceInvoker.Config.class)
@Component(service = ServiceInvoker.class,
		configurationPid = "org.eclipse.fennec.services.client.amqp.invoker",
		property = ServiceInvoker.FLAVOR_PROPERTY + "=AMQP")
@RequireMessaging(protocol = "amqp")
public class AmqpServiceInvoker implements ServiceInvoker {

	@ObjectClassDefinition(name = "Fennec Services AMQP Invoker",
			description = "How this runtime calls services announced over AMQP.")
	public @interface Config {

		@AttributeDefinition(name = "Reply timeout (seconds)",
				description = "How long a call waits for its answer before failing. A transport failure, "
						+ "so the proxy rebinds and retries once.")
		int reply_timeout_seconds() default 10;

		@AttributeDefinition(name = "User name", required = false,
				description = "The user this node connects as. Empty: the broker's default.")
		String username() default "";

		@AttributeDefinition(name = "Password", type = AttributeType.PASSWORD, required = false,
				description = "The user's password.")
		String password() default "";

		@AttributeDefinition(name = "TLS key store", required = false,
				description = "The certificate and key this node shows brokers announced with amqps:// "
						+ "(PKCS12).")
		String tls_keystore() default "";

		@AttributeDefinition(name = "TLS key store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the key store and of its key.")
		String tls_keystore_password() default "";

		@AttributeDefinition(name = "TLS trust store", required = false,
				description = "The authorities whose broker certificates this node believes (PKCS12). "
						+ "Empty: the JVM's.")
		String tls_truststore() default "";

		@AttributeDefinition(name = "TLS trust store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the trust store.")
		String tls_truststore_password() default "";
	}

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	/** Whoever is watching calls, if anyone is (#126). */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile CallTracer tracer;

	private CallTracer tracer() {
		CallTracer bound = tracer;
		return bound == null ? CallTracer.NONE : bound;
	}

	/** One link per broker and exchange, opened by the first call that goes there. */
	private final Map<String, MessagingLink> links = new LinkedHashMap<>();

	private BundleContext context;

	private Config config;

	private String source;

	@Activate
	void activate(BundleContext context, Config config) {
		this.context = context;
		this.config = config;
		String uuid = context.getProperty(Constants.FRAMEWORK_UUID);
		this.source = "/consumer/" + (uuid == null ? ClientOrigin.ANONYMOUS : uuid);
	}

	@Deactivate
	void deactivate() {
		synchronized (links) {
			for (MessagingLink link : links.values()) {
				link.close();
			}
			links.clear();
		}
	}

	@Override
	public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
		if (locator == null || operationName == null) {
			throw new DdsrException("locator and operationName must not be null");
		}
		AmqpFlavor flavor = amqpFlavorOf(locator);
		AmqpOperationFlavor operationFlavor = AmqpFlavors.operationFlavor(flavor, operationName);
		if (operationFlavor == null) {
			throw new DdsrException("operation '" + operationName + "' has no AMQP flavor on this service");
		}
		ServiceOperation operation = operationFlavor.getOperation();
		if (operation == null) {
			throw new DdsrException("the AMQP flavor for '" + operationName + "' binds no operation");
		}

		String routingKey = AmqpFlavors.routingKey(flavor, operationFlavor);
		String consumes = AmqpFlavors.consumes(operationFlavor);

		CloudEvent request;
		byte[] document;
		try {
			ServiceInvocation invocation = Invocations.invocation(operation,
					args == null ? Map.of() : args);
			document = write(Invocations.roots(invocation), consumes);
			request = AmqpMessages.request(source, operation.getName(), consumes);
		} catch (Exception unwritable) {
			throw new DdsrException("could not write the call to " + operationName + ": "
					+ unwritable.getMessage(), unwritable);
		}

		MessagingLink link = linkFor(flavor);
		// The trace context travels in the envelope's extensions, where
		// CloudEvents puts it on every wire (#126). Written after the
		// span exists, because what travels must name THIS call.
		try (CallSpan span = tracer().calling(spanNameOf(operation, operationName),
				TraceCarrier.over(request.getExtensions().map()))) {
			span.attribute("rpc.system", "fennec.services")
					.attribute("rpc.method", operationName)
					.attribute("server.address", routingKey)
					.attribute("fennec.flavor", "AMQP")
					.attribute(ClientOrigin.ATTRIBUTE, source);
			try {
				byte[] answer = link.request(routingKey, AmqpMessages.write(request, document),
						AmqpFlavors.QOS, TimeUnit.SECONDS.toMillis(config.reply_timeout_seconds()));
				return valueOf(answer, operationName);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				DdsrException failed = DdsrException.transport("waiting for the answer to "
						+ operationName + " was interrupted", interrupted);
				span.failed(failed);
				throw failed;
			} catch (TimeoutException silent) {
				DdsrException failed = DdsrException.transport("no answer to " + operationName + " under "
						+ routingKey + " within " + config.reply_timeout_seconds() + "s", silent);
				span.failed(failed);
				throw failed;
			} catch (DdsrException alreadyOurs) {
				span.failed(alreadyOurs);
				throw alreadyOurs;
			} catch (Exception failed) {
				DdsrException transport = DdsrException.transport("calling " + operationName
						+ " over AMQP failed: " + failed.getMessage(), failed);
				span.failed(transport);
				throw transport;
			}
		}
	}

	private static String spanNameOf(ServiceOperation operation, String operationName) {
		if (operation.eContainer() instanceof ServiceInterface contract && contract.getName() != null) {
			return contract.getName() + "/" + operation.getName();
		}
		return operationName;
	}

	/** What the answer says, or why there is none. */
	private Object valueOf(byte[] answer, String operationName) throws Exception {
		CloudEventCodec.Message message = AmqpMessages.read(answer, CloudEvents.TYPE_INVOKE_REPLY);
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

	private AmqpFlavor amqpFlavorOf(ServiceLocator locator) {
		ServiceImplementation implementation = locator.implementation();
		if (implementation != null) {
			for (ServiceFlavor flavor : implementation.getFlavors()) {
				if (flavor instanceof AmqpFlavor amqp) {
					return amqp;
				}
			}
		}
		throw new DdsrException("this service does not expose an AMQP flavor");
	}

	/**
	 * The link to the broker and exchange this service announces,
	 * connected. Opened by the first call and kept — it reconnects on
	 * its own — unless its broker was never there.
	 */
	private MessagingLink linkFor(AmqpFlavor flavor) {
		if (flavor.getBrokers().isEmpty()) {
			throw new DdsrException("the AMQP flavor of this service announces no broker to call it on");
		}
		String url = flavor.getBrokers().get(0);
		String exchange = AmqpFlavors.exchange(flavor);
		String key = url + " " + exchange;
		synchronized (links) {
			MessagingLink link = links.get(key);
			try {
				if (link == null) {
					String id = UUID.randomUUID().toString().substring(0, 8);
					link = MessagingLink.open(context,
							new LinkSettings("fennec.services.amqp.invoker." + id, url, "ddsr-consumer-" + id,
									config.tls_keystore(), config.tls_keystore_password(),
									config.tls_truststore(), config.tls_truststore_password())
									.onExchange(exchange).as(config.username(), config.password()));
					links.put(key, link);
				}
				if (link.awaitConnected(config.reply_timeout_seconds(), TimeUnit.SECONDS)) {
					return link;
				}
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw DdsrException.transport("connecting to the AMQP broker at " + url + " was interrupted",
						interrupted);
			} catch (RuntimeException unreachable) {
				throw DdsrException.transport("cannot reach the AMQP broker at " + url + ": "
						+ unreachable.getMessage(), unreachable);
			}
			if (!link.hasBeenConnected()) {
				links.remove(key);
				link.close();
			}
			throw DdsrException.transport("cannot reach the AMQP broker at " + url + " within "
					+ config.reply_timeout_seconds() + "s", null);
		}
	}

	private byte[] write(List<EObject> roots, String contentType) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, resourceSets, contentType, roots);
		return out.toByteArray();
	}
}
