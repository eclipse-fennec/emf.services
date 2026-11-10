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

// A link is a messaging instance, and an instance needs a binding to be
// one: said here, so a launch that is resolved gets the binding with
// the bundle that cannot do without it.
@Export
@Version("1.0.0")
@RequireMessaging(protocol = "mqtt3")
package org.eclipse.fennec.services.messaging;

import org.gecko.messaging.api.annotations.RequireMessaging;
import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
