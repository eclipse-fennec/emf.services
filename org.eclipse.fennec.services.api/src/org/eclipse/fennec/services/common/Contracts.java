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

package org.eclipse.fennec.services.common;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;

/**
 * Small questions about a contract that both sides of the wire ask.
 */
public final class Contracts {

	private Contracts() {
	}

	/**
	 * The names of the contracts an implementation serves, in the order
	 * it names them.
	 *
	 * <p>Asked on the broker side to route an event to the right
	 * subscribers, and on the consumer side to remember what a reference
	 * was about — the same six lines, written twice, until this existed.
	 *
	 * @param implementation may be {@code null}, which is the normal
	 *        state of a withdrawn registration
	 * @return the names, never {@code null}; an unnamed interface is
	 *         skipped rather than represented by a null entry
	 */
	public static Set<String> interfaceNamesOf(ServiceImplementation implementation) {
		Set<String> names = new LinkedHashSet<>();
		if (implementation == null) {
			return names;
		}
		for (ServiceInterface si : implementation.getServiceInterfaces()) {
			if (si.getName() != null) {
				names.add(si.getName());
			}
		}
		return names;
	}
}
