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
package org.eclipse.fennec.services.m2t;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Holds the Java rendering to model/type-pins.tsv, the table both
 * languages are checked against (#151). Its TypeScript twin is
 * packages/ddsr-example-stubs/test/type-pins.test.ts: one table, so a
 * mapping cannot change on one side without the other noticing.
 */
class TypePinsTest {

	private static final Path PINS = Path.of("model", "type-pins.tsv");

	private static final Path GENERATED = Path.of("src-gen", "org", "eclipse", "fennec",
			"services", "m2t", "example");

	private record Pin(String contract, String operation, String slot, boolean optional, String java) {
	}

	private static List<Pin> pins() throws IOException {
		List<Pin> pins = new ArrayList<>();
		for (String line : Files.readAllLines(PINS, StandardCharsets.UTF_8)) {
			if (line.isBlank() || line.startsWith("#")) {
				continue;
			}
			String[] cells = line.split("\t");
			pins.add(new Pin(cells[0], cells[1], cells[2], Boolean.parseBoolean(cells[3]), cells[4]));
		}
		return pins;
	}

	/** The signature the table describes, as service-interface.mtl writes it. */
	private static String signature(String operation, List<Pin> rows) {
		String returned = "void";
		String thrown = null;
		List<String> parameters = new ArrayList<>();
		for (Pin pin : rows) {
			switch (pin.slot()) {
			case "return" -> returned = pin.java();
			case "throws" -> thrown = pin.java();
			default -> parameters.add(pin.java() + " " + pin.slot());
			}
		}
		return returned + " " + operation + "(" + String.join(", ", parameters) + ")"
				+ (thrown == null ? "" : " throws " + thrown) + ";";
	}

	@Test
	void everySignatureIsTheOneTheTableDescribes() throws IOException {
		Map<String, List<Pin>> operations = new LinkedHashMap<>();
		for (Pin pin : pins()) {
			operations.computeIfAbsent(pin.contract() + "#" + pin.operation(), key -> new ArrayList<>()).add(pin);
		}
		assertThat(operations).as("the table is read at all").isNotEmpty();

		for (Map.Entry<String, List<Pin>> entry : operations.entrySet()) {
			String contract = entry.getValue().get(0).contract();
			String operation = entry.getValue().get(0).operation();
			String source = Files.readString(GENERATED.resolve(contract + ".java"), StandardCharsets.UTF_8);
			assertThat(source)
					.as("%s.%s renders as type-pins.tsv says", contract, operation)
					.contains(signature(operation, entry.getValue()));
		}
	}

	@Test
	void anOptionalPrimitiveIsBoxed() throws IOException {
		// The table's optional column is what makes Integer and int
		// different rows; a Java primitive cannot say "left out".
		for (Pin pin : pins()) {
			if (pin.optional()) {
				assertThat(pin.java()).as("%s.%s.%s", pin.contract(), pin.operation(), pin.slot())
						.isNotIn("int", "long", "short", "byte", "char", "float", "double", "boolean");
			}
		}
	}
}
