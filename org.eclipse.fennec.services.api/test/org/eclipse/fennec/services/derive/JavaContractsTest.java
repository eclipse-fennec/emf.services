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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;

import org.eclipse.fennec.services.Delivery;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.junit.jupiter.api.Test;
import org.osgi.util.promise.Promise;
import org.osgi.util.pushstream.PushStream;

/**
 * What a Java interface says, read as a contract.
 *
 * <p>The derived contract is identified by its fingerprint, so the
 * interesting question is not only "is it right" but "is it the same
 * every time" — reflection gives no order, and a contract that changes
 * between runs would make every consumer's fingerprint check fail at
 * random.
 */
class JavaContractsTest {

    /** Deliberately not in alphabetical order, so sorting is doing work. */
    interface Payment {
        double charge(double amount, String currency);

        double getBalance(String accountId);

        void cancel(String reference) throws PaymentDeclined, AccountUnknown;
    }

    interface Slots {
        List<String> names(int[] codes, Integer limit, ServiceInterface contract);
    }

    interface Overloaded {
        double charge(double amount);

        double charge(double amount, String currency);
    }

    interface Untyped {
        Object anything();
    }

    interface Raw {
        List<?> everything();
    }

    static class PaymentDeclined extends Exception {
        private static final long serialVersionUID = 1L;
    }

