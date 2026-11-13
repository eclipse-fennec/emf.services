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

package org.eclipse.fennec.services.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.fennec.services.BoolProperty;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;
import org.junit.jupiter.api.Test;

/** ServiceReference.getProperty / getPropertyKeys, generated from the model's body annotations (#258). */
class ServiceReferencePropertiesTest {

	private static ServiceReference reference() {
		ServicesFactory f = ServicesFactory.eINSTANCE;
		ServiceReference reference = f.createServiceReference();
		StringProperty availability = f.createStringProperty();
		availability.setName("availability");
		availability.setValue("REACHABLE");
		IntProperty ranking = f.createIntProperty();
		ranking.setName("service.ranking");
		ranking.setValue(7);
		BoolProperty secure = f.createBoolProperty();
		secure.setName("secure");
		secure.setValue(true);
		StringListProperty regions = f.createStringListProperty();
		regions.setName("regions");
		regions.getValue().addAll(List.of("eu", "us"));
		reference.getProperties().addAll(List.of(availability, ranking, secure, regions));
		return reference;
	}

	@Test
	void aPropertyIsLookedUpByNameWhateverItsType() {
		ServiceReference reference = reference();

		assertThat(reference.getProperty("availability")).isEqualTo("REACHABLE");
		assertThat(reference.getProperty("service.ranking")).isEqualTo(7);
		assertThat(reference.getProperty("secure")).isEqualTo(true);
		assertThat(reference.getProperty("regions")).isEqualTo(List.of("eu", "us"));
	}

	@Test
	void anUnknownOrMissingKeyIsNull() {
		assertThat(reference().getProperty("nobody")).isNull();
		assertThat(reference().getProperty(null)).isNull();
	}

	@Test
	void theKeysAreTheNamesOfAllProperties() {
		assertThat(reference().getPropertyKeys()).containsExactly("availability", "service.ranking", "secure", "regions");
		assertThat(ServicesFactory.eINSTANCE.createServiceReference().getPropertyKeys()).isEmpty();
	}
}
