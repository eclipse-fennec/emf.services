/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttQos;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Mqtt Flavor</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttFlavorImpl#getBrokers <em>Brokers</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttFlavorImpl#getRequestTopic <em>Request Topic</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttFlavorImpl#getResponseTopic <em>Response Topic</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttFlavorImpl#getDefaultQos <em>Default Qos</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MqttFlavorImpl#isDefaultRetained <em>Default Retained</em>}</li>
 * </ul>
 *
 * @generated
 */
public class MqttFlavorImpl extends ServiceFlavorImpl implements MqttFlavor {
	/**
	 * The cached value of the '{@link #getBrokers() <em>Brokers</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBrokers()
	 * @generated
	 * @ordered
	 */
	protected EList<String> brokers;

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
	 * The default value of the '{@link #getDefaultQos() <em>Default Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDefaultQos()
	 * @generated
	 * @ordered
	 */
	protected static final MqttQos DEFAULT_QOS_EDEFAULT = MqttQos.AT_LEAST_ONCE;

	/**
	 * The cached value of the '{@link #getDefaultQos() <em>Default Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDefaultQos()
	 * @generated
	 * @ordered
	 */
	protected MqttQos defaultQos = DEFAULT_QOS_EDEFAULT;

	/**
	 * The default value of the '{@link #isDefaultRetained() <em>Default Retained</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDefaultRetained()
	 * @generated
	 * @ordered
	 */
	protected static final boolean DEFAULT_RETAINED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isDefaultRetained() <em>Default Retained</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDefaultRetained()
	 * @generated
	 * @ordered
	 */
	protected boolean defaultRetained = DEFAULT_RETAINED_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MqttFlavorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.MQTT_FLAVOR;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getBrokers() {
		if (brokers == null) {
			brokers = new EDataTypeUniqueEList<String>(String.class, this, ServicesPackage.MQTT_FLAVOR__BROKERS);
		}
		return brokers;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_FLAVOR__REQUEST_TOPIC, oldRequestTopic, requestTopic));
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_FLAVOR__RESPONSE_TOPIC, oldResponseTopic, responseTopic));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public MqttQos getDefaultQos() {
		return defaultQos;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDefaultQos(MqttQos newDefaultQos) {
		MqttQos oldDefaultQos = defaultQos;
		defaultQos = newDefaultQos == null ? DEFAULT_QOS_EDEFAULT : newDefaultQos;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_FLAVOR__DEFAULT_QOS, oldDefaultQos, defaultQos));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isDefaultRetained() {
		return defaultRetained;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDefaultRetained(boolean newDefaultRetained) {
		boolean oldDefaultRetained = defaultRetained;
		defaultRetained = newDefaultRetained;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MQTT_FLAVOR__DEFAULT_RETAINED, oldDefaultRetained, defaultRetained));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.MQTT_FLAVOR__BROKERS:
				return getBrokers();
			case ServicesPackage.MQTT_FLAVOR__REQUEST_TOPIC:
				return getRequestTopic();
			case ServicesPackage.MQTT_FLAVOR__RESPONSE_TOPIC:
				return getResponseTopic();
			case ServicesPackage.MQTT_FLAVOR__DEFAULT_QOS:
				return getDefaultQos();
			case ServicesPackage.MQTT_FLAVOR__DEFAULT_RETAINED:
				return isDefaultRetained();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case ServicesPackage.MQTT_FLAVOR__BROKERS:
				getBrokers().clear();
				getBrokers().addAll((Collection<? extends String>)newValue);
				return;
			case ServicesPackage.MQTT_FLAVOR__REQUEST_TOPIC:
				setRequestTopic((String)newValue);
				return;
			case ServicesPackage.MQTT_FLAVOR__RESPONSE_TOPIC:
				setResponseTopic((String)newValue);
				return;
			case ServicesPackage.MQTT_FLAVOR__DEFAULT_QOS:
				setDefaultQos((MqttQos)newValue);
				return;
			case ServicesPackage.MQTT_FLAVOR__DEFAULT_RETAINED:
				setDefaultRetained((Boolean)newValue);
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
			case ServicesPackage.MQTT_FLAVOR__BROKERS:
				getBrokers().clear();
				return;
			case ServicesPackage.MQTT_FLAVOR__REQUEST_TOPIC:
				setRequestTopic(REQUEST_TOPIC_EDEFAULT);
				return;
			case ServicesPackage.MQTT_FLAVOR__RESPONSE_TOPIC:
				setResponseTopic(RESPONSE_TOPIC_EDEFAULT);
				return;
			case ServicesPackage.MQTT_FLAVOR__DEFAULT_QOS:
				setDefaultQos(DEFAULT_QOS_EDEFAULT);
				return;
			case ServicesPackage.MQTT_FLAVOR__DEFAULT_RETAINED:
				setDefaultRetained(DEFAULT_RETAINED_EDEFAULT);
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
			case ServicesPackage.MQTT_FLAVOR__BROKERS:
				return brokers != null && !brokers.isEmpty();
			case ServicesPackage.MQTT_FLAVOR__REQUEST_TOPIC:
				return REQUEST_TOPIC_EDEFAULT == null ? requestTopic != null : !REQUEST_TOPIC_EDEFAULT.equals(requestTopic);
			case ServicesPackage.MQTT_FLAVOR__RESPONSE_TOPIC:
				return RESPONSE_TOPIC_EDEFAULT == null ? responseTopic != null : !RESPONSE_TOPIC_EDEFAULT.equals(responseTopic);
			case ServicesPackage.MQTT_FLAVOR__DEFAULT_QOS:
				return defaultQos != DEFAULT_QOS_EDEFAULT;
			case ServicesPackage.MQTT_FLAVOR__DEFAULT_RETAINED:
				return defaultRetained != DEFAULT_RETAINED_EDEFAULT;
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
		result.append(" (brokers: ");
		result.append(brokers);
		result.append(", requestTopic: ");
		result.append(requestTopic);
		result.append(", responseTopic: ");
		result.append(responseTopic);
		result.append(", defaultQos: ");
		result.append(defaultQos);
		result.append(", defaultRetained: ");
		result.append(defaultRetained);
		result.append(')');
		return result.toString();
	}

} //MqttFlavorImpl
