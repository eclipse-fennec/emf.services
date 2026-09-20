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

package org.eclipse.fennec.services.cloudevents;

/**
 * A message did not carry a readable CloudEvents envelope.
 *
 * <p>Unchecked on purpose: every caller sits on a transport path that
 * already has a way to report a broken message — a status code, a reply
 * with a diagnostic, a log line and a dropped event — and none of them
 * gains anything from a checked exception on the way there.
 */
public class CloudEventException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CloudEventException(String message) {
		super(message);
	}

	public CloudEventException(String message, Throwable cause) {
		super(message, cause);
	}
}
