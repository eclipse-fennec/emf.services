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

import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.junit.jupiter.api.Test;

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
