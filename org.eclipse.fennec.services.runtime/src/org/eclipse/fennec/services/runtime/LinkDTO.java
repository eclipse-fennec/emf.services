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

import org.osgi.dto.DTO;

/**
 * One connection of this node to a messaging broker (#297), as a runtime
 * service shows it — so a health check sees a node that is cut off from
 * its broker instead of a node that quietly hears nothing.
 */
public class LinkDTO extends DTO {

	/** The messaging instance behind it, unique in the framework. */
	public String name;

	/** Where its broker is. */
	public String url;

	/**
	 * {@code connecting}, {@code connected}, {@code reconnecting} (lost,
	 * still trying), {@code gave-up} (it does not try again) or
	 * {@code closed}.
	 */
	public String state;

	/** Why it gave up, where it did; else {@code null}. */
	public String cause;
}
