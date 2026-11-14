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


package org.eclipse.fennec.tags.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclEngineImpl;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclSettingDelegateFactory;
import org.eclipse.fennec.m2x.ocl.parser.OclParserSupport;
import org.eclipse.fennec.tags.EffectiveTag;
import org.eclipse.fennec.tags.MatchMode;
import org.eclipse.fennec.tags.Tag;
import org.eclipse.fennec.tags.TagOrigin;
import org.eclipse.fennec.tags.TagRegistry;
import org.eclipse.fennec.tags.TagsFactory;
import org.eclipse.fennec.tags.TagsPackage;
import org.eclipse.fennec.tags.service.TagSubjects.Subject;
import org.eclipse.fennec.tags.service.core.TagServiceImpl;
import org.eclipse.fennec.tags.service.store.XmiTagStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The tag service over a fake source of targets (#274): the tree, the
 * administrator's assignments, what a target carries and why, the
 * queries, renames that keep old names working, and a change that cannot
 * be kept being rolled back.
 */
class TagServiceTest {

	private static final String CONTRACT = "contract";
	private static final String IMPLEMENTATION = "implementation";

	@BeforeAll
	static void registerOcl() {
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.put(
				"http://www.eclipse.org/fennec/m2x/ocl/1.0",
				new OclSettingDelegateFactory(new OclEngineImpl(new OclParserSupport())));
		((EStructuralFeature.Internal) TagsPackage.Literals.TAG__PATH).setSettingDelegate(null);
	}

	/** A store in memory that can be told to fail. */
	private static final class MemoryStore implements TagStore {
		TagRegistry kept = TagsFactory.eINSTANCE.createTagRegistry();
		boolean failing;
		int saves;

		@Override
		public TagRegistry load() {
			return EcoreUtil.copy(kept);
		}

		@Override
		public void save(TagRegistry registry) throws IOException {
			if (failing) {
				throw new IOException("disk full");
			}
			saves++;
			kept = EcoreUtil.copy(registry);
		}
	}

	/** Targets of two kinds, changeable by the test. */
	private static final class FakeSubjects implements TagSubjects {
		final List<Subject> subjects = new ArrayList<>();

		void add(String kind, String key, String... requestedTags) {
			subjects.add(new Subject(kind, key, Map.of(), List.of(requestedTags)));
		}

		@Override
		public Set<String> kinds() {
			return Set.of(CONTRACT, IMPLEMENTATION);
		}

		@Override
		public Collection<Subject> subjects(String kind) {
			return subjects.stream().filter(s -> s.kind().equals(kind)).toList();
		}
	}

	private final MemoryStore store = new MemoryStore();
	private final FakeSubjects subjects = new FakeSubjects();
	private TagServiceImpl tags;

	@BeforeEach
	void setUp() {
		tags = new TagServiceImpl(store);
		tags.addSubjects(subjects);
		tags.putTag(null, "payment", "Payment", null, false);
		tags.putTag("payment", "method", "Method", null, false);
		tags.putTag("payment/method", "card", "Card", null, true);
		tags.putTag("payment/method", "paypal", "PayPal", null, true);
		tags.putTag("payment", "terms", "Terms", null, false);
		tags.putTag("payment/terms", "instant", "Instant", null, false);
		tags.putTag("payment/terms", "installment", "Installment", null, false);
	}

	// ------------------------------------------------------------------ tree

	@Test
	void theTreeIsAnsweredAsCopies() {
		List<Tag> roots = tags.tags();

		assertThat(roots).extracting(Tag::getId).containsExactly("payment");
		roots.get(0).setId("changed");
		assertThat(tags.tags().get(0).getId()).as("a caller's copy changes nothing").isEqualTo("payment");
	}

	@Test
	void putTagUpdatesWhatIsThere() {
		String path = tags.putTag("payment/method", "card", "Credit card", "Visa and friends", false);

		assertThat(path).isEqualTo("payment/method/card");
		Tag card = tags.tags().get(0).getChildren().get(0).getChildren().get(0);
		assertThat(card.getName()).isEqualTo("Credit card");
		assertThat(card.isSelfAssignable()).isFalse();
		assertThat(card.getParent().getChildren()).hasSize(2);
	}

	@Test
	void anIdWithASlashOrUnderAMissingParentIsRefused() {
		assertThatThrownBy(() -> tags.putTag(null, "a/b", null, null, false)).isInstanceOf(InvalidTagException.class);
		assertThatThrownBy(() -> tags.putTag("nope", "x", null, null, false)).isInstanceOf(UnknownTagException.class);
	}

	@Test
	void aRenameKeepsTheOldPathsWorkingForTheWholeSubtree() {
		tags.assign("payment/method/card", IMPLEMENTATION, "card-provider");
		subjects.add(IMPLEMENTATION, "card-provider");

		String renamed = tags.moveTag("payment", null, "billing");

		assertThat(renamed).isEqualTo("billing");
		assertThat(tags.find(List.of("billing/method/card"), MatchMode.ANY, IMPLEMENTATION)).containsExactly("card-provider");
		assertThat(tags.find(List.of("payment/method/card"), MatchMode.ANY, IMPLEMENTATION))
				.as("the old path still names the tag").containsExactly("card-provider");
		assertThat(tags.tagsOf(IMPLEMENTATION, "card-provider")).extracting(EffectiveTag::getPath)
				.as("answers carry the current path").containsExactly("billing/method/card");
	}

	@Test
	void aMoveUnderAnotherParentMovesTheSubtree() {
		tags.putTag(null, "shop", null, null, false);

		assertThat(tags.moveTag("payment/terms", "shop", null)).isEqualTo("shop/terms");
		assertThat(tags.moveTag("payment/terms/instant", "shop/terms", "now")).as("found by its former path").isEqualTo("shop/terms/now");
	}

	@Test
	void aTagCannotMoveUnderItselfOrOntoATakenId() {
		assertThatThrownBy(() -> tags.moveTag("payment", "payment/method", null)).isInstanceOf(InvalidTagException.class);
		assertThatThrownBy(() -> tags.moveTag("payment/terms", "payment", "method")).isInstanceOf(InvalidTagException.class);
	}

	@Test
	void removingATagTakesItsSubtreeAndTheirAssignments() {
		tags.assign("payment/method/card", IMPLEMENTATION, "card-provider");
		subjects.add(IMPLEMENTATION, "card-provider");

		tags.removeTag("payment/method");

		assertThat(tags.tags().get(0).getChildren()).extracting(Tag::getId).containsExactly("terms");
		assertThat(tags.tagsOf(IMPLEMENTATION, "card-provider")).isEmpty();
		assertThat(store.kept.getAssignments()).isEmpty();
	}

	// ------------------------------------------------------------- carried

	@Test
	void anAdministratorsAssignmentIsCarried() {
		subjects.add(CONTRACT, "Payment");

		tags.assign("payment/terms/instant", CONTRACT, "Payment");
		tags.assign("payment/terms/instant", CONTRACT, "Payment");

		assertThat(tags.tagsOf(CONTRACT, "Payment")).extracting(EffectiveTag::getPath, EffectiveTag::getOrigin)
				.as("assigning twice changes nothing").containsExactly(tuple("payment/terms/instant", TagOrigin.ADMIN));
	}

	@Test
	void aTargetGetsWhatItAsksForOnlyWhereTheTagAllowsIt() {
		subjects.add(IMPLEMENTATION, "card-provider", "payment/method/card", "payment/terms/instant", "nowhere");

		assertThat(tags.tagsOf(IMPLEMENTATION, "card-provider")).extracting(EffectiveTag::getPath)
				.as("card is self-assignable, instant is not, nowhere does not exist").containsExactly("payment/method/card");
	}

	@Test
	void aTagCarriedForTwoReasonsIsReportedTwice() {
		subjects.add(IMPLEMENTATION, "card-provider", "payment/method/card");
		tags.assign("payment/method/card", IMPLEMENTATION, "card-provider");

		assertThat(tags.tagsOf(IMPLEMENTATION, "card-provider")).extracting(EffectiveTag::getOrigin)
				.containsExactly(TagOrigin.SOURCE, TagOrigin.ADMIN);
	}

	@Test
	void unassignLeavesWhatTheTargetCarriesForAnotherReason() {
		subjects.add(IMPLEMENTATION, "card-provider", "payment/method/card");
		tags.assign("payment/method/card", IMPLEMENTATION, "card-provider");

		tags.unassign("payment/method/card", IMPLEMENTATION, "card-provider");

		assertThat(tags.tagsOf(IMPLEMENTATION, "card-provider")).extracting(EffectiveTag::getOrigin)
				.containsExactly(TagOrigin.SOURCE);
	}

	// --------------------------------------------------------------- find

	@Test
	void aParentFindsWhatCarriesAChild() {
		subjects.add(IMPLEMENTATION, "card-provider", "payment/method/card");
		subjects.add(IMPLEMENTATION, "paypal-provider", "payment/method/paypal");
		subjects.add(IMPLEMENTATION, "unrelated");

		assertThat(tags.find(List.of("payment"), MatchMode.ANY, IMPLEMENTATION))
				.containsExactly("card-provider", "paypal-provider");
		assertThat(tags.find(List.of("payment/method/card"), MatchMode.ANY, IMPLEMENTATION))
				.containsExactly("card-provider");
	}

	@Test
	void anyAndAll() {
		subjects.add(CONTRACT, "Payment");
		subjects.add(CONTRACT, "InstallmentPlan");
		tags.assign("payment/terms/instant", CONTRACT, "Payment");
		tags.assign("payment/terms/installment", CONTRACT, "InstallmentPlan");
		tags.assign("payment/method/card", CONTRACT, "InstallmentPlan");

		assertThat(tags.find(List.of("payment/terms/instant", "payment/terms/installment"), MatchMode.ANY, CONTRACT))
				.containsExactly("InstallmentPlan", "Payment");
		assertThat(tags.find(List.of("payment/terms/installment", "payment/method/card"), MatchMode.ALL, CONTRACT))
				.containsExactly("InstallmentPlan");
		assertThat(tags.find(List.of("payment/terms/instant", "nowhere"), MatchMode.ALL, CONTRACT))
				.as("a path no tag has matches nothing").isEmpty();
	}

	@Test
	void onlyTargetsTheirSourceKnowsAreFound() {
		tags.assign("payment/terms/instant", CONTRACT, "Gone");

		assertThat(tags.find(List.of("payment"), MatchMode.ANY, CONTRACT)).isEmpty();
		assertThat(tags.tagsOf(CONTRACT, "Gone")).as("the assignment waits for it").hasSize(1);

		subjects.add(CONTRACT, "Gone");
		assertThat(tags.find(List.of("payment"), MatchMode.ANY, CONTRACT)).containsExactly("Gone");
	}

	@Test
	void aTagCreatedLaterAppliesToATargetThatAskedForItEarlier() {
		subjects.add(IMPLEMENTATION, "card-provider", "payment/method/debit");
		assertThat(tags.tagsOf(IMPLEMENTATION, "card-provider")).isEmpty();

		tags.putTag("payment/method", "debit", null, null, true);

		assertThat(tags.find(List.of("payment/method/debit"), MatchMode.ANY, IMPLEMENTATION)).containsExactly("card-provider");
	}

	// ------------------------------------------------------------ keeping

	@Test
	void everyChangeIsKeptBeforeItIsAcknowledged() {
		int before = store.saves;

		tags.putTag(null, "iot", null, null, false);

		assertThat(store.saves).isEqualTo(before + 1);
		assertThat(store.kept.getTags()).extracting(Tag::getId).contains("iot");
	}

	@Test
	void aChangeThatCannotBeKeptIsRolledBack() {
		store.failing = true;

		assertThatThrownBy(() -> tags.putTag(null, "iot", null, null, false)).isInstanceOf(TagStoreException.class);
		assertThatThrownBy(() -> tags.removeTag("payment")).isInstanceOf(TagStoreException.class);

		assertThat(tags.tags()).extracting(Tag::getId).containsExactly("payment");
		assertThat(tags.tags().get(0).getChildren()).hasSize(2);
	}

	@Test
	void aRestartReadsWhatWasKept(@TempDir Path dir) {
		XmiTagStore xmi = new XmiTagStore(dir.resolve("broker-tags.xmi"));
		TagServiceImpl first = new TagServiceImpl(xmi);
		first.addSubjects(subjects);
		first.putTag(null, "iot", "IoT", "things", true);
		first.putTag("iot", "sensors", null, null, true);
		first.assign("iot/sensors", CONTRACT, "Thermometer");
		first.moveTag("iot", null, "things");
		subjects.add(CONTRACT, "Thermometer");

		TagServiceImpl restarted = new TagServiceImpl(xmi);
		restarted.addSubjects(subjects);

		assertThat(Files.exists(dir.resolve("broker-tags.xmi"))).isTrue();
		assertThat(restarted.tagsOf(CONTRACT, "Thermometer")).extracting(EffectiveTag::getPath)
				.containsExactly("things/sensors");
		assertThat(restarted.find(List.of("iot/sensors"), MatchMode.ANY, CONTRACT))
				.as("former paths survive the restart").containsExactly("Thermometer");
	}
}
