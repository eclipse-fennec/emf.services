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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.provider.rest.RestDispatcher;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.eclipse.fennec.tags.service.TagService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * The broker's consumer API under a tag (#279): {@code <broker>/scoped/<tag>}
 * answers {@code /references}, {@code /events}, {@code /catalog} and
 * {@code /consumers} exactly as the broker does, with only what the tag
 * covers. A consumer needs no code of its own — another broker URL.
 *
 * <p>Served from the broker's own documents, copied per tag and run by the
 * same {@link RestDispatcher} the generic distribution uses, so placement,
 * documents and stream frames are the broker's. Two things change in the
 * copy: only the reading operations are offered, and the role the broker's
 * documents ask for, {@code service-consumer}, becomes {@code tags-consumer}
 * — whoever holds only that sees the registry through tags and nothing
 * else. The facade itself reaches the broker in-process.
 *
 * <p>Who may see which tag is decided in front of this — by the gateway
 * and OPA, from the path ({@code /scoped/<tag>/…}). The facade decides
 * nothing about access; it narrows the answers to the tag.
 */
@JakartarsResource
@JakartarsApplicationSelect("(osgi.jakartars.name=" + TagFacadeApplication.NAME + ")")
@JakartarsName("fennec-tags-facade-resource")
@Component(service = TagFacade.class, configurationPid = "org.eclipse.fennec.tags.ddsr.facade",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = TagFacade.Config.class)
@Path("/")
public class TagFacade {

	private static final Logger LOG = Logger.getLogger(TagFacade.class.getName());

	/** The segment the facade lives under, before the tag path. */
	public static final String SCOPED = "scoped";

	/** The role whoever reads through the facade holds. */
	public static final String ROLE = "tags-consumer";

	/** The role the broker's consumer documents ask for, replaced in the copies. */
	static final String BROKER_CONSUMER = "service-consumer";

	@ObjectClassDefinition(name = "Fennec tag facade", description = "The broker's consumer API under a tag.")
	public @interface Config {

		@AttributeDefinition(name = "Authentication mode",
				description = "The broker's mode — bearer, bearer-optional, mtls or none — from the same placeholder.")
		String auth_mode() default "bearer";
	}

	/** One of the broker's consumer contracts, as the facade offers it. */
	private record Served(String document, String contract, Set<String> operations) {
	}

	private static final Map<String, Served> SERVED = Map.of(
			"references", new Served("broker-lookup-api.xmi", "BrokerLookup", Set.of("getServiceReferences")),
			"events", new Served("broker-events-api.xmi", "BrokerEvents", Set.of("events")),
			"catalog", new Served("broker-catalog-api.xmi", "BrokerCatalog", Set.of("listCatalog", "getCatalogEntry")));

	private final TagService tags;

	private final ScopedSessions sessions;

	/** The broker's implementation of each of its contracts, once found. */
	private final Map<String, Held> held = new ConcurrentHashMap<>();

	/** A service of the broker's, and where it was found — gone when its bundle is. */
	private record Held(ServiceReference<?> reference, Object service) {
	}

	private final ComponentServiceObjects<ResourceSet> resourceSets;

	private final BundleContext context;

	private final String mode;

	/** One dispatcher per tag and contract; the events one holds that tag's broadcast. */
	private final Map<String, RestDispatcher> dispatchers = new ConcurrentHashMap<>();

	/** Per tag: the references shown there, by its lookup or its events. */
	private final Map<String, Set<String>> shown = new ConcurrentHashMap<>();

	@Activate
	public TagFacade(Config config, BundleContext context, @Reference TagService tags,
			@Reference BrokerSessions sessions,
			@Reference(target = "(emf.name=services)") ComponentServiceObjects<ResourceSet> resourceSets) {
		this.tags = tags;
		this.sessions = new ScopedSessions(sessions, resourceSets);
		this.resourceSets = resourceSets;
		this.context = context;
		this.mode = config.auth_mode();
		LOG.info("[Tags] the broker's consumer API under a tag: /" + SCOPED + "/<tag>/…, mode " + mode);
	}

