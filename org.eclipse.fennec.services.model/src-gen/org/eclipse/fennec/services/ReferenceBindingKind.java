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
 * A representation of the literals of the enumeration '<em><b>Reference Binding Kind</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Kind of binding callback for a ComponentReference. BIND = called when a target is bound; UNBIND = called when a target is unbound; UPDATED = called when target properties change; FIELD = the reference targets a field, not a method.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getReferenceBindingKind()
 * @model
 * @generated
 */
@ProviderType
public enum ReferenceBindingKind implements Enumerator {
	/**
	 * The '<em><b>BIND</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BIND_VALUE
	 * @generated
	 * @ordered
	 */
	BIND(0, "BIND", "BIND"),

	/**
	 * The '<em><b>UNBIND</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNBIND_VALUE
	 * @generated
	 * @ordered
	 */
	UNBIND(1, "UNBIND", "UNBIND"),

	/**
	 * The '<em><b>UPDATED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UPDATED_VALUE
	 * @generated
	 * @ordered
	 */
	UPDATED(2, "UPDATED", "UPDATED"),

	/**
	 * The '<em><b>FIELD</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #FIELD_VALUE
	 * @generated
	 * @ordered
	 */
	FIELD(3, "FIELD", "FIELD");

	/**
	 * The '<em><b>BIND</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BIND
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int BIND_VALUE = 0;

	/**
	 * The '<em><b>UNBIND</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNBIND
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int UNBIND_VALUE = 1;

	/**
	 * The '<em><b>UPDATED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UPDATED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int UPDATED_VALUE = 2;

	/**
	 * The '<em><b>FIELD</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #FIELD
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int FIELD_VALUE = 3;

	/**
	 * An array of all the '<em><b>Reference Binding Kind</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ReferenceBindingKind[] VALUES_ARRAY =
		new ReferenceBindingKind[] {
			BIND,
			UNBIND,
			UPDATED,
			FIELD,
		};

	/**
	 * A public read-only list of all the '<em><b>Reference Binding Kind</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<ReferenceBindingKind> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Reference Binding Kind</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReferenceBindingKind get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ReferenceBindingKind result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Reference Binding Kind</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReferenceBindingKind getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ReferenceBindingKind result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Reference Binding Kind</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReferenceBindingKind get(int value) {
		switch (value) {
			case BIND_VALUE: return BIND;
			case UNBIND_VALUE: return UNBIND;
			case UPDATED_VALUE: return UPDATED;
			case FIELD_VALUE: return FIELD;
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
	private ReferenceBindingKind(int value, String name, String literal) {
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
	
} //ReferenceBindingKind
