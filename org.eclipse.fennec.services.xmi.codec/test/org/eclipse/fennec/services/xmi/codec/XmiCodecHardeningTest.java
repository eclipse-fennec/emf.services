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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import jakarta.ws.rs.WebApplicationException;

/**
 * Pins the S3 hardening of {@link XmiCodec}: a wire document is
 * untrusted input, so the parser must reject DTDs and the codec must
 * never fetch anything a document points at.
 * <p>
 * Plain JUnit, no OSGi runtime — {@link ComponentServiceObjects} is the
 * only OSGi API involved and is stubbed below.
 */
class XmiCodecHardeningTest {

	private static final String NS = "http://eclipse.org/fennec/services/1.0";

	/**
	 * Minimal stand-in for the emf.osgi prototype service. Hands out a
	 * plain ResourceSet with the DDSR package and the XMI factory
	 * registered, and records that the codec released it again.
	 */
	private static final class ResourceSetObjects implements ComponentServiceObjects<ResourceSet> {

		private int outstanding;

		@Override
		public ResourceSet getService() {
			outstanding++;
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			rs.getPackageRegistry().put(NS, ServicesPackage.eINSTANCE);
			return rs;
		}

		@Override
		public void ungetService(ResourceSet service) {
			outstanding--;
		}

		@Override
		public ServiceReference<ResourceSet> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	private final ResourceSetObjects rsObjects = new ResourceSetObjects();

	private static InputStream utf8(String xml) {
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}

	// ------------------------------------------------------------------
	// Baseline: hardening must not break the legitimate wire format.
	// ------------------------------------------------------------------

	@Test
	void readsAWellFormedDocument() throws IOException {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:ServiceProvider xmlns:services="%s" xmlns:xmi="http://www.omg.org/XMI"
				                      xmi:version="2.0" name="payments-java" version="1.0.0"/>
				""".formatted(NS);

		EObject root = XmiCodec.read(utf8(xml), rsObjects);

		assertThat(root).isInstanceOf(ServiceProvider.class);
		assertThat(((ServiceProvider) root).getName()).isEqualTo("payments-java");
	}

	@Test
	void resolvesSameDocumentReferences() throws IOException {
		// A sibling-root ServiceInterface referenced from the impl by an
		// intra-document fragment. This needs no I/O and must keep working.
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<xmi:XMI xmlns:xmi="http://www.omg.org/XMI" xmlns:services="%s" xmi:version="2.0">
				  <services:ServiceProvider name="payments-java" version="1.0.0">
				    <implementations name="payments-java-rest" version="1.0.0"
				                     serviceInterfaces="/1"/>
				  </services:ServiceProvider>
				  <services:ServiceInterface name="Payment" version="1.0.0"/>
				</xmi:XMI>
				""".formatted(NS);

		XmiBundle bundle = XmiCodec.readBundle(utf8(xml), rsObjects);

		ServiceProvider provider = bundle.roots().stream()
				.filter(ServiceProvider.class::isInstance)
				.map(ServiceProvider.class::cast)
				.findFirst()
				.orElseThrow();
		ServiceImplementation impl = provider.getImplementations().get(0);
		assertThat(impl.getServiceInterfaces()).hasSize(1);
		assertThat(((InternalEObject) impl.getServiceInterfaces().get(0)).eIsProxy())
				.as("same-document reference must be resolved, not left as a proxy")
				.isFalse();
	}

	// ------------------------------------------------------------------
	// XXE and entity expansion.
	// ------------------------------------------------------------------

	@Test
	void rejectsExternalEntityInElementContent(@TempDir Path tmp) throws IOException {
		Path secret = tmp.resolve("secret.txt");
		Files.writeString(secret, "TOP-SECRET-BROKER-CONTENT");

		// The entity has to sit in element content: XML well-formedness
		// already forbids an external entity reference inside an attribute
		// value, so an attribute-based payload proves nothing about our
		// hardening. RestFlavor.contentTypes is multi-valued and therefore
		// serialized as a child element with text content — the shape a real
		// publish body uses (see ARCHITECTURE.md §3.2).
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<!DOCTYPE provider [
				  <!ENTITY leak SYSTEM "%s">
				]>
				<services:ServiceProvider xmlns:services="%s" xmlns:xmi="http://www.omg.org/XMI"
				                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				                      xmi:version="2.0" name="payments-java" version="1.0.0">
				  <implementations name="payments-java-rest" version="1.0.0">
				    <flavors xsi:type="services:RestFlavor" name="rest" host="http://h" basePath="/p">
				      <contentTypes>&leak;</contentTypes>
				    </flavors>
				  </implementations>
				</services:ServiceProvider>
				""".formatted(secret.toUri(), NS);

		assertThatThrownBy(() -> XmiCodec.read(utf8(xml), rsObjects))
				.isInstanceOf(XmiCodecException.class)
				.as("the parser message carries the local path of the entity target — it must not reach the client")
				.hasMessageNotContaining("secret.txt")
				.hasMessageNotContaining("TOP-SECRET-BROKER-CONTENT");
	}

	@Test
	void rejectsEntityExpansionBomb() {
		// "Billion laughs", scaled down: without a DTD there is nothing to
		// expand, so this must be refused at the doctype, not survived by
		// an expansion counter.
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<!DOCTYPE provider [
				  <!ENTITY a "aaaaaaaaaa">
				  <!ENTITY b "&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;">
				  <!ENTITY c "&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;">
				  <!ENTITY d "&c;&c;&c;&c;&c;&c;&c;&c;&c;&c;">
				]>
				<services:ServiceProvider xmlns:services="%s" xmlns:xmi="http://www.omg.org/XMI"
				                      xmi:version="2.0" name="&d;" version="1.0.0"/>
				""".formatted(NS);

		assertThatThrownBy(() -> XmiCodec.read(utf8(xml), rsObjects))
				.isInstanceOf(XmiCodecException.class);
	}

