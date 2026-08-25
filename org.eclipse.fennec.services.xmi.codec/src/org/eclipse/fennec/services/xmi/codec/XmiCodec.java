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

package org.eclipse.fennec.services.xmi.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.osgi.service.component.ComponentServiceObjects;


/**
 * Stateless XMI codec used by the JAX-RS message body reader/writer
 * pair (server + client side). The wire ResourceSet is obtained from
 * a {@link ComponentServiceObjects} so each call gets a fresh, fully
 * configured prototype instance from emf.osgi and is released back to
 * the framework on completion.
 * <p>
 * <b>Hardening (S3).</b> Every read path treats the incoming document as
 * untrusted:
 * <ul>
 *   <li>the parser rejects DTDs outright and refuses external entities,
 *       which closes XXE (file disclosure) and entity-expansion DoS
 *       ("billion laughs") — a legitimate DDSR wire document never has
 *       a DTD;</li>
 *   <li>no reference in the document is ever fetched. Cross-document
 *       {@code href}s stay unresolved proxies, so a caller-supplied URI
 *       cannot make us issue a request (SSRF).</li>
 * </ul>
 * Leaving cross-document refs unresolved is not a behavioural loss: the
 * broker reads the catalog name from the proxy URI's last path segment
 * and the operation index from its {@code //@operations.N} fragment
 * <em>without</em> resolving (see {@code DdsrBrokerImpl.catalogNameOf}
 * and {@code resolveLiveOperation}, and ARCHITECTURE.md §2.4).
 * Same-document references are resolved by the parser at load time and
 * are unaffected.
 */
public final class XmiCodec {

	private static final Logger LOG = Logger.getLogger(XmiCodec.class.getName());

	/**
	 * SAX feature that makes the parser reject any document carrying a
	 * {@code <!DOCTYPE ...>} declaration. This is the single most
	 * effective switch here: without a DTD there is no external entity
	 * to dereference and no entity to expand recursively.
	 */
	private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

	private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";

	private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";

	private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

	/**
	 * {@code javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING} as a
	 * literal, so this bundle does not need an import on {@code javax.xml}
	 * just for one constant. Turns on the JAXP processing limits.
	 */
	private static final String SECURE_PROCESSING = "http://javax.xml.XMLConstants/feature/secure-processing";

	private XmiCodec() {
	}

	public static void write(OutputStream out, ComponentServiceObjects<ResourceSet> rsObjects, EObject... roots)
			throws IOException {
		write(out, rsObjects, Arrays.asList(roots));
	}

	public static void write(OutputStream out, ComponentServiceObjects<ResourceSet> rsObjects,
			Collection<? extends EObject> roots) throws IOException {
		ResourceSet rs = rsObjects.getService();
		try {
			Resource res = rs.createResource(URI.createURI("ddsr-wire.xmi"));
			for (EObject eo : roots) {
				// Containment is exclusive — if the caller hands us a live
				// object we copy it so we don't steal it out of its
				// resource. Free-floating roots (e.g. Copier output) are
				// added directly.
				res.getContents().add(eo.eResource() == null ? eo : EcoreUtil.copy(eo));
			}
			res.save(out, Map.of(XMIResource.OPTION_ENCODING, "UTF-8"));
		} finally {
			rsObjects.ungetService(rs);
		}
	}

	public static EObject read(InputStream in, ComponentServiceObjects<ResourceSet> rsObjects) throws IOException {
		return readGuarded(in, rsObjects, res -> {
			if (res.getContents().isEmpty()) {
				throw new XmiCodecException(XmiCodecException.Reason.EMPTY, "XMI body is empty");
			}
			if (res.getContents().size() > 1) {
				throw new XmiCodecException(XmiCodecException.Reason.UNEXPECTED_ROOTS,
						"XMI body must contain exactly one root, got " + res.getContents().size());
			}
			EObject root = res.getContents().get(0);
			// Detach so the caller can attach the tree to its own resource
			// without it being yanked back when we release the wire RS.
			res.getContents().remove(root);
			return root;
		});
	}

