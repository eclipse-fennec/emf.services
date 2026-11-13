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

package org.eclipse.fennec.services.models.atlas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.fennec.model.atlas.rest.client.api.ResolvedEPackage;
import org.eclipse.fennec.model.atlas.rest.client.api.VersionMismatchException;
import org.eclipse.fennec.services.models.ModelDocument;
import org.junit.jupiter.api.Test;

class AtlasModelSourceTest {

	private static final String NS_URI = "http://example.org/orders/1.0";

	/** A stand-in for the fingerprint: the package's name and class count. */
	private static final Function<EPackage, String> FINGERPRINTS = ePackage -> "fp1:" + ePackage.getName() + "-"
			+ ePackage.getEClassifiers().size();

	private final AtomicInteger reads = new AtomicInteger();

	@Test
	void aPinnedModelComesFromTheAtlasWithTheFingerprintComputedHere() throws IOException {
		AtlasModelSource source = source((nsURI, fingerprint) -> released(orders()));

		Optional<ModelDocument> found = source.resolve(NS_URI, "fp1:orders-1");

		assertThat(found).isPresent();
		assertThat(found.get().fingerprint()).isEqualTo("fp1:orders-1");
		assertThat(found.get().origin()).isEqualTo("atlas shop/release version 1.0");
		EPackage read = read(found.get().ecore());
		assertThat(read.getNsURI()).isEqualTo(NS_URI);
		assertThat(read.getEClassifier("Order")).isNotNull();
	}

	@Test
	void anAnswerInAnotherVersionThanThePinIsNoAnswer() {
		AtlasModelSource source = source((nsURI, fingerprint) -> released(orders()));

		assertThat(source.resolve(NS_URI, "fp1:orders-2")).isEmpty();
	}

	@Test
	void theAtlasRefusingThePinIsNoAnswer() {
		AtlasModelSource source = source((nsURI, fingerprint) -> {
			throw new VersionMismatchException("412 at " + nsURI);
		});

		assertThat(source.resolve(NS_URI, "fp1:orders-1")).isEmpty();
	}

	@Test
	void anAtlasThatCannotBeReachedIsNoAnswer() {
		AtlasModelSource source = source((nsURI, fingerprint) -> {
			throw new IllegalStateException("connection refused");
		});

		assertThat(source.resolve(NS_URI, "fp1:orders-1")).isEmpty();
	}

	@Test
	void aModelTheAtlasDoesNotHoldIsNoAnswer() {
		AtlasModelSource source = source((nsURI, fingerprint) -> Optional.empty());

		assertThat(source.resolve(NS_URI, null)).isEmpty();
		assertThat(source.resolve(null, null)).isEmpty();
	}

	@Test
	void aPinnedAnswerIsAskedOnceAnUnpinnedOneEveryTime() {
		AtlasModelSource source = source((nsURI, fingerprint) -> released(orders()));

		source.resolve(NS_URI, "fp1:orders-1");
		source.resolve(NS_URI, "fp1:orders-1");
		assertThat(reads).hasValue(1);

		source.resolve(NS_URI, null);
		source.resolve(NS_URI, null);
		assertThat(reads).hasValue(3);
	}

	@Test
	void anUnpinnedQuestionGetsWhatTheAtlasReleasedNow() {
		AtlasModelSource source = source((nsURI, fingerprint) -> released(orders()));

		assertThat(source.resolve(NS_URI, null)).map(ModelDocument::fingerprint).contains("fp1:orders-1");
	}

	private AtlasModelSource source(AtlasModelSource.AtlasRead atlas) {
		AtlasModelSource.AtlasRead counting = (nsURI, fingerprint) -> {
			reads.incrementAndGet();
			return atlas.apply(nsURI, fingerprint);
		};
		return new AtlasModelSource(counting, FINGERPRINTS, "shop", "release", () -> {
		});
	}

	private static Optional<ResolvedEPackage> released(EPackage ePackage) {
		return Optional.of(new ResolvedEPackage(ePackage, ePackage.getNsURI(), "shop", null, "release", "1.0",
				null));
	}

	private static EPackage orders() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("orders");
		ePackage.setNsPrefix("orders");
		ePackage.setNsURI(NS_URI);
		EClass order = EcoreFactory.eINSTANCE.createEClass();
		order.setName("Order");
		EAttribute id = EcoreFactory.eINSTANCE.createEAttribute();
		id.setName("id");
		id.setEType(EcorePackage.Literals.ESTRING);
		order.getEStructuralFeatures().add(id);
		ePackage.getEClassifiers().add(order);
		return ePackage;
	}

	private static EPackage read(byte[] ecore) throws IOException {
		Resource resource = new EcoreResourceFactoryImpl().createResource(URI.createURI("read.ecore"));
		resource.load(new ByteArrayInputStream(ecore), Map.of());
		return (EPackage) resource.getContents().get(0);
	}
}