	@GET
	@Path("{tag: .+?}/{api: references|events|catalog}{rest: (/.*)?}")
	public Response get(@PathParam("tag") String tag, @PathParam("api") String api, @PathParam("rest") String rest,
			@Context UriInfo uriInfo, @Context HttpHeaders headers) {
		return dispatcher(tag, api).get(rest == null ? "" : rest, uriInfo, headers);
	}

	@PUT
	@Path("{tag: .+?}/consumers/{consumerId}")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Response putSession(@PathParam("tag") String tag, @PathParam("consumerId") String consumerId,
			InputStream entity) throws IOException {
		return sessions.put(consumerId, entity);
	}

	@GET
	@Path("{tag: .+?}/consumers/{consumerId}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getSession(@PathParam("tag") String tag, @PathParam("consumerId") String consumerId) {
		return sessions.get(consumerId);
	}

	@DELETE
	@Path("{tag: .+?}/consumers/{consumerId}")
	@Produces(MediaType.APPLICATION_XML)
	public Response deleteSession(@PathParam("tag") String tag, @PathParam("consumerId") String consumerId) {
		return sessions.delete(consumerId);
	}

	// ------------------------------------------------------------------

	private RestDispatcher dispatcher(String tag, String api) {
		return dispatchers.computeIfAbsent(tag + " " + api, key -> build(tag, api));
	}

	private RestDispatcher build(String tag, String api) {
		Served served = SERVED.get(api);
		List<EObject> roots = document(served.document());
		ServiceProvider provider = (ServiceProvider) roots.get(0);
		ServiceInterface contract = (ServiceInterface) roots.get(1);
		RestFlavor flavor = (RestFlavor) provider.getImplementations().get(0).getFlavors().get(0);
		flavor.getOperationFlavors().removeIf(operationFlavor -> !(operationFlavor instanceof RestOperationFlavor rest)
				|| rest.getOperation() == null || !served.operations().contains(rest.getOperation().getName()));
		for (TreeIterator<EObject> all = contract.eAllContents(); all.hasNext();) {
			if (all.next() instanceof SecurityRequirement requirement) {
				requirement.getScopes().replaceAll(scope -> BROKER_CONSUMER.equals(scope) ? ROLE : scope);
			}
		}
		Set<String> shownHere = shown.computeIfAbsent(tag, t -> ConcurrentHashMap.newKeySet());
		Supplier<Object> broker = brokerImplementation(served.contract());
		Object implementation = switch (api) {
			case "references" -> new ScopedLookup(broker, tags, tag, shownHere);
			case "events" -> new ScopedEvents(broker, tags, tag, shownHere);
			default -> new ScopedCatalog(broker, tags, tag);
		};
		return RestDispatcher.serving(flavor, implementation, contract.getName() + " under " + tag, resourceSets, mode);
	}

	/** The broker's own document, read anew: every tag gets a copy of its own. */
	private List<EObject> document(String name) {
		for (Bundle bundle : context.getBundles()) {
			if ("org.eclipse.fennec.services.broker.rest".equals(bundle.getSymbolicName())) {
				URL entry = bundle.getEntry(name);
				if (entry != null) {
					try (InputStream in = entry.openStream()) {
						List<EObject> roots = XmiCodec.readBundle(in, resourceSets).roots();
						return new ArrayList<>(EcoreUtil.copyAll(roots));
					} catch (IOException unreadable) {
						throw new IllegalStateException("cannot read the broker's " + name, unreadable);
					}
				}
			}
		}
		throw new IllegalStateException("the broker's " + name + " is not in this framework");
	}

	/**
	 * The broker's implementation of one of its contracts, found when it is
	 * used and kept while it is there. It is the object the broker's own
	 * distribution calls, found the way that distribution finds it.
	 */
	private Supplier<Object> brokerImplementation(String contract) {
		String filter = "(&(ddsr.contract=" + contract + ")(!(ddsr.distribution=*)))";
		return () -> {
			Held known = held.get(contract);
			if (known != null && known.reference().getBundle() != null) {
				return known.service();
			}
			try {
				ServiceReference<?>[] found = context.getServiceReferences((String) null, filter);
				if (found == null || found.length == 0) {
					return null;
				}
				Object service = context.getService(found[0]);
				held.put(contract, new Held(found[0], service));
				return service;
			} catch (InvalidSyntaxException impossible) {
				throw new IllegalStateException(impossible);
			}
		};
	}

}
