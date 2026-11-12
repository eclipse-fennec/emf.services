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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

/**
 * The four holder types of the osgi.async intent (Remote Services
 * §100.6), read the same way at both ends of a call.
 */
class AsyncReturnsTest {

	interface Shapes {
		Promise<String> promised();

		Future<Integer> future();

		CompletionStage<List<String>> stage();

		CompletableFuture<Void> nothing();

		String now();
	}

	private static Type heldBy(String method) throws NoSuchMethodException {
		return AsyncReturns.valueType(Shapes.class.getMethod(method).getGenericReturnType());
	}

	@Test
	void theFourHolderTypesOfTheIntentAreHoldersAndNothingElseIs() throws Exception {
		for (String method : new String[] { "promised", "future", "stage", "nothing" }) {
			assertThat(AsyncReturns.isHolder(Shapes.class.getMethod(method).getReturnType())).as(method).isTrue();
		}
		assertThat(AsyncReturns.isHolder(String.class)).isFalse();
		assertThat(AsyncReturns.isHolder(List.class)).isFalse();
		assertThat(AsyncReturns.isHolder(null)).isFalse();
	}

	@Test
	void aHolderHoldsItsTypeArgument() throws Exception {
		assertThat(heldBy("promised")).isEqualTo(String.class);
		assertThat(heldBy("future")).isEqualTo(Integer.class);
		assertThat(AsyncReturns.rawOf(heldBy("stage"))).isEqualTo(List.class);
		assertThat(heldBy("nothing")).as("a holder of Void holds nothing").isEqualTo(void.class);
	}

	@Test
	void aPlainValueIsDoneAtOnce() {
		List<Object> seen = new ArrayList<>();

		AsyncReturns.whenDone("now", (value, failure) -> seen.add(value));

		assertThat(seen).containsExactly("now");
		assertThat(AsyncReturns.isPending("now")).isFalse();
	}

	/** A promise tells its listeners on a thread of its own, so the test waits for the telling. */
	private static final long PATIENCE_SECONDS = 5;

	@Test
	void aPromiseIsDoneWhenItResolvesAndNotBefore() throws Exception {
		Deferred<String> deferred = new Deferred<>();
		CompletableFuture<Object> seen = new CompletableFuture<>();

		AsyncReturns.whenDone(deferred.getPromise(), (value, failure) -> seen.complete(value));
		assertThat(seen).as("nothing yet — and nothing waited for it").isNotDone();
		deferred.resolve("later");

		assertThat(seen.get(PATIENCE_SECONDS, TimeUnit.SECONDS)).isEqualTo("later");
		assertThat(AsyncReturns.isPending(deferred.getPromise())).isTrue();
	}

	@Test
	void aFailedPromiseHandsOverItsFailure() throws Exception {
		IllegalStateException refused = new IllegalStateException("no");
		CompletableFuture<Throwable> seen = new CompletableFuture<>();

		AsyncReturns.whenDone(Promises.failed(refused), (value, failure) -> seen.complete(failure));

		assertThat(seen.get(PATIENCE_SECONDS, TimeUnit.SECONDS)).isSameAs(refused);
	}

	@Test
	void aStageHandsOverTheFailureItselfAndNotItsWrapper() {
		IllegalStateException refused = new IllegalStateException("no");
		CompletableFuture<String> stage = new CompletableFuture<>();
		List<Throwable> seen = new ArrayList<>();

		AsyncReturns.whenDone(stage.thenApply(String::trim), (value, failure) -> seen.add(failure));
		stage.completeExceptionally(refused);

		assertThat(seen).containsExactly(refused);
	}

	@Test
	void aPlainFutureIsWaitedFor() {
		FutureTask<Integer> task = new FutureTask<>(() -> 42);
		task.run();
		List<Object> seen = new ArrayList<>();

		AsyncReturns.whenDone(task, (value, failure) -> seen.add(value));

		assertThat(seen).containsExactly(42);
	}

	/** What a dispatcher on a request thread does: a service that answers later is like one that throws. */
	@Test
	void settledGivesTheValueOrThrowsAsAThrowingMethodWould() throws Exception {
		IllegalStateException refused = new IllegalStateException("no");

		assertThat(AsyncReturns.settled(Promises.resolved("later"))).isEqualTo("later");
		assertThat(AsyncReturns.settled(CompletableFuture.completedFuture(7))).isEqualTo(7);
		assertThat(AsyncReturns.settled("now")).isEqualTo("now");
		assertThatThrownBy(() -> AsyncReturns.settled(Promises.failed(refused)))
				.isInstanceOf(InvocationTargetException.class).hasCause(refused);
	}

	/** What a proxy does: the holder a method declares, settled as the call settles. */
	@Test
	void aHolderOfTheDeclaredTypeSettlesAsTheCallDoes() throws Exception {
		CompletableFuture<Object> call = new CompletableFuture<>();
		Object promise = AsyncReturns.holderFor(Promise.class, call);
		Object future = AsyncReturns.holderFor(Future.class, call);

		assertThat(promise).isInstanceOf(Promise.class);
		assertThat(((Promise<?>) promise).isDone()).isFalse();
		call.complete("answered");

		assertThat(((Promise<?>) promise).getValue()).as("getValue waits for the promise").isEqualTo("answered");
		assertThat(((Future<?>) future).get()).isEqualTo("answered");
		assertThat(AsyncReturns.holderFor(CompletionStage.class, call)).isSameAs(call);
	}

	@Test
	void aPromiseFailsWithTheExceptionASynchronousCallWouldHaveThrown() throws Exception {
		IllegalStateException refused = new IllegalStateException("no");
		CompletableFuture<Object> call = new CompletableFuture<>();
		Promise<?> promise = (Promise<?>) AsyncReturns.holderFor(Promise.class, call);

		call.completeExceptionally(refused);

		assertThat(promise.getFailure()).isSameAs(refused);
	}
}
