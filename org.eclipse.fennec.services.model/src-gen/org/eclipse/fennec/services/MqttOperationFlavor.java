/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Mqtt Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * MQTT binding for one operation. Topic/QoS/retained values override the MqttFlavor defaults when set.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.MqttOperationFlavor#getRequestTopic <em>Request Topic</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MqttOperationFlavor#getResponseTopic <em>Response Topic</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MqttOperationFlavor#getQos <em>Qos</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MqttOperationFlavor#isRetained <em>Retained</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MqttOperationFlavor#isCorrelation <em>Correlation</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MqttOperationFlavor#getReturnPath <em>Return Path</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getMqttOperationFlavor()
 * @model
 * @generated
 */
@ProviderType
public interface MqttOperationFlavor extends ServiceOperationFlavor {
	/**
	 * Returns the value of the '<em><b>Request Topic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Overrides MqttFlavor.requestTopic for this operation. Null = use the flavor default.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Request Topic</em>' attribute.
	 * @see #setRequestTopic(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttOperationFlavor_RequestTopic()
	 * @model
	 * @generated
	 */
	String getRequestTopic();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttOperationFlavor#getRequestTopic <em>Request Topic</em>}' attribute.
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
	 * Overrides MqttFlavor.responseTopic for this operation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Response Topic</em>' attribute.
	 * @see #setResponseTopic(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttOperationFlavor_ResponseTopic()
	 * @model
	 * @generated
	 */
	String getResponseTopic();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttOperationFlavor#getResponseTopic <em>Response Topic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Response Topic</em>' attribute.
	 * @see #getResponseTopic()
	 * @generated
	 */
	void setResponseTopic(String value);

	/**
	 * Returns the value of the '<em><b>Qos</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.MqttQos}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Overrides MqttFlavor.defaultQos for this operation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Qos</em>' attribute.
	 * @see org.eclipse.fennec.services.MqttQos
	 * @see #setQos(MqttQos)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttOperationFlavor_Qos()
	 * @model
	 * @generated
	 */
	MqttQos getQos();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttOperationFlavor#getQos <em>Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Qos</em>' attribute.
	 * @see org.eclipse.fennec.services.MqttQos
	 * @see #getQos()
	 * @generated
	 */
	void setQos(MqttQos value);

	/**
	 * Returns the value of the '<em><b>Retained</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Overrides MqttFlavor.defaultRetained for this operation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Retained</em>' attribute.
	 * @see #setRetained(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttOperationFlavor_Retained()
	 * @model
	 * @generated
	 */
	boolean isRetained();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttOperationFlavor#isRetained <em>Retained</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Retained</em>' attribute.
	 * @see #isRetained()
	 * @generated
	 */
	void setRetained(boolean value);

	/**
	 * Returns the value of the '<em><b>Correlation</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * If true, request and response are matched via MQTT v5 correlationId. If false, the framework assumes a one-way / fire-and-forget operation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Correlation</em>' attribute.
	 * @see #setCorrelation(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttOperationFlavor_Correlation()
	 * @model default="true" required="true"
	 * @generated
	 */
	boolean isCorrelation();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttOperationFlavor#isCorrelation <em>Correlation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Correlation</em>' attribute.
	 * @see #isCorrelation()
	 * @generated
	 */
	void setCorrelation(boolean value);

	/**
	 * Returns the value of the '<em><b>Return Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Optional alternative convention for asynchronous responses: a topic pattern with placeholders the producer fills in when publishing the response.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Return Path</em>' attribute.
	 * @see #setReturnPath(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMqttOperationFlavor_ReturnPath()
	 * @model
	 * @generated
	 */
	String getReturnPath();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MqttOperationFlavor#getReturnPath <em>Return Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Return Path</em>' attribute.
	 * @see #getReturnPath()
	 * @generated
	 */
	void setReturnPath(String value);

} // MqttOperationFlavor
