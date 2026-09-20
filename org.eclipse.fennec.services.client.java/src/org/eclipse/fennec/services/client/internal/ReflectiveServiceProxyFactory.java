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

package org.eclipse.fennec.services.client.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.ServiceProxyFactory;
import org.eclipse.fennec.services.client.TrackedServiceLocator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;

/**
 * Builds {@link java.lang.reflect.Proxy} instances backed by the
 * REST-flavor {@link ServiceInvoker}. Each method call on the proxy
 * becomes an {@code invoker.invoke(loc, methodName, argMap)} call.
 *
 * <p>Argument naming: if the interface is compiled with
 * {@code -parameters}, the actual parameter name is used as the
 * argument-map key; otherwise the synthetic {@code "arg0", "arg1"} …
 * fallback is applied. The chosen names must match what the wire
 * convention expects (query-param names for GET; for our current
 * single-EObject convention the key is ignored anyway).
 */
@Component(service = ServiceProxyFactory.class)
@ServiceDescription("DDSR reflective service-proxy factory")
public final class ReflectiveServiceProxyFactory implements ServiceProxyFactory {

	/**
	 * The invokers this runtime has, by the flavor each speaks.
	 *
	 * <p>Dynamic and multiple, with method injection: they are the
	 * audience, not the cast. A runtime with only the REST transport
	 * installed has one, a runtime with both has two, and a proxy over a
	 * service that announces a flavor nobody here speaks fails saying
	 * exactly that instead of calling the wrong way.
	 */
	private final Map<FlavorKind, ServiceInvoker> invokers = new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addInvoker(ServiceInvoker invoker, Map<String, Object> properties) {
		FlavorKind kind = flavorOf(properties);
		if (kind != null) {
			invokers.put(kind, invoker);
		}
	}

	void removeInvoker(ServiceInvoker invoker, Map<String, Object> properties) {
		FlavorKind kind = flavorOf(properties);
		if (kind != null) {
			invokers.remove(kind, invoker);
		}
	}

	private static FlavorKind flavorOf(Map<String, Object> properties) {
		Object declared = properties == null ? null : properties.get(ServiceInvoker.FLAVOR_PROPERTY);
		if (declared == null) {
			return null;
		}
		return FlavorKind.getByName(String.valueOf(declared));
	}

