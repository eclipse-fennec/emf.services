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

package org.eclipse.fennec.services.rsa.internal;

import java.util.Map;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.FlavorDistribution;
import org.osgi.service.component.annotations.Component;

/**
 * A node that exports nothing, said out loud.
 *
 * <p>An admin needs a distribution the way it needs a discovery: the
 * deployment's configuration names both, and a reference that names
 * something is mandatory. But a consumer-only node has no transport to
 * serve on and should not therefore be left without an admin — it still
 * has to import.
 *
 * <p>So it configures this one: {@code distribution.target} points at
 * {@code ddsr.rsa.flavor=none}, and the node says in its configuration
 * that it exports nothing, rather than leaving it to be inferred from a
 * missing service. Supporting no configuration type at all is what makes
 * {@code exportService} answer "not mine" for every request, which is
 * exactly what the specification asks of an admin that cannot serve.
 */
@Component(service = FlavorDistribution.class, property = "ddsr.rsa.flavor=none")
public class NothingIsExported implements FlavorDistribution {

	@Override
	public String[] supportedConfigs() {
		// None. Every export is therefore somebody else's.
		return new String[0];
	}

	@Override
	public FlavorKind flavor() {
		// There is no flavor of "nothing". Nobody asks: an export never
		// reaches a distribution that supports no configuration type.
		return null;
	}

	@Override
	public ExportedEndpoint export(Object service, ServiceInterface contract, Map<String, ?> properties) {
		throw new UnsupportedOperationException(
				"this node is configured not to export (distribution.target points at ddsr.rsa.flavor=none)");
	}
}
