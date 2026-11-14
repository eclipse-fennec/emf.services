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

package org.eclipse.fennec.services.derive;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Says that a method returning a stream returns one every receiver shares
 * (#252): its contract slot is a {@code Delivery.BROADCAST} instead of a
 * {@code STREAM}. Such a method takes no arguments — it is subscribed to,
 * not called — and the service is asked for the stream once, whoever
 * receives it.
 *
 * <p>{@link #overflow} is what a receiver may lose when it falls behind,
 * the contract's {@code OverflowPolicy}: {@code FAIL} (nothing, it fails
 * and starts over), {@code DISCARD_OLDEST} (latest wins) or {@code BLOCK}.
 * A string, so that an interface needs nothing but this package.
 *
 * <p>Read by {@link JavaContracts}; the generated interface of a contract
 * carries it as well.
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Broadcast {

	/** {@code FAIL}, {@code DISCARD_OLDEST} or {@code BLOCK}. */
	String overflow() default "FAIL";
}
