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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The messaging links of a node, as a runtime service collects them
 * (#297): bound dynamically, every change counted, so the runtime
 * service's {@code service.changecount} moves when a link connects,
 * loses its broker or gives up.
 */
public final class LinkStates {

	private final List<LinkState> links = new CopyOnWriteArrayList<>();

	private final AtomicLong changes = new AtomicLong();

	/** A link came. */
	public void added(LinkState link) {
		links.add(link);
		changes.incrementAndGet();
	}

	/** A link changed its state. */
	public void updated(LinkState link) {
		changes.incrementAndGet();
	}

	/** A link went. */
	public void removed(LinkState link) {
		links.remove(link);
		changes.incrementAndGet();
	}

	/** How often the links changed — added to the runtime service's own count. */
	public long changes() {
		return changes.get();
	}

	/** Every link as it stands now. */
	public List<LinkDTO> snapshot() {
		List<LinkDTO> all = new ArrayList<>();
		for (LinkState link : links) {
			all.add(link.link());
		}
		return all;
	}
}