	// ------------------------------------------------------------------
	// SSRF / arbitrary reference resolution.
	// ------------------------------------------------------------------

	@Test
	void doesNotFetchCrossDocumentReferences(@TempDir Path tmp) throws IOException {
		// A resolvable, real target: before the hardening EcoreUtil.resolveAll
		// would have loaded this file. It stands in for "any URI the caller
		// picks", including a broker-internal address.
		Path sideDoc = tmp.resolve("side.xmi");
		Files.writeString(sideDoc, """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:ServiceInterface xmlns:services="%s" xmlns:xmi="http://www.omg.org/XMI"
				                       xmi:version="2.0" name="SmuggledIn" version="9.9.9"/>
				""".formatted(NS));

		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:ServiceProvider xmlns:services="%s" xmlns:xmi="http://www.omg.org/XMI"
				                      xmi:version="2.0" name="payments-java" version="1.0.0">
				  <implementations name="payments-java-rest" version="1.0.0">
				    <serviceInterfaces href="%s#/"/>
				  </implementations>
				</services:ServiceProvider>
				""".formatted(NS, sideDoc.toUri());

		XmiBundle bundle = XmiCodec.readBundle(utf8(xml), rsObjects);

		ServiceProvider provider = (ServiceProvider) bundle.roots().get(0);
		ServiceImplementation impl = provider.getImplementations().get(0);
		assertThat(impl.getServiceInterfaces()).hasSize(1);

		InternalEObject ref = (InternalEObject) impl.getServiceInterfaces().get(0);
		assertThat(ref.eIsProxy())
				.as("cross-document href must stay an unresolved proxy — no fetch")
				.isTrue();
		assertThat(ref.eProxyURI().toString())
				.as("the URI is kept so the broker can read name and index off it")
				.contains("side.xmi");
	}

	@Test
	void keepsTheProxyUriReadableForRewiring(@TempDir Path tmp) throws IOException {
		// The shape the broker actually relies on (ARCHITECTURE.md §2.4):
		// catalog name in the last path segment, operation index in the
		// fragment. Both must survive unresolved.
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:ServiceProvider xmlns:services="%s" xmlns:xmi="http://www.omg.org/XMI"
				                      xmi:version="2.0" name="payments-java" version="1.0.0">
				  <implementations name="payments-java-rest" version="1.0.0">
				    <serviceInterfaces href="http://broker.example/ddsr/rest/catalog/Payment"/>
				  </implementations>
				</services:ServiceProvider>
				""".formatted(NS);

		XmiBundle bundle = XmiCodec.readBundle(utf8(xml), rsObjects);

		ServiceProvider provider = (ServiceProvider) bundle.roots().get(0);
		InternalEObject ref = (InternalEObject) provider.getImplementations().get(0)
				.getServiceInterfaces().get(0);

		assertThat(ref.eIsProxy()).isTrue();
		org.eclipse.emf.common.util.URI uri = ref.eProxyURI();
		assertThat(uri.segment(uri.segmentCount() - 1))
				.as("DdsrBrokerImpl.catalogNameOf reads the catalog name from here")
				.isEqualTo("Payment");
	}

	// ------------------------------------------------------------------
	// Body size limit (S7).
	// ------------------------------------------------------------------

	/**
	 * Well-formed XMI padded past a target size. The padding sits in
	 * element content, so the document stays valid all the way up — what
	 * stops an oversized one has to be the size cap, nothing else.
	 */
	private static String paddedProvider(int targetBytes) {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<services:ServiceProvider xmlns:services=\"").append(NS).append("\"\n")
				.append("    xmlns:xmi=\"http://www.omg.org/XMI\"\n")
				.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
				.append("    xmi:version=\"2.0\" name=\"payments-java\" version=\"1.0.0\">\n")
				.append("  <implementations name=\"payments-java-rest\" version=\"1.0.0\">\n")
				.append("    <flavors xsi:type=\"services:RestFlavor\" name=\"rest\"")
				.append(" host=\"http://h\" basePath=\"/p\">\n");
		String filler = "x".repeat(1000);
		while (xml.length() < targetBytes) {
			xml.append("      <contentTypes>").append(filler).append("</contentTypes>\n");
		}
		xml.append("    </flavors>\n  </implementations>\n</services:ServiceProvider>\n");
		return xml.toString();
	}

