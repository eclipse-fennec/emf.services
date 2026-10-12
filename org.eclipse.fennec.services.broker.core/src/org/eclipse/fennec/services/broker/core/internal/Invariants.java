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
package org.eclipse.fennec.services.broker.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.util.ServicesValidator;

/**
 * The validation gate: a document the broker is about to take in is
 * checked against the model's armed invariants first.
 *
 * <p>The invariants live in the ecore as OCL (issue #7) and the
 * generated {@link ServicesValidator} hands them to the registered
 * validation delegate. That delegate is the fennec m2x engine, which
 * the broker launches carry as a runrequire and a plain JUnit test
 * registers by hand. Without it EMF does not skip a constraint — it
 * reports every one of them as violated with the constraint's own
 * severity, which is ERROR. A gate that ran blindly would therefore
 * refuse every publish in any runtime that lacks the engine. So the
 * gate first asks whether the delegate is there, and when it is not,
 * it says so once at WARNING and lets the document through: the
 * structural checks the generated validator does on its own still run
 * on the way, only the OCL ones do not. That is an honest "not
 * checked", which is the most a broker without an engine can offer.
 *
 * <p>Only severity ERROR refuses. A WARNING from a constraint is
 * reported in the log and the document is taken.
 *
 * <p>One of EMF's own checks is left out: that every proxy resolves. A
 * contract names the type of a parameter with {@code eType}, and that
 * type lives in the payload model — which the broker does not have and
 * is not meant to have (#100: it carries documents, it does not
 * interpret payloads). Such a reference is a proxy on the broker by
 * design, not a defect of the document. What a proxy must NOT be is a
 * reference into the contract itself; those the broker rewires to its
 * catalog entry before asking, and the model's own invariants
 * ({@code bindingsReferenceOperationParameters},
 * {@code operationFlavorsCoverInterfaces}) still fail on a proxy left
 * there, because a proxy is never a member of the live contract.
 */
final class Invariants {

	private static final Logger LOG = Logger.getLogger(Invariants.class.getName());

	/** The delegate URI the ecore names — the m2x OCL engine. */
	static final String OCL_DELEGATE = "http://www.eclipse.org/fennec/m2x/ocl/1.0";

	private static final AtomicBoolean WARNED = new AtomicBoolean();

	private Invariants() {
		// no instances
	}

	/**
	 * Validates one document and returns the violations as one line, or
	 * empty when the document may be taken in.
	 *
	 * @param document the root to validate — the whole containment tree
	 *                 below it is walked
	 * @return the ERROR messages joined, or empty
	 */
	static Optional<String> violations(EObject document) {
		if (!armed()) {
			return Optional.empty();
		}
		// The generated package registers its validator on init(); a
		// package that arrived through the OSGi configurator may not
		// have gone through that path. Idempotent, same thing init()
		// would have done.
		EValidator.Registry.INSTANCE.putIfAbsent(ServicesPackage.eINSTANCE, ServicesValidator.INSTANCE);
		Diagnostic diagnostic = Diagnostician.INSTANCE.validate(document);
		List<String> errors = messages(diagnostic, Diagnostic.ERROR);
		if (errors.isEmpty()) {
			List<String> warnings = messages(diagnostic, Diagnostic.WARNING);
			if (!warnings.isEmpty()) {
				LOG.warning("[DDSR] document accepted with warnings: " + warnings);
			}
			return Optional.empty();
		}
		String violations = String.join("; ", errors);
		// A refusal is the broker telling a publisher that its document is
		// wrong. The caller gets it as a Diagnostic; the log keeps it for
		// whoever looks at the broker rather than at the publisher.
		LOG.warning("[DDSR] document refused by the model's invariants: " + violations);
		return Optional.of(violations);
	}

	private static boolean armed() {
		boolean present = EValidator.ValidationDelegate.Registry.INSTANCE.getValidationDelegate(OCL_DELEGATE) != null;
		if (!present && WARNED.compareAndSet(false, true)) {
			LOG.warning("[DDSR] no OCL validation delegate registered for " + OCL_DELEGATE
					+ " — the model's invariants are NOT checked on publish; add the m2x OCL engine to the launch");
		}
		return present;
	}

	/**
	 * The leaf messages of the given severity — the summary line of the
	 * root is noise, and the proxy check is not counted (see the class
	 * comment).
	 */
	private static List<String> messages(Diagnostic diagnostic, int severity) {
		List<String> out = new ArrayList<>();
		collect(diagnostic, severity, out);
		return out;
	}

	private static boolean isProxyCheck(Diagnostic diagnostic) {
		return EObjectValidator.DIAGNOSTIC_SOURCE.equals(diagnostic.getSource())
				&& diagnostic.getCode() == EObjectValidator.EOBJECT__EVERY_PROXY_RESOLVES;
	}

	private static void collect(Diagnostic diagnostic, int severity, List<String> out) {
		if (diagnostic.getChildren().isEmpty()) {
			if (diagnostic.getSeverity() == severity && diagnostic.getMessage() != null
					&& !isProxyCheck(diagnostic)) {
				out.add(diagnostic.getMessage());
			}
			return;
		}
		for (Diagnostic child : diagnostic.getChildren()) {
			collect(child, severity, out);
		}
	}
}
