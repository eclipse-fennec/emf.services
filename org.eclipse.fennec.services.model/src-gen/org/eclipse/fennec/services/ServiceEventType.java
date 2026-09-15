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
 * A representation of the literals of the enumeration '<em><b>Service Event Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Lifecycle event types. Values follow org.osgi.framework.ServiceEvent and are bitwise-combinable so listener filters can subscribe to multiple types at once. UNSPECIFIED = no type set; exists only so that none of the meaningful literals is the EMF default, because EMF omits an attribute whose value equals the default and a required type would then be missing on the wire (see OPEN_ISSUES W4). REGISTERED = service appeared; MODIFIED = properties changed, still matches subscriber filter; UNREGISTERING = service is being removed; MODIFIED_ENDMATCH = properties changed, no longer matches subscriber filter. UPGRADE_AVAILABLE = a newer ServiceImplementation that declares replaces = the referenced one has been published under DEPRECATE_AND_DRAIN; the old service stays live, consumers should re-lookup and migrate at their own pace. RETIRED = the service has been fully removed from the registry after an UNREGISTERING; sessions still holding a lease on it are released. Both extend the OSGi set; see docs/UPDATE_POLICY.md. ServiceEvent.reasonCode tells why an UNREGISTERING / RETIRED happened.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceEventType()
 * @model
 * @generated
 */
@ProviderType
public enum ServiceEventType implements Enumerator {
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
	 * The '<em><b>REGISTERED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REGISTERED_VALUE
	 * @generated
	 * @ordered
	 */
	REGISTERED(1, "REGISTERED", "REGISTERED"),

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
	 * The '<em><b>UNREGISTERING</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNREGISTERING_VALUE
	 * @generated
	 * @ordered
	 */
	UNREGISTERING(4, "UNREGISTERING", "UNREGISTERING"),

	/**
	 * The '<em><b>MODIFIED ENDMATCH</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #MODIFIED_ENDMATCH_VALUE
	 * @generated
	 * @ordered
	 */
	MODIFIED_ENDMATCH(8, "MODIFIED_ENDMATCH", "MODIFIED_ENDMATCH"),

	/**
	 * The '<em><b>UPGRADE AVAILABLE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UPGRADE_AVAILABLE_VALUE
	 * @generated
	 * @ordered
	 */
	UPGRADE_AVAILABLE(16, "UPGRADE_AVAILABLE", "UPGRADE_AVAILABLE"),

	/**
	 * The '<em><b>RETIRED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RETIRED_VALUE
	 * @generated
	 * @ordered
	 */
	RETIRED(32, "RETIRED", "RETIRED");

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
	 * The '<em><b>REGISTERED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REGISTERED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int REGISTERED_VALUE = 1;

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
	 * The '<em><b>UNREGISTERING</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNREGISTERING
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int UNREGISTERING_VALUE = 4;

	/**
	 * The '<em><b>MODIFIED ENDMATCH</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #MODIFIED_ENDMATCH
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int MODIFIED_ENDMATCH_VALUE = 8;

	/**
	 * The '<em><b>UPGRADE AVAILABLE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UPGRADE_AVAILABLE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int UPGRADE_AVAILABLE_VALUE = 16;

	/**
	 * The '<em><b>RETIRED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RETIRED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int RETIRED_VALUE = 32;

	/**
	 * An array of all the '<em><b>Service Event Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ServiceEventType[] VALUES_ARRAY =
		new ServiceEventType[] {
			UNSPECIFIED,
			REGISTERED,
			MODIFIED,
			UNREGISTERING,
			MODIFIED_ENDMATCH,
			UPGRADE_AVAILABLE,
			RETIRED,
		};

	/**
	 * A public read-only list of all the '<em><b>Service Event Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<ServiceEventType> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Service Event Type</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ServiceEventType get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ServiceEventType result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Service Event Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ServiceEventType getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ServiceEventType result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Service Event Type</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ServiceEventType get(int value) {
		switch (value) {
			case UNSPECIFIED_VALUE: return UNSPECIFIED;
			case REGISTERED_VALUE: return REGISTERED;
			case MODIFIED_VALUE: return MODIFIED;
			case UNREGISTERING_VALUE: return UNREGISTERING;
			case MODIFIED_ENDMATCH_VALUE: return MODIFIED_ENDMATCH;
			case UPGRADE_AVAILABLE_VALUE: return UPGRADE_AVAILABLE;
			case RETIRED_VALUE: return RETIRED;
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
	private ServiceEventType(int value, String name, String literal) {
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
	
} //ServiceEventType
