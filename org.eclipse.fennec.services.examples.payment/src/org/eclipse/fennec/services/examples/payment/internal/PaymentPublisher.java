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

package org.eclipse.fennec.services.examples.payment.internal;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.Registration;
import org.eclipse.fennec.services.BoolProperty;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.DoubleProperty;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.FloatProperty;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.LongProperty;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ShortProperty;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceProvider;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Publishes the Java Payment implementation into the DDSR broker on
 * activate. Two steps:
 * <ol>
 *   <li><b>Ensure catalog entry:</b> builds a Payment ServiceInterface
 *       (operations, parameters) and posts it via {@link BrokerCatalog#addCatalogEntry}.
 *       Idempotent: an {@code ALREADY_EXISTS} response is swallowed so
 *       the publisher can coexist with other publishers (TS colleague,
 *       another Java instance) that already registered Payment.</li>
 *   <li><b>Publish implementation:</b> builds a ServiceProvider with a
 *       RestFlavor pointing at this bundle's {@code PaymentResource}
 *       and calls {@code DdsrProvider.publish}.</li>
 * </ol>
 *
 * <p>On deactivate the publication is withdrawn.
 */
@Component(
		service = PaymentPublisher.class,
		configurationPid = "org.eclipse.fennec.services.examples.payment",
		// REQUIRE, not OPTIONAL: with an optional configuration the
		// component activates BEFORE the configurator applies the baked-in
		// config and publishes a ghost registration with the OCD defaults
		// (LAN IPs), then re-activates and publishes again — consumers can
		// briefly discover the stale endpoint (O2). The configuration is
		// embedded in the bundle, so requiring it costs nothing.
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		immediate = true)
@Designate(ocd = PaymentPublisher.Config.class)
public final class PaymentPublisher {

	private static final Logger LOG = Logger.getLogger(PaymentPublisher.class.getName());

	@ObjectClassDefinition(name = "DDSR Example Payment Publisher",
			description = "Publishes the Java Payment service into the broker on activation")
	public @interface Config {

		@AttributeDefinition(
				name = "Public URL",
				description = "External URL where the PaymentResource is reachable, e.g. http://192.168.1.6:9091/payments")
		String public_url() default "http://192.168.1.6:9091/payments";

		@AttributeDefinition(
				name = "Broker URL",
				description = "Base URL of the DDSR broker. Used to build the canonical "
						+ "per-catalog-entry URL for SI references in the publish body.")
		String broker_url() default "http://192.168.1.6:8887/ddsr/rest";

		@AttributeDefinition(
				name = "Provider name",
				description = "Symbolic name used for the published ServiceProvider")
		String provider_name() default "payments-java";
	}

	// DYNAMIC references so transient restarts of downstream services
	// (RestTransport → CatalogHttpProxy etc. during launch bootstrap)
	// only rebind the fields rather than tearing down the publisher
	// and triggering a repeat publish.
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.RELUCTANT)
	private volatile DdsrClient client;

	@Reference(
			target = "(ddsr.broker.transport=rest)",
			policy = ReferencePolicy.DYNAMIC,
			policyOption = ReferencePolicyOption.RELUCTANT)
	private volatile BrokerCatalog catalog;

	@Reference
	private ComponentServiceObjects<ResourceSet> rsObjects;

	private Registration registration;

	@Activate
	void activate(Config config) {
		if (registration != null) {
			// Belt-and-suspenders: should never happen with DYNAMIC refs
			// (no deactivate/activate cycle), but stay safe.
			return;
		}
		try {
			ServiceInterface paymentApi = ensurePaymentCatalogEntry();

			// Park paymentApi in a Resource whose URI is the canonical
			// catalog-entry URL on the broker. EMF will then serialise
			// every cross-reference to paymentApi (impl.serviceInterfaces,
			// operationFlavors[i].operation) as a cross-document
			// `href="<broker>/catalog/Payment"` rather than embedding the
			// whole SI in the publish body. The broker recognises the
			// URI and rewires to its live catalog entry.
			ResourceSet rs = rsObjects.getService();
			try {
				String entryUrl = config.broker_url().replaceFirst("/+$", "")
						+ "/catalog/" + paymentApi.getName();
				Resource catalogEntryRes = rs.createResource(
						org.eclipse.emf.common.util.URI.createURI(entryUrl));
				catalogEntryRes.getContents().add(paymentApi);

				URI url = URI.create(config.public_url());
				ServiceProvider provider = buildProvider(config, url, paymentApi);
				ServiceImplementation impl = provider.getImplementations().get(0);

				this.registration = client.provider().publish(provider, impl);
				LOG.info("[DDSR-Payment-Java] published " + config.provider_name()
						+ " at " + url + " (SI ref → " + entryUrl
						+ ") — registration=" + (registration != null ? "ok" : "null"));
			} finally {
				rsObjects.ungetService(rs);
			}
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR-Payment-Java] publish FAILED", t);
		}
	}

	@Deactivate
	void deactivate() {
		if (registration != null) {
			try {
				// Deliberately synchronous: deactivation returns only after
				// the broker acknowledged the withdrawal, so consumers are
				// informed while this provider's endpoint is still up
				// (DECISIONS_PARITY FR-P3/D2).
				Diagnostic d = registration.withdraw();
				// stdout, not JUL: shutdown path — JUL's cleanup hook may
				// already have reset the LogManager (DECISIONS_PARITY D14).
				System.out.println("[DDSR-Payment-Java] withdrawn from broker: "
						+ (d == null ? "null" : d.getSeverity() + "/" + d.getCode()));
				if (d != null && d.getSeverity() == DiagnosticSeverity.ERROR) {
					LOG.log(Level.WARNING, "withdraw rejected by the broker: code={0} {1}",
							new Object[] { d.getCode(), d.getMessage() });
				}
			} catch (Exception withdrawFailure) {
				// Shutdown must proceed even against a dead broker — but a
				// failed withdrawal leaves a dangling registration behind
				// and has to be visible, not swallowed. Deliberately NOT
				// only via LOG: on the JVM-shutdown path JUL's own cleanup
				// hook may already have reset the LogManager, and the
				// warning would vanish without a trace (D14).
				System.err.println("[DDSR-Payment-Java] withdraw failed, broker may still list this provider: "
						+ withdrawFailure);
				withdrawFailure.printStackTrace();
				LOG.log(Level.WARNING, "withdraw failed, broker may still list this provider",
						withdrawFailure);
			}
			registration = null;
		}
	}

	// ------------------------------------------------------------

	private ServiceInterface ensurePaymentCatalogEntry() {
		ServiceInterface payment = ServicesFactory.eINSTANCE.createServiceInterface();
		payment.setName("Payment");
		payment.setVersion("1.0.0");
		payment.setDescription("Payment processing service — charges accounts and reports balances.");

		ServiceOperation charge = ServicesFactory.eINSTANCE.createServiceOperation();
		charge.setName("charge");
		charge.setReturnType("double");
		charge.setDescription("Charge an amount. Returns the remaining balance.");
		charge.getParameters().add(parameter("amount",   0, "double", false, null, "Amount to charge."));
		charge.getParameters().add(parameter("currency", 1, "string", true,  "EUR", "ISO 4217 currency code."));
		payment.getOperations().add(charge);

		ServiceOperation getBalance = ServicesFactory.eINSTANCE.createServiceOperation();
		getBalance.setName("getBalance");
		getBalance.setReturnType("double");
		getBalance.setDescription("Get current account balance.");
		getBalance.getParameters().add(parameter("accountId", 0, "string", false, null, "The account identifier."));
		payment.getOperations().add(getBalance);

		Diagnostic d = catalog.addCatalogEntry(payment, "payments-java-publisher");
		if (d.getSeverity() == DiagnosticSeverity.ERROR
				&& d.getCode() == 202 /* CODE_CATALOG_ENTRY_ALREADY_EXISTS */) {
			// Someone else (TS colleague, prior run) already added Payment.
			// Fine — keep going with the impl publish.
			LOG.info("[DDSR-Payment-Java] Payment catalog entry already exists, reusing");
			return payment;
		}
		if (d.getSeverity() == DiagnosticSeverity.ERROR
				|| d.getSeverity() == DiagnosticSeverity.CANCEL) {
			throw new IllegalStateException("could not add Payment to catalog: " + d.getMessage());
		}
		LOG.info("[DDSR-Payment-Java] Payment catalog entry added");
		return payment;
	}

	private static Parameter parameter(String name, int index, String type, boolean optional,
			String defaultValue, String description) {
		Parameter p = ServicesFactory.eINSTANCE.createParameter();
		p.setName(name);
		p.setIndex(index);
		p.setType(type);
		p.setOptional(optional);
		if (defaultValue != null) {
			p.setDefaultValue(defaultValue);
		}
		if (description != null) {
			p.setDescription(description);
		}
		return p;
	}

	private static ServiceProvider buildProvider(Config config, URI url, ServiceInterface paymentApi) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(config.provider_name());
		provider.setVersion("1.0.0");
		provider.setSymbolicName("org.eclipse.fennec.services.examples.payment");

		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(config.provider_name() + "-rest");
		impl.setVersion("1.0.0");
		impl.setImplementationId("org.eclipse.fennec.services.examples.payment.rest");
		impl.setDescription("Java reference Payment implementation");
		// Use the full paymentApi (not a name-only stub) — the wire side
		// includes it as a sibling root in the bundle so the impl's
		// operation cross-refs below resolve intra-document.
		impl.getServiceInterfaces().add(paymentApi);

		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName("payments-java-rest");
		flavor.setKind(FlavorKind.REST);
		flavor.setHost(url.getScheme() + "://" + url.getAuthority());
		flavor.setBasePath(url.getPath() != null ? url.getPath() : "");
		flavor.getContentTypes().add("application/json");

		flavor.getOperationFlavors().add(opFlavor("charge",     HttpMethod.POST, "/charge",
				findOperation(paymentApi, "charge")));
		flavor.getOperationFlavors().add(opFlavor("getBalance", HttpMethod.GET,  "/balance",
				findOperation(paymentApi, "getBalance")));

		impl.getFlavors().add(flavor);
		addExampleProperties(impl);
		provider.getImplementations().add(impl);
		return provider;
	}

	/**
	 * A representative set covering every {@code Property} subclass. The
	 * broker copies implementation properties onto the ServiceReference,
	 * where consumers read them and LDAP filters match them. The TS
	 * example provider sets the same keys (with {@code ddsr.provider.lang
	 * = typescript}), so the cross-language tests can assert typed
	 * round-trip fidelity in both directions.
	 */
	private static void addExampleProperties(ServiceImplementation impl) {
		StringProperty lang = ServicesFactory.eINSTANCE.createStringProperty();
		lang.setName("ddsr.provider.lang");
		lang.setValue("java");
		impl.getProperties().add(lang);

		IntProperty ranking = ServicesFactory.eINSTANCE.createIntProperty();
		ranking.setName("service.ranking");
		ranking.setValue(10);
		impl.getProperties().add(ranking);

		LongProperty maxAmount = ServicesFactory.eINSTANCE.createLongProperty();
		maxAmount.setName("payments.maxAmountCents");
		maxAmount.setValue(5_000_000_000L);
		impl.getProperties().add(maxAmount);

		DoubleProperty feeRate = ServicesFactory.eINSTANCE.createDoubleProperty();
		feeRate.setName("payments.feeRate");
		feeRate.setValue(0.025d);
		impl.getProperties().add(feeRate);

		FloatProperty timeout = ServicesFactory.eINSTANCE.createFloatProperty();
		timeout.setName("payments.timeoutSeconds");
		timeout.setValue(1.5f);
		impl.getProperties().add(timeout);

		ShortProperty retries = ServicesFactory.eINSTANCE.createShortProperty();
		retries.setName("payments.maxRetries");
		retries.setValue((short) 3);
		impl.getProperties().add(retries);

		BoolProperty sandbox = ServicesFactory.eINSTANCE.createBoolProperty();
		sandbox.setName("payments.sandbox");
		sandbox.setValue(true);
		impl.getProperties().add(sandbox);

		StringListProperty tags = ServicesFactory.eINSTANCE.createStringListProperty();
		tags.setName("payments.tags");
		tags.getValue().add("demo");
		tags.getValue().add("payments");
		impl.getProperties().add(tags);
	}

	private static RestOperationFlavor opFlavor(String name, HttpMethod method, String path,
			ServiceOperation operation) {
		RestOperationFlavor of = ServicesFactory.eINSTANCE.createRestOperationFlavor();
		of.setName(name);
		of.setMethod(method);
		of.setPath(path);
		of.setOperation(operation);
		of.getProduces().add("application/json");
		return of;
	}

	private static ServiceOperation findOperation(ServiceInterface iface, String name) {
		for (ServiceOperation op : iface.getOperations()) {
			if (name.equals(op.getName())) {
				return op;
			}
		}
		throw new IllegalStateException("operation '" + name + "' not found on " + iface.getName());
	}
}
