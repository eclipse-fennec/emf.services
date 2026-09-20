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

/**
 * A call as a message, and its answer — `ServiceInvocation` and
 * `ServiceInvocationResult` from the model (#101).
 *
 * What the REST flavor spreads over path, query, header and body has to
 * have one form where a transport carries nothing but messages, and the
 * model has said what that form is since it was written. This module is
 * what finally fills it in: named arguments become `Argument`s whose
 * values are the `Property` that fits their type, and the answer is
 * either a value or a `Diagnostic`.
 *
 * Two things the message does NOT carry, on purpose and by the model's
 * own documentation: the correlation and the reply address. Those are
 * the envelope's, and since #101 there is an envelope to put them in.
 *
 * **The call description travels with the call.** `ServiceInvocation`
 * points at its operation and each `Argument` at its parameter, by
 * reference and not by name — so the document carries a second root
 * describing the operation being called, and the references resolve
 * inside it. That is the same self-contained shape every other document
 * on this wire has, and it is what lets a receiver read the values
 * without dereferencing anything.
 */

import type {
  Argument, Diagnostic, EObjectProperty, Parameter, Property,
  ServiceInvocation, ServiceInvocationResult, ServiceOperation,
} from '@ddsr/model';
import { DDSRFactory, DiagnosticSeverity } from '@ddsr/model';
import type { EObject } from '@emfts/core';
import { deserializeFromXmi, serializeToXmi } from '../xmi/xmi-support';
import { asRoots, eClassName, firstOfClass, toArray } from '../internal/emf-util';
import { propertyValue, props } from '../properties';

const factory = DDSRFactory.eINSTANCE;

/** What a decoded call says: which operation, and the values by name. */
export interface DecodedInvocation {
  operation: string;
  args: Record<string, unknown>;
}

/** What a decoded answer says: a value, or why there is none. */
export interface DecodedResult {
  value?: unknown;
  error?: string;
}

/** Whether this is a model object rather than a plain value. */
function isEObject(value: unknown): value is EObject {
  return typeof (value as { eClass?: unknown })?.eClass === 'function';
}

/**
 * The value, in the Property that fits it.
 *
 * <p>The parameter's declared type decides where it can — a `1` sent
 * for an `int` parameter must not arrive as a double, which is exactly
 * the kind of quiet difference a JSON envelope could not express. Where
 * the contract says nothing, the JavaScript value decides, and a number
 * is a double because that is what a JavaScript number is.
 */
export function propertyFor(name: string, value: unknown, declaredType?: string): Property {
  if (isEObject(value)) {
    const property = factory.createEObjectProperty();
    property.name = name;
    property.value = value;
    return property;
  }
  switch ((declaredType ?? '').toLowerCase()) {
    case 'int':
    case 'integer':
      return props.int(name, Number(value));
    case 'long':
      return props.long(name, Number(value));
    case 'short':
      return props.short(name, Number(value));
    case 'float':
      return props.float(name, Number(value));
    case 'double':
      return props.double(name, Number(value));
    case 'boolean':
    case 'bool':
      return props.bool(name, value === true || value === 'true');
    case 'string':
      return props.string(name, String(value));
    default:
      break;
  }
  if (typeof value === 'boolean') return props.bool(name, value);
  if (typeof value === 'number') return props.double(name, value);
  if (Array.isArray(value)) return props.stringList(name, value.map(String));
  return props.string(name, String(value));
}

/** The value a Property carries, models included. */
export function valueOf(property: Property | undefined): unknown {
  if (!property) return undefined;
  if (eClassName(property) === 'EObjectProperty') {
    return (property as EObjectProperty).value;
  }
  return propertyValue(property);
}

/**
 * A description of the operation being called: its name and the
 * parameters the call fills, copied out of the contract.
 *
 * <p>A copy rather than the contract's own objects, because pushing a
 * contained object into a resource takes it OUT of its container — the
 * consumer's locator would lose the operation it just called. A copy
 * costs a few lines on the wire and nothing else: what the receiver
 * needs is the names and the order, and those are what a copy has.
 */
function describe(operation: ServiceOperation, args: Record<string, unknown>): ServiceOperation {
  const description = factory.createServiceOperation();
  description.name = operation.name;
  const declared = toArray<Parameter>(operation.parameters);
  let index = 0;
  for (const name of Object.keys(args)) {
    const source = declared.find(p => p.name === name);
    const parameter = factory.createParameter();
    parameter.name = name;
    parameter.index = source?.index ?? index;
    if (source?.type) parameter.type = source.type;
    description.parameters.push(parameter);
    index += 1;
  }
  return description;
}

/** One call, as the document that travels. */
export function encodeInvocation(
  operation: ServiceOperation, args: Record<string, unknown>,
): string {
  const description = describe(operation, args);
  const declared = toArray<Parameter>(operation.parameters);
  const invocation: ServiceInvocation = factory.createServiceInvocation();
  invocation.operation = description;
  for (const parameter of toArray<Parameter>(description.parameters)) {
    const argument: Argument = factory.createArgument();
    argument.parameter = parameter;
    const declaredType = declared.find(p => p.name === parameter.name)?.type ?? parameter.type;
    argument.value = propertyFor(parameter.name!, args[parameter.name!], declaredType);
    invocation.arguments.push(argument);
  }
  return serializeToXmi(invocation as unknown as EObject, description as unknown as EObject);
}

/** The inverse: which operation, and the values by the names they fill. */
export function decodeInvocation(document: string): DecodedInvocation {
  const invocation = firstOfClass<ServiceInvocation>(
    asRoots(deserializeFromXmi(document)), 'ServiceInvocation');
  if (!invocation) throw new Error('the message carries no ServiceInvocation');
  const args: Record<string, unknown> = {};
  for (const argument of toArray<Argument>(invocation.arguments)) {
    const name = argument.parameter?.name;
    if (name) args[name] = valueOf(argument.value);
  }
  return { operation: invocation.operation?.name ?? '', args };
}

/** An answer that has a value — or none, for an operation that returns nothing. */
export function encodeResult(value: unknown): string {
  const result: ServiceInvocationResult = factory.createServiceInvocationResult();
  if (value !== undefined && value !== null) {
    result.value = propertyFor('result', value);
  }
  // A model value rides inside the EObjectProperty, which contains it
  // — the value travels with the message, because a reference would
  // point at something the receiver does not have.
  return serializeToXmi(result as unknown as EObject);
}

/**
 * An answer that failed. A Diagnostic because that is how this registry
 * reports failures everywhere — a transport turns it into a status
 * where it has one, and MQTT has none.
 */
export function encodeFailure(message: string, code = 0): string {
  const result: ServiceInvocationResult = factory.createServiceInvocationResult();
  const diagnostic: Diagnostic = factory.createDiagnostic();
  diagnostic.severity = DiagnosticSeverity.ERROR;
  diagnostic.message = message;
  diagnostic.code = code;
  result.diagnostic = diagnostic;
  return serializeToXmi(result as unknown as EObject);
}

/** The inverse of both: a value, or the message of the failure. */
export function decodeResult(document: string): DecodedResult {
  const result = firstOfClass<ServiceInvocationResult>(
    asRoots(deserializeFromXmi(document)), 'ServiceInvocationResult');
  if (!result) throw new Error('the message carries no ServiceInvocationResult');
  if (result.diagnostic) {
    return { error: result.diagnostic.message ?? 'the call failed without a message' };
  }
  return { value: valueOf(result.value) };
}
