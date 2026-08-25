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
 * A representation of the literals of the enumeration '<em><b>Component State</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Runtime state of a ComponentConfiguration. Values follow ComponentConfigurationDTO and are bitwise-combinable so the same int can express composite states in transit. UNSATISFIED_CONFIGURATION = required configuration missing; UNSATISFIED_REFERENCE = required service missing; SATISFIED = all dependencies present; ACTIVE = activate() succeeded; FAILED_ACTIVATION = activate() threw — failure detail in ComponentConfiguration.failure.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getComponentState()
 * @model
 * @generated
 */
@ProviderType
public enum ComponentState implements Enumerator {
	/**
	 * The '<em><b>UNSATISFIED CONFIGURATION</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNSATISFIED_CONFIGURATION_VALUE
	 * @generated
	 * @ordered
	 */
	UNSATISFIED_CONFIGURATION(1, "UNSATISFIED_CONFIGURATION", "UNSATISFIED_CONFIGURATION"),

	/**
	 * The '<em><b>UNSATISFIED REFERENCE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNSATISFIED_REFERENCE_VALUE
	 * @generated
	 * @ordered
	 */
	UNSATISFIED_REFERENCE(2, "UNSATISFIED_REFERENCE", "UNSATISFIED_REFERENCE"),

	/**
	 * The '<em><b>SATISFIED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SATISFIED_VALUE
	 * @generated
	 * @ordered
	 */
	SATISFIED(4, "SATISFIED", "SATISFIED"),

	/**
	 * The '<em><b>ACTIVE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ACTIVE_VALUE
	 * @generated
	 * @ordered
	 */
	ACTIVE(8, "ACTIVE", "ACTIVE"),

	/**
	 * The '<em><b>FAILED ACTIVATION</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #FAILED_ACTIVATION_VALUE
	 * @generated
	 * @ordered
	 */
	FAILED_ACTIVATION(16, "FAILED_ACTIVATION", "FAILED_ACTIVATION");

	/**
	 * The '<em><b>UNSATISFIED CONFIGURATION</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNSATISFIED_CONFIGURATION
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int UNSATISFIED_CONFIGURATION_VALUE = 1;

	/**
	 * The '<em><b>UNSATISFIED REFERENCE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNSATISFIED_REFERENCE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int UNSATISFIED_REFERENCE_VALUE = 2;

	/**
	 * The '<em><b>SATISFIED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SATISFIED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int SATISFIED_VALUE = 4;

	/**
	 * The '<em><b>ACTIVE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ACTIVE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ACTIVE_VALUE = 8;

	/**
	 * The '<em><b>FAILED ACTIVATION</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #FAILED_ACTIVATION
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int FAILED_ACTIVATION_VALUE = 16;

	/**
	 * An array of all the '<em><b>Component State</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ComponentState[] VALUES_ARRAY =
		new ComponentState[] {
			UNSATISFIED_CONFIGURATION,
			UNSATISFIED_REFERENCE,
			SATISFIED,
			ACTIVE,
			FAILED_ACTIVATION,
		};

	/**
	 * A public read-only list of all the '<em><b>Component State</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<ComponentState> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Component State</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ComponentState get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ComponentState result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Component State</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ComponentState getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ComponentState result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Component State</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ComponentState get(int value) {
		switch (value) {
			case UNSATISFIED_CONFIGURATION_VALUE: return UNSATISFIED_CONFIGURATION;
			case UNSATISFIED_REFERENCE_VALUE: return UNSATISFIED_REFERENCE;
			case SATISFIED_VALUE: return SATISFIED;
			case ACTIVE_VALUE: return ACTIVE;
			case FAILED_ACTIVATION_VALUE: return FAILED_ACTIVATION;
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
	private ComponentState(int value, String name, String literal) {
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
	
} //ComponentState
