/*
 */
package org.eclipse.fennec.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Delivery</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * How the value of a slot arrives (Parameter.delivery). VALUE = at once and complete; the default, so a slot that says nothing is a value and the literal is not serialized. PROMISE = one value that arrives later, as an event: the operation may take its time, and a caller should not wait for it on a thread or with a short timeout. Only a return value can be a promise. It says WHEN the value comes and nothing about what it is, so it is not part of the sd1 fingerprint — the same contract may be read synchronously. STREAM = any number of values, one after the other, over time: their number is not known in advance, the stream ends or fails, and either end may stop it. A return value that is a stream flows from the provider to the caller, an argument that is one flows from the caller to the provider, and an operation may have both. STREAM changes what is exchanged and is part of the sd1 fingerprint. What the kinds are called in a language is the language's: Java PushStream or Flow.Publisher for a stream and Promise or CompletionStage for a promise, TypeScript AsyncIterable and Promise, Python AsyncIterator and Awaitable. BROADCAST = a stream that one source sends to every receiver at once, at its own pace (#252): it is subscribed to, not called, so it has no arguments and no session, and only a return value can be one. The source never learns how fast a receiver is — there is no backpressure — and each receiver protects itself with a buffer whose overflow the contract states (Parameter.overflow). BROADCAST is part of the sd1 fingerprint, together with the overflow policy.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getDelivery()
 * @model
 * @generated
 */
@ProviderType
public enum Delivery implements Enumerator {
	/**
	 * The '<em><b>VALUE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #VALUE_VALUE
	 * @generated
	 * @ordered
	 */
	VALUE(0, "VALUE", "VALUE"),

	/**
	 * The '<em><b>PROMISE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #PROMISE_VALUE
	 * @generated
	 * @ordered
	 */
	PROMISE(1, "PROMISE", "PROMISE"),

	/**
	 * The '<em><b>STREAM</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #STREAM_VALUE
	 * @generated
	 * @ordered
	 */
	STREAM(2, "STREAM", "STREAM"),

	/**
	 * The '<em><b>BROADCAST</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BROADCAST_VALUE
	 * @generated
	 * @ordered
	 */
	BROADCAST(3, "BROADCAST", "BROADCAST");

	/**
	 * The '<em><b>VALUE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #VALUE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int VALUE_VALUE = 0;

	/**
	 * The '<em><b>PROMISE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #PROMISE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int PROMISE_VALUE = 1;

	/**
	 * The '<em><b>STREAM</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #STREAM
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int STREAM_VALUE = 2;

	/**
	 * The '<em><b>BROADCAST</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BROADCAST
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int BROADCAST_VALUE = 3;

	/**
	 * An array of all the '<em><b>Delivery</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final Delivery[] VALUES_ARRAY =
		new Delivery[] {
			VALUE,
			PROMISE,
			STREAM,
			BROADCAST,
		};

	/**
	 * A public read-only list of all the '<em><b>Delivery</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<Delivery> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Delivery</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static Delivery get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			Delivery result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Delivery</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static Delivery getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			Delivery result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Delivery</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static Delivery get(int value) {
		switch (value) {
			case VALUE_VALUE: return VALUE;
			case PROMISE_VALUE: return PROMISE;
			case STREAM_VALUE: return STREAM;
			case BROADCAST_VALUE: return BROADCAST;
		}
		return null;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final int value;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String name;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String literal;

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private Delivery(int value, String name, String literal) {
		this.value = value;
		this.name = name;
		this.literal = literal;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getValue() {
	  return value;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName() {
	  return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getLiteral() {
	  return literal;
	}

	/**
	 * Returns the literal value of the enumerator, which is its string representation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		return literal;
	}
	
} //Delivery
