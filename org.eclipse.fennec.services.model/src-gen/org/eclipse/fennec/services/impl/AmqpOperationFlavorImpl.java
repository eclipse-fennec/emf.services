/*
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.fennec.services.AmqpOperationFlavor;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Amqp Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.AmqpOperationFlavorImpl#getRoutingKey <em>Routing Key</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AmqpOperationFlavorImpl extends ServiceOperationFlavorImpl implements AmqpOperationFlavor {
	/**
	 * The default value of the '{@link #getRoutingKey() <em>Routing Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRoutingKey()
	 * @generated
	 * @ordered
	 */
	protected static final String ROUTING_KEY_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRoutingKey() <em>Routing Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRoutingKey()
	 * @generated
	 * @ordered
	 */
	protected String routingKey = ROUTING_KEY_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AmqpOperationFlavorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.AMQP_OPERATION_FLAVOR;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRoutingKey() {
		return routingKey;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRoutingKey(String newRoutingKey) {
		String oldRoutingKey = routingKey;
		routingKey = newRoutingKey;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.AMQP_OPERATION_FLAVOR__ROUTING_KEY, oldRoutingKey, routingKey));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.AMQP_OPERATION_FLAVOR__ROUTING_KEY:
				return getRoutingKey();
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
			case ServicesPackage.AMQP_OPERATION_FLAVOR__ROUTING_KEY:
				setRoutingKey((String)newValue);
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
			case ServicesPackage.AMQP_OPERATION_FLAVOR__ROUTING_KEY:
				setRoutingKey(ROUTING_KEY_EDEFAULT);
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
			case ServicesPackage.AMQP_OPERATION_FLAVOR__ROUTING_KEY:
				return ROUTING_KEY_EDEFAULT == null ? routingKey != null : !ROUTING_KEY_EDEFAULT.equals(routingKey);
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
		result.append(" (routingKey: ");
		result.append(routingKey);
		result.append(')');
		return result.toString();
	}

} //AmqpOperationFlavorImpl
