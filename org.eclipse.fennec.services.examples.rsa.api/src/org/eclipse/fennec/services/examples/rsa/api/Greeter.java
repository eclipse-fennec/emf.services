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

package org.eclipse.fennec.services.examples.rsa.api;

/**
 * An ordinary Java interface.
 *
 * <p>Nothing about it knows this registry: no contract document beside
 * it, no annotations of ours, no model. That is the point — everything a
 * consumer needs to call it is read from this file and from the service
 * properties, and if that is not enough then the export path is not
 * finished.
 */
public interface Greeter {

	/**
	 * @param name who to greet
	 * @return the greeting
	 */
	String greet(String name);

	/** @return how many greetings have been handed out */
	int greeted();
}