	/**
	 * The invoker for a service, by the flavors it announces — in the
	 * order it announces them, because that order is the provider's own
	 * statement about what it prefers. A lookup already filtered by what
	 * this consumer said it speaks, so anything still listed here is
	 * fair game.
	 */
	private ServiceInvoker invokerFor(ServiceLocator locator) {
		ServiceImplementation implementation = locator.implementation();
		List<String> announced = new ArrayList<>();
		if (implementation != null) {
			for (ServiceFlavor flavor : implementation.getFlavors()) {
				if (flavor.getKind() == null) {
					continue;
				}
				announced.add(flavor.getKind().getName());
				ServiceInvoker invoker = invokers.get(flavor.getKind());
				if (invoker != null) {
					return invoker;
				}
			}
		}
		// Nothing announced at all is the older shape of a locator, and
		// one invoker is then the only sensible reading of it.
		if (announced.isEmpty() && invokers.size() == 1) {
			return invokers.values().iterator().next();
		}
		throw new DdsrException("this service is reachable over " + announced
				+ " and this runtime speaks " + invokers.keySet()
				+ " — no transport in common");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T newProxy(Class<T> serviceInterface, ServiceLocator locator) {
		if (serviceInterface == null || !serviceInterface.isInterface()) {
			throw new DdsrException("serviceInterface must be a Java interface");
		}
		if (locator == null) {
			throw new DdsrException("locator must not be null");
		}
		InvocationHandler handler = new RemoteCallHandler(this::invokerFor, locator, serviceInterface);
		return (T) Proxy.newProxyInstance(
				serviceInterface.getClassLoader(),
				new Class<?>[] { serviceInterface },
				handler);
	}

	private static final class RemoteCallHandler implements InvocationHandler {

		/**
		 * Asked per call rather than bound once: a tracked locator may
		 * rebind to another implementation, and the new one does not have
		 * to be reachable the same way as the old.
		 */
		private final Function<ServiceLocator, ServiceInvoker> invokerFor;
		private final ServiceLocator locator;
		private final Class<?> serviceInterface;

		RemoteCallHandler(Function<ServiceLocator, ServiceInvoker> invokerFor, ServiceLocator locator,
				Class<?> serviceInterface) {
			this.invokerFor = invokerFor;
			this.locator = locator;
			this.serviceInterface = serviceInterface;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getDeclaringClass() == Object.class) {
				// toString / hashCode / equals on the proxy itself.
				return handleObjectMethod(method, args);
			}
			Map<String, Object> argMap = buildArgMap(method, args);
			Object result;
			try {
				result = invokerFor.apply(locator).invoke(locator, method.getName(), argMap);
			} catch (DdsrException failure) {
				// #59: the registered provider did not answer. Rebind away from
				// it and retry exactly once; a second failure is the caller's.
				if (!failure.isTransportFailure() || !(locator instanceof TrackedServiceLocator tracked)
						|| !tracked.rebind(true)) {
					throw failure;
				}
				// Asked again after the rebind: the service it bound to now
				// may be reachable a different way than the one that failed.
				result = invokerFor.apply(locator).invoke(locator, method.getName(), argMap);
			}
			return coerceReturn(method, result);
		}

		private Object handleObjectMethod(Method method, Object[] args) {
			switch (method.getName()) {
			case "toString":
				return "remote-proxy[" + serviceInterface.getSimpleName() + "@" + locator.reference().getId() + "]";
			case "hashCode":
				return System.identityHashCode(this);
			case "equals":
				return args[0] == this;
			default:
				return null;
			}
		}

		/**
		 * Build the argument map by **DDSR-model** parameter names, not
		 * Java reflection: the named slots come from the operation the
		 * flavor binds, or from the contract when no flavor wired the
		 * cross-reference. Method arguments are then placed positionally
		 * by their index, independent of any {@code -parameters} javac
		 * flag.
		 */
		private Map<String, Object> buildArgMap(Method method, Object[] args) {
			if (args == null || args.length == 0) {
				return Map.of();
			}
			List<String> names = modelParameterNames(method.getName());
			if (names == null) {
				throw new DdsrException("no DDSR operation metadata for '" + method.getName()
						+ "' on " + serviceInterface.getName() + " — cannot map arguments");
			}
			if (names.size() < args.length) {
				throw new DdsrException("operation '" + method.getName()
						+ "' has " + names.size() + " parameter(s) in the catalog but the Java call passed "
						+ args.length);
			}
			Map<String, Object> m = new LinkedHashMap<>();
			for (int i = 0; i < args.length; i++) {
				m.put(names.get(i), args[i]);
			}
			return m;
		}

		private List<String> modelParameterNames(String operationName) {
			// 1. Prefer the direct link if the publisher wired
			//    OperationFlavor.operation → ServiceOperation. Any flavor:
			//    which transport it is decides where a call travels, not
			//    what the call is called.
			ServiceImplementation impl = locator.implementation();
			if (impl != null) {
				for (ServiceFlavor flavor : impl.getFlavors()) {
					for (ServiceOperationFlavor of : flavor.getOperationFlavors()) {
						ServiceOperation op = of.getOperation();
						boolean matches = operationName.equals(of.getName())
								|| (op != null && operationName.equals(op.getName()));
						if (matches && op != null) {
							return paramNames(op);
						}
					}
				}
			}
			// 2. Fallback: walk impl.serviceInterfaces[*].operations and
			//    look up by operation name. Works even when the publisher
			//    skipped the operation cross-ref on the flavor.
			if (impl != null) {
				for (ServiceInterface si : impl.getServiceInterfaces()) {
					for (ServiceOperation op : si.getOperations()) {
						if (operationName.equals(op.getName())) {
							return paramNames(op);
						}
					}
				}
			}
			return null;
		}

		private static List<String> paramNames(ServiceOperation op) {
			List<String> names = new ArrayList<>(op.getParameters().size());
			for (Parameter p : op.getParameters()) {
				names.add(p.getName());
			}
			return names;
		}

		private static Object coerceReturn(Method method, Object result) {
			Class<?> rt = method.getReturnType();
			if (rt == void.class || result == null) {
				return null;
			}
			// Boxed primitive accepted for primitive return type.
			Class<?> effective = rt.isPrimitive() ? boxOf(rt) : rt;
			if (effective.isInstance(result)) {
				return result;
			}
			// Common case from JSON / text responses: result is a String
			// that needs to be parsed into a primitive.
			if (result instanceof String) {
				Object parsed = parseFromString((String) result, rt);
				if (parsed != null) {
					return parsed;
				}
			}
			throw new DdsrException("cannot coerce result of type "
					+ result.getClass().getName() + " to " + rt.getName()
					+ " for " + method);
		}

		private static Class<?> boxOf(Class<?> primitive) {
			if (primitive == double.class)  return Double.class;
			if (primitive == float.class)   return Float.class;
			if (primitive == long.class)    return Long.class;
			if (primitive == int.class)     return Integer.class;
			if (primitive == short.class)   return Short.class;
			if (primitive == byte.class)    return Byte.class;
			if (primitive == boolean.class) return Boolean.class;
			if (primitive == char.class)    return Character.class;
			return primitive;
		}

		private static Object parseFromString(String s, Class<?> target) {
			String v = s.trim();
			try {
				if (target == String.class)                          return v;
				if (target == double.class  || target == Double.class)  return Double.valueOf(v);
				if (target == float.class   || target == Float.class)   return Float.valueOf(v);
				if (target == long.class    || target == Long.class)    return Long.valueOf(v);
				if (target == int.class     || target == Integer.class) return Integer.valueOf(v);
				if (target == short.class   || target == Short.class)   return Short.valueOf(v);
				if (target == byte.class    || target == Byte.class)    return Byte.valueOf(v);
				if (target == boolean.class || target == Boolean.class) return Boolean.valueOf(v);
			} catch (NumberFormatException ignore) {
				// fall through
			}
			return null;
		}
	}
}
