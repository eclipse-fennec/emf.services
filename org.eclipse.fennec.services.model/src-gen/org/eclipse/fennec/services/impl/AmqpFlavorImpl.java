/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

import org.eclipse.fennec.services.AmqpFlavor;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Amqp Flavor</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.AmqpFlavorImpl#getBrokers <em>Brokers</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.AmqpFlavorImpl#getExchange <em>Exchange</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.AmqpFlavorImpl#getRequestRoutingKey <em>Request Routing Key</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AmqpFlavorImpl extends ServiceFlavorImpl implements AmqpFlavor {
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
	 * The default value of the '{@link #getExchange() <em>Exchange</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExchange()
	 * @generated
	 * @ordered
	 */
	protected static final String EXCHANGE_EDEFAULT = "amq.topic";

	/**
	 * The cached value of the '{@link #getExchange() <em>Exchange</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExchange()
	 * @generated
	 * @ordered
	 */
	protected String exchange = EXCHANGE_EDEFAULT;

	/**
	 * The default value of the '{@link #getRequestRoutingKey() <em>Request Routing Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRequestRoutingKey()
	 * @generated
	 * @ordered
	 */
	protected static final String REQUEST_ROUTING_KEY_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRequestRoutingKey() <em>Request Routing Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRequestRoutingKey()
	 * @generated
	 * @ordered
	 */
	protected String requestRoutingKey = REQUEST_ROUTING_KEY_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AmqpFlavorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.AMQP_FLAVOR;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getBrokers() {
		if (brokers == null) {
			brokers = new EDataTypeUniqueEList<String>(String.class, this, ServicesPackage.AMQP_FLAVOR__BROKERS);
		}
		return brokers;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getExchange() {
		return exchange;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setExchange(String newExchange) {
		String oldExchange = exchange;
		exchange = newExchange;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.AMQP_FLAVOR__EXCHANGE, oldExchange, exchange));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRequestRoutingKey() {
		return requestRoutingKey;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRequestRoutingKey(String newRequestRoutingKey) {
		String oldRequestRoutingKey = requestRoutingKey;
		requestRoutingKey = newRequestRoutingKey;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.AMQP_FLAVOR__REQUEST_ROUTING_KEY, oldRequestRoutingKey, requestRoutingKey));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.AMQP_FLAVOR__BROKERS:
				return getBrokers();
			case ServicesPackage.AMQP_FLAVOR__EXCHANGE:
				return getExchange();
			case ServicesPackage.AMQP_FLAVOR__REQUEST_ROUTING_KEY:
				return getRequestRoutingKey();
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
			case ServicesPackage.AMQP_FLAVOR__BROKERS:
				getBrokers().clear();
				getBrokers().addAll((Collection<? extends String>)newValue);
				return;
			case ServicesPackage.AMQP_FLAVOR__EXCHANGE:
				setExchange((String)newValue);
				return;
			case ServicesPackage.AMQP_FLAVOR__REQUEST_ROUTING_KEY:
				setRequestRoutingKey((String)newValue);
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
			case ServicesPackage.AMQP_FLAVOR__BROKERS:
				getBrokers().clear();
				return;
			case ServicesPackage.AMQP_FLAVOR__EXCHANGE:
				setExchange(EXCHANGE_EDEFAULT);
				return;
			case ServicesPackage.AMQP_FLAVOR__REQUEST_ROUTING_KEY:
				setRequestRoutingKey(REQUEST_ROUTING_KEY_EDEFAULT);
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
			case ServicesPackage.AMQP_FLAVOR__BROKERS:
				return brokers != null && !brokers.isEmpty();
			case ServicesPackage.AMQP_FLAVOR__EXCHANGE:
				return EXCHANGE_EDEFAULT == null ? exchange != null : !EXCHANGE_EDEFAULT.equals(exchange);
			case ServicesPackage.AMQP_FLAVOR__REQUEST_ROUTING_KEY:
				return REQUEST_ROUTING_KEY_EDEFAULT == null ? requestRoutingKey != null : !REQUEST_ROUTING_KEY_EDEFAULT.equals(requestRoutingKey);
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
		result.append(", exchange: ");
		result.append(exchange);
		result.append(", requestRoutingKey: ");
		result.append(requestRoutingKey);
		result.append(')');
		return result.toString();
	}

} //AmqpFlavorImpl
