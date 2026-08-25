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

package org.eclipse.fennec.services.broker.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.eclipse.fennec.services.BoolProperty;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.DoubleProperty;
import org.eclipse.fennec.services.FloatProperty;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.LongProperty;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ShortProperty;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;
import org.junit.jupiter.api.Test;

/**
 * Parser and matching of the minimal RFC-1960 evaluator, against the
 * typed property hierarchy. OSGi filter semantics are the reference:
 * case-insensitive keys, numeric comparison for numeric types, any-match
 * over string lists, absent property never matches.
 */
class LdapFilterTest {

	private static ServiceReference reference() {
		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId("ref-1");

		StringProperty lang = ServicesFactory.eINSTANCE.createStringProperty();
		lang.setName("ddsr.provider.lang");
		lang.setValue("java");
		ref.getProperties().add(lang);

		IntProperty ranking = ServicesFactory.eINSTANCE.createIntProperty();
		ranking.setName("service.ranking");
		ranking.setValue(10);
		ref.getProperties().add(ranking);

		LongProperty big = ServicesFactory.eINSTANCE.createLongProperty();
		big.setName("maxAmount");
		big.setValue(5_000_000_000L);
		ref.getProperties().add(big);

		DoubleProperty rate = ServicesFactory.eINSTANCE.createDoubleProperty();
		rate.setName("feeRate");
		rate.setValue(0.025d);
		ref.getProperties().add(rate);

		FloatProperty timeout = ServicesFactory.eINSTANCE.createFloatProperty();
		timeout.setName("timeout");
		timeout.setValue(1.5f);
		ref.getProperties().add(timeout);

		ShortProperty retries = ServicesFactory.eINSTANCE.createShortProperty();
		retries.setName("retries");
		retries.setValue((short) 3);
		ref.getProperties().add(retries);

		BoolProperty sandbox = ServicesFactory.eINSTANCE.createBoolProperty();
		sandbox.setName("sandbox");
		sandbox.setValue(true);
		ref.getProperties().add(sandbox);

		StringListProperty tags = ServicesFactory.eINSTANCE.createStringListProperty();
		tags.setName("tags");
		tags.getValue().add("demo");
		tags.getValue().add("payments");
		ref.getProperties().add(tags);

		StringListProperty empty = ServicesFactory.eINSTANCE.createStringListProperty();
		empty.setName("emptyList");
		ref.getProperties().add(empty);

		return ref;
	}

	private static boolean matches(String filter) {
		return LdapFilter.parse(filter).matches(reference());
	}

	// --- equality and presence ----------------------------------------

	@Test
	void stringEquality() {
		assertThat(matches("(ddsr.provider.lang=java)")).isTrue();
		assertThat(matches("(ddsr.provider.lang=typescript)")).isFalse();
	}

	@Test
	void keysMatchCaseInsensitively() {
		assertThat(matches("(DDSR.Provider.LANG=java)"))
				.as("OSGi property keys are case-insensitive in filters")
				.isTrue();
	}

	@Test
	void valuesMatchCaseSensitively() {
		assertThat(matches("(ddsr.provider.lang=JAVA)")).isFalse();
	}

	@Test
	void presence() {
		assertThat(matches("(sandbox=*)")).isTrue();
		assertThat(matches("(no.such.key=*)")).isFalse();
		assertThat(matches("(emptyList=*)"))
				.as("an empty list is a present property with no values")
				.isTrue();
	}

	@Test
	void anAbsentPropertyNeverMatchesButItsNegationDoes() {
		assertThat(matches("(no.such.key=java)")).isFalse();
		assertThat(matches("(!(no.such.key=java))")).isTrue();
	}

	// --- numbers --------------------------------------------------------

	@Test
	void numericComparisonNotLexicographic() {
		assertThat(matches("(service.ranking>=9)")).isTrue();
		assertThat(matches("(service.ranking<=9)")).isFalse();
		assertThat(matches("(service.ranking=10)")).isTrue();
		// Lexicographically "10" < "9" — numerically it is not.
		assertThat(matches("(maxAmount>=999)")).isTrue();
	}

	@Test
	void longRangeSurvives() {
		assertThat(matches("(maxAmount=5000000000)")).isTrue();
		assertThat(matches("(maxAmount>=6000000000)")).isFalse();
	}

