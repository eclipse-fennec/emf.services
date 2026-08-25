/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Service Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationFlavorImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationFlavorImpl#getOperation <em>Operation</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationFlavorImpl#getConsumes <em>Consumes</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationFlavorImpl#getProduces <em>Produces</em>}</li>
 * </ul>
 *
 * @generated
 */
public abstract class ServiceOperationFlavorImpl extends MinimalEObjectImpl.Container implements ServiceOperationFlavor {
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
	 * The cached value of the '{@link #getOperation() <em>Operation</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOperation()
	 * @generated
	 * @ordered
	 */
	protected ServiceOperation operation;

	/**
	 * The cached value of the '{@link #getConsumes() <em>Consumes</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConsumes()
	 * @generated
	 * @ordered
	 */
	protected EList<String> consumes;

	/**
	 * The cached value of the '{@link #getProduces() <em>Produces</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProduces()
	 * @generated
	 * @ordered
	 */
	protected EList<String> produces;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServiceOperationFlavorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.SERVICE_OPERATION_FLAVOR;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_OPERATION_FLAVOR__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceOperation getOperation() {
		if (operation != null && operation.eIsProxy()) {
			InternalEObject oldOperation = (InternalEObject)operation;
			operation = (ServiceOperation)eResolveProxy(oldOperation);
			if (operation != oldOperation) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.SERVICE_OPERATION_FLAVOR__OPERATION, oldOperation, operation));
			}
		}
		return operation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServiceOperation basicGetOperation() {
		return operation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOperation(ServiceOperation newOperation) {
		ServiceOperation oldOperation = operation;
		operation = newOperation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_OPERATION_FLAVOR__OPERATION, oldOperation, operation));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getConsumes() {
		if (consumes == null) {
			consumes = new EDataTypeUniqueEList<String>(String.class, this, ServicesPackage.SERVICE_OPERATION_FLAVOR__CONSUMES);
		}
		return consumes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getProduces() {
		if (produces == null) {
			produces = new EDataTypeUniqueEList<String>(String.class, this, ServicesPackage.SERVICE_OPERATION_FLAVOR__PRODUCES);
		}
		return produces;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__NAME:
				return getName();
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__OPERATION:
				if (resolve) return getOperation();
				return basicGetOperation();
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__CONSUMES:
				return getConsumes();
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__PRODUCES:
				return getProduces();
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
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__OPERATION:
				setOperation((ServiceOperation)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__CONSUMES:
				getConsumes().clear();
				getConsumes().addAll((Collection<? extends String>)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__PRODUCES:
				getProduces().clear();
				getProduces().addAll((Collection<? extends String>)newValue);
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
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__OPERATION:
				setOperation((ServiceOperation)null);
				return;
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__CONSUMES:
				getConsumes().clear();
				return;
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__PRODUCES:
				getProduces().clear();
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
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__OPERATION:
				return operation != null;
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__CONSUMES:
				return consumes != null && !consumes.isEmpty();
			case ServicesPackage.SERVICE_OPERATION_FLAVOR__PRODUCES:
				return produces != null && !produces.isEmpty();
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
		result.append(", consumes: ");
		result.append(consumes);
		result.append(", produces: ");
		result.append(produces);
		result.append(')');
		return result.toString();
	}

} //ServiceOperationFlavorImpl