    static class AccountUnknown extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static ServiceOperation operation(ServiceInterface contract, String name) {
        return contract.getOperations().stream()
                .filter(o -> name.equals(o.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no operation " + name));
    }

    /** The same contract as {@link Payment}, by a service that answers later (osgi.async). */
    interface LaterPayment {
        Promise<Double> charge(double amount, String currency);

        CompletableFuture<Double> getBalance(String accountId);

        Future<Void> cancel(String accountId);
    }

    /**
     * That an answer comes later is each end's own business: to a
     * contract Promise&lt;Double&gt; returns a double. A consumer written
     * against the synchronous interface and a provider written against
     * the asynchronous one therefore describe — and find — the same
     * contract.
     */
    @Test
    void aMethodThatAnswersLaterReturnsWhatItsHolderHolds() {
        ServiceInterface later = JavaContracts.contractOf(LaterPayment.class, "1.0.0");

        assertThat(operation(later, "charge").getReturnValue().getType()).isEqualTo("double");
        assertThat(operation(later, "charge").getReturnValue().getDelivery())
                .as("said in the contract, for whoever generates code or sets a timeout")
                .isEqualTo(Delivery.PROMISE);
        assertThat(operation(later, "getBalance").getReturnValue().getType()).isEqualTo("double");
        assertThat(operation(later, "cancel").getReturnValue())
                .as("a holder of Void returns nothing").isNull();
    }

    /** Values that arrive over time, in both directions. */
    interface Sensor {
        PushStream<Double> readings(String sensorId);

        Flow.Publisher<Double> flow(String sensorId);

        void feed(String sensorId, PushStream<Double> samples);

        List<Double> history(String sensorId);
    }

    interface StreamOfWhat {
        @SuppressWarnings("rawtypes")
        PushStream anything();
    }

    /**
     * Parameter.delivery: a stream is a slot whose values come one after
     * the other. PushStream and the JDK's Flow.Publisher both say so, on
     * a return value and on an argument alike — and a list, which
     * arrives complete, is not one.
     */
    @Test
    void aStreamIsAStreamInTheContractWhicheverJavaTypeSaysSo() {
        ServiceInterface sensor = JavaContracts.contractOf(Sensor.class, "1.0.0");

        for (String operation : new String[] { "readings", "flow" }) {
            Parameter result = operation(sensor, operation).getReturnValue();
            assertThat(result.getDelivery()).as(operation).isEqualTo(Delivery.STREAM);
            assertThat(result.getType()).as(operation).isEqualTo("double");
            assertThat(result.getUpperBound()).as("a stream is unbounded").isEqualTo(-1);
        }
        Parameter samples = operation(sensor, "feed").getParameters().get(1);
        assertThat(samples.getDelivery()).as("a stream the caller hands over").isEqualTo(Delivery.STREAM);
        assertThat(operation(sensor, "history").getReturnValue().getDelivery())
                .as("a list arrives complete").isEqualTo(Delivery.VALUE);
    }

    @Test
    void aStreamHasToSayWhatFlowsInIt() {
        assertThatThrownBy(() -> JavaContracts.contractOf(StreamOfWhat.class, "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("stream");
    }

    @Test
    void theContractIsNamedAfterTheInterface() {
        ServiceInterface contract = JavaContracts.contractOf(Payment.class, "1.2.3");

        assertThat(contract.getName()).isEqualTo("Payment");
        assertThat(contract.getVersion()).isEqualTo("1.2.3");
    }

    @Test
    void operationsAreOrderedSoTheContractIsTheSameEveryTime() {
        // Class.getMethods() has no defined order. Sorting by name is
        // what makes the fingerprint of a derived contract reproducible.
        assertThat(JavaContracts.contractOf(Payment.class, "1.0.0").getOperations())
                .extracting(ServiceOperation::getName)
                .containsExactly("cancel", "charge", "getBalance");
    }

    @Test
    void aParameterKeepsItsPositionAndItsType() {
        ServiceOperation charge = operation(JavaContracts.contractOf(Payment.class, "1.0.0"), "charge");

        assertThat(charge.getParameters())
                .extracting(Parameter::getIndex, Parameter::getType, Parameter::isOptional)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(0, "double", false),
                        org.assertj.core.groups.Tuple.tuple(1, "string", true));
    }

    @Test
    void aValueThatCanBeNullIsOptional() {
        // The unboxed double cannot be absent, the boxed Integer can —
        // that is what optional says, and it is the same reading the
        // templates use in the other direction.
        ServiceOperation names = operation(JavaContracts.contractOf(Slots.class, "1.0.0"), "names");

        assertThat(names.getParameters()).extracting(Parameter::getName, Parameter::isOptional)
                .contains(org.assertj.core.groups.Tuple.tuple("arg1", true));
    }

    @Test
    void aReturnValueIsASlotLikeAnyOther() {
        ServiceOperation charge = operation(JavaContracts.contractOf(Payment.class, "1.0.0"), "charge");

        assertThat(charge.getReturnValue()).isNotNull();
        assertThat(charge.getReturnValue().getType()).isEqualTo("double");
    }

    @Test
    void voidHasNoReturnValue() {
        assertThat(operation(JavaContracts.contractOf(Payment.class, "1.0.0"), "cancel").getReturnValue()).isNull();
    }

    @Test
    void aCollectionOrArrayIsAMultiValuedSlot() {
        ServiceOperation names = operation(JavaContracts.contractOf(Slots.class, "1.0.0"), "names");

        assertThat(names.getReturnValue().getUpperBound()).isEqualTo(-1);
        assertThat(names.getReturnValue().getType()).isEqualTo("string");
        assertThat(names.getParameters().get(0).getUpperBound()).isEqualTo(-1);
        assertThat(names.getParameters().get(0).getType()).isEqualTo("int");
    }

    @Test
    void aModelledTypeTravelsAsItself() {
        ServiceOperation names = operation(
                JavaContracts.contractOf(Slots.class, "1.0.0",
                        type -> ServicesPackage.eINSTANCE.getServiceInterface()),
                "names");

        assertThat(names.getParameters().get(2).getEType())
                .isSameAs(ServicesPackage.eINSTANCE.getServiceInterface());
    }

    @Test
    void whatAnOperationThrowsBecomesAnErrorOfTheContract() {
        ServiceInterface contract = JavaContracts.contractOf(Payment.class, "1.0.0");

        assertThat(contract.getExceptions()).extracting("name")
                .containsExactly("AccountUnknown", "PaymentDeclined");
        assertThat(operation(contract, "cancel").getExceptions())
                .as("the operation names the ones it raises")
                .containsExactlyElementsOf(contract.getExceptions());
        assertThat(contract.getExceptions().get(0).getType())
                .as("symbolic on the wire, not a Java class name")
                .isEqualTo("AccountUnknown");
    }

    @Test
    void anOverloadedInterfaceIsRefusedRatherThanDerivedAmbiguously() {
        assertThatThrownBy(() -> JavaContracts.contractOf(Overloaded.class, "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("charge")
                .hasMessageContaining("more than once");
    }

    @Test
    void aTypeTheModelDoesNotKnowIsRefusedWithTheSlotThatCausedIt() {
        assertThatThrownBy(() -> JavaContracts.contractOf(Untyped.class, "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Untyped.anything")
                .hasMessageContaining("java.lang.Object");
    }

    @Test
    void aCollectionWithoutAnElementTypeIsRefused() {
        assertThatThrownBy(() -> JavaContracts.contractOf(Raw.class, "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("what travels in it");
    }

    @Test
    void theFingerprintOfADerivedContractIsPinned() {
        // Not a hand-computed expectation — the value is whatever the
        // rules produce. What it pins is that it does not move: a
        // consumer built against a derived contract carries this string,
        // and a JVM that returned methods in another order, or a change
        // to the derivation rules, would leave every such consumer
        // unable to bind. If this fails, the question is whether the
        // contract was MEANT to change.
        assertThat(ServiceDescriptionFingerprint.fingerprint(JavaContracts.contractOf(Payment.class, "1.0.0")))
                .isEqualTo("sd1:cf1462e195397dec8e97596924542e044ef69607aa40419435dc25cc292e6b39");
    }

    @Test
    void derivingTwiceGivesTheSameContract() {
        assertThat(ServiceDescriptionFingerprint.fingerprint(JavaContracts.contractOf(Payment.class, "1.0.0")))
                .isEqualTo(ServiceDescriptionFingerprint.fingerprint(JavaContracts.contractOf(Payment.class, "1.0.0")));
    }

    @Test
    void onlyAnInterfaceHasAContract() {
        assertThatThrownBy(() -> JavaContracts.contractOf(String.class, "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("derived from an interface");
        assertThatThrownBy(() -> JavaContracts.contractOf(Payment.class, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("needs a version");
    }
}
