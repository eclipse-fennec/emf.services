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
package org.eclipse.fennec.services.broker.rest.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationBase;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;

import jakarta.ws.rs.core.Application;

/**
 * The broker's replacement for the whiteboard's default application:
 * where the hand-written resources — events, consumers, registry —
 * live, now with an authentication mode of its own (SECURITY.md §7a).
 *
 * <p>A replacement rather than a named application of its own, because
 * the default application is where a plain {@code @JakartarsResource}
 * lands and where the server-span filter of telemetry.rest looks for
 * it. Taking its name keeps both, and every path stays where it was.
 *
 * <p>The class says nothing about authentication; the configuration
 * does. Declarative Services copies a component's configuration onto
 * the service it registers, so {@code fennec.auth} and the extension
 * selector that requires the matching filter arrive as properties of
 * this application — from the same placeholder as the three generic
 * broker contracts, so the four cannot disagree.
 *
 * <p>Required configuration: without one there is no application, and
 * the resources, which require an authentication filter, are not
 * served at all. If this application cannot be deployed — mode bearer
 * on a node without a verifier — the whiteboard falls back to its
 * built-in default, which states no mode, and the resources stay down
 * there too. Fail closed both ways.
 */
@Component(service = Application.class,
		configurationPid = "org.eclipse.fennec.services.broker.rest.application",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@JakartarsApplicationBase("/")
@JakartarsName(".default")
public class BrokerApplication extends Application {
}