	/**
	 * Read a (possibly multi-root) XMI document and return all roots as
	 * a detached {@link XmiBundle}. Used by the lookup endpoint and its
	 * REST proxy where the wire format is envelope + sibling roots.
	 * <p>
	 * Cross-document references are deliberately left unresolved — see
	 * the class comment on why that is safe and intended.
	 */
	public static XmiBundle readBundle(InputStream in, ComponentServiceObjects<ResourceSet> rsObjects)
			throws IOException {
		return readGuarded(in, rsObjects, res -> {
			// resolveAll settles proxies that can be reached without I/O.
			// The deny-all URI handler is still installed at this point, so
			// every external fetch fails and EcoreUtil leaves such a
			// reference as a proxy instead of propagating the failure —
			// which is the state the broker's rewiring logic expects.
			EcoreUtil.resolveAll(res);
			List<EObject> roots = new ArrayList<>(res.getContents());
			res.getContents().clear();
			return new XmiBundle(roots);
		});
	}

	/** What a read path does with the loaded resource before it is released. */
	private interface ResourceReader<T> {
		T read(Resource res) throws IOException;
	}

	/**
	 * Runs a read path with the untrusted-input guards in place: secure
	 * parser options for the load, and the deny-all URI handler installed
	 * for the <em>whole</em> operation.
	 * <p>
	 * The handler has to outlive the parse: proxy resolution happens after
	 * {@code load} returns, so guarding only the load would leave
	 * {@code EcoreUtil.resolveAll} free to fetch whatever the document
	 * points at.
	 */
	private static <T> T readGuarded(InputStream in, ComponentServiceObjects<ResourceSet> rsObjects,
			ResourceReader<T> reader) throws IOException {
		ResourceSet rs = rsObjects.getService();
		// Front of the list so it shadows the file:/http: handlers. The
		// ResourceSet is a prototype instance, but we still restore the
		// list so a pooled implementation cannot leak the handler into an
		// unrelated call.
		List<URIHandler> handlers = rs.getURIConverter().getURIHandlers();
		URIHandler denyAll = new DenyExternalAccess();
		handlers.add(0, denyAll);
		try {
			Resource res = rs.createResource(URI.createURI("ddsr-wire.xmi"));
			try {
				// Size cap (S7) sits here so every parse path is covered:
				// the resources that take a raw InputStream as well as the
				// ones that go through a MessageBodyReader.
				res.load(WireBody.limited(in), secureLoadOptions());
			} catch (IOException parseError) {
				// Malformed or unacceptable XMI is a client problem — 400,
				// not 500. The parser message stays in the log: it can carry
				// local paths or the URI a rejected entity pointed at, and
				// echoing that back would hand an attacker a probing
				// oracle (S5).
				LOG.log(Level.WARNING, "rejected XMI body", parseError);
				throw new XmiCodecException(XmiCodecException.Reason.MALFORMED, "malformed or unacceptable XMI body");
			}
			return reader.read(res);
		} finally {
			handlers.remove(denyAll);
			rsObjects.ungetService(rs);
		}
	}

	/**
	 * Load options that make the underlying SAX parser hostile to
	 * everything a DDSR wire document has no business containing.
	 * <p>
	 * The features are handed over as EMF's {@code OPTION_PARSER_FEATURES}
	 * so they reach the actual {@code XMLReader}.
	 */
	private static Map<String, Object> secureLoadOptions() {
		Map<String, Boolean> features = new HashMap<>();
		features.put(DISALLOW_DOCTYPE, Boolean.TRUE);
		features.put(EXTERNAL_GENERAL_ENTITIES, Boolean.FALSE);
		features.put(EXTERNAL_PARAMETER_ENTITIES, Boolean.FALSE);
		features.put(LOAD_EXTERNAL_DTD, Boolean.FALSE);
		features.put(SECURE_PROCESSING, Boolean.TRUE);

		Map<String, Object> options = new HashMap<>();
		options.put(XMIResource.OPTION_PARSER_FEATURES, features);
		return options;
	}

	/**
	 * URI handler that claims every URI and then refuses to do any I/O.
	 * Installed only for the duration of a load, so the sole effect is
	 * that demand-loads triggered by proxy resolution fail — the wire
	 * document itself is read from the caller's stream and never goes
	 * through the URI converter.
	 */
	private static final class DenyExternalAccess extends URIHandlerImpl {

		@Override
		public boolean canHandle(URI uri) {
			return true;
		}

		@Override
		public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException {
			LOG.log(Level.WARNING, () -> "refused to resolve external reference in an XMI body: " + uri);
			throw new IOException("external reference resolution is disabled");
		}

		@Override
		public OutputStream createOutputStream(URI uri, Map<?, ?> options) throws IOException {
			throw new IOException("external reference resolution is disabled");
		}

		@Override
		public boolean exists(URI uri, Map<?, ?> options) {
			return false;
		}
	}
}
