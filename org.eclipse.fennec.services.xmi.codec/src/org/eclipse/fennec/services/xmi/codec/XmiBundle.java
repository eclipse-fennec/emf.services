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

package org.eclipse.fennec.services.xmi.codec;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EObject;

/**
 * Holder for resource methods that need to return — or for clients
 * that need to read — a multi-root XMI document (e.g. an envelope
 * plus referenced ServiceInterfaces, so cross-references resolve
 * inside the document).
 *
 * <p>Roots are expected to be already detached or fresh copies — the
 * writer does not re-copy them, which means a Copier-built envelope
 * can keep its rewired cross-references intact.
 */
public final class XmiBundle {

	private final List<EObject> roots;

	public XmiBundle(EObject... roots) {
		this.roots = List.of(roots);
	}

	public XmiBundle(Collection<? extends EObject> roots) {
		this.roots = List.copyOf(roots);
	}

	public List<EObject> roots() {
		return roots;
	}
}
