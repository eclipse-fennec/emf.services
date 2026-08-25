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

package org.eclipse.fennec.services.broker.rest.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Self-publishes the broker's own REST API into its catalog and
 * implementation index. Eat-your-own-dogfood: the broker becomes the
 * first entry in the registry it provides.
 *
 * <p>At {@code @Activate} this component:
 * <ol>
 * <li>Loads the three API descriptions (BrokerCatalog,
 *     BrokerImplementations, BrokerLookup) from the bundle resources
 *     and calls {@code addCatalogEntry} for each. Idempotent against
 *     prior state: an {@code ALREADY_EXISTS} diagnostic is swallowed.</li>
 * <li>Builds a ServiceProvider {@code ddsr-broker} programmatically,
 *     wraps a ServiceImplementation with a RestFlavor and per-operation
 *     RestOperationFlavors that point at the actual REST endpoints,
 *     and calls {@code publishImplementation}.</li>
 * </ol>
 *
 * <p>After activation a consumer can:
 * <pre>
 * GET /references?interface=BrokerCatalog&amp;flavors=REST
 * </pre>
 * and discover the broker's own REST surface.
 */
@Component(
		service = BrokerSelfPublisher.class,
		configurationPid = "org.eclipse.fennec.services.broker.rest",
		immediate = true)
@Designate(ocd = BrokerSelfPublisher.Config.class)
public final class BrokerSelfPublisher {

	private static final Logger LOG = Logger.getLogger(BrokerSelfPublisher.class.getName());

	@ObjectClassDefinition(
			name = "DDSR Broker Self-Publisher",
			description = "Self-publication of the broker's REST endpoints into its own catalog")
	public @interface Config {

		@AttributeDefinition(
				name = "Public URL",
				description = "External URL where the broker's REST endpoint is reachable. "
						+ "The authority part (scheme://host:port) becomes RestFlavor.host; "
						+ "the path becomes RestFlavor.basePath. Consumers use this to build "
						+ "operation URLs without any client-side fallback config.")
		String public_url() default "http://localhost:8887/ddsr/rest";
	}

	@Reference
	private BrokerCatalog catalog;

	@Reference
	private BrokerImplementations implementations;

	@Reference
	private ComponentServiceObjects<ResourceSet> rsObjects;

