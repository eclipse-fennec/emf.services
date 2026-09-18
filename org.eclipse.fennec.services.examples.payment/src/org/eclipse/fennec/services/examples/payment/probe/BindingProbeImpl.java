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

package org.eclipse.fennec.services.examples.payment.probe;

import org.osgi.service.component.annotations.Component;

/**
 * The one hand-written class of the BindingProbe: it answers with what it
 * received, and says nothing about where the values came from.
 * <p>
 * That is the point of the probe. Path, query and header are decided by the
 * published flavor and carried out by the generated resource; if a consumer
 * placed a value somewhere else, this method would see a null and the answer
 * would say so.
 */
@Component(service = BindingProbe.class)
public class BindingProbeImpl implements BindingProbe {

	@Override
	public String echo(String id, String currency, String tenant) {
		return id + "|" + currency + "|" + tenant;
	}
}
