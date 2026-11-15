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

package org.eclipse.fennec.services.runtime;

/**
 * A messaging link of this node, registered as a service for as long as
 * it is open (#297). Both runtime services list every one; the link
 * changes {@link #STATE} whenever its state does, so whoever tracks it is
 * told.
 */
public interface LinkState {

	/** The service property that carries the link's state. */
	String STATE = "fennec.link.state";

	/** The link as it stands now. */
	LinkDTO link();
}
