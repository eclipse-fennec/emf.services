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
 * A representation of the literals of the enumeration '<em><b>Service Scope</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * OSGi service scope. SINGLETON = one instance shared by all consumers; BUNDLE = one instance per consuming bundle/provider; PROTOTYPE = a new instance per consumer request.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceScope()
 * @model
 * @generated
 */
@ProviderType
public enum ServiceScope implements Enumerator {
	/**
	 * The '<em><b>SINGLETON</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SINGLETON_VALUE
	 * @generated
	 * @ordered
	 */
	SINGLETON(0, "SINGLETON", "SINGLETON"),

	/**
	 * The '<em><b>BUNDLE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BUNDLE_VALUE
	 * @generated
	 * @ordered
	 */
	BUNDLE(1, "BUNDLE", "BUNDLE"),

	/**
	 * The '<em><b>PROTOTYPE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #PROTOTYPE_VALUE
	 * @generated
	 * @ordered
	 */
	PROTOTYPE(2, "PROTOTYPE", "PROTOTYPE");

	/**
	 * The '<em><b>SINGLETON</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SINGLETON
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int SINGLETON_VALUE = 0;

	/**
	 * The '<em><b>BUNDLE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BUNDLE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int BUNDLE_VALUE = 1;

	/**
	 * The '<em><b>PROTOTYPE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #PROTOTYPE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int PROTOTYPE_VALUE = 2;

	/**
	 * An array of all the '<em><b>Service Scope</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ServiceScope[] VALUES_ARRAY =
		new ServiceScope[] {
			SINGLETON,
			BUNDLE,
			PROTOTYPE,
		};

	/**
	 * A public read-only list of all the '<em><b>Service Scope</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<ServiceScope> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Service Scope</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ServiceScope get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ServiceScope result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Service Scope</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ServiceScope getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ServiceScope result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Service Scope</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ServiceScope get(int value) {
		switch (value) {
			case SINGLETON_VALUE: return SINGLETON;
			case BUNDLE_VALUE: return BUNDLE;
			case PROTOTYPE_VALUE: return PROTOTYPE;
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
	private ServiceScope(int value, String name, String literal) {
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
	
} //ServiceScope