	@Activate
	void activate(Config config) {
		try {
			ServiceInterface catalog = ensureCatalogEntry("broker-catalog-api.xmi");
			ServiceInterface impls   = ensureCatalogEntry("broker-implementations-api.xmi");
			ServiceInterface lookup  = ensureCatalogEntry("broker-lookup-api.xmi");

			publishSelf(catalog, impls, lookup, URI.create(config.public_url()));
			LOG.info("[DDSR] BrokerSelfPublisher: catalog seeded and self-impl published");
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR] BrokerSelfPublisher failed", t);
		}
	}

	// ------------------------------------------------------------
	// Step 1: seed catalog from packaged XMIs
	// ------------------------------------------------------------

	private ServiceInterface ensureCatalogEntry(String resourceName) throws IOException {
		ServiceInterface si = readBundleInterface(resourceName);
		Diagnostic d = catalog.addCatalogEntry(si, "broker-self-publisher");
		if (d.getSeverity() == DiagnosticSeverity.ERROR
				&& d.getCode() == DdsrDiagnostics.CODE_CATALOG_ENTRY_ALREADY_EXISTS) {
			// Already present (from a restored snapshot). Fall through to
			// the catalog lookup so the publish step references the live
			// catalog object, not our throwaway parse.
		}
		return findInCatalog(si.getName());
	}

	private ServiceInterface findInCatalog(String name) {
		for (ServiceInterface si : catalog.getRegistry().getCatalog()) {
			if (name.equals(si.getName())) {
				return si;
			}
		}
		return null;
	}

	private ServiceInterface readBundleInterface(String resourceName) throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
			if (in == null) {
				throw new IOException("bundle resource not found: " + resourceName);
			}
			EObject eo = XmiCodec.read(in, rsObjects);
			if (!(eo instanceof ServiceInterface)) {
				throw new IOException(resourceName + " does not contain a ServiceInterface root");
			}
			return (ServiceInterface) eo;
		}
	}

	// ------------------------------------------------------------
	// Step 2: self-publish provider + impl + RestFlavor
	// ------------------------------------------------------------

	private void publishSelf(ServiceInterface catalogApi, ServiceInterface implsApi, ServiceInterface lookupApi,
			URI publicUrl) {
		if (catalogApi == null || implsApi == null || lookupApi == null) {
			LOG.warning("[DDSR] cannot self-publish: at least one API interface is missing from the catalog");
			return;
		}
		// Idempotent: if a provider named ddsr-broker is already in the
		// registry (e.g. restored from snapshot), skip — re-publishing
		// would double-add provider + implementation.
		for (ServiceProvider existing : catalog.getRegistry().getProviders()) {
			if ("ddsr-broker".equals(existing.getName())) {
				return;
			}
		}

		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("ddsr-broker");
		provider.setVersion("1.0.0");
		provider.setSymbolicName("org.eclipse.fennec.services.broker");

		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("ddsr-broker-rest");
		impl.setVersion("1.0.0");
		impl.setImplementationId("org.eclipse.fennec.services.broker.rest");
		impl.setDescription("Self-publishing entry: the DDSR broker exposes its own catalog/implementations/lookup API.");
		impl.getServiceInterfaces().add(catalogApi);
		impl.getServiceInterfaces().add(implsApi);
		impl.getServiceInterfaces().add(lookupApi);

		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName("ddsr-broker-rest");
		flavor.setKind(FlavorKind.REST);
		// Split the configured public URL into authority (host) and path
		// (basePath). Consumers get a self-contained RestFlavor — no
		// client-side broker-URL config needed for URI building.
		flavor.setHost(publicUrl.getScheme() + "://" + publicUrl.getAuthority());
		flavor.setBasePath(publicUrl.getPath() != null ? publicUrl.getPath() : "");
		flavor.getContentTypes().add("application/xml");

		// Per-operation REST bindings. The path/method tuples mirror what
		// the resource classes actually expose.
		flavor.getOperationFlavors().add(opFlavor("listCatalog",          catalogApi, "listCatalog",          HttpMethod.GET,    "/catalog",                 200));
		flavor.getOperationFlavors().add(opFlavor("addCatalogEntry",      catalogApi, "addCatalogEntry",      HttpMethod.POST,   "/catalog",                 200, 409));
		flavor.getOperationFlavors().add(opFlavor("deprecateCatalogEntry",catalogApi, "deprecateCatalogEntry",HttpMethod.PUT,    "/catalog/{name}/deprecate",200, 404));
		flavor.getOperationFlavors().add(opFlavor("removeCatalogEntry",   catalogApi, "removeCatalogEntry",   HttpMethod.DELETE, "/catalog/{name}",          200, 404, 409));
		flavor.getOperationFlavors().add(opFlavor("publishImplementation",implsApi,   "publishImplementation",HttpMethod.POST,   "/implementations",         200, 403, 422));
		flavor.getOperationFlavors().add(opFlavor("withdrawImplementation",implsApi,  "withdrawImplementation",HttpMethod.DELETE,"/implementations",         200, 403, 404));
		flavor.getOperationFlavors().add(opFlavor("getServiceReferences", lookupApi,  "getServiceReferences", HttpMethod.GET,    "/references",              200, 400));

		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);

		Diagnostic d = implementations.publishImplementation(provider, impl);
		if (d.getSeverity() == DiagnosticSeverity.ERROR) {
			LOG.warning("[DDSR] self-publish failed: code=" + d.getCode() + " " + d.getMessage());
		}
	}

	private static RestOperationFlavor opFlavor(String name, ServiceInterface iface, String opName,
			HttpMethod method, String path, int... returnCodes) {
		RestOperationFlavor of = ServicesFactory.eINSTANCE.createRestOperationFlavor();
		of.setName(name);
		of.setOperation(findOperation(iface, opName));
		of.setMethod(method);
		of.setPath(path);
		of.getProduces().add("application/xml");
		List<Integer> codes = new ArrayList<>(returnCodes.length);
		for (int c : returnCodes) {
			codes.add(c);
		}
		of.getReturnCodes().addAll(codes);
		return of;
	}

	private static ServiceOperation findOperation(ServiceInterface iface, String opName) {
		for (ServiceOperation so : iface.getOperations()) {
			if (opName.equals(so.getName())) {
				return so;
			}
		}
		throw new IllegalStateException(
				"operation '" + opName + "' missing on interface " + iface.getName()
						+ ", available: " + iface.getOperations());
	}
}
