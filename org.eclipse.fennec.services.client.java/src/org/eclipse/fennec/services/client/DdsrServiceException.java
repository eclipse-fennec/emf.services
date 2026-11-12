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

package org.eclipse.fennec.services.client;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceException;

/**
 * The provider answered, and the answer is one of the errors the
 * contract declares.
 *
 * <p>{@link #declared()} is that {@link ServiceException}. Over REST it
 * was recognised by the status the flavor binds it to, over a messaging
 * broker by the code of the Diagnostic the answer carries — the same
 * error either way, which is why one class reports both. Not a
 * transport failure, so nothing retries it: calling again would get the
 * same answer.
 *
 * <p>A proxy built over a Java interface turns it into the exception
 * class the method declares for that error, where it declares one, so
 * a caller writes {@code catch (PersonNotFoundException e)}; this one
 * is then its cause. The twin of {@code DdsrServiceError} in the
 * TypeScript client.
 */
public class DdsrServiceException extends DdsrException {

	private static final long serialVersionUID = 1L;

	private final transient ServiceException declared;
	private final int status;

	/**
	 * @param message  what the provider said
	 * @param declared the error of the contract this is
	 * @param status   the HTTP status it travelled as, or 0 over a
	 *                 transport that has none
	 */
	public DdsrServiceException(String message, ServiceException declared, int status) {
		super(message);
		this.declared = declared;
		this.status = status;
	}

	/** Recognised in the Diagnostic an answer carried. */
	public DdsrServiceException(String message, ServiceException declared, Diagnostic diagnostic) {
		super(message, diagnostic);
		this.declared = declared;
		this.status = 0;
	}

	/** The error of the contract this is. */
	public ServiceException declared() {
		return declared;
	}

	/** The HTTP status it travelled as, or 0 over a transport that has none. */
	public int status() {
		return status;
	}
}
