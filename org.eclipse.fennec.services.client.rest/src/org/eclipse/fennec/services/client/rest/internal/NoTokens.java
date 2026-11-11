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

package org.eclipse.fennec.services.client.rest.internal;

import org.eclipse.fennec.services.common.TokenSupplier;
import org.osgi.service.component.annotations.Component;

/**
 * The token supplier of a node that shows no token.
 *
 * <p>There so that "this node has no token" is something a
 * configuration says — {@code (fennec.tokens=none)} as the target of
 * the transport's reference — rather than something that follows from a
 * service being absent. A reference that may be absent cannot tell "not
 * configured" from "not started yet", and a node that calls in between
 * calls without the token it was configured to show.
 */
@Component(property = TokenSupplier.KIND + "=" + TokenSupplier.KIND_NONE)
public final class NoTokens implements TokenSupplier {

	@Override
	public String token() {
		return null;
	}
}
