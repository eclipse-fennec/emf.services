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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.fennec.services.DoubleProperty;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceInvocation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/**
 * The same cases as the TypeScript {@code invocation.test.ts}: the two
 * sides build the same document, because a call may cross languages.
 */
class InvocationsTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private ServiceOperation charge() {
		Parameter amount = F.createParameter();
		amount.setName("amount");
		amount.setType("double");
		amount.setIndex(0);
		Parameter times = F.createParameter();
		times.setName("times");
		times.setType("int");
		times.setIndex(1);

		ServiceOperation operation = F.createServiceOperation();
		operation.setName("charge");
		operation.getParameters().add(amount);
		operation.getParameters().add(times);

		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Payment");
		contract.getOperations().add(operation);
		return operation;
	}

	private static Map<String, Object> args(Object... pairs) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			arguments.put(String.valueOf(pairs[i]), pairs[i + 1]);
		}
		return arguments;
	}

	@Test
	void the_call_names_its_operation_and_its_parameters_by_reference() {
		ServiceInvocation invocation = Invocations.invocation(charge(), args("amount", 10.5, "times", 3));

		assertThat(Invocations.operationNameOf(invocation)).isEqualTo("charge");
		assertThat(invocation.getArguments()).hasSize(2);
		assertThat(invocation.getArguments().get(0).getParameter())
			.as("the parameter of the description that travels, not of the contract")
			.isSameAs(invocation.getOperation().getParameters().get(0));
	}

	@Test
	void the_description_travels_as_a_second_root() {
		ServiceInvocation invocation = Invocations.invocation(charge(), args("amount", 1.0));

		assertThat(Invocations.roots(invocation))
			.as("the references have to resolve inside the document")
			.containsExactly(invocation, invocation.getOperation());
	}

	@Test
	void the_contract_it_was_called_on_is_left_alone() {
		ServiceOperation operation = charge();
		ServiceInterface contract = (ServiceInterface) operation.eContainer();

		Invocations.invocation(operation, args("amount", 1.0));

		assertThat(operation.eContainer())
			.as("a call must not cost the caller the contract it just called")
			.isSameAs(contract);
		assertThat(operation.getParameters()).hasSize(2);
	}

	@Test
	void the_values_come_back_under_the_names_they_fill() {
		ServiceInvocation invocation = Invocations.invocation(charge(), args("amount", 10.5, "times", 3));

		assertThat(Invocations.argumentsOf(invocation))
			.containsEntry("amount", 10.5)
			.containsEntry("times", 3);
	}

	@Test
	void an_int_stays_an_int_because_the_contract_said_so() {
		ServiceInvocation invocation = Invocations.invocation(charge(), args("amount", 10, "times", 3));

		assertThat(invocation.getArguments().get(1).getValue())
			.as("a JSON envelope could not have said this")
			.isInstanceOf(IntProperty.class);
		assertThat(invocation.getArguments().get(0).getValue())
			.as("and 10 sent for a double parameter is a double, not an int")
			.isInstanceOf(DoubleProperty.class);
	}

	@Test
	void a_modelled_argument_travels_inside_the_message() {
		ServiceOperation store = F.createServiceOperation();
		store.setName("store");
		Parameter person = F.createParameter();
		person.setName("person");
		store.getParameters().add(person);
		ServiceInterface value = F.createServiceInterface();
		value.setName("Payment");

		ServiceInvocation invocation = Invocations.invocation(store, args("person", value));

		assertThat(Invocations.argumentsOf(invocation).get("person"))
			.as("a reference would point at something the receiver does not have")
			.isSameAs(value);
		assertThat(invocation.getArguments().get(0).getValue().eContents())
			.as("contained, so it is written with the message")
			.containsExactly(value);
	}

	@Test
	void an_answer_is_a_value_or_a_diagnostic() {
		assertThat(Invocations.valueOf(Invocations.result(990.0))).isEqualTo(990.0);
		assertThat(Invocations.valueOf(Invocations.result(null))).isNull();

		ServiceInvocationResult failed = Invocations.failure("account-1 is not known", 404);
		assertThat(failed.getDiagnostic().getMessage()).isEqualTo("account-1 is not known");
		assertThat(failed.getDiagnostic().getCode()).isEqualTo(404);
		assertThat(Invocations.valueOf(failed))
			.as("a failure has no value, and must not read as one")
			.isNull();
	}

	@Test
	void a_call_without_an_operation_is_refused() {
		assertThatThrownBy(() -> Invocations.invocation(null, args()))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
