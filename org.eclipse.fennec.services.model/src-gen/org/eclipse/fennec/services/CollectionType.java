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
 * A representation of the literals of the enumeration '<em><b>Collection Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Element type expected by a reference field/collection. SERVICE = injected service objects; REFERENCE = ServiceReferences; SERVICEOBJECTS = ComponentServiceObjects-like accessors; PROPERTIES = property maps; TUPLE = (props, service) tuples.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getCollectionType()
 * @model
 * @generated
 */
@ProviderType
public enum CollectionType implements Enumerator {
	/**
	 * The '<em><b>SERVICE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SERVICE_VALUE
	 * @generated
	 * @ordered
	 */
	SERVICE(0, "SERVICE", "SERVICE"),

	/**
	 * The '<em><b>REFERENCE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REFERENCE_VALUE
	 * @generated
	 * @ordered
	 */
	REFERENCE(1, "REFERENCE", "REFERENCE"),

	/**
	 * The '<em><b>SERVICEOBJECTS</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SERVICEOBJECTS_VALUE
	 * @generated
	 * @ordered
	 */
	SERVICEOBJECTS(2, "SERVICEOBJECTS", "SERVICEOBJECTS"),

	/**
	 * The '<em><b>PROPERTIES</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #PROPERTIES_VALUE
	 * @generated
	 * @ordered
	 */
	PROPERTIES(3, "PROPERTIES", "PROPERTIES"),

	/**
	 * The '<em><b>TUPLE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TUPLE_VALUE
	 * @generated
	 * @ordered
	 */
	TUPLE(4, "TUPLE", "TUPLE");

	/**
	 * The '<em><b>SERVICE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SERVICE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int SERVICE_VALUE = 0;

	/**
	 * The '<em><b>REFERENCE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REFERENCE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int REFERENCE_VALUE = 1;

	/**
	 * The '<em><b>SERVICEOBJECTS</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SERVICEOBJECTS
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int SERVICEOBJECTS_VALUE = 2;

	/**
	 * The '<em><b>PROPERTIES</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #PROPERTIES
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int PROPERTIES_VALUE = 3;

	/**
	 * The '<em><b>TUPLE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TUPLE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int TUPLE_VALUE = 4;

	/**
	 * An array of all the '<em><b>Collection Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final CollectionType[] VALUES_ARRAY =
		new CollectionType[] {
			SERVICE,
			REFERENCE,
			SERVICEOBJECTS,
			PROPERTIES,
			TUPLE,
		};

	/**
	 * A public read-only list of all the '<em><b>Collection Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<CollectionType> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Collection Type</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static CollectionType get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			CollectionType result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Collection Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static CollectionType getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			CollectionType result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Collection Type</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static CollectionType get(int value) {
		switch (value) {
			case SERVICE_VALUE: return SERVICE;
			case REFERENCE_VALUE: return REFERENCE;
			case SERVICEOBJECTS_VALUE: return SERVICEOBJECTS;
			case PROPERTIES_VALUE: return PROPERTIES;
			case TUPLE_VALUE: return TUPLE;
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
	private CollectionType(int value, String name, String literal) {
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
	
} //CollectionType
