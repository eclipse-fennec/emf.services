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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The method a dispatcher calls for an operation: by name and by how many
 * arguments the contract declares (#266).
 *
 * <p>Looked up on a public interface of the service first. A service is
 * often an object of a class nobody outside may see — a private nested
 * class, a lambda, a generated proxy — that implements the public
 * interface of its contract; the method of its class cannot be called
 * from here ({@code IllegalAccessException}), the same method declared by
 * the interface can. Only where no public interface declares it is the
 * class's own public method taken — a public class's first, then any, which
 * a caller in the same package may still call.
 */
public final class ServiceMethods {

	private ServiceMethods() {
	}

	/**
	 * @return the method to call on {@code service}, or empty when nothing
	 *         callable answers to the name with that many arguments
	 */
	public static Optional<Method> find(Object service, String name, int arity) {
		Class<?> type = service.getClass();
		for (Class<?> contract : publicInterfaces(type)) {
			for (Method method : contract.getMethods()) {
				if (matches(method, name, arity)) {
					return Optional.of(method);
				}
			}
		}
		Method hidden = null;
		for (Method method : type.getMethods()) {
			if (matches(method, name, arity)) {
				if (Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
					return Optional.of(method);
				}
				hidden = hidden == null ? method : hidden;
			}
		}
		return Optional.ofNullable(hidden);
	}

	/** The same, failing with what was looked for. */
	public static Method require(Object service, String name, int arity) throws NoSuchMethodException {
		return find(service, name, arity).orElseThrow(() -> new NoSuchMethodException("the service behind this"
				+ " contract, a " + service.getClass().getName() + ", has no method '" + name + "' taking " + arity
				+ " argument(s)"));
	}

	/**
	 * Calls an operation on a service that answers by name (#301). A failure
	 * of the call arrives as the {@link InvocationTargetException} a method
	 * throws, so a caller handles it as one.
	 *
	 * <p>Only the handler's way: a method is still invoked by the caller
	 * itself, because {@link Method#invoke} checks access from the class that
	 * calls it, and an implementation may be a class only its own package sees.
	 *
	 * @param handler the implementation
	 * @param operation the operation's name
	 * @param arguments by parameter name, in the contract's order
	 * @return what the operation answers
	 * @throws InvocationTargetException when the call failed
	 */
	public static Object call(OperationHandler handler, String operation, Map<String, Object> arguments)
			throws InvocationTargetException {
		try {
			return handler.invoke(operation, arguments);
		} catch (Exception failure) {
			throw new InvocationTargetException(failure, failure.getMessage());
		}
	}

	private static boolean matches(Method method, String name, int arity) {
		return method.getName().equals(name) && method.getParameterCount() == arity;
	}

	/** Every public interface the type implements, its own first, then those of its superclasses. */
	private static Set<Class<?>> publicInterfaces(Class<?> type) {
		Set<Class<?>> found = new LinkedHashSet<>();
		Deque<Class<?>> pending = new ArrayDeque<>();
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			pending.addAll(List.of(current.getInterfaces()));
		}
		while (!pending.isEmpty()) {
			Class<?> next = pending.poll();
			if (found.contains(next)) {
				continue;
			}
			if (Modifier.isPublic(next.getModifiers())) {
				found.add(next);
			}
			pending.addAll(List.of(next.getInterfaces()));
		}
		return found;
	}
}
