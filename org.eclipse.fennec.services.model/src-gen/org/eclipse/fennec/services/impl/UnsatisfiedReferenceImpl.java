/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectResolvingEList;

import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.UnsatisfiedReference;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Unsatisfied Reference</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.UnsatisfiedReferenceImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.UnsatisfiedReferenceImpl#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.UnsatisfiedReferenceImpl#getTargetServices <em>Target Services</em>}</li>
 * </ul>
 *
 * @generated
 */
public class UnsatisfiedReferenceImpl extends MinimalEObjectImpl.Container implements UnsatisfiedReference {
	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getTarget() <em>Target</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTarget()
	 * @generated
	 * @ordered
	 */
	protected static final String TARGET_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTarget() <em>Target</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTarget()
	 * @generated
	 * @ordered
	 */
	protected String target = TARGET_EDEFAULT;

	/**
	 * The cached value of the '{@link #getTargetServices() <em>Target Services</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetServices()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceReference> targetServices;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected UnsatisfiedReferenceImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.UNSATISFIED_REFERENCE;
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
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.UNSATISFIED_REFERENCE__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTarget() {
		return target;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTarget(String newTarget) {
		String oldTarget = target;
		target = newTarget;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.UNSATISFIED_REFERENCE__TARGET, oldTarget, target));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceReference> getTargetServices() {
		if (targetServices == null) {
			targetServices = new EObjectResolvingEList<ServiceReference>(ServiceReference.class, this, ServicesPackage.UNSATISFIED_REFERENCE__TARGET_SERVICES);
		}
		return targetServices;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.UNSATISFIED_REFERENCE__NAME:
				return getName();
			case ServicesPackage.UNSATISFIED_REFERENCE__TARGET:
				return getTarget();
			case ServicesPackage.UNSATISFIED_REFERENCE__TARGET_SERVICES:
				return getTargetServices();
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
			case ServicesPackage.UNSATISFIED_REFERENCE__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.UNSATISFIED_REFERENCE__TARGET:
				setTarget((String)newValue);
				return;
			case ServicesPackage.UNSATISFIED_REFERENCE__TARGET_SERVICES:
				getTargetServices().clear();
				getTargetServices().addAll((Collection<? extends ServiceReference>)newValue);
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
			case ServicesPackage.UNSATISFIED_REFERENCE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.UNSATISFIED_REFERENCE__TARGET:
				setTarget(TARGET_EDEFAULT);
				return;
			case ServicesPackage.UNSATISFIED_REFERENCE__TARGET_SERVICES:
				getTargetServices().clear();
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
			case ServicesPackage.UNSATISFIED_REFERENCE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.UNSATISFIED_REFERENCE__TARGET:
				return TARGET_EDEFAULT == null ? target != null : !TARGET_EDEFAULT.equals(target);
			case ServicesPackage.UNSATISFIED_REFERENCE__TARGET_SERVICES:
				return targetServices != null && !targetServices.isEmpty();
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
		result.append(" (name: ");
		result.append(name);
		result.append(", target: ");
		result.append(target);
		result.append(')');
		return result.toString();
	}

} //UnsatisfiedReferenceImpl
