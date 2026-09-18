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

package org.eclipse.fennec.services.rsa.distribution.rest;

import java.util.Locale;

import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;

/**
 * The REST flavor a derived contract gets.
 *
 * <p>A hand-written flavor says things the model cannot know: that a
 * lookup is a GET, that an identifier belongs in the path, that a name
 * on the wire differs from the parameter's. A derived one knows none of
 * that — a Java interface does not say which of its methods are safe to
 * repeat, or which argument is an identity. So this picks the shape that
 * is correct without knowing:
 *
 * <ul>
 * <li><strong>POST for everything.</strong> Not because every operation
 *     changes something, but because nothing here can tell which do, and
 *     announcing a GET for an operation that charges an account is a lie
 *     a cache will act on.</li>
 * <li><strong>One path per operation</strong>, its name — which is also
 *     why the derivation refuses overloads.</li>
 * <li><strong>Scalars in the query, a model in the body.</strong> A
 *     modelled slot is a document; the rest fits in a URL.</li>
 * </ul>
 *
 * <p>A provider that wants a better shape writes the flavor instead of
 * deriving it. Same trade as generated versus generic serving (#84):
 * derivation carries the traffic, hand-written says more.
 */
final class RestFlavors {

	/** What a modelled value travels as. */
	private static final String XMI = "application/xml";

	/** What everything else travels as. */
	private static final String TEXT = "text/plain";

	private RestFlavors() {
	}

	/** Whether a slot carries a model rather than a scalar. */
	private static boolean isModel(Parameter slot) {
		return slot.getEType() != null;
	}

	/** A flavor for {@code contract}, mounted under {@code basePath}. */
	static RestFlavor flavorFor(ServiceInterface contract, String basePath) {
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(contract.getName() + "-rest");
		flavor.setBasePath(basePath);
		flavor.getContentTypes().add("application/xml");
		for (ServiceOperation operation : contract.getOperations()) {
			flavor.getOperationFlavors().add(operationFlavor(operation));
		}
		return flavor;
	}

	/** Where a contract is served when nothing says otherwise. */
	static String basePathFor(ServiceInterface contract) {
		return "/" + contract.getName().toLowerCase(Locale.ROOT);
	}

	private static RestOperationFlavor operationFlavor(ServiceOperation operation) {
		RestOperationFlavor operationFlavor = ServicesFactory.eINSTANCE.createRestOperationFlavor();
		operationFlavor.setName(operation.getName());
		operationFlavor.setOperation(operation);
		operationFlavor.setMethod(HttpMethod.POST);
		operationFlavor.setPath("/" + operation.getName());
		// What comes back decides the media type. A model is XMI; a
		// scalar is text, and saying XML about an integer is not a
		// harmless inaccuracy — there is no writer for it, so the call
		// fails with a 500 that says nothing.
		if (operation.getReturnValue() != null) {
			operationFlavor.getProduces().add(isModel(operation.getReturnValue()) ? XMI : TEXT);
		}

		boolean bodyTaken = false;
		for (Parameter parameter : operation.getParameters()) {
			RestParameterBinding binding = ServicesFactory.eINSTANCE.createRestParameterBinding();
			binding.setParameter(parameter);
			if (!isModel(parameter)) {
				binding.setBinding(ParameterBinding.QUERY);
			} else if (!bodyTaken) {
				binding.setBinding(ParameterBinding.BODY);
				operationFlavor.getConsumes().add(XMI);
				bodyTaken = true;
			} else {
				// A request has one body. Saying so is better than
				// putting a document into a query string.
				throw new IllegalArgumentException(operation.getName()
						+ " takes more than one modelled argument, and a derived REST flavor has one body to"
						+ " put them in. Write the flavor by hand, or take a single document.");
			}
			operationFlavor.getParameterBindings().add(binding);
		}
		return operationFlavor;
	}
}
