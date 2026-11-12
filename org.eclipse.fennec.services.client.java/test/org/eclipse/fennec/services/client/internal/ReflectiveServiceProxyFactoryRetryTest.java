/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.DdsrServiceException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.TrackedServiceLocator;
import org.junit.jupiter.api.Test;
import org.osgi.util.promise.Promise;
import org.osgi.util.pushstream.PushStream;

/** #59: a transport failure makes the proxy rebind once and retry once — nothing more. */
class ReflectiveServiceProxyFactoryRetryTest {

	interface Payment {
		double getBalance(String accountId);
	}

	private static final class ScriptedInvoker implements ServiceInvoker {
		final List<RuntimeException> failures = new ArrayList<>();
		int calls;

		@Override
		public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
			calls++;
			if (!failures.isEmpty()) {
				throw failures.remove(0);
			}
			return 42.0;
		}
	}

	/** A tracked locator whose rebind outcome the test scripts. */
	private static final class ScriptedLocator implements TrackedServiceLocator {
		private final ServiceImplementation implementation;
		private final ServiceReference reference;
		boolean rebindAnswer = true;
		final List<Boolean> rebindCalls = new ArrayList<>();

		ScriptedLocator() {
			ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
			si.setName("Payment");
			ServiceOperation op = ServicesFactory.eINSTANCE.createServiceOperation();
			op.setName("getBalance");
			Parameter p = ServicesFactory.eINSTANCE.createParameter();
			p.setName("accountId");
			p.setType("string");
			op.getParameters().add(p);
			si.getOperations().add(op);
			implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
			implementation.setName("impl");
			implementation.getServiceInterfaces().add(si);
			RestFlavor rest = ServicesFactory.eINSTANCE.createRestFlavor();
			rest.setName("rest");
			rest.setHost("http://provider:9091");
			rest.setBasePath("/payments");
			RestOperationFlavor of = ServicesFactory.eINSTANCE.createRestOperationFlavor();
			of.setName("getBalance");
			of.setPath("/balance");
			of.setOperation(op);
			rest.getOperationFlavors().add(of);
			implementation.getFlavors().add(rest);
			reference = ServicesFactory.eINSTANCE.createServiceReference();
			reference.setId("ref-1");
		}

		@Override public ServiceReference reference() { return reference; }
		@Override public ServiceImplementation implementation() { return implementation; }
		@Override public Optional<RestFlavor> restFlavor() { return Optional.of((RestFlavor) implementation.getFlavors().get(0)); }
		@Override public Optional<URI> urlFor(String operationName) { return Optional.of(URI.create("http://provider:9091/payments/balance")); }
		@Override public State state() { return State.LIVE; }
		@Override public String interfaceName() { return "Payment"; }
		@Override public String filter() { return null; }

		@Override
		public boolean rebind(boolean excludeCurrent) {
			rebindCalls.add(excludeCurrent);
			return rebindAnswer;
		}
	}

	private static Payment proxy(ScriptedInvoker invoker, ServiceLocator locator) {
		ReflectiveServiceProxyFactory factory = new ReflectiveServiceProxyFactory();
		InvokerRegistry invokers = new InvokerRegistry();
		factory.invokers = invokers;
		invokers.addInvoker(invoker, Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));
		return factory.newProxy(Payment.class, locator);
	}

	@Test
	void aTransportFailureRebindsAwayFromTheFailedRegistrationAndRetriesOnce() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(DdsrException.transport("connect refused", new RuntimeException("ECONNREFUSED")));
		ScriptedLocator locator = new ScriptedLocator();

		double balance = proxy(invoker, locator).getBalance("acc-1");

		assertThat(balance).isEqualTo(42.0);
		assertThat(invoker.calls).isEqualTo(2);
		assertThat(locator.rebindCalls).as("excludeCurrent — never back to the one that failed").containsExactly(true);
	}

	@Test
	void aSecondTransportFailureIsTheCallers() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(DdsrException.transport("timeout", null));
		invoker.failures.add(DdsrException.transport("timeout again", null));
		ScriptedLocator locator = new ScriptedLocator();

		assertThatThrownBy(() -> proxy(invoker, locator).getBalance("acc-1"))
				.isInstanceOf(DdsrException.class)
				.hasMessageContaining("timeout again");
		assertThat(invoker.calls).isEqualTo(2);
	}

	@Test
	void whenNothingElseIsRegisteredTheFailurePropagatesWithoutARetry() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(DdsrException.transport("connect refused", null));
		ScriptedLocator locator = new ScriptedLocator();
		locator.rebindAnswer = false;

		assertThatThrownBy(() -> proxy(invoker, locator).getBalance("acc-1"))
				.isInstanceOf(DdsrException.class)
				.hasMessageContaining("connect refused");
		assertThat(invoker.calls).isEqualTo(1);
	}

	@Test
	void aBrokerOrApplicationFailureIsNeverRetried() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(new DdsrException("HTTP 500 from the provider"));
		ScriptedLocator locator = new ScriptedLocator();

		assertThatThrownBy(() -> proxy(invoker, locator).getBalance("acc-1")).isInstanceOf(DdsrException.class);
		assertThat(invoker.calls).isEqualTo(1);
		assertThat(locator.rebindCalls).isEmpty();
	}

	@Test
	void anUntrackedLocatorGetsNoRetryEither() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(DdsrException.transport("connect refused", null));
		ScriptedLocator tracked = new ScriptedLocator();
		ServiceLocator plain = new ServiceLocator() {
			@Override public ServiceReference reference() { return tracked.reference(); }
			@Override public ServiceImplementation implementation() { return tracked.implementation(); }
			@Override public Optional<RestFlavor> restFlavor() { return tracked.restFlavor(); }
			@Override public Optional<URI> urlFor(String operationName) { return tracked.urlFor(operationName); }
		};

		assertThatThrownBy(() -> proxy(invoker, plain).getBalance("acc-1")).isInstanceOf(DdsrException.class);
		assertThat(invoker.calls).isEqualTo(1);
	}

	/** What the generated interface declares for the contract's 'AccountUnknown'. */
	public static class AccountUnknownException extends Exception {
		private static final long serialVersionUID = 1L;

		public AccountUnknownException(String message) {
			super(message);
		}
	}

	/** The same contract, as an interface that declares its error. */
	interface CheckedPayment {
		double getBalance(String accountId) throws AccountUnknownException;
	}

	private static DdsrServiceException accountUnknown() {
		ServiceException declared = ServicesFactory.eINSTANCE.createServiceException();
		declared.setName("AccountUnknown");
		declared.setType("payment.AccountUnknown");
		return new DdsrServiceException("no account acc-1", declared, 404);
	}

	/**
	 * #185: a caller written against the interface catches the class the
	 * method declares — an unchecked exception would go past its catch.
	 */
	@Test
	void aDeclaredErrorIsThrownAsTheExceptionTheMethodDeclares() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		DdsrServiceException answered = accountUnknown();
		invoker.failures.add(answered);
		ScriptedLocator locator = new ScriptedLocator();
		ReflectiveServiceProxyFactory factory = new ReflectiveServiceProxyFactory();
		InvokerRegistry invokers = new InvokerRegistry();
		factory.invokers = invokers;
		invokers.addInvoker(invoker, Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));
		CheckedPayment payment = factory.newProxy(CheckedPayment.class, locator);

		assertThatThrownBy(() -> payment.getBalance("acc-1"))
				.isInstanceOf(AccountUnknownException.class)
				.hasMessage("no account acc-1")
				.hasCause(answered);
		assertThat(invoker.calls).as("a declared error is an answer, not a reason to ask again").isEqualTo(1);
		assertThat(locator.rebindCalls).isEmpty();
	}

	@Test
	void whereTheMethodDeclaresNothingTheDeclaredErrorStaysWhatItIs() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		DdsrServiceException answered = accountUnknown();
		invoker.failures.add(answered);

		assertThatThrownBy(() -> proxy(invoker, new ScriptedLocator()).getBalance("acc-1")).isSameAs(answered);
	}

	/** The same contract, by a consumer that does not want to wait (osgi.async). */
	interface LaterPayment {
		Promise<Double> getBalance(String accountId) throws AccountUnknownException;
	}

	interface FuturePayment {
		CompletableFuture<Double> getBalance(String accountId);
	}

	private static <T> T asyncProxy(Class<T> type, ScriptedInvoker invoker) {
		ReflectiveServiceProxyFactory factory = new ReflectiveServiceProxyFactory();
		InvokerRegistry invokers = new InvokerRegistry();
		factory.invokers = invokers;
		invokers.addInvoker(invoker, Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));
		return factory.newProxy(type, new ScriptedLocator());
	}

	/**
	 * A method that returns a holder gets it back at once and the call is
	 * made elsewhere; what it holds is what the synchronous method would
	 * have returned.
	 */
	@Test
	void aMethodThatReturnsAPromiseIsAnsweredLater() throws Exception {
		ScriptedInvoker invoker = new ScriptedInvoker();

		Promise<Double> balance = asyncProxy(LaterPayment.class, invoker).getBalance("acc-1");

		assertThat(balance.getValue()).isEqualTo(42.0);
		assertThat(invoker.calls).isEqualTo(1);
	}

	@Test
	void aCompletableFutureIsAHolderToo() throws Exception {
		assertThat(asyncProxy(FuturePayment.class, new ScriptedInvoker()).getBalance("acc-1")
				.get(5, TimeUnit.SECONDS)).isEqualTo(42.0);
	}

	/** §100.6: the holder fails with the same exception a synchronous call would have thrown. */
	@Test
	void theHolderFailsWithTheExceptionTheMethodDeclares() throws Exception {
		ScriptedInvoker invoker = new ScriptedInvoker();
		DdsrServiceException answered = accountUnknown();
		invoker.failures.add(answered);

		Promise<Double> balance = asyncProxy(LaterPayment.class, invoker).getBalance("acc-1");

		assertThat(balance.getFailure())
				.isInstanceOf(AccountUnknownException.class)
				.hasMessage("no account acc-1")
				.hasCause(answered);
	}

	@Test
	void anyOtherFailureEndsTheHolderAndNotTheCall() throws Exception {
		ScriptedInvoker invoker = new ScriptedInvoker();
		DdsrException down = new DdsrException("HTTP 500 from the provider");
		invoker.failures.add(down);

		CompletableFuture<Double> balance = asyncProxy(FuturePayment.class, invoker).getBalance("acc-1");

		assertThatThrownBy(() -> balance.get(5, TimeUnit.SECONDS)).hasCause(down);
	}

	/** A transport that is told its answer: it completes the stage itself and waits nowhere. */
	private static final class ToldInvoker implements ServiceInvoker {
		final List<CompletableFuture<Object>> asked = new ArrayList<>();
		boolean usedTheBlockingExecutor;

		@Override
		public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
			throw new AssertionError("a call that answers later must not be made the waiting way");
		}

		@Override
		public CompletionStage<Object> invokeAsync(ServiceLocator locator, String operationName,
				Map<String, Object> args, Executor blocking) {
			CompletableFuture<Object> answer = new CompletableFuture<>();
			asked.add(answer);
			return answer;
		}
	}

	/**
	 * The invoker is asked the way that does not wait. One that has its
	 * answers delivered — a messaging broker — settles the holder from
	 * the thread that brings the answer, and no thread of the factory's
	 * is occupied in between.
	 */
	@Test
	void anInvokerThatIsToldItsAnswerSettlesTheHolderItself() throws Exception {
		ToldInvoker invoker = new ToldInvoker();
		ReflectiveServiceProxyFactory factory = new ReflectiveServiceProxyFactory();
		InvokerRegistry invokers = new InvokerRegistry();
		factory.invokers = invokers;
		invokers.addInvoker(invoker, Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));

		CompletableFuture<Double> balance = factory.newProxy(FuturePayment.class, new ScriptedLocator())
				.getBalance("acc-1");

		assertThat(balance).as("asked, and not answered yet").isNotDone();
		assertThat(invoker.asked).hasSize(1);
		invoker.asked.get(0).complete(42.0);

		assertThat(balance.get(5, TimeUnit.SECONDS)).isEqualTo(42.0);
	}

	/** #59 in the shape that does not wait: one rebind, one more call. */
	@Test
	void aTransportFailureRebindsAndAsksOnceMoreWithoutWaitingEither() throws Exception {
		ToldInvoker invoker = new ToldInvoker();
		ScriptedLocator locator = new ScriptedLocator();
		ReflectiveServiceProxyFactory factory = new ReflectiveServiceProxyFactory();
		InvokerRegistry invokers = new InvokerRegistry();
		factory.invokers = invokers;
		invokers.addInvoker(invoker, Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));

		CompletableFuture<Double> balance = factory.newProxy(FuturePayment.class, locator).getBalance("acc-1");
		invoker.asked.get(0).completeExceptionally(DdsrException.transport("no answer", null));
		for (int i = 0; i < 100 && invoker.asked.size() < 2; i++) {
			Thread.sleep(20);
		}

		assertThat(locator.rebindCalls).containsExactly(true);
		assertThat(invoker.asked).as("asked once more after the rebind").hasSize(2);
		invoker.asked.get(1).complete(7.0);
		assertThat(balance.get(5, TimeUnit.SECONDS)).isEqualTo(7.0);
	}

	/** The operation as a stream (Delivery.STREAM), in the two types Java has for one. */
	interface FlowBalances {
		Flow.Publisher<Double> getBalance(String accountId);
	}

	interface PushedBalances {
		PushStream<Double> getBalance(String accountId);
	}

	/** And with the stream on the way in. */
	interface FedBalances {
		void getBalance(Flow.Publisher<Double> accountIds);
	}

	/** A transport that carries streams: hands out whatever the test submits. */
	private static final class StreamingInvoker implements ServiceInvoker {
		final SubmissionPublisher<Object> values = new SubmissionPublisher<>();
		int streamsAsked;

		@Override
		public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
			throw new AssertionError("a stream is not asked for the way a value is");
		}

		@Override
		public Flow.Publisher<Object> invokeStream(ServiceLocator locator, String operationName,
				Map<String, Object> args) {
			streamsAsked++;
			return values;
		}
	}

	private static <T> T streaming(Class<T> type, ServiceInvoker invoker) {
		ReflectiveServiceProxyFactory factory = new ReflectiveServiceProxyFactory();
		InvokerRegistry invokers = new InvokerRegistry();
		factory.invokers = invokers;
		invokers.addInvoker(invoker, Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));
		return factory.newProxy(type, new ScriptedLocator());
	}

	/** Collects what a stream hands out. */
	private static final class Collecting implements Flow.Subscriber<Double> {
		final List<Double> taken = new CopyOnWriteArrayList<>();
		final CompletableFuture<Throwable> ended = new CompletableFuture<>();

		@Override
		public void onSubscribe(Flow.Subscription asked) {
			asked.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Double value) {
			taken.add(value);
		}

		@Override
		public void onError(Throwable failure) {
			ended.complete(failure);
		}

		@Override
		public void onComplete() {
			ended.complete(null);
		}
	}

	@Test
	void aMethodThatReturnsAPublisherHandsOutTheValuesOfTheStream() throws Exception {
		StreamingInvoker invoker = new StreamingInvoker();
		Collecting collecting = new Collecting();

		streaming(FlowBalances.class, invoker).getBalance("acc-1").subscribe(collecting);
		for (int i = 0; i < 100 && !invoker.values.hasSubscribers(); i++) {
			Thread.sleep(20);
		}
		invoker.values.submit(1.5);
		invoker.values.submit("2.5");
		invoker.values.close();

		assertThat(collecting.ended.get(5, TimeUnit.SECONDS)).isNull();
		assertThat(collecting.taken).as("a value that came as text is the number the method declares")
				.containsExactly(1.5, 2.5);
	}

	@Test
	void aMethodThatReturnsAPushStreamGetsOneFedFromTheSameStream() throws Exception {
		StreamingInvoker invoker = new StreamingInvoker();
		List<Double> taken = new CopyOnWriteArrayList<>();

		Promise<Void> done = streaming(PushedBalances.class, invoker).getBalance("acc-1").forEach(taken::add);
		for (int i = 0; i < 100 && !invoker.values.hasSubscribers(); i++) {
			Thread.sleep(20);
		}
		invoker.values.submit(1.5);
		invoker.values.submit(2.5);
		invoker.values.close();

		done.getValue();
		assertThat(taken).containsExactly(1.5, 2.5);
	}

	/** A declared error ends the stream as the class the method would throw; nothing is thrown at the call. */
	@Test
	void aStreamThatFailsEndsWithTheFailureAndTheCallItselfDoesNotThrow() throws Exception {
		StreamingInvoker invoker = new StreamingInvoker();
		Collecting collecting = new Collecting();
		DdsrException down = new DdsrException("the provider went away");

		streaming(FlowBalances.class, invoker).getBalance("acc-1").subscribe(collecting);
		for (int i = 0; i < 100 && !invoker.values.hasSubscribers(); i++) {
			Thread.sleep(20);
		}
		invoker.values.closeExceptionally(down);

		assertThat(collecting.ended.get(5, TimeUnit.SECONDS)).isSameAs(down);
	}

	@Test
	void aTransportThatCarriesNoStreamsSaysSoThroughTheStream() throws Exception {
		Collecting collecting = new Collecting();

		streaming(FlowBalances.class, new ScriptedInvoker()).getBalance("acc-1").subscribe(collecting);

		assertThat(collecting.ended.get(5, TimeUnit.SECONDS))
				.isInstanceOf(DdsrException.class).hasMessageContaining("carries no streams");
	}

	@Test
	void aStreamAsAnArgumentIsRefusedBeforeAnythingIsSent() {
		StreamingInvoker invoker = new StreamingInvoker();

		assertThatThrownBy(() -> streaming(FedBalances.class, invoker).getBalance(subscriber -> { }))
				.isInstanceOf(DdsrException.class).hasMessageContaining("takes a stream");
		assertThat(invoker.streamsAsked).isZero();
	}
}
