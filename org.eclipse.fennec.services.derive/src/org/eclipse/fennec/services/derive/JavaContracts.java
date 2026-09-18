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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;

/**
 * The contract of a plain Java interface.
 *
 * <p>An OSGi service is a Java interface and some properties. To publish
 * one into this registry it has to be said as a contract, and saying it
 * by hand next to the interface is the drift every part of this project
 * has been removing. So it is derived — which is what lets the RSA
 * facade export an ordinary service (#24), and what lets any provider
 * skip writing a contract document.
 *
 * <p><strong>The derived contract has to be the same every time.</strong>
 * Its fingerprint identifies it, and a consumer built against one
 * fingerprint does not accept another. Two things in reflection are not
 * stable on their own and are therefore decided here:
 *
 * <ul>
 * <li>{@link Class#getMethods()} returns methods in no specified order —
 *     it differs between JVMs and even between runs. Operations are
 *     therefore sorted by name, and by parameter types where a name
 *     repeats.</li>
 * <li>Parameter names are only in the class file when it was compiled
 *     with {@code -parameters}. Without that they read {@code arg0},
 *     {@code arg1} — stable, but not the source names. A build that
 *     turns the flag on or off therefore changes the contract, which is
 *     worth knowing before it happens.</li>
 * </ul>
 *
 * <p>What it refuses, it refuses loudly. A contract that quietly says
 * less than the interface does is worse than none: the provider would
 * serve something no consumer can address.
 */
public final class JavaContracts {

    /** How a Java type is named when the model has no metamodel type for it. */
    private static final Map<Class<?>, String> NEUTRAL_TYPES = neutralTypes();

    private JavaContracts() {
    }

    /**
     * Derive the contract of a service interface.
     *
     * @param service the interface an OSGi service is registered under
     * @param version the contract's version — a contract without one
     *                cannot be addressed, so it is asked for rather than
     *                invented
     */
    public static ServiceInterface contractOf(Class<?> service, String version) {
        return contractOf(service, version, JavaContracts::registeredEClass);
    }

    /**
     * Derive the contract, resolving model types through {@code types}.
     *
     * @param types what a Java class is in the metamodel, or {@code null}
     *              when it is not a modelled type. The default asks the
     *              EPackage registry; a caller that knows its packages can
     *              answer faster and without depending on what happens to
     *              be registered.
     */
    public static ServiceInterface contractOf(Class<?> service, String version,
            Function<Class<?>, EClassifier> types) {
        if (service == null || !service.isInterface()) {
            throw new IllegalArgumentException(
                    "a contract is derived from an interface, got " + (service == null ? "null" : service.getName()));
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("a contract needs a version; " + service.getName() + " got none");
        }

        ServiceInterface contract = ServicesFactory.eINSTANCE.createServiceInterface();
        contract.setName(service.getSimpleName());
        contract.setVersion(version);

        Map<String, ServiceException> errors = new LinkedHashMap<>();
        for (Method method : operations(service)) {
            contract.getOperations().add(operationOf(method, types, errors));
        }
        contract.getExceptions().addAll(errors.values());
        return contract;
    }

    /**
     * The methods that are operations, in the order the contract states
     * them.
     *
     * <p>Static methods are not part of what a service instance offers.
     * A {@code default} method is: a caller cannot tell it apart from
     * any other, and the service answers it.
     */
    private static List<Method> operations(Class<?> service) {
        List<Method> methods = new ArrayList<>();
        for (Method method : service.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                methods.add(method);
            }
        }
        methods.sort(Comparator.comparing(Method::getName).thenComparing(JavaContracts::signature));


