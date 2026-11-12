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

package org.eclipse.fennec.services.messaging;

import java.util.Locale;

/**
 * How a channel is spelled on the broker a URL names.
 *
 * <p>Over a messaging broker everything is one transport; what the
 * protocol decides is the spelling. MQTT separates the levels of a
 * topic with {@code /}, AMQP 0-9-1 the words of a routing key with
 * {@code .} — and both take {@code #} for "everything below". An owner
 * that names its channels through this says them once and is
 * understood on either.
 *
 * <p>A name is given the way MQTT writes it, because that is the
 * spelling the frozen wire names have ({@code ddsr/events}); on AMQP
 * the same name is {@code ddsr.events}.
 *
 * @param protocol  {@code mqtt} or {@code amqp} — also what a service
 *                  that travels over this broker says its transport is
 * @param separator what separates the levels of a channel there
 */
public record Channels(String protocol, String separator) {

	/** MQTT, the spelling names are given in. */
	public static final Channels MQTT = new Channels("mqtt", "/");

	/** AMQP 0-9-1 on a topic exchange. */
	public static final Channels AMQP = new Channels("amqp", ".");

	/** The spelling on the broker this URL names, by its scheme. */
	public static Channels of(String brokerUrl) {
		String url = brokerUrl == null ? "" : brokerUrl.strip().toLowerCase(Locale.ROOT);
		return url.startsWith("amqp:") || url.startsWith("amqps:") ? AMQP : MQTT;
	}

	/** A name as this protocol spells it. */
	public String spell(String name) {
		return "/".equals(separator) ? name : name.replace("/", separator);
	}

	/** The channel one level below a prefix. */
	public String under(String prefix, String segment) {
		return spell(prefix) + separator + segment;
	}

	/** The filter that hears everything below a prefix. */
	public String everythingUnder(String prefix) {
		return spell(prefix) + separator + "#";
	}

	/** Whether a channel is the one named by this segment, directly below anything. */
	public boolean endsWith(String channel, String segment) {
		return channel != null && channel.endsWith(separator + segment);
	}
}
