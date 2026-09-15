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
 * A representation of the literals of the enumeration '<em><b>Update Policy</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * How the broker treats a ServiceImplementation once a successor for it is published (docs/UPDATE_POLICY.md). UNSPECIFIED = not set: on a ServiceInterface the broker default applies (DEPRECATE_AND_DRAIN), on a ServiceImplementation the policy of its ServiceInterface applies — it is the EMF default literal so that any explicit choice always reaches the wire (same reasoning as ServiceEventType.UNSPECIFIED, OPEN_ISSUES W4). EVERGREEN = old and new stay registered side by side indefinitely; retirement is a manual withdraw (broker API itself, infrastructure services). DEPRECATE_AND_DRAIN = the successor is published, the old one is flagged and lookups prefer the successor, consumers get UPGRADE_AVAILABLE, the old one is retired once the last ConsumerSession lease on it has been released or expired. HARD_CUTOVER = the successor is published, after cutoverGraceMillis the broker sends UNREGISTERING for the old one regardless of open leases (security fix, breaking change).
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getUpdatePolicy()
 * @model
 * @generated
 */
@ProviderType
public enum UpdatePolicy implements Enumerator {
	/**
	 * The '<em><b>UNSPECIFIED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNSPECIFIED_VALUE
	 * @generated
	 * @ordered
	 */
	UNSPECIFIED(0, "UNSPECIFIED", "UNSPECIFIED"),

	/**
	 * The '<em><b>EVERGREEN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #EVERGREEN_VALUE
	 * @generated
	 * @ordered
	 */
	EVERGREEN(1, "EVERGREEN", "EVERGREEN"),

	/**
	 * The '<em><b>DEPRECATE AND DRAIN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DEPRECATE_AND_DRAIN_VALUE
	 * @generated
	 * @ordered
	 */
	DEPRECATE_AND_DRAIN(2, "DEPRECATE_AND_DRAIN", "DEPRECATE_AND_DRAIN"),

	/**
	 * The '<em><b>HARD CUTOVER</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #HARD_CUTOVER_VALUE
	 * @generated
	 * @ordered
	 */
	HARD_CUTOVER(3, "HARD_CUTOVER", "HARD_CUTOVER");

	/**
	 * The '<em><b>UNSPECIFIED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNSPECIFIED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int UNSPECIFIED_VALUE = 0;

	/**
	 * The '<em><b>EVERGREEN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #EVERGREEN
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int EVERGREEN_VALUE = 1;

	/**
	 * The '<em><b>DEPRECATE AND DRAIN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DEPRECATE_AND_DRAIN
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int DEPRECATE_AND_DRAIN_VALUE = 2;

	/**
	 * The '<em><b>HARD CUTOVER</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #HARD_CUTOVER
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int HARD_CUTOVER_VALUE = 3;

	/**
	 * An array of all the '<em><b>Update Policy</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final UpdatePolicy[] VALUES_ARRAY =
		new UpdatePolicy[] {
			UNSPECIFIED,
			EVERGREEN,
			DEPRECATE_AND_DRAIN,
			HARD_CUTOVER,
		};

	/**
	 * A public read-only list of all the '<em><b>Update Policy</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<UpdatePolicy> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Update Policy</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static UpdatePolicy get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			UpdatePolicy result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Update Policy</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static UpdatePolicy getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			UpdatePolicy result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Update Policy</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static UpdatePolicy get(int value) {
		switch (value) {
			case UNSPECIFIED_VALUE: return UNSPECIFIED;
			case EVERGREEN_VALUE: return EVERGREEN;
			case DEPRECATE_AND_DRAIN_VALUE: return DEPRECATE_AND_DRAIN;
			case HARD_CUTOVER_VALUE: return HARD_CUTOVER;
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
	private UpdatePolicy(int value, String name, String literal) {
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
	
} //UpdatePolicy
