/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Consumer Capability</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ConsumerCapabilityImpl#getConsumerId <em>Consumer Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ConsumerCapabilityImpl#getSupportedFlavors <em>Supported Flavors</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ConsumerCapabilityImpl#getProperties <em>Properties</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ConsumerCapabilityImpl extends MinimalEObjectImpl.Container implements ConsumerCapability {
	/**
	 * The default value of the '{@link #getConsumerId() <em>Consumer Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConsumerId()
	 * @generated
	 * @ordered
	 */
	protected static final String CONSUMER_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getConsumerId() <em>Consumer Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConsumerId()
	 * @generated
	 * @ordered
	 */
	protected String consumerId = CONSUMER_ID_EDEFAULT;

	/**
	 * The cached value of the '{@link #getSupportedFlavors() <em>Supported Flavors</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSupportedFlavors()
	 * @generated
	 * @ordered
	 */
	protected EList<FlavorKind> supportedFlavors;

	/**
	 * The cached value of the '{@link #getProperties() <em>Properties</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProperties()
	 * @generated
	 * @ordered
	 */
	protected EList<Property> properties;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ConsumerCapabilityImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.CONSUMER_CAPABILITY;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getConsumerId() {
		return consumerId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setConsumerId(String newConsumerId) {
		String oldConsumerId = consumerId;
		consumerId = newConsumerId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.CONSUMER_CAPABILITY__CONSUMER_ID, oldConsumerId, consumerId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<FlavorKind> getSupportedFlavors() {
		if (supportedFlavors == null) {
			supportedFlavors = new EDataTypeUniqueEList<FlavorKind>(FlavorKind.class, this, ServicesPackage.CONSUMER_CAPABILITY__SUPPORTED_FLAVORS);
		}
		return supportedFlavors;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Property> getProperties() {
		if (properties == null) {
			properties = new EObjectContainmentEList<Property>(Property.class, this, ServicesPackage.CONSUMER_CAPABILITY__PROPERTIES);
		}
		return properties;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.CONSUMER_CAPABILITY__PROPERTIES:
				return ((InternalEList<?>)getProperties()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.CONSUMER_CAPABILITY__CONSUMER_ID:
				return getConsumerId();
			case ServicesPackage.CONSUMER_CAPABILITY__SUPPORTED_FLAVORS:
				return getSupportedFlavors();
			case ServicesPackage.CONSUMER_CAPABILITY__PROPERTIES:
				return getProperties();
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
			case ServicesPackage.CONSUMER_CAPABILITY__CONSUMER_ID:
				setConsumerId((String)newValue);
				return;
			case ServicesPackage.CONSUMER_CAPABILITY__SUPPORTED_FLAVORS:
				getSupportedFlavors().clear();
				getSupportedFlavors().addAll((Collection<? extends FlavorKind>)newValue);
				return;
			case ServicesPackage.CONSUMER_CAPABILITY__PROPERTIES:
				getProperties().clear();
				getProperties().addAll((Collection<? extends Property>)newValue);
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
			case ServicesPackage.CONSUMER_CAPABILITY__CONSUMER_ID:
				setConsumerId(CONSUMER_ID_EDEFAULT);
				return;
			case ServicesPackage.CONSUMER_CAPABILITY__SUPPORTED_FLAVORS:
				getSupportedFlavors().clear();
				return;
			case ServicesPackage.CONSUMER_CAPABILITY__PROPERTIES:
				getProperties().clear();
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
			case ServicesPackage.CONSUMER_CAPABILITY__CONSUMER_ID:
				return CONSUMER_ID_EDEFAULT == null ? consumerId != null : !CONSUMER_ID_EDEFAULT.equals(consumerId);
			case ServicesPackage.CONSUMER_CAPABILITY__SUPPORTED_FLAVORS:
				return supportedFlavors != null && !supportedFlavors.isEmpty();
			case ServicesPackage.CONSUMER_CAPABILITY__PROPERTIES:
				return properties != null && !properties.isEmpty();
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
		result.append(" (consumerId: ");
		result.append(consumerId);
		result.append(", supportedFlavors: ");
		result.append(supportedFlavors);
		result.append(')');
		return result.toString();
	}

} //ConsumerCapabilityImpl
