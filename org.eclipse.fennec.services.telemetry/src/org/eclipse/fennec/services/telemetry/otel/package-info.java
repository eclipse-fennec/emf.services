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

/**
 * OpenTelemetry behind the project's two telemetry seams.
 *
 * <p>Private on purpose: everything anyone else needs is in
 * {@code org.eclipse.fennec.services.telemetry} and
 * {@code org.eclipse.fennec.services.runtime}, both in the api bundle.
 * Nothing should import this package, and a deployment that drops this
 * bundle should lose its telemetry and nothing else.
 */
package org.eclipse.fennec.services.telemetry.otel;
