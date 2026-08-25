/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Mqtt Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * MQTT pub/sub transport. Operations are addressed via a request topic; responses go either to a response topic, a per-operation override topic, or via correlationId-on-shared-topic depending on configuration.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.MqttFlavor#getBrokers <em>Brokers</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MqttFlavor#getRequestTopic <em>Request Topic</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MqttFlavor#getResponseTopic <em>Response Topic</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MqttFlavor#getDefaultQos <em>Default Qos</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MqttFlavor#isDefaultRetained <em>Default Retained</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getMqttFlavor()
 * @model
 * @generated
 */
@ProviderType
public interface MqttFlavor extends ServiceFlavor {
	/**
	 * Returns the value of the '<em><b>Brokers</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * One or more broker URLs (mqtt://… or mqtts://…). Multiple brokers indicate a high-availability cluster; the client picks one and fails over.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Brokers</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttFlavor_Brokers()
	 * @model required="true"
	 * @generated
	 */
	EList<String> getBrokers();

	/**
	 * Returns the value of the '<em><b>Request Topic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Default topic for incoming requests. Operations can override via MqttOperationFlavor.requestTopic.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Request Topic</em>' attribute.
	 * @see #setRequestTopic(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttFlavor_RequestTopic()
	 * @model required="true"
	 * @generated
	 */
	String getRequestTopic();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttFlavor#getRequestTopic <em>Request Topic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Request Topic</em>' attribute.
	 * @see #getRequestTopic()
	 * @generated
	 */
	void setRequestTopic(String value);

	/**
	 * Returns the value of the '<em><b>Response Topic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Default topic for responses. Null = use MQTT v5 response-topic property or correlate via correlationId on the request topic.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Response Topic</em>' attribute.
	 * @see #setResponseTopic(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttFlavor_ResponseTopic()
	 * @model
	 * @generated
	 */
	String getResponseTopic();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttFlavor#getResponseTopic <em>Response Topic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Response Topic</em>' attribute.
	 * @see #getResponseTopic()
	 * @generated
	 */
	void setResponseTopic(String value);

	/**
	 * Returns the value of the '<em><b>Default Qos</b></em>' attribute.
	 * The default value is <code>"AT_LEAST_ONCE"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.MqttQos}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Default QoS for operations. Per-operation override possible via MqttOperationFlavor.qos.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Default Qos</em>' attribute.
	 * @see org.eclipse.fennec.services.MqttQos
	 * @see #setDefaultQos(MqttQos)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttFlavor_DefaultQos()
	 * @model default="AT_LEAST_ONCE" required="true"
	 * @generated
	 */
	MqttQos getDefaultQos();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttFlavor#getDefaultQos <em>Default Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Default Qos</em>' attribute.
	 * @see org.eclipse.fennec.services.MqttQos
	 * @see #getDefaultQos()
	 * @generated
	 */
	void setDefaultQos(MqttQos value);

	/**
	 * Returns the value of the '<em><b>Default Retained</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Default retained flag on published messages. Per-operation override possible.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Default Retained</em>' attribute.
	 * @see #setDefaultRetained(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttFlavor_DefaultRetained()
	 * @model default="false" required="true"
	 * @generated
	 */
	boolean isDefaultRetained();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttFlavor#isDefaultRetained <em>Default Retained</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Default Retained</em>' attribute.
	 * @see #isDefaultRetained()
	 * @generated
	 */
	void setDefaultRetained(boolean value);

} // MqttFlavor
