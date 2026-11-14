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
 * A representation of the literals of the enumeration '<em><b>Overflow Policy</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * What a receiver of a BROADCAST does when its buffer is full (Parameter.overflow). The values follow the per-subscription buffer of Gecko Messaging. FAIL = the receiver must not lose a value: its subscription ends with an error and it starts over, e.g. from a snapshot; the default, so a contract that says nothing promises no loss. DISCARD_OLDEST = latest wins: the oldest buffered value is dropped for the new one, for readings where only the current value counts. BLOCK = the receiver holds up its own delivery until there is room; nothing is lost, but a messaging broker may drop or disconnect a receiver that stays slow. A receiver may make the policy stricter (replace DISCARD_OLDEST with FAIL or BLOCK), never looser.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getOverflowPolicy()
 * @model
 * @generated
 */
@ProviderType
public enum OverflowPolicy implements Enumerator {
	/**
	 * The '<em><b>FAIL</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #FAIL_VALUE
	 * @generated
	 * @ordered
	 */
	FAIL(0, "FAIL", "FAIL"),

	/**
	 * The '<em><b>DISCARD OLDEST</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DISCARD_OLDEST_VALUE
	 * @generated
	 * @ordered
	 */
	DISCARD_OLDEST(1, "DISCARD_OLDEST", "DISCARD_OLDEST"),

	/**
	 * The '<em><b>BLOCK</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BLOCK_VALUE
	 * @generated
	 * @ordered
	 */
	BLOCK(2, "BLOCK", "BLOCK");

	/**
	 * The '<em><b>FAIL</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #FAIL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int FAIL_VALUE = 0;

	/**
	 * The '<em><b>DISCARD OLDEST</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DISCARD_OLDEST
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int DISCARD_OLDEST_VALUE = 1;

	/**
	 * The '<em><b>BLOCK</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BLOCK
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int BLOCK_VALUE = 2;

	/**
	 * An array of all the '<em><b>Overflow Policy</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final OverflowPolicy[] VALUES_ARRAY =
		new OverflowPolicy[] {
			FAIL,
			DISCARD_OLDEST,
			BLOCK,
		};

	/**
	 * A public read-only list of all the '<em><b>Overflow Policy</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<OverflowPolicy> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Overflow Policy</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static OverflowPolicy get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			OverflowPolicy result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Overflow Policy</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static OverflowPolicy getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			OverflowPolicy result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Overflow Policy</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static OverflowPolicy get(int value) {
		switch (value) {
			case FAIL_VALUE: return FAIL;
			case DISCARD_OLDEST_VALUE: return DISCARD_OLDEST;
			case BLOCK_VALUE: return BLOCK;
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
	private OverflowPolicy(int value, String name, String literal) {
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
	
} //OverflowPolicy