	private static XmiCodecException.Reason reasonOf(Throwable t) {
		return ((XmiCodecException) t).reason();
	}

	@Test
	void rejectsABodyOverTheWireLimit() {
		String xml = paddedProvider(WireBody.MAX_BYTES + 1);
		assertThat(xml.length())
				.as("fixture must actually exceed the cap")
				.isGreaterThan(WireBody.MAX_BYTES);

		assertThatThrownBy(() -> XmiCodec.read(utf8(xml), rsObjects))
				.isInstanceOf(XmiCodecException.class)
				.extracting(XmiCodecHardeningTest::reasonOf)
				.as("too big is its own reason — the body is not malformed")
				.isEqualTo(XmiCodecException.Reason.TOO_LARGE);
	}

	@Test
	void acceptsABodyUnderTheWireLimit() throws IOException {
		// Same shape, comfortably below the cap: must still parse.
		EObject root = XmiCodec.read(utf8(paddedProvider(WireBody.MAX_BYTES / 2)), rsObjects);

		assertThat(((ServiceProvider) root).getName()).isEqualTo("payments-java");
	}

	@Test
	void readFullyRejectsAnOversizedStream() {
		byte[] oversized = new byte[WireBody.MAX_BYTES + 1];

		assertThatThrownBy(() -> WireBody.readFully(new ByteArrayInputStream(oversized)))
				.isInstanceOf(XmiCodecException.class)
				.extracting(XmiCodecHardeningTest::reasonOf)
				.isEqualTo(XmiCodecException.Reason.TOO_LARGE);
	}

	@Test
	void readFullyAcceptsABodyExactlyAtTheLimit() throws IOException {
		byte[] atLimit = new byte[WireBody.MAX_BYTES];

		assertThat(WireBody.readFully(new ByteArrayInputStream(atLimit)))
				.as("the limit itself must still be accepted")
				.hasSize(WireBody.MAX_BYTES);
	}

	// ------------------------------------------------------------------
	// Wire format: required attributes must be explicit
	// ------------------------------------------------------------------

	@Test
void aRequiredEnumAttributeIsAlwaysOnTheWire() throws IOException {
		// ServiceEvent.type is lowerBound=1. EMF omits an attribute whose
		// value equals the feature's default, so no meaningful literal may
		// BE the default — that is why ServiceEventType leads with
		// UNSPECIFIED (W4). Without it a REGISTERED event went out with no
		// type at all, and a non-EMF reader (the TypeScript client) would
		// have to know EMF's default rules to interpret the document.
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(ServiceEventType.REGISTERED);
		var ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId("abc");
		event.setReference(ref);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, rsObjects, List.of(event, ref));

		assertThat(out.toString(StandardCharsets.UTF_8))
				.as("the required type must be on the wire, not implied by a default")
				.contains("type=\"REGISTERED\"");
	}

	// ------------------------------------------------------------------
	// The JAX-RS providers translate, the codec does not
	// ------------------------------------------------------------------

	@Test
	void theProviderTurnsAnOversizedBodyInto413() {
		XmiMessageBodyReader reader = new XmiMessageBodyReader(rsObjects);
		// Valid XML, just too much of it. A buffer of zero bytes would be
		// rejected as malformed at the first byte and would prove nothing
		// about the size limit.
		String oversized = paddedProvider(WireBody.MAX_BYTES + 1);

		assertThatThrownBy(() -> reader.readFrom(null, null, null, null, null, utf8(oversized)))
				.as("a status code belongs to the transport, and this is the transport")
				.isInstanceOf(WebApplicationException.class)
				.extracting(e -> ((WebApplicationException) e).getResponse().getStatus())
				.isEqualTo(413);
	}

	@Test
	void theProviderTurnsAMalformedBodyInto400() {
		XmiMessageBodyReader reader = new XmiMessageBodyReader(rsObjects);

		assertThatThrownBy(() -> reader.readFrom(null, null, null, null, null, utf8("<not-xml")))
				.isInstanceOf(WebApplicationException.class)
				.extracting(e -> ((WebApplicationException) e).getResponse().getStatus())
				.isEqualTo(400);
	}

	// ------------------------------------------------------------------
	// Housekeeping.
	// ------------------------------------------------------------------

	@Test
	void releasesTheResourceSetEvenOnRejection() {
		String xml = "<not-xml";

		assertThatThrownBy(() -> XmiCodec.read(utf8(xml), rsObjects))
				.isInstanceOf(XmiCodecException.class);

		assertThat(rsObjects.outstanding)
				.as("the prototype ResourceSet must go back to the framework on the error path too")
				.isZero();
	}
}
