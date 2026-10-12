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
 * A representation of the literals of the enumeration '<em><b>Security Scheme Kind</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Discriminator of SecurityScheme, as FlavorKind is for ServiceFlavor. Projected as the 'kind' attribute of the implied 'services.security' capability, so a consumer can require a way of proving itself at lookup time.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getSecuritySchemeKind()
 * @model
 * @generated
 */
@ProviderType
public enum SecuritySchemeKind implements Enumerator {
	/**
	 * The '<em><b>BEARER TOKEN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BEARER_TOKEN_VALUE
	 * @generated
	 * @ordered
	 */
	BEARER_TOKEN(0, "BEARER_TOKEN", "BEARER_TOKEN"),

	/**
	 * The '<em><b>MUTUAL TLS</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #MUTUAL_TLS_VALUE
	 * @generated
	 * @ordered
	 */
	MUTUAL_TLS(1, "MUTUAL_TLS", "MUTUAL_TLS"),

	/**
	 * The '<em><b>CONNECTION</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #CONNECTION_VALUE
	 * @generated
	 * @ordered
	 */
	CONNECTION(2, "CONNECTION", "CONNECTION");

	/**
	 * The '<em><b>BEARER TOKEN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BEARER_TOKEN
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int BEARER_TOKEN_VALUE = 0;

	/**
	 * The '<em><b>MUTUAL TLS</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #MUTUAL_TLS
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int MUTUAL_TLS_VALUE = 1;

	/**
	 * The '<em><b>CONNECTION</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #CONNECTION
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int CONNECTION_VALUE = 2;

	/**
	 * An array of all the '<em><b>Security Scheme Kind</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final SecuritySchemeKind[] VALUES_ARRAY =
		new SecuritySchemeKind[] {
			BEARER_TOKEN,
			MUTUAL_TLS,
			CONNECTION,
		};

	/**
	 * A public read-only list of all the '<em><b>Security Scheme Kind</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<SecuritySchemeKind> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Security Scheme Kind</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static SecuritySchemeKind get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			SecuritySchemeKind result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Security Scheme Kind</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static SecuritySchemeKind getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			SecuritySchemeKind result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Security Scheme Kind</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static SecuritySchemeKind get(int value) {
		switch (value) {
			case BEARER_TOKEN_VALUE: return BEARER_TOKEN;
			case MUTUAL_TLS_VALUE: return MUTUAL_TLS;
			case CONNECTION_VALUE: return CONNECTION;
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
	private SecuritySchemeKind(int value, String name, String literal) {
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
	
} //SecuritySchemeKind
