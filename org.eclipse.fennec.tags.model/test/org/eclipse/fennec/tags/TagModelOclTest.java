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

package org.eclipse.fennec.tags;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclEngineImpl;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclSettingDelegateFactory;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclValidationDelegateFactory;
import org.eclipse.fennec.m2x.ocl.parser.OclParserSupport;
import org.eclipse.fennec.tags.util.TagsValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The OCL in the tag model (#273): a tag's path is derived from the ids
 * up the tree, so renaming a parent moves every descendant without
 * rewriting anything; ids carry no '/' and are unique among siblings.
 *
 * <p>Plain JUnit with the Fennec OCL engine registered standalone; in a
 * launch the same factories arrive as DS services.
 */
class TagModelOclTest {

	private static final String DELEGATE_URI = "http://www.eclipse.org/fennec/m2x/ocl/1.0";

	@BeforeAll
	static void registerOclDelegates() {
		OclEngineImpl engine = new OclEngineImpl(new OclParserSupport());
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.put(
				DELEGATE_URI, new OclSettingDelegateFactory(engine));
		EValidator.ValidationDelegate.Registry.INSTANCE.put(
				DELEGATE_URI, new OclValidationDelegateFactory(engine));
		EValidator.Registry.INSTANCE.put(TagsPackage.eINSTANCE, TagsValidator.INSTANCE);
		((EStructuralFeature.Internal) TagsPackage.Literals.TAG__PATH).setSettingDelegate(null);
	}

	@Test
	void aRootsPathIsItsId() {
		TagRegistry registry = TagsFactory.eINSTANCE.createTagRegistry();
		Tag iot = tag("iot");
		registry.getTags().add(iot);

		assertThat(iot.getPath()).isEqualTo("iot");
	}

	@Test
	void aChildsPathJoinsTheIdsFromTheRoot() {
		Tag payment = tag("payment");
		Tag method = child(payment, "method");
		Tag card = child(method, "card");

		assertThat(method.getPath()).isEqualTo("payment/method");
		assertThat(card.getPath()).isEqualTo("payment/method/card");
	}

	@Test
	void renamingAParentMovesEveryDescendant() {
		Tag payment = tag("payment");
		Tag card = child(child(payment, "method"), "card");

		payment.setId("billing");

		assertThat(card.getPath()).isEqualTo("billing/method/card");
	}

	@Test
	void movingATagMovesItsSubtree() {
		Tag payment = tag("payment");
		Tag terms = child(payment, "terms");
		Tag instant = child(terms, "instant");
		Tag shop = tag("shop");

		shop.getChildren().add(terms);

		assertThat(instant.getPath()).isEqualTo("shop/terms/instant");
	}

	@Test
	void anIdMayNotContainASlash() {
		TagRegistry registry = TagsFactory.eINSTANCE.createTagRegistry();
		registry.getTags().add(tag("iot/sensors"));

		assertThat(validate(registry)).contains("idHasNoSlash");
	}

	@Test
	void siblingsMayNotShareAnId() {
		Tag payment = tag("payment");
		child(payment, "card");
		child(payment, "card");
		TagRegistry registry = TagsFactory.eINSTANCE.createTagRegistry();
		registry.getTags().add(payment);

		assertThat(validate(registry)).contains("childIdsUnique");
	}

	@Test
	void rootsMayNotShareAnId() {
		TagRegistry registry = TagsFactory.eINSTANCE.createTagRegistry();
		registry.getTags().add(tag("iot"));
		registry.getTags().add(tag("iot"));

		assertThat(validate(registry)).contains("rootIdsUnique");
	}

	@Test
	void theSameIdUnderDifferentParentsIsFine() {
		TagRegistry registry = TagsFactory.eINSTANCE.createTagRegistry();
		Tag iot = tag("iot");
		Tag marketing = tag("marketing");
		child(iot, "weather");
		child(marketing, "weather");
		registry.getTags().add(iot);
		registry.getTags().add(marketing);

		assertThat(Diagnostician.INSTANCE.validate(registry).getSeverity()).isEqualTo(Diagnostic.OK);
	}

	// ------------------------------------------------------------------

	private static Tag tag(String id) {
		Tag tag = TagsFactory.eINSTANCE.createTag();
		tag.setId(id);
		return tag;
	}

	private static Tag child(Tag parent, String id) {
		Tag child = tag(id);
		parent.getChildren().add(child);
		return child;
	}

	private static String validate(TagRegistry registry) {
		Diagnostic diagnostic = Diagnostician.INSTANCE.validate(registry);
		StringBuilder messages = new StringBuilder();
		for (Diagnostic child : diagnostic.getChildren()) {
			messages.append(child.getMessage()).append('\n');
		}
		return messages.toString();
	}
}
