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
 * A representation of the literals of the enumeration '<em><b>Lifecycle Hook Kind</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Kind of lifecycle callback a component declares. Language-neutral renaming of OSGi DS activate/deactivate/modified/activation-fields/init.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getLifecycleHookKind()
 * @model
 * @generated
 */
@ProviderType
public enum LifecycleHookKind implements Enumerator {
	/**
	 * The '<em><b>ACTIVATE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ACTIVATE_VALUE
	 * @generated
	 * @ordered
	 */
	ACTIVATE(0, "ACTIVATE", "ACTIVATE"),

	/**
	 * The '<em><b>DEACTIVATE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DEACTIVATE_VALUE
	 * @generated
	 * @ordered
	 */
	DEACTIVATE(1, "DEACTIVATE", "DEACTIVATE"),

	/**
	 * The '<em><b>MODIFIED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #MODIFIED_VALUE
	 * @generated
	 * @ordered
	 */
	MODIFIED(2, "MODIFIED", "MODIFIED"),

	/**
	 * The '<em><b>ACTIVATION FIELD</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ACTIVATION_FIELD_VALUE
	 * @generated
	 * @ordered
	 */
	ACTIVATION_FIELD(3, "ACTIVATION_FIELD", "ACTIVATION_FIELD");

	/**
	 * The '<em><b>ACTIVATE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ACTIVATE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ACTIVATE_VALUE = 0;

	/**
	 * The '<em><b>DEACTIVATE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DEACTIVATE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int DEACTIVATE_VALUE = 1;

	/**
	 * The '<em><b>MODIFIED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #MODIFIED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int MODIFIED_VALUE = 2;

	/**
	 * The '<em><b>ACTIVATION FIELD</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ACTIVATION_FIELD
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ACTIVATION_FIELD_VALUE = 3;

	/**
	 * An array of all the '<em><b>Lifecycle Hook Kind</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final LifecycleHookKind[] VALUES_ARRAY =
		new LifecycleHookKind[] {
			ACTIVATE,
			DEACTIVATE,
			MODIFIED,
			ACTIVATION_FIELD,
		};

	/**
	 * A public read-only list of all the '<em><b>Lifecycle Hook Kind</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<LifecycleHookKind> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Lifecycle Hook Kind</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static LifecycleHookKind get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			LifecycleHookKind result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Lifecycle Hook Kind</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static LifecycleHookKind getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			LifecycleHookKind result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Lifecycle Hook Kind</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static LifecycleHookKind get(int value) {
		switch (value) {
			case ACTIVATE_VALUE: return ACTIVATE;
			case DEACTIVATE_VALUE: return DEACTIVATE;
			case MODIFIED_VALUE: return MODIFIED;
			case ACTIVATION_FIELD_VALUE: return ACTIVATION_FIELD;
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
	private LifecycleHookKind(int value, String name, String literal) {
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
	
} //LifecycleHookKind
