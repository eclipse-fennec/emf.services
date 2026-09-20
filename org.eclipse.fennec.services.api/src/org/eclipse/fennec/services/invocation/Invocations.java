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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.Argument;
import org.eclipse.fennec.services.BoolProperty;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.DoubleProperty;
import org.eclipse.fennec.services.EObjectProperty;
import org.eclipse.fennec.services.FloatProperty;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.LongProperty;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ShortProperty;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;

/**
 * A call as a message, and its answer — {@link ServiceInvocation} and
 * {@link ServiceInvocationResult}.
 *
 * <p>What the REST flavor spreads over path, query, header and body has
 * to have one form where a transport carries nothing but messages, and
 * the model has said what that form is since it was written. This class
 * is the Java side of filling it in; the TypeScript mirror is
 * {@code invocation.ts} in {@code @ddsr/client}, and the two produce the
 * same document because a call may cross languages.
 *
 * <p>Two things a call deliberately does <em>not</em> carry, by the
 * model's own documentation: the correlation and the reply address.
 * Those belong to the envelope, and since #101 there is one.
 *
 * <p><strong>The call description travels with the call.</strong> A
 * {@code ServiceInvocation} points at its operation and each
 * {@link Argument} at its parameter — by reference and not by name,
 * because a name is a convention two ends can read differently. So
 * {@link #roots(ServiceInvocation)} hands a writer a second root that
 * describes the operation, and the references resolve inside the
 * document. Self-contained, like everything else on this wire.
 *
 * <p>Nothing here serialises. Which encoding a call travels in is the
 * contract's choice (#100) and therefore the codec's business, not
 * this class's.
 */
public final class Invocations {

	private Invocations() {
	}

	private static final ServicesFactory FACTORY = ServicesFactory.eINSTANCE;

	/**
	 * A call to {@code operation} with these arguments, ready to be
	 * written.
	 *
	 * <p>The operation is described rather than referenced: a copy of it
	 * carrying the parameters this call fills. A copy because putting a
	 * contained object into a resource takes it OUT of its container —
	 * a caller that serialised its own contract's operation would lose
	 * it from the contract.
	 *
	 * @param operation the operation being called
	 * @param arguments the values, by parameter name; a name the
	 *                  operation does not declare is still sent, because
	 *                  refusing it here would hide the mistake rather
	 *                  than report it
	 */
	public static ServiceInvocation invocation(ServiceOperation operation, Map<String, ?> arguments) {
		if (operation == null) {
			throw new IllegalArgumentException("a call needs the operation it calls");
		}
		ServiceOperation description = describe(operation, arguments);
		ServiceInvocation invocation = FACTORY.createServiceInvocation();
		invocation.setOperation(description);
		for (Parameter parameter : description.getParameters()) {
			Argument argument = FACTORY.createArgument();
			argument.setParameter(parameter);
			argument.setValue(property(parameter.getName(),
					arguments == null ? null : arguments.get(parameter.getName()), parameter.getType()));
			invocation.getArguments().add(argument);
		}
		return invocation;
	}

	/**
	 * The roots of the document for one call: the invocation and the
	 * description of the operation it names, in that order.
	 */
	public static List<EObject> roots(ServiceInvocation invocation) {
		List<EObject> roots = new ArrayList<>();
		roots.add(invocation);
		if (invocation.getOperation() != null && invocation.getOperation().eContainer() == null) {
			roots.add(invocation.getOperation());
		}
		return roots;
	}

	/** Which operation a call names, or {@code null} when it names none. */
	public static String operationNameOf(ServiceInvocation invocation) {
		return invocation == null || invocation.getOperation() == null
				? null
				: invocation.getOperation().getName();
	}

