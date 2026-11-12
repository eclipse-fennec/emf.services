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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

/**
 * A method that answers later: the holder types of the {@code osgi.async}
 * intent (Remote Services §100.6) and what each end does with them.
 *
 * <p>The specification names four return types a distribution provider
 * with that intent supports — {@code org.osgi.util.promise.Promise},
 * {@link Future}, {@link CompletionStage} and {@link CompletableFuture}
 * — and says what they hold is typed by the other intents. So to a
 * contract a method returning {@code Promise<String>} returns a string:
 * that an answer comes later is a statement about the two ends, each
 * for itself, and nothing a wire has to carry. A service may answer
 * later while its caller waits, and the other way round.
 *
 * <p>The promise types are reached by name and through reflection on
 * purpose. This bundle is on every node; the promise bundle is only
 * where somebody uses one, and a method that returns a Promise brings
 * the class — and its {@code Deferred} — with it.
 */
public final class AsyncReturns {

	private static final String PROMISE = "org.osgi.util.promise.Promise";

	private AsyncReturns() {
	}

	/** Whether a method with this return type answers later. */
	public static boolean isHolder(Class<?> returned) {
		return returned != null && (isPromise(returned) || returned == Future.class
				|| returned == CompletionStage.class || returned == CompletableFuture.class);
	}

	private static boolean isPromise(Class<?> type) {
		return PROMISE.equals(type.getName());
	}

	private static final String PUSH_STREAM = "org.osgi.util.pushstream.PushStream";

	/**
	 * Whether a slot of this Java type is a stream — values that arrive
	 * one after the other, over time ({@code Delivery.STREAM} in the
	 * model). Two types say so in Java: the OSGi {@code PushStream} and
	 * the JDK's {@link Flow.Publisher}, which every reactive library
	 * adapts to. What they carry is their type argument
	 * ({@link #valueType}).
	 */
	public static boolean isStream(Class<?> type) {
		return type != null && (type == Flow.Publisher.class || PUSH_STREAM.equals(type.getName()));
	}

	/**
	 * What a holder holds: the type argument of the generic return type,
	 * or {@code void} where it says nothing useful ({@code Promise<Void>},
	 * a raw type, a wildcard).
	 */
	public static Type valueType(Type genericReturn) {
		if (genericReturn instanceof ParameterizedType parameterized
				&& parameterized.getActualTypeArguments().length == 1) {
			Type held = parameterized.getActualTypeArguments()[0];
			if (held == Void.class || held instanceof WildcardType) {
				return void.class;
			}
			return held;
		}
		return void.class;
	}

	/** The class of a type, for a type that is a class or a parameterized one. */
	public static Class<?> rawOf(Type type) {
		if (type instanceof Class<?> raw) {
			return raw;
		}
		if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw) {
			return raw;
		}
		return Object.class;
	}

	/** Whether what a method returned is a holder still to be waited for. */
	public static boolean isPending(Object returned) {
		return returned instanceof Future<?> || returned instanceof CompletionStage<?>
				|| (returned != null && implementsPromise(returned.getClass()));
	}

	private static boolean implementsPromise(Class<?> type) {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (Class<?> implemented : current.getInterfaces()) {
				if (isPromise(implemented) || implementsPromise(implemented)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Tells {@code done} how a returned value turned out — at once for a
	 * plain value, when it is settled for a holder. Never blocks for a
	 * Promise or a CompletionStage; a plain {@link Future} has no way
	 * to be told and is waited for on the calling thread.
	 *
	 * @param returned what the method returned
	 * @param done     takes the value, or the failure the holder ended
	 *                 with — exactly one of them is non-null, or both
	 *                 are null for a holder of nothing
	 */
	public static void whenDone(Object returned, BiConsumer<Object, Throwable> done) {
		if (returned instanceof CompletionStage<?> stage) {
			stage.whenComplete((value, failure) -> done.accept(failure == null ? value : null, unwrapped(failure)));
		} else if (returned != null && implementsPromise(returned.getClass())) {
			promiseDone(returned, done);
		} else if (returned instanceof Future<?> future) {
			try {
				done.accept(future.get(), null);
			} catch (ExecutionException failed) {
				done.accept(null, unwrapped(failed));
			} catch (CancellationException cancelled) {
				done.accept(null, cancelled);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				done.accept(null, interrupted);
			}
		} else {
			done.accept(returned, null);
		}
	}

	/**
	 * The value a returned holder ends with, waited for. A failure comes
	 * out the way a throwing method's does, so a caller treats a service
	 * that answers later and one that answers at once alike.
	 *
	 * @throws InvocationTargetException with the failure the holder ended with
	 */
	public static Object settled(Object returned) throws InvocationTargetException {
		if (!isPending(returned)) {
			return returned;
		}
		CompletableFuture<Object> outcome = new CompletableFuture<>();
		whenDone(returned, (value, failure) -> {
			if (failure != null) {
				outcome.completeExceptionally(failure);
			} else {
				outcome.complete(value);
			}
		});
		try {
			return outcome.get();
		} catch (ExecutionException failed) {
			throw new InvocationTargetException(unwrapped(failed));
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new InvocationTargetException(interrupted);
		}
	}

	/**
	 * A holder of the type a method declares, settled as {@code outcome}
	 * settles — failed with the very exception a synchronous call would
	 * have thrown, which is what the intent asks for.
	 */
	public static Object holderFor(Class<?> declared, CompletableFuture<Object> outcome) {
		if (!isPromise(declared)) {
			return outcome;
		}
		try {
			Class<?> deferredType = Class.forName("org.osgi.util.promise.Deferred", true, declared.getClassLoader());
			Object deferred = deferredType.getConstructor().newInstance();
			outcome.whenComplete((value, failure) -> {
				try {
					if (failure == null) {
						deferredType.getMethod("resolve", Object.class).invoke(deferred, value);
					} else {
						deferredType.getMethod("fail", Throwable.class).invoke(deferred, unwrapped(failure));
					}
				} catch (ReflectiveOperationException unsettled) {
					throw new IllegalStateException("the promise could not be settled", unsettled);
				}
			});
			return deferredType.getMethod("getPromise").invoke(deferred);
		} catch (ReflectiveOperationException noPromises) {
			throw new IllegalStateException("a method returns a Promise and the promise package cannot build one",
					noPromises);
		}
	}

	private static void promiseDone(Object promise, BiConsumer<Object, Throwable> done) {
		try {
			Class<?> type = promiseInterface(promise.getClass());
			type.getMethod("onResolve", Runnable.class).invoke(promise, (Runnable) () -> {
				try {
					Object failure = type.getMethod("getFailure").invoke(promise);
					if (failure != null) {
						done.accept(null, (Throwable) failure);
					} else {
						done.accept(type.getMethod("getValue").invoke(promise), null);
					}
				} catch (ReflectiveOperationException unreadable) {
					done.accept(null, unwrapped(unreadable));
				}
			});
		} catch (ReflectiveOperationException unusable) {
			done.accept(null, unwrapped(unusable));
		}
	}

	private static Class<?> promiseInterface(Class<?> type) {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (Class<?> implemented : current.getInterfaces()) {
				if (isPromise(implemented)) {
					return implemented;
				}
				Class<?> inherited = promiseInterface(implemented);
				if (inherited != null) {
					return inherited;
				}
			}
		}
		return isPromise(type) ? type : null;
	}

	/** The failure itself, without what a future or reflection wrapped around it. */
	private static Throwable unwrapped(Throwable failure) {
		Throwable current = failure;
		while ((current instanceof CompletionException || current instanceof ExecutionException
				|| current instanceof InvocationTargetException) && current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}
}
