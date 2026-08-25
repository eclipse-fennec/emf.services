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
 * A representation of the literals of the enumeration '<em><b>Connection State</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * State of a LocalServiceRegistry's link to its Remote Registry. CONNECTED = snapshot received and event stream alive; DEGRADED = link lost, reconnect in progress, cached state still served to read-only consumers; OFFLINE = never connected, or reconnect has permanently failed. While DEGRADED or OFFLINE, write operations (publish / withdraw / catalog mutations) MUST be rejected with a Diagnostic; reads continue against the last known snapshot.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getConnectionState()
 * @model
 * @generated
 */
@ProviderType
public enum ConnectionState implements Enumerator {
	/**
	 * The '<em><b>CONNECTED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #CONNECTED_VALUE
	 * @generated
	 * @ordered
	 */
	CONNECTED(0, "CONNECTED", "CONNECTED"),

	/**
	 * The '<em><b>DEGRADED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DEGRADED_VALUE
	 * @generated
	 * @ordered
	 */
	DEGRADED(1, "DEGRADED", "DEGRADED"),

	/**
	 * The '<em><b>OFFLINE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #OFFLINE_VALUE
	 * @generated
	 * @ordered
	 */
	OFFLINE(2, "OFFLINE", "OFFLINE");

	/**
	 * The '<em><b>CONNECTED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #CONNECTED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int CONNECTED_VALUE = 0;

	/**
	 * The '<em><b>DEGRADED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DEGRADED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int DEGRADED_VALUE = 1;

	/**
	 * The '<em><b>OFFLINE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #OFFLINE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int OFFLINE_VALUE = 2;

	/**
	 * An array of all the '<em><b>Connection State</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ConnectionState[] VALUES_ARRAY =
		new ConnectionState[] {
			CONNECTED,
			DEGRADED,
			OFFLINE,
		};

	/**
	 * A public read-only list of all the '<em><b>Connection State</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<ConnectionState> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Connection State</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ConnectionState get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ConnectionState result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Connection State</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ConnectionState getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ConnectionState result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Connection State</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ConnectionState get(int value) {
		switch (value) {
			case CONNECTED_VALUE: return CONNECTED;
			case DEGRADED_VALUE: return DEGRADED;
			case OFFLINE_VALUE: return OFFLINE;
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
	private ConnectionState(int value, String name, String literal) {
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
	
} //ConnectionState
