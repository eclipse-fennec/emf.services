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
import org.eclipse.fennec.services.UpdatePolicy;
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
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ShortProperty;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.ServiceProvider;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
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
				name = "Publish the BindingProbe contract",
				description = "Additionally publish the tiny BindingProbe contract whose one operation "
						+ "carries its three arguments in path, query and header. Off by default: it is "
						+ "a second registration, and the demo should not grow one for everybody.")
		boolean publish_binding_probe() default false;

		@AttributeDefinition(
				name = "Broker URL",
				description = "Base URL of the DDSR broker. Used to build the canonical "
						+ "per-catalog-entry URL for SI references in the publish body.")
		String broker_url() default "http://192.168.1.6:8887/ddsr/rest";

		@AttributeDefinition(
				name = "Provider name",
				description = "Symbolic name used for the published ServiceProvider")
		String provider_name() default "payments-java";

		@AttributeDefinition(
				name = "Implementation version",
				description = "Version of the published ServiceImplementation. A second instance with another "
						+ "version coexists (EVERGREEN) or supersedes the first (see update.policy / replaces.version).",
				required = false)
		String impl_version() default "1.0.0";

		@AttributeDefinition(
				name = "Update policy",
				description = "UNSPECIFIED (inherit / broker default DEPRECATE_AND_DRAIN), EVERGREEN, "
						+ "DEPRECATE_AND_DRAIN or HARD_CUTOVER — UPDATE_POLICY.md §2. Only meaningful with replaces.version.",
				required = false)
		String update_policy() default "UNSPECIFIED";

		@AttributeDefinition(
				name = "Replaces version",
				description = "Version of the previously published implementation this one supersedes "
						+ "(same provider.name). Empty = plain publish.",
				required = false)
		String replaces_version() default "";
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

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> rsObjects;

	private Registration registration;

	/**
	 * The BindingProbe registration (#74). A second, tiny contract whose one
	 * operation carries its three arguments in three different places, so
	 * the harness can prove that a consumer places them where the published
	 * flavor says — and not where the type of the value would suggest.
	 */
	private Registration probeRegistration;
	/**
	 * Held for the life of the registration, not just the publish call:
	 * emf.osgi clears every Resource of a prototype ResourceSet on
	 * ungetService, which would detach paymentApi from its catalog-URL
	 * Resource and leave impl.serviceInterfaces dangling for the next
	 * serialization, e.g. the re-publish on reconnect (#50).
	 */
	private ResourceSet catalogEntryResourceSet;

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
			catalogEntryResourceSet = rs;
			String entryUrl = config.broker_url().replaceFirst("/+$", "")
					+ "/catalog/" + paymentApi.getName();
			Resource catalogEntryRes = rs.createResource(
					org.eclipse.emf.common.util.URI.createURI(entryUrl));
			catalogEntryRes.getContents().add(paymentApi);

			URI url = URI.create(config.public_url());
			ServiceProvider provider = buildProvider(config, url, paymentApi);
			ServiceImplementation impl = provider.getImplementations().get(0);

			this.registration = client.provider().publish(provider, impl);
			if (config.publish_binding_probe()) {
				this.probeRegistration = publishBindingProbe(config, url);
			}
			LOG.info("[DDSR-Payment-Java] published " + config.provider_name()
					+ " at " + url + " (SI ref → " + entryUrl
					+ ") — registration=" + (registration != null ? "ok" : "null"));
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR-Payment-Java] publish FAILED", t);
		}
	}

	/**
	 * Publish the BindingProbe contract by LOADING it: contract, flavor and
	 * every parameter binding live in one document in {@code model/} of this
	 * bundle — one document on purpose, so every reference between flavor and
	 * contract is intra-document and resolves wherever the file is read. The
	 * same document is what the interface is generated from (#75) and what the
	 * generic distribution serves it by (#84). Nothing here restates it, so
	 * what this provider serves and what it announces cannot drift apart.
	 *
	 * <p>Only the deployment facts are filled in here: where this instance is
	 * reachable is configuration, not contract.
	 */
	private Registration publishBindingProbe(Config config, URI url) {
		Bundle bundle = FrameworkUtil.getBundle(PaymentPublisher.class);
		org.eclipse.emf.common.util.URI modelUri = org.eclipse.emf.common.util.URI
				.createURI(bundle.getEntry("model/binding-probe.xmi").toString());
		Resource loaded = catalogEntryResourceSet.getResource(modelUri, true);
		ServiceProvider provider = (ServiceProvider) loaded.getContents().get(0);
		ServiceImplementation impl = provider.getImplementations().get(0);
		ServiceInterface probeApi = impl.getServiceInterfaces().get(0);

		Diagnostic added = catalog.addCatalogEntry(probeApi, "payments-java-publisher");
		if (added.getSeverity() == DiagnosticSeverity.ERROR && added.getCode() != 202) {
			throw new IllegalStateException("could not add BindingProbe to catalog: " + added.getMessage());
		}

		// Same parking as the Payment entry: the contract moves out of the
		// bundle document into one named by its canonical catalog URL, so the
		// publish body references it instead of carrying a second copy.
		String entryUrl = config.broker_url().replaceFirst("/+$", "") + "/catalog/" + probeApi.getName();
		Resource entryResource = catalogEntryResourceSet.createResource(
				org.eclipse.emf.common.util.URI.createURI(entryUrl));
		entryResource.getContents().add(probeApi);

		// The flavor's basePath in the model is where this provider mounts the
		// endpoint inside itself — the generated resource carries it as its
		// @Path, and it is what keeps two contracts of one provider apart.
		// What a CONSUMER sees is that path behind the deployment's own, so
		// the published value is the configured path plus the modelled one.
		RestFlavor flavor = (RestFlavor) impl.getFlavors().get(0);
		String mountPath = flavor.getBasePath() != null ? flavor.getBasePath() : "";
		flavor.setHost(url.getScheme() + "://" + url.getAuthority());
		flavor.setBasePath((url.getPath() != null ? url.getPath() : "") + mountPath);

		return client.provider().publish(provider, impl);
	}

	@Deactivate
	void deactivate() {
		if (probeRegistration != null) {
			try {
				probeRegistration.withdraw();
			} catch (Exception withdrawFailed) {
				LOG.log(Level.WARNING, "[DDSR-Payment-Java] BindingProbe withdraw failed", withdrawFailed);
			}
			probeRegistration = null;
		}
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
		if (catalogEntryResourceSet != null) {
			rsObjects.ungetService(catalogEntryResourceSet);
			catalogEntryResourceSet = null;
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
		charge.setReturnValue(returnValue("double"));
		charge.setDescription("Charge an amount. Returns the remaining balance.");
		charge.getParameters().add(parameter("amount",   0, "double", false, null, "Amount to charge."));
		charge.getParameters().add(parameter("currency", 1, "string", true,  "EUR", "ISO 4217 currency code."));
		payment.getOperations().add(charge);

		ServiceOperation getBalance = ServicesFactory.eINSTANCE.createServiceOperation();
		getBalance.setName("getBalance");
		getBalance.setReturnValue(returnValue("double"));
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

	/**
	 * The return slot, a Parameter of its own since #41. NamedElement
	 * forces a name on it; {@code result} is the convention both SDKs
	 * use, and the name never reaches the fingerprint — it has no wire
	 * role.
	 */
	private static Parameter returnValue(String type) {
		Parameter p = ServicesFactory.eINSTANCE.createParameter();
		p.setName("result");
		p.setType(type);
		return p;
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

	private static String blankToDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private static ServiceProvider buildProvider(Config config, URI url, ServiceInterface paymentApi) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(config.provider_name());
		provider.setVersion("1.0.0");
		provider.setSymbolicName("org.eclipse.fennec.services.examples.payment");

		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(config.provider_name() + "-rest");
		impl.setVersion(blankToDefault(config.impl_version(), "1.0.0"));
		impl.setImplementationId("org.eclipse.fennec.services.examples.payment.rest");
		impl.setDescription("Java reference Payment implementation");
		// Update policy (UPDATE_POLICY.md §2, harness scenario G): an
		// instance that supersedes an earlier version names it via replaces
		// — a (name, version) stub the broker resolves against the live
		// registration; the SDK ships it as a sibling root of the body.
		UpdatePolicy policy = UpdatePolicy.getByName(blankToDefault(config.update_policy(), "UNSPECIFIED").trim());
		impl.setUpdatePolicy(policy != null ? policy : UpdatePolicy.UNSPECIFIED);
		String replacesVersion = config.replaces_version();
		if (replacesVersion != null && !replacesVersion.isBlank()) {
			ServiceImplementation predecessor = ServicesFactory.eINSTANCE.createServiceImplementation();
			predecessor.setName(impl.getName());
			predecessor.setVersion(replacesVersion.trim());
			predecessor.setImplementationId(impl.getImplementationId());
			impl.setReplaces(predecessor);
		}
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
		// This demo serves every argument as a query parameter. Saying so
		// binds the endpoint to the catalog instead of to a convention: a
		// consumer reads where each value goes instead of guessing from its
		// type (#74).
		for (Parameter parameter : operation.getParameters()) {
			RestParameterBinding binding = ServicesFactory.eINSTANCE.createRestParameterBinding();
			binding.setParameter(parameter);
			binding.setBinding(ParameterBinding.QUERY);
			of.getParameterBindings().add(binding);
		}
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
