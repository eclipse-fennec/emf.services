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

package org.eclipse.fennec.services.invocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

/**
 * What travels with an answer.
 *
 * <p>An operation's answer can need context that is not part of the
 * answer. {@code getServiceReferences} returns a registry envelope
 * whose implementations point at the contracts they serve; a document
 * carrying only the envelope leaves those pointing nowhere, and a
 * consumer reads a registration whose contract it cannot resolve. The
 * contract can say {@code returnValue : LocalServiceRegistry}. It
 * cannot say "and whatever else that value needs in order to be
 * readable" (#88).
 *
 * <p>So the wire says it instead, by a rule rather than by a
 * declaration:
 *
 * <blockquote>An answer travels with every object it references that
 * belongs to nobody — no container and no resource — and, from those,
 * with everything they reference on the same terms.</blockquote>
 *
 * <p>The two halves of that rule are what make it safe. <em>No
 * container</em> and <em>no resource</em> together mean the object is
 * free-floating: a copy made for this answer, which is exactly what the
 * lookup builds and exactly what would otherwise be lost. An object
 * that <em>is</em> contained or in a resource belongs to something that
 * did not ask to travel — a live catalog entry sits in the broker's
 * registry, so a request for one contract does not drag the whole
 * registry onto the wire, which is the greedy reading this rule was
 * written to avoid.
 *
 * <p>A proxy is never followed. A cross-document href is a deliberate
 * statement that the target lives elsewhere and is not meant to be
 * resolved here — the publish convention is built on it.
 */
public final class ResultDocument {

	private ResultDocument() {
	}

	/**
	 * The roots of the document for one answer: the answer itself,
	 * followed by whatever it needs in order to be readable.
	 *
	 * <p>Order is stable and meaningful: the answer is first, so a
	 * reader that wants only the value takes the first root — which is
	 * what both SDKs already do.
	 *
	 * @param result the value an operation returned
	 * @return one root for a value that needs no context, several when
	 *         it does
	 */
	public static List<EObject> roots(EObject result) {
		List<EObject> roots = new ArrayList<>();
		if (result == null) {
			return roots;
		}
		roots.add(result);
		Set<EObject> known = new LinkedHashSet<>();
		known.add(result);

		Deque<EObject> pending = new ArrayDeque<>();
		pending.add(result);
		while (!pending.isEmpty()) {
			EObject root = pending.poll();
			for (EObject referenced : referencedBy(root)) {
				if (!travelsWithTheAnswer(referenced) || !known.add(referenced)) {
					continue;
				}
				roots.add(referenced);
				pending.add(referenced);
			}
		}
		return roots;
	}

	/**
	 * Whether an answer needs more than itself.
	 *
	 * <p>Asked separately because the common case — a value that
	 * references nothing detached — should stay a single-root document,
	 * byte for byte what it was before this rule existed.
	 */
	public static boolean needsSiblings(EObject result) {
		return roots(result).size() > 1;
	}

	/** Everything one root and its contents point at, containment aside. */
	private static List<EObject> referencedBy(EObject root) {
		List<EObject> referenced = new ArrayList<>(root.eCrossReferences());
		root.eAllContents().forEachRemaining(contained -> referenced.addAll(contained.eCrossReferences()));
		return referenced;
	}

	/**
	 * Whether an object belongs to nobody, and therefore to this answer.
	 *
	 * <p>A proxy is excluded before the question is even asked: resolving
	 * it would fetch, and an unresolved one is a statement that the
	 * target lives elsewhere.
	 */
	private static boolean travelsWithTheAnswer(EObject referenced) {
		return referenced != null && !referenced.eIsProxy()
				&& referenced.eContainer() == null && referenced.eResource() == null;
	}
}