        // An operation is addressed by name — in a flavor's path, in a
        // proxy's call, in the consumer's lookup. Two methods of one name
        // would be one name for two things, and whichever answered would
        // be luck. Rename one, or split the interface.
        Map<String, Long> byName = methods.stream()
                .collect(Collectors.groupingBy(Method::getName, Collectors.counting()));
        String overloaded = byName.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> e.getKey() + " (" + e.getValue() + "×)")
                .sorted()
                .collect(Collectors.joining(", "));
        if (!overloaded.isEmpty()) {
            throw new IllegalArgumentException(service.getName()
                    + " cannot be a contract: an operation is addressed by name, and these are declared more than once: "
                    + overloaded);
        }
        return methods;
    }

    /** The part of a signature that tells two same-named methods apart. */
    private static String signature(Method method) {
        return Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(","));
    }

    private static ServiceOperation operationOf(Method method, Function<Class<?>, EClassifier> types,
            Map<String, ServiceException> errors) {
        ServiceOperation operation = ServicesFactory.eINSTANCE.createServiceOperation();
        operation.setName(method.getName());

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            operation.getParameters().add(parameterOf(parameters[i], i, method, types));
        }
        if (method.getReturnType() != void.class) {
            Parameter result = slot("result", method.getReturnType(), method.getGenericReturnType(), method, types);
            operation.setReturnValue(result);
        }
        for (Class<?> raised : sorted(method.getExceptionTypes())) {
            operation.getExceptions().add(errors.computeIfAbsent(raised.getSimpleName(), name -> {
                ServiceException declared = ServicesFactory.eINSTANCE.createServiceException();
                declared.setName(name);
                // Symbolic, not the Java FQN: what a consumer in another
                // language sees on the wire is a name, not a class.
                declared.setType(name);
                return declared;
            }));
        }
        return operation;
    }

    private static List<Class<?>> sorted(Class<?>[] types) {
        List<Class<?>> all = new ArrayList<>(List.of(types));
        all.sort(Comparator.<Class<?>, String>comparing(Class::getSimpleName)
                .thenComparing(Class::getName));
        return all;
    }

    private static Parameter parameterOf(java.lang.reflect.Parameter parameter, int index, Method method,
            Function<Class<?>, EClassifier> types) {
        Parameter slot = slot(parameter.getName(), parameter.getType(), parameter.getParameterizedType(), method,
                types);
        slot.setIndex(index);
        return slot;
    }

    /**
     * One slot of an operation — a parameter or the return value.
     *
     * <p>A boxed primitive says the value may be absent, which is what
     * {@code optional} means here; the unboxed one cannot be null and is
     * therefore required. That is the same reading the templates use in
     * the other direction (#75).
     */
    private static Parameter slot(String name, Class<?> raw, Type generic, Method method,
            Function<Class<?>, EClassifier> types) {
        Parameter slot = ServicesFactory.eINSTANCE.createParameter();
        slot.setName(name);

        Class<?> element = raw;
        if (raw.isArray()) {
            element = raw.getComponentType();
            slot.setUpperBound(-1);
        } else if (Collection.class.isAssignableFrom(raw)) {
            element = elementOf(generic, method, name);
            slot.setUpperBound(-1);
        }

        if (element.isPrimitive()) {
            slot.setType(NEUTRAL_TYPES.get(element));
        } else if (NEUTRAL_TYPES.containsKey(element)) {
            slot.setType(NEUTRAL_TYPES.get(element));
            // Boxed, or a String: it can be null, so the contract says so.
            slot.setOptional(true);
        } else {
            EClassifier modelled = types == null ? null : types.apply(element);
            if (modelled == null) {
                throw new IllegalArgumentException(method.getDeclaringClass().getSimpleName() + "." + method.getName()
                        + ": no contract type for " + element.getName() + " (slot '" + name + "'). "
                        + "Primitives, their boxed forms, String and modelled EObject types can be derived; "
                        + "anything else has to be modelled first.");
            }
            slot.setEType(modelled);
            slot.setOptional(true);
        }
        return slot;
    }

    private static Class<?> elementOf(Type generic, Method method, String name) {
        if (generic instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments().length == 1
                && parameterized.getActualTypeArguments()[0] instanceof Class<?> element) {
            return element;
        }
        throw new IllegalArgumentException(method.getDeclaringClass().getSimpleName() + "." + method.getName()
                + ": slot '" + name + "' is a collection without a single element type — "
                + "a contract has to say what travels in it.");
    }

    /**
     * The EClass a Java class stands for, asked of the packages that are
     * registered. Only what is loaded can answer, which is why a caller
     * that knows its own packages should say so rather than rely on this.
     */
    private static EClassifier registeredEClass(Class<?> candidate) {
        if (!EObject.class.isAssignableFrom(candidate)) {
            return null;
        }
        for (Object registered : List.copyOf(EPackage.Registry.INSTANCE.values())) {
            if (!(registered instanceof EPackage ePackage)) {
                continue;
            }
            for (EClassifier classifier : ePackage.getEClassifiers()) {
                if (classifier instanceof EClass eClass && candidate.equals(eClass.getInstanceClass())) {
                    return eClass;
                }
            }
        }
        return null;
    }

    private static Map<Class<?>, String> neutralTypes() {
        Map<Class<?>, String> types = new LinkedHashMap<>();
        types.put(boolean.class, "boolean");
        types.put(Boolean.class, "boolean");
        types.put(byte.class, "byte");
        types.put(Byte.class, "byte");
        types.put(short.class, "short");
        types.put(Short.class, "short");
        types.put(int.class, "int");
        types.put(Integer.class, "int");
        types.put(long.class, "long");
        types.put(Long.class, "long");
        types.put(float.class, "float");
        types.put(Float.class, "float");
        types.put(double.class, "double");
        types.put(Double.class, "double");
        types.put(String.class, "string");
        return types;
    }
}
