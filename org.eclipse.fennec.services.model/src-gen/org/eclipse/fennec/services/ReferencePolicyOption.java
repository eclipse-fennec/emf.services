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
 * A representation of the literals of the enumeration '<em><b>Reference Policy Option</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Reference rebinding eagerness. RELUCTANT = keep current binding even if a higher-ranked target appears; GREEDY = switch to a higher-ranked target.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getReferencePolicyOption()
 * @model
 * @generated
 */
@ProviderType
public enum ReferencePolicyOption implements Enumerator {
	/**
	 * The '<em><b>RELUCTANT</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RELUCTANT_VALUE
	 * @generated
	 * @ordered
	 */
	RELUCTANT(0, "RELUCTANT", "RELUCTANT"),

	/**
	 * The '<em><b>GREEDY</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #GREEDY_VALUE
	 * @generated
	 * @ordered
	 */
	GREEDY(1, "GREEDY", "GREEDY");

	/**
	 * The '<em><b>RELUCTANT</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RELUCTANT
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int RELUCTANT_VALUE = 0;

	/**
	 * The '<em><b>GREEDY</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #GREEDY
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int GREEDY_VALUE = 1;

	/**
	 * An array of all the '<em><b>Reference Policy Option</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ReferencePolicyOption[] VALUES_ARRAY =
		new ReferencePolicyOption[] {
			RELUCTANT,
			GREEDY,
		};

	/**
	 * A public read-only list of all the '<em><b>Reference Policy Option</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<ReferencePolicyOption> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Reference Policy Option</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReferencePolicyOption get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ReferencePolicyOption result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Reference Policy Option</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReferencePolicyOption getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ReferencePolicyOption result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Reference Policy Option</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReferencePolicyOption get(int value) {
		switch (value) {
			case RELUCTANT_VALUE: return RELUCTANT;
			case GREEDY_VALUE: return GREEDY;
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
	private ReferencePolicyOption(int value, String name, String literal) {
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
	
} //ReferencePolicyOption