	/** The values of a call, by the names of the parameters they fill. */
	public static Map<String, Object> argumentsOf(ServiceInvocation invocation) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		if (invocation == null) {
			return arguments;
		}
		for (Argument argument : invocation.getArguments()) {
			Parameter parameter = argument.getParameter();
			if (parameter != null && parameter.getName() != null) {
				arguments.put(parameter.getName(), value(argument.getValue()));
			}
		}
		return arguments;
	}

	/** An answer that has a value, or none for an operation that returns nothing. */
	public static ServiceInvocationResult result(Object value) {
		ServiceInvocationResult result = FACTORY.createServiceInvocationResult();
		if (value != null) {
			result.setValue(property("result", value, null));
		}
		return result;
	}

	/**
	 * An answer that failed.
	 *
	 * <p>A {@link Diagnostic} because that is how this registry reports
	 * failures everywhere — a transport turns it into a status where it
	 * has one, and a message-only transport has none.
	 */
	public static ServiceInvocationResult failure(String message, int code) {
		ServiceInvocationResult result = FACTORY.createServiceInvocationResult();
		Diagnostic diagnostic = FACTORY.createDiagnostic();
		diagnostic.setSeverity(DiagnosticSeverity.ERROR);
		diagnostic.setMessage(message);
		diagnostic.setCode(code);
		result.setDiagnostic(diagnostic);
		return result;
	}

	/** The value an answer carries, or {@code null} for none and for a failure. */
	public static Object valueOf(ServiceInvocationResult result) {
		return result == null || result.getDiagnostic() != null ? null : value(result.getValue());
	}

	/**
	 * The value, in the Property that fits it.
	 *
	 * <p>The parameter's declared type decides where it can: a {@code 1}
	 * sent for an {@code int} parameter must not arrive as a double,
	 * which is exactly the kind of quiet difference a JSON envelope
	 * could not express. Where the contract says nothing, the Java value
	 * decides.
	 *
	 * @param declaredType the model's type name, or {@code null}
	 */
	public static Property property(String name, Object value, String declaredType) {
		Property property = typed(value, declaredType);
		if (property != null) {
			property.setName(name);
		}
		return property;
	}

	/** The value a Property carries, models included. */
	public static Object value(Property property) {
		if (property == null) {
			return null;
		}
		if (property instanceof StringProperty p) {
			return p.getValue();
		}
		if (property instanceof IntProperty p) {
			return p.getValue();
		}
		if (property instanceof LongProperty p) {
			return p.getValue();
		}
		if (property instanceof DoubleProperty p) {
			return p.getValue();
		}
		if (property instanceof FloatProperty p) {
			return p.getValue();
		}
		if (property instanceof ShortProperty p) {
			return p.getValue();
		}
		if (property instanceof BoolProperty p) {
			return p.isValue();
		}
		if (property instanceof StringListProperty p) {
			return p.getValue().toArray(String[]::new);
		}
		if (property instanceof EObjectProperty p) {
			return p.getValue();
		}
		return null;
	}

	private static ServiceOperation describe(ServiceOperation operation, Map<String, ?> arguments) {
		ServiceOperation description = FACTORY.createServiceOperation();
		description.setName(operation.getName());
		int index = 0;
		for (String name : arguments == null ? List.<String>of() : List.copyOf(arguments.keySet())) {
			Parameter declared = declaredParameter(operation, name);
			Parameter parameter = FACTORY.createParameter();
			parameter.setName(name);
			parameter.setIndex(declared != null ? declared.getIndex() : index);
			if (declared != null && declared.getType() != null) {
				parameter.setType(declared.getType());
			}
			description.getParameters().add(parameter);
			index++;
		}
		return description;
	}

	private static Parameter declaredParameter(ServiceOperation operation, String name) {
		for (Parameter parameter : operation.getParameters()) {
			if (name.equals(parameter.getName())) {
				return parameter;
			}
		}
		return null;
	}

	private static Property typed(Object value, String declaredType) {
		if (value instanceof EObject model) {
			EObjectProperty property = FACTORY.createEObjectProperty();
			property.setValue(model);
			return property;
		}
		if (value == null) {
			return null;
		}
		String declared = declaredType == null ? "" : declaredType.toLowerCase();
		switch (declared) {
		case "int", "integer":
			return intProperty(asNumber(value).intValue());
		case "long":
			return longProperty(asNumber(value).longValue());
		case "short":
			return shortProperty(asNumber(value).shortValue());
		case "float":
			return floatProperty(asNumber(value).floatValue());
		case "double":
			return doubleProperty(asNumber(value).doubleValue());
		case "boolean", "bool":
			return boolProperty(value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString()));
		case "string":
			return stringProperty(value.toString());
		default:
			break;
		}
		if (value instanceof Integer i) {
			return intProperty(i);
		}
		if (value instanceof Long l) {
			return longProperty(l);
		}
		if (value instanceof Short s) {
			return shortProperty(s);
		}
		if (value instanceof Float f) {
			return floatProperty(f);
		}
		if (value instanceof Double d) {
			return doubleProperty(d);
		}
		if (value instanceof Boolean b) {
			return boolProperty(b);
		}
		if (value instanceof String[] strings) {
			StringListProperty property = FACTORY.createStringListProperty();
			for (String entry : strings) {
				property.getValue().add(entry);
			}
			return property;
		}
		return stringProperty(value.toString());
	}

	private static Number asNumber(Object value) {
		return value instanceof Number number ? number : Double.valueOf(value.toString());
	}

	private static Property intProperty(int value) {
		IntProperty property = FACTORY.createIntProperty();
		property.setValue(value);
		return property;
	}

	private static Property longProperty(long value) {
		LongProperty property = FACTORY.createLongProperty();
		property.setValue(value);
		return property;
	}

	private static Property shortProperty(short value) {
		ShortProperty property = FACTORY.createShortProperty();
		property.setValue(value);
		return property;
	}

	private static Property floatProperty(float value) {
		FloatProperty property = FACTORY.createFloatProperty();
		property.setValue(value);
		return property;
	}

	private static Property doubleProperty(double value) {
		DoubleProperty property = FACTORY.createDoubleProperty();
		property.setValue(value);
		return property;
	}

	private static Property boolProperty(boolean value) {
		BoolProperty property = FACTORY.createBoolProperty();
		property.setValue(value);
		return property;
	}

	private static Property stringProperty(String value) {
		StringProperty property = FACTORY.createStringProperty();
		property.setValue(value);
		return property;
	}
}
