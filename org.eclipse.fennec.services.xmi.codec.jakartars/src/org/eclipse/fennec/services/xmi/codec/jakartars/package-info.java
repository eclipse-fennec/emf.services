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
 * The XMI codec as Jakarta REST providers (#265): the message body
 * readers and writers a JAX-RS whiteboard picks up, or a Jakarta REST
 * client registers, and the mapping of the codec's refusals to HTTP
 * statuses. Apart from the codec so that a node that only speaks over a
 * messaging broker needs no JAX-RS whiteboard.
 */
@Export
@Version("1.0.0")
package org.eclipse.fennec.services.xmi.codec.jakartars;

import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
