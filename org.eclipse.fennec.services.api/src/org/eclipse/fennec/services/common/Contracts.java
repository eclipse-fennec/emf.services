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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.fennec.services.Delivery;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;

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

	/**
	 * The slots of a contract that are streams, as
	 * {@code <operation>.<slot>} — for whoever has to say that it cannot
	 * carry one.
	 *
	 * <p>A stream is a statement of the contract ({@code Delivery.STREAM}),
	 * and a transport either carries it or says that it does not. One
	 * that served such a contract as if the slot were a value would hand
	 * a caller a stream object where it expects elements, and fail on
	 * the first call instead of at the start.
	 *
	 * @return the stream slots in declared order, empty when the
	 *         contract has none
	 */
	public static List<String> streamSlots(ServiceInterface contract) {
		List<String> streams = new ArrayList<>();
		if (contract == null) {
			return streams;
		}
		for (ServiceOperation operation : contract.getOperations()) {
			for (Parameter parameter : operation.getParameters()) {
				if (parameter.getDelivery() == Delivery.STREAM) {
					streams.add(operation.getName() + "." + parameter.getName());
				}
			}
			Parameter returned = operation.getReturnValue();
			if (returned != null && returned.getDelivery() == Delivery.STREAM) {
				streams.add(operation.getName() + ".return");
			}
		}
		return streams;
	}

	/**
	 * Refuses a contract whose operations TAKE a stream, on behalf of a
	 * transport that carries a stream back to the caller and none the
	 * other way: a stream as an argument is many requests to one
	 * responder, which is another exchange than many answers to one
	 * request.
	 *
	 * @param carrier who is refusing, for the message
	 * @throws IllegalArgumentException when an argument is a stream
	 */
	public static void requireNoStreamArguments(ServiceInterface contract, String carrier) {
		List<String> handedOver = streamSlots(contract).stream().filter(slot -> !slot.endsWith(".return")).toList();
		if (!handedOver.isEmpty()) {
			throw new IllegalArgumentException((contract.getName() == null ? "the contract" : contract.getName())
					+ " takes streams as arguments " + handedOver + ", and " + carrier
					+ " carries a stream to the caller only");
		}
	}

	/**
	 * Refuses a contract with stream slots on behalf of a transport that
	 * does not carry streams — at the start, with the slots named.
	 *
	 * @param carrier who is refusing, for the message
	 * @throws IllegalArgumentException when the contract has a stream slot
	 */
	public static void requireNoStreams(ServiceInterface contract, String carrier) {
		List<String> streams = streamSlots(contract);
		if (!streams.isEmpty()) {
			throw new IllegalArgumentException((contract.getName() == null ? "the contract" : contract.getName())
					+ " has stream slots " + streams + ", and " + carrier + " does not carry streams yet");
		}
	}
}
