/*
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Service Invocation Result</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInvocationResultImpl#getValue <em>Value</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInvocationResultImpl#getDiagnostic <em>Diagnostic</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ServiceInvocationResultImpl extends MinimalEObjectImpl.Container implements ServiceInvocationResult {
	/**
	 * The cached value of the '{@link #getValue() <em>Value</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getValue()
	 * @generated
	 * @ordered
	 */
	protected Property value;

	/**
	 * The cached value of the '{@link #getDiagnostic() <em>Diagnostic</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDiagnostic()
	 * @generated
	 * @ordered
	 */
	protected Diagnostic diagnostic;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServiceInvocationResultImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.SERVICE_INVOCATION_RESULT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Property getValue() {
		return value;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetValue(Property newValue, NotificationChain msgs) {
		Property oldValue = value;
		value = newValue;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INVOCATION_RESULT__VALUE, oldValue, newValue);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setValue(Property newValue) {
		if (newValue != value) {
			NotificationChain msgs = null;
			if (value != null)
				msgs = ((InternalEObject)value).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ServicesPackage.SERVICE_INVOCATION_RESULT__VALUE, null, msgs);
			if (newValue != null)
				msgs = ((InternalEObject)newValue).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ServicesPackage.SERVICE_INVOCATION_RESULT__VALUE, null, msgs);
			msgs = basicSetValue(newValue, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INVOCATION_RESULT__VALUE, newValue, newValue));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Diagnostic getDiagnostic() {
		return diagnostic;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetDiagnostic(Diagnostic newDiagnostic, NotificationChain msgs) {
		Diagnostic oldDiagnostic = diagnostic;
		diagnostic = newDiagnostic;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INVOCATION_RESULT__DIAGNOSTIC, oldDiagnostic, newDiagnostic);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDiagnostic(Diagnostic newDiagnostic) {
		if (newDiagnostic != diagnostic) {
			NotificationChain msgs = null;
			if (diagnostic != null)
				msgs = ((InternalEObject)diagnostic).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ServicesPackage.SERVICE_INVOCATION_RESULT__DIAGNOSTIC, null, msgs);
			if (newDiagnostic != null)
				msgs = ((InternalEObject)newDiagnostic).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ServicesPackage.SERVICE_INVOCATION_RESULT__DIAGNOSTIC, null, msgs);
			msgs = basicSetDiagnostic(newDiagnostic, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INVOCATION_RESULT__DIAGNOSTIC, newDiagnostic, newDiagnostic));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.SERVICE_INVOCATION_RESULT__VALUE:
				return basicSetValue(null, msgs);
			case ServicesPackage.SERVICE_INVOCATION_RESULT__DIAGNOSTIC:
				return basicSetDiagnostic(null, msgs);
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
			case ServicesPackage.SERVICE_INVOCATION_RESULT__VALUE:
				return getValue();
			case ServicesPackage.SERVICE_INVOCATION_RESULT__DIAGNOSTIC:
				return getDiagnostic();
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
			case ServicesPackage.SERVICE_INVOCATION_RESULT__VALUE:
				setValue((Property)newValue);
				return;
			case ServicesPackage.SERVICE_INVOCATION_RESULT__DIAGNOSTIC:
				setDiagnostic((Diagnostic)newValue);
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
			case ServicesPackage.SERVICE_INVOCATION_RESULT__VALUE:
				setValue((Property)null);
				return;
			case ServicesPackage.SERVICE_INVOCATION_RESULT__DIAGNOSTIC:
				setDiagnostic((Diagnostic)null);
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
			case ServicesPackage.SERVICE_INVOCATION_RESULT__VALUE:
				return value != null;
			case ServicesPackage.SERVICE_INVOCATION_RESULT__DIAGNOSTIC:
				return diagnostic != null;
		}
		return super.eIsSet(featureID);
	}

} //ServiceInvocationResultImpl
