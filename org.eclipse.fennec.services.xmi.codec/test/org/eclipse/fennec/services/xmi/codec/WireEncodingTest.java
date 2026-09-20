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
import java.nio.charset.StandardCharsets;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.protobuf.resource.ProtobufResource;
import org.eclipse.fennec.protobuf.resource.ProtobufResourceFactory;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * The encoding is a choice, not an assumption (#100).
 *
 * <p>How an argument or a result became bytes used to be decided inside
 * each transport and nobody could say otherwise: XMI over REST because
 * the wire resource was created as {@code ddsr-wire.xmi}, hand-rolled
 * JSON over MQTT. The codec now asks the ResourceSet which factory
 * serves a content type, which is EMF's own mechanism and needs no
 * serializer abstraction on top.
 *
 * <p>These tests use a <b>real</b> second encoding rather than a
 * stand-in: {@code ProtobufResourceFactory} from Eclipse Fennec
 * emf.util, registered for its own content type exactly the way any
 * other would be. A fake factory would prove that the lookup happens;
 * only a real one proves that what comes out the other end is the
 * model again.
 */
class WireEncodingTest {

	private static final String NS = "http://eclipse.org/fennec/services/1.0";

	/**
	 * Hands out a ResourceSet with XMI and protobuf registered, the way
	 * a deployment that wanted both would have it.
	 */
	private static final class BothEncodings implements ComponentServiceObjects<ResourceSet> {

		int outstanding;

		@Override
		public ResourceSet getService() {
			outstanding++;
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			rs.getResourceFactoryRegistry().getContentTypeToFactoryMap()
					.put(ProtobufResource.CONTENT_TYPE, new ProtobufResourceFactory());
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

	private final BothEncodings rsObjects = new BothEncodings();

	// ------------------------------------------------------------------
	// XMI stays what it was
	// ------------------------------------------------------------------

	@Test
	@DisplayName("no content type is XMI, exactly as before")
	void nothingSaidIsXmi() throws IOException {
		byte[] written = write(null, contract());

		assertThat(new String(written, StandardCharsets.UTF_8))
				.startsWith("<?xml")
				.contains("ServiceInterface")
				.contains("Payment");
	}

	@Test
	@DisplayName("application/xml is XMI too, including the +xml family and a charset parameter")
	void theXmlFamilyIsXmi() throws IOException {
		for (String contentType : new String[] { "application/xml", "text/xml", "APPLICATION/XML",
				"application/xml; charset=UTF-8", "application/services+xml" }) {
			assertThat(new String(write(contentType, contract()), StandardCharsets.UTF_8))
					.as("%s must be served as XMI", contentType)
					.startsWith("<?xml");
		}
	}

	@Test
	@DisplayName("XMI still round-trips through the codec")
	void xmiRoundTrips() throws IOException {
		byte[] written = write(null, contract());

		ServiceInterface read = (ServiceInterface) XmiCodec.read(new ByteArrayInputStream(written), rsObjects);

		assertThat(read.getName()).isEqualTo("Payment");
		assertThat(read.getOperations()).extracting(ServiceOperation::getName).containsExactly("charge");
	}

	// ------------------------------------------------------------------
	// A second encoding, for real
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a registered content type is written in that encoding, not in XMI under another name")
	void protobufIsActuallyProtobuf() throws IOException {
		byte[] written = write(ProtobufResource.CONTENT_TYPE, contract());

		assertThat(new String(written, StandardCharsets.UTF_8))
				.as("this must not be XML with a protobuf label on it")
				.doesNotStartWith("<?xml");
		assertThat(written).isNotEmpty();
	}

	@Test
	@DisplayName("and it comes back as the model, which is the only thing that proves the encoding works")
	void protobufRoundTrips() throws IOException {
		byte[] written = write(ProtobufResource.CONTENT_TYPE, contract());

		ServiceInterface read = (ServiceInterface) XmiCodec.read(
				new ByteArrayInputStream(written), rsObjects, ProtobufResource.CONTENT_TYPE);

		assertThat(read.getName()).isEqualTo("Payment");
		assertThat(read.getVersion()).isEqualTo("1.0.0");
		assertThat(read.getOperations()).extracting(ServiceOperation::getName).containsExactly("charge");
	}

	@Test
	@DisplayName("the two encodings carry the same model and are not the same bytes")
	void theTwoEncodingsDiffer() throws IOException {
		byte[] asXmi = write(null, contract());
		byte[] asProtobuf = write(ProtobufResource.CONTENT_TYPE, contract());

		assertThat(asProtobuf).isNotEqualTo(asXmi);
		assertThat(((ServiceInterface) XmiCodec.read(new ByteArrayInputStream(asProtobuf), rsObjects,
				ProtobufResource.CONTENT_TYPE)).getName())
				.isEqualTo(((ServiceInterface) XmiCodec.read(new ByteArrayInputStream(asXmi), rsObjects)).getName());
	}

	// ------------------------------------------------------------------
	// Saying something nobody registered
	// ------------------------------------------------------------------

	@Test
	@DisplayName("an unregistered content type is refused, not quietly served as XMI")
	void anUnknownEncodingIsRefused() {
		assertThatThrownBy(() -> write("application/x-nobody-registered-this", contract()))
				.isInstanceOf(XmiCodecException.class)
				.hasMessageContaining("application/x-nobody-registered-this");
	}

	@Test
	@DisplayName("reading an unregistered content type is refused too")
	void readingAnUnknownEncodingIsRefused() {
		assertThatThrownBy(() -> XmiCodec.read(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), rsObjects,
				"application/x-nobody-registered-this"))
				.isInstanceOf(XmiCodecException.class);
	}

	@Test
	@DisplayName("a ResourceSet without protobuf refuses protobuf — the choice is the deployment's")
	void withoutTheFactoryTheEncodingIsNotAvailable() {
		ComponentServiceObjects<ResourceSet> xmiOnly = new ComponentServiceObjects<>() {

			@Override
			public ResourceSet getService() {
				ResourceSet rs = new ResourceSetImpl();
				rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
						.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
				rs.getPackageRegistry().put(NS, ServicesPackage.eINSTANCE);
				return rs;
			}

			@Override
			public void ungetService(ResourceSet service) {
			}

			@Override
			public ServiceReference<ResourceSet> getServiceReference() {
				throw new UnsupportedOperationException("not needed");
			}
		};

		assertThatThrownBy(() -> {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			XmiCodec.write(out, xmiOnly, ProtobufResource.CONTENT_TYPE, java.util.List.of(contract()));
		}).isInstanceOf(XmiCodecException.class);
	}

	// ------------------------------------------------------------------
	// The ResourceSet is still handed back
	// ------------------------------------------------------------------

	@Test
	@DisplayName("every encoding releases the ResourceSet it borrowed")
	void theResourceSetIsAlwaysReturned() throws IOException {
		write(null, contract());
		write(ProtobufResource.CONTENT_TYPE, contract());
		try {
			write("application/x-nobody-registered-this", contract());
		} catch (XmiCodecException expected) {
			// the refusal path has to hand it back as well
		}

		assertThat(rsObjects.outstanding).isZero();
	}

	// ------------------------------------------------------------------
	// Fixtures
	// ------------------------------------------------------------------

	private byte[] write(String contentType, ServiceInterface root) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, rsObjects, contentType, java.util.List.of(root));
		return out.toByteArray();
	}

	private static ServiceInterface contract() {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName("Payment");
		si.setVersion("1.0.0");
		ServiceOperation charge = ServicesFactory.eINSTANCE.createServiceOperation();
		charge.setName("charge");
		si.getOperations().add(charge);
		return si;
	}
}
