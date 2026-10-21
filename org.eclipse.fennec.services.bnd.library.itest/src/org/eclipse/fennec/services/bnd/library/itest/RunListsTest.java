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
package org.eclipse.fennec.services.bnd.library.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * The run lists of the project libraries, held to what a launch of them
 * gets: every bundle they name is installed and resolves, in one
 * framework with the container a downstream launch brings itself.
 */
class RunListsTest {

	private static final Pattern ENTRY = Pattern.compile("^\\s*([A-Za-z0-9_.\\-]+);version=", Pattern.MULTILINE);

	private static final Pattern RANGE = Pattern.compile("^fennec\\.services\\.range:\\s*(\\S+)\\s*$", Pattern.MULTILINE);

	private final BundleContext context = FrameworkUtil.getBundle(RunListsTest.class).getBundleContext();

	@ParameterizedTest
	@ValueSource(strings = { "client", "rsa", "security" })
	void everyBundleOfTheListIsThereAndResolves(String role) throws IOException {
		Map<String, Bundle> installed = Arrays.stream(context.getBundles())
				.collect(Collectors.toMap(Bundle::getSymbolicName, Function.identity(), (a, b) -> a));

		List<String> named = names(read("library/" + role + "/bndrun.bnd"));
		assertThat(named).as("the %s list names bundles", role).isNotEmpty();
		for (String symbolicName : named) {
			Bundle bundle = installed.get(symbolicName);
			assertThat(bundle).as("%s from the %s list is installed", symbolicName, role).isNotNull();
			assertThat(bundle.getState()).as("%s from the %s list resolves", symbolicName, role)
					.isIn(Bundle.RESOLVED, Bundle.STARTING, Bundle.ACTIVE);
		}
	}

	@Test
	void nothingInTheFrameworkIsLeftUnresolved() {
		List<String> unresolved = new ArrayList<>();
		for (Bundle bundle : context.getBundles()) {
			if (bundle.getState() == Bundle.INSTALLED) {
				unresolved.add(bundle.getSymbolicName());
			}
		}
		assertThat(unresolved).as("bundles that did not resolve").isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "client/project.bnd", "client/bndrun.bnd", "rsa/project.bnd", "rsa/bndrun.bnd",
			"security/project.bnd", "security/bndrun.bnd" })
	void theRangeIsTheReleaseLine(String file) throws IOException {
		Matcher base = Pattern.compile("^base-version:\\s*(\\d+)\\.(\\d+)\\.(\\d+)\\s*$", Pattern.MULTILINE)
				.matcher(read("fennec.bnd"));
		assertThat(base.find()).as("base-version in cnf/ext/fennec.bnd").isTrue();
		String expected = "[" + base.group(1) + "." + base.group(2) + "." + base.group(3) + "," + base.group(1) + "."
				+ (Integer.parseInt(base.group(2)) + 1) + ".0)";

		Matcher range = RANGE.matcher(read("library/" + file));
		assertThat(range.find()).as("%s states fennec.services.range", file).isTrue();
		assertThat(range.group(1)).as("%s follows base-version", file).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "client", "rsa", "security" })
	void theWorkspaceLibraryIndexesEveryBundleOfOurs(String role) throws IOException {
		// A downstream workspace resolves the lists from that index; a bundle
		// of ours missing there is a launch that fails to find it.
		String index = read("template/fennecServices.maven");
		for (String symbolicName : names(read("library/" + role + "/bndrun.bnd"))) {
			if (symbolicName.startsWith("org.eclipse.fennec.services.")) {
				assertThat(index).as("%s is in the fennecServices index", symbolicName)
						.contains(":" + symbolicName + ":");
			}
		}
		assertThat(index).as("the project libraries resolve from the same index")
				.contains(":org.eclipse.fennec.services.bnd.library.project:");
	}

	private static List<String> names(String bnd) {
		List<String> names = new ArrayList<>();
		Matcher matcher = ENTRY.matcher(bnd);
		while (matcher.find()) {
			names.add(matcher.group(1));
		}
		return names;
	}

	private String read(String path) throws IOException {
		URL entry = context.getBundle().getEntry(path);
		assertThat(entry).as("%s is packaged with the test", path).isNotNull();
		try (InputStream in = entry.openStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
