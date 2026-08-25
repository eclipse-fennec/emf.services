/*
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.MqttQos;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Mqtt Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttOperationFlavorImpl#getRequestTopic <em>Request Topic</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttOperationFlavorImpl#getResponseTopic <em>Response Topic</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttOperationFlavorImpl#getQos <em>Qos</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttOperationFlavorImpl#isRetained <em>Retained</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttOperationFlavorImpl#isCorrelation <em>Correlation</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttOperationFlavorImpl#getReturnPath <em>Return Path</em>}</li>
 * </ul>
 *
 * @generated
 */
public class MqttOperationFlavorImpl extends ServiceOperationFlavorImpl implements MqttOperationFlavor {
	/**
	 * The default value of the '{@link #getRequestTopic() <em>Request Topic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRequestTopic()
	 * @generated
	 * @ordered
	 */
	protected static final String REQUEST_TOPIC_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRequestTopic() <em>Request Topic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRequestTopic()
	 * @generated
	 * @ordered
	 */
	protected String requestTopic = REQUEST_TOPIC_EDEFAULT;

	/**
	 * The default value of the '{@link #getResponseTopic() <em>Response Topic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResponseTopic()
	 * @generated
	 * @ordered
	 */
	protected static final String RESPONSE_TOPIC_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getResponseTopic() <em>Response Topic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResponseTopic()
	 * @generated
	 * @ordered
	 */
	protected String responseTopic = RESPONSE_TOPIC_EDEFAULT;

	/**
	 * The default value of the '{@link #getQos() <em>Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getQos()
	 * @generated
	 * @ordered
	 */
	protected static final MqttQos QOS_EDEFAULT = MqttQos.AT_MOST_ONCE;

	/**
	 * The cached value of the '{@link #getQos() <em>Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getQos()
	 * @generated
	 * @ordered
	 */
	protected MqttQos qos = QOS_EDEFAULT;

	/**
	 * The default value of the '{@link #isRetained() <em>Retained</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isRetained()
	 * @generated
	 * @ordered
	 */
	protected static final boolean RETAINED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isRetained() <em>Retained</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isRetained()
	 * @generated
	 * @ordered
	 */
	protected boolean retained = RETAINED_EDEFAULT;

	/**
	 * The default value of the '{@link #isCorrelation() <em>Correlation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isCorrelation()
	 * @generated
	 * @ordered
	 */
	protected static final boolean CORRELATION_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isCorrelation() <em>Correlation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isCorrelation()
	 * @generated
	 * @ordered
	 */
	protected boolean correlation = CORRELATION_EDEFAULT;

	/**
	 * The default value of the '{@link #getReturnPath() <em>Return Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReturnPath()
	 * @generated
	 * @ordered
	 */
	protected static final String RETURN_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getReturnPath() <em>Return Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReturnPath()
	 * @generated
	 * @ordered
	 */
	protected String returnPath = RETURN_PATH_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MqttOperationFlavorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.MQTT_OPERATION_FLAVOR;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRequestTopic() {
		return requestTopic;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRequestTopic(String newRequestTopic) {
		String oldRequestTopic = requestTopic;
		requestTopic = newRequestTopic;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_OPERATION_FLAVOR__REQUEST_TOPIC, oldRequestTopic, requestTopic));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getResponseTopic() {
		return responseTopic;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setResponseTopic(String newResponseTopic) {
		String oldResponseTopic = responseTopic;
		responseTopic = newResponseTopic;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_OPERATION_FLAVOR__RESPONSE_TOPIC, oldResponseTopic, responseTopic));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public MqttQos getQos() {
		return qos;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setQos(MqttQos newQos) {
		MqttQos oldQos = qos;
		qos = newQos == null ? QOS_EDEFAULT : newQos;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_OPERATION_FLAVOR__QOS, oldQos, qos));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isRetained() {
		return retained;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRetained(boolean newRetained) {
		boolean oldRetained = retained;
		retained = newRetained;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_OPERATION_FLAVOR__RETAINED, oldRetained, retained));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isCorrelation() {
		return correlation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCorrelation(boolean newCorrelation) {
		boolean oldCorrelation = correlation;
		correlation = newCorrelation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_OPERATION_FLAVOR__CORRELATION, oldCorrelation, correlation));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getReturnPath() {
		return returnPath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setReturnPath(String newReturnPath) {
		String oldReturnPath = returnPath;
		returnPath = newReturnPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_OPERATION_FLAVOR__RETURN_PATH, oldReturnPath, returnPath));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.MQTT_OPERATION_FLAVOR__REQUEST_TOPIC:
				return getRequestTopic();
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RESPONSE_TOPIC:
				return getResponseTopic();
			case ServicesPackage.MQTT_OPERATION_FLAVOR__QOS:
				return getQos();
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RETAINED:
				return isRetained();
			case ServicesPackage.MQTT_OPERATION_FLAVOR__CORRELATION:
				return isCorrelation();
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RETURN_PATH:
				return getReturnPath();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case ServicesPackage.MQTT_OPERATION_FLAVOR__REQUEST_TOPIC:
				setRequestTopic((String)newValue);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RESPONSE_TOPIC:
				setResponseTopic((String)newValue);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__QOS:
				setQos((MqttQos)newValue);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RETAINED:
				setRetained((Boolean)newValue);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__CORRELATION:
				setCorrelation((Boolean)newValue);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RETURN_PATH:
				setReturnPath((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case ServicesPackage.MQTT_OPERATION_FLAVOR__REQUEST_TOPIC:
				setRequestTopic(REQUEST_TOPIC_EDEFAULT);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RESPONSE_TOPIC:
				setResponseTopic(RESPONSE_TOPIC_EDEFAULT);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__QOS:
				setQos(QOS_EDEFAULT);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RETAINED:
				setRetained(RETAINED_EDEFAULT);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__CORRELATION:
				setCorrelation(CORRELATION_EDEFAULT);
				return;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RETURN_PATH:
				setReturnPath(RETURN_PATH_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case ServicesPackage.MQTT_OPERATION_FLAVOR__REQUEST_TOPIC:
				return REQUEST_TOPIC_EDEFAULT == null ? requestTopic != null : !REQUEST_TOPIC_EDEFAULT.equals(requestTopic);
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RESPONSE_TOPIC:
				return RESPONSE_TOPIC_EDEFAULT == null ? responseTopic != null : !RESPONSE_TOPIC_EDEFAULT.equals(responseTopic);
			case ServicesPackage.MQTT_OPERATION_FLAVOR__QOS:
				return qos != QOS_EDEFAULT;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RETAINED:
				return retained != RETAINED_EDEFAULT;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__CORRELATION:
				return correlation != CORRELATION_EDEFAULT;
			case ServicesPackage.MQTT_OPERATION_FLAVOR__RETURN_PATH:
				return RETURN_PATH_EDEFAULT == null ? returnPath != null : !RETURN_PATH_EDEFAULT.equals(returnPath);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (requestTopic: ");
		result.append(requestTopic);
		result.append(", responseTopic: ");
		result.append(responseTopic);
		result.append(", qos: ");
		result.append(qos);
		result.append(", retained: ");
		result.append(retained);
		result.append(", correlation: ");
		result.append(correlation);
		result.append(", returnPath: ");
		result.append(returnPath);
		result.append(')');
		return result.toString();
	}

} //MqttOperationFlavorImpl
