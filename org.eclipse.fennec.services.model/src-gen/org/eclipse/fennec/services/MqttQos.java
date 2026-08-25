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
 * A representation of the literals of the enumeration '<em><b>Mqtt Qos</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * MQTT quality-of-service level. AT_MOST_ONCE (0, fire-and-forget); AT_LEAST_ONCE (1, ack-with-redelivery — DDSR default); EXACTLY_ONCE (2, four-way handshake).
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage#getMqttQos()
 * @model
 * @generated
 */
@ProviderType
public enum MqttQos implements Enumerator {
	/**
	 * The '<em><b>AT MOST ONCE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #AT_MOST_ONCE_VALUE
	 * @generated
	 * @ordered
	 */
	AT_MOST_ONCE(0, "AT_MOST_ONCE", "AT_MOST_ONCE"),

	/**
	 * The '<em><b>AT LEAST ONCE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #AT_LEAST_ONCE_VALUE
	 * @generated
	 * @ordered
	 */
	AT_LEAST_ONCE(1, "AT_LEAST_ONCE", "AT_LEAST_ONCE"),

	/**
	 * The '<em><b>EXACTLY ONCE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #EXACTLY_ONCE_VALUE
	 * @generated
	 * @ordered
	 */
	EXACTLY_ONCE(2, "EXACTLY_ONCE", "EXACTLY_ONCE");

	/**
	 * The '<em><b>AT MOST ONCE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #AT_MOST_ONCE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int AT_MOST_ONCE_VALUE = 0;

	/**
	 * The '<em><b>AT LEAST ONCE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #AT_LEAST_ONCE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int AT_LEAST_ONCE_VALUE = 1;

	/**
	 * The '<em><b>EXACTLY ONCE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #EXACTLY_ONCE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int EXACTLY_ONCE_VALUE = 2;

	/**
	 * An array of all the '<em><b>Mqtt Qos</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final MqttQos[] VALUES_ARRAY =
		new MqttQos[] {
			AT_MOST_ONCE,
			AT_LEAST_ONCE,
			EXACTLY_ONCE,
		};

	/**
	 * A public read-only list of all the '<em><b>Mqtt Qos</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<MqttQos> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Mqtt Qos</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static MqttQos get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			MqttQos result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Mqtt Qos</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static MqttQos getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			MqttQos result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Mqtt Qos</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static MqttQos get(int value) {
		switch (value) {
			case AT_MOST_ONCE_VALUE: return AT_MOST_ONCE;
			case AT_LEAST_ONCE_VALUE: return AT_LEAST_ONCE;
			case EXACTLY_ONCE_VALUE: return EXACTLY_ONCE;
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
	private MqttQos(int value, String name, String literal) {
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
	
} //MqttQos
