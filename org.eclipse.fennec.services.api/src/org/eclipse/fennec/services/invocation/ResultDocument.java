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
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.InternalEObject;

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
		return result == null ? new ArrayList<>() : rootsOfAll(List.of(result));
	}

	/**
	 * The roots of the document for a many-valued answer (#302): the
	 * values in their order, followed by whatever any of them needs in
	 * order to be readable, each object once.
	 *
	 * <p>A reader of such an answer takes every root of the return type
	 * as the values, in document order; the roots of other types are
	 * context, as they are for a single value.
	 *
	 * @param values the values an operation returned
	 */
	public static List<EObject> rootsOfAll(Collection<? extends EObject> values) {
		List<EObject> roots = new ArrayList<>();
		Set<EObject> known = Collections.newSetFromMap(new IdentityHashMap<>());
		Deque<EObject> pending = new ArrayDeque<>();
		for (EObject value : values) {
			if (value != null && known.add(value)) {
				roots.add(value);
				pending.add(value);
			}
		}
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
	 * The values of a many-valued answer, read from its document (#302):
	 * every root of the return type, in document order. The other roots
	 * are context.
	 *
	 * <p>The type is compared by namespace and name, not by identity: a
	 * contract names the return type as a cross-document href that a
	 * reader need not resolve, and an answer's classes come from whatever
	 * package the reader registered. A root whose class is the type or
	 * inherits from it is a value.
	 *
	 * @param roots the roots of the answer's document
	 * @param type  the return value's {@code eType}; without one, every
	 *              root is a value
	 */
	public static List<EObject> values(List<EObject> roots, EClassifier type) {
		if (type == null) {
			return List.copyOf(roots);
		}
		String wanted = typeName(type);
		List<EObject> values = new ArrayList<>();
		for (EObject root : roots) {
			if (isA(root.eClass(), wanted)) {
				values.add(root);
			}
		}
		return values;
	}

	private static boolean isA(EClass eClass, String wanted) {
		if (wanted.equals(typeName(eClass))) {
			return true;
		}
		for (EClass superType : eClass.getEAllSuperTypes()) {
			if (wanted.equals(typeName(superType))) {
				return true;
			}
		}
		return false;
	}

	/** {@code <nsURI>#//<Name>}, from a proxy's URI or from the classifier itself. */
	private static String typeName(EClassifier type) {
		if (type.eIsProxy()) {
			return ((InternalEObject) type).eProxyURI().toString();
		}
		EPackage ePackage = type.getEPackage();
		return (ePackage == null ? "" : ePackage.getNsURI()) + "#//" + type.getName();
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
