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
 * A representation of the literals of the enumeration '<em><b>Reference Cardinality</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Cardinality of a ComponentReference. ZERO_OR_ONE = 0..1; ONE = 1..1; ZERO_OR_MANY = 0..*; ONE_OR_MANY = 1..*. Drives whether a missing reference leaves a component UNSATISFIED.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getReferenceCardinality()
 * @model
 * @generated
 */
@ProviderType
public enum ReferenceCardinality implements Enumerator {
	/**
	 * The '<em><b>ZERO OR ONE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ZERO_OR_ONE_VALUE
	 * @generated
	 * @ordered
	 */
	ZERO_OR_ONE(0, "ZERO_OR_ONE", "ZERO_OR_ONE"),

	/**
	 * The '<em><b>ONE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ONE_VALUE
	 * @generated
	 * @ordered
	 */
	ONE(1, "ONE", "ONE"),

	/**
	 * The '<em><b>ZERO OR MANY</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ZERO_OR_MANY_VALUE
	 * @generated
	 * @ordered
	 */
	ZERO_OR_MANY(2, "ZERO_OR_MANY", "ZERO_OR_MANY"),

	/**
	 * The '<em><b>ONE OR MANY</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ONE_OR_MANY_VALUE
	 * @generated
	 * @ordered
	 */
	ONE_OR_MANY(3, "ONE_OR_MANY", "ONE_OR_MANY");

	/**
	 * The '<em><b>ZERO OR ONE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ZERO_OR_ONE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ZERO_OR_ONE_VALUE = 0;

	/**
	 * The '<em><b>ONE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ONE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ONE_VALUE = 1;

	/**
	 * The '<em><b>ZERO OR MANY</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ZERO_OR_MANY
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ZERO_OR_MANY_VALUE = 2;

	/**
	 * The '<em><b>ONE OR MANY</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ONE_OR_MANY
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ONE_OR_MANY_VALUE = 3;

	/**
	 * An array of all the '<em><b>Reference Cardinality</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ReferenceCardinality[] VALUES_ARRAY =
		new ReferenceCardinality[] {
			ZERO_OR_ONE,
			ONE,
			ZERO_OR_MANY,
			ONE_OR_MANY,
		};

	/**
	 * A public read-only list of all the '<em><b>Reference Cardinality</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<ReferenceCardinality> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Reference Cardinality</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReferenceCardinality get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ReferenceCardinality result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Reference Cardinality</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReferenceCardinality getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ReferenceCardinality result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Reference Cardinality</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReferenceCardinality get(int value) {
		switch (value) {
			case ZERO_OR_ONE_VALUE: return ZERO_OR_ONE;
			case ONE_VALUE: return ONE;
			case ZERO_OR_MANY_VALUE: return ZERO_OR_MANY;
			case ONE_OR_MANY_VALUE: return ONE_OR_MANY;
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
	private ReferenceCardinality(int value, String name, String literal) {
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
	
} //ReferenceCardinality