	@Test
	void floatingPointComparison() {
		assertThat(matches("(feeRate<=0.03)")).isTrue();
		assertThat(matches("(feeRate>=0.03)")).isFalse();
		assertThat(matches("(timeout=1.5)")).isTrue();
		assertThat(matches("(retries>=3)")).isTrue();
	}

	@Test
	void anUnparsableNumberSimplyDoesNotMatch() {
		assertThat(matches("(service.ranking=abc)")).isFalse();
		assertThat(matches("(!(service.ranking=abc))")).isTrue();
	}

	// --- booleans -------------------------------------------------------

	@Test
	void booleans() {
		assertThat(matches("(sandbox=true)")).isTrue();
		assertThat(matches("(sandbox=TRUE)")).isTrue();
		assertThat(matches("(sandbox=false)")).isFalse();
		assertThat(matches("(sandbox=yes)")).isFalse();
	}

	// --- string lists -----------------------------------------------------

	@Test
	void stringListMatchesWhenAnyElementMatches() {
		assertThat(matches("(tags=demo)")).isTrue();
		assertThat(matches("(tags=payments)")).isTrue();
		assertThat(matches("(tags=prod)")).isFalse();
		assertThat(matches("(emptyList=anything)"))
				.as("no element, no value match")
				.isFalse();
	}

	// --- substrings -------------------------------------------------------

	@Test
	void substrings() {
		assertThat(matches("(ddsr.provider.lang=ja*)")).isTrue();
		assertThat(matches("(ddsr.provider.lang=*va)")).isTrue();
		assertThat(matches("(ddsr.provider.lang=j*a)")).isTrue();
		assertThat(matches("(ddsr.provider.lang=*av*)")).isTrue();
		assertThat(matches("(ddsr.provider.lang=j*x*a)")).isFalse();
		assertThat(matches("(tags=pay*)")).isTrue();
	}

	@Test
	void substringPartsMayNotOverlap() {
		// "java" must not satisfy j*ava*a (the trailing 'a' is consumed).
		assertThat(matches("(ddsr.provider.lang=j*ava*a)")).isFalse();
	}

	// --- composition ------------------------------------------------------

	@Test
	void nestedAndOrNot() {
		assertThat(matches("(&(ddsr.provider.lang=java)(service.ranking>=5))")).isTrue();
		assertThat(matches("(|(ddsr.provider.lang=typescript)(service.ranking>=5))")).isTrue();
		assertThat(matches("(&(ddsr.provider.lang=java)(!(sandbox=true)))")).isFalse();
		assertThat(matches("(|(&(tags=demo)(sandbox=true))(no.such.key=*))")).isTrue();
	}

	// --- escaping ---------------------------------------------------------

	@Test
	void escapedSpecialCharacters() {
		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		StringProperty odd = ServicesFactory.eINSTANCE.createStringProperty();
		odd.setName("path");
		odd.setValue("a(b)*c");
		ref.getProperties().add(odd);

		assertThat(LdapFilter.parse("(path=a\\(b\\)\\*c)").matches(ref)).isTrue();
		assertThat(LdapFilter.parse("(path=a\\(b\\)*)").matches(ref))
				.as("unescaped * is still a wildcard")
				.isTrue();
	}

	// --- broken input -------------------------------------------------------

	@Test
	void syntaxErrorsThrowAtParseTime() {
		assertThatIllegalArgumentException().isThrownBy(() -> LdapFilter.parse("((oops"));
		assertThatIllegalArgumentException().isThrownBy(() -> LdapFilter.parse("(a=b"));
		assertThatIllegalArgumentException().isThrownBy(() -> LdapFilter.parse("(a=b)(c=d)"));
		assertThatIllegalArgumentException().isThrownBy(() -> LdapFilter.parse("(=b)"));
		assertThatIllegalArgumentException().isThrownBy(() -> LdapFilter.parse(""));
		assertThatIllegalArgumentException().isThrownBy(() -> LdapFilter.parse(null));
	}

	/**
	 * The first (hand-written) iteration rejected {@code ~=}; the OSGi
	 * filter supports approximate matching, so the adapter does too —
	 * a deliberate behavior change towards more OSGi conformity.
	 */
	@Test
	void approximateMatchingIsSupported() {
		assertThat(matches("(ddsr.provider.lang~=JAVA)")).isTrue();
		assertThat(matches("(ddsr.provider.lang~=typescript)")).isFalse();
	}
}
