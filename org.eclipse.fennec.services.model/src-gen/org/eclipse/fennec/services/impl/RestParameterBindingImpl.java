/*
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Rest Parameter Binding</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.RestParameterBindingImpl#getParameter <em>Parameter</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.RestParameterBindingImpl#getBinding <em>Binding</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.RestParameterBindingImpl#getWireName <em>Wire Name</em>}</li>
 * </ul>
 *
 * @generated
 */
public class RestParameterBindingImpl extends MinimalEObjectImpl.Container implements RestParameterBinding {
	/**
	 * The cached value of the '{@link #getParameter() <em>Parameter</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getParameter()
	 * @generated
	 * @ordered
	 */
	protected Parameter parameter;

	/**
	 * The default value of the '{@link #getBinding() <em>Binding</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBinding()
	 * @generated
	 * @ordered
	 */
	protected static final ParameterBinding BINDING_EDEFAULT = ParameterBinding.BODY;

	/**
	 * The cached value of the '{@link #getBinding() <em>Binding</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBinding()
	 * @generated
	 * @ordered
	 */
	protected ParameterBinding binding = BINDING_EDEFAULT;

	/**
	 * The default value of the '{@link #getWireName() <em>Wire Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getWireName()
	 * @generated
	 * @ordered
	 */
	protected static final String WIRE_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getWireName() <em>Wire Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getWireName()
	 * @generated
	 * @ordered
	 */
	protected String wireName = WIRE_NAME_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected RestParameterBindingImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.REST_PARAMETER_BINDING;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Parameter getParameter() {
		if (parameter != null && parameter.eIsProxy()) {
			InternalEObject oldParameter = (InternalEObject)parameter;
			parameter = (Parameter)eResolveProxy(oldParameter);
			if (parameter != oldParameter) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.REST_PARAMETER_BINDING__PARAMETER, oldParameter, parameter));
			}
		}
		return parameter;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Parameter basicGetParameter() {
		return parameter;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setParameter(Parameter newParameter) {
		Parameter oldParameter = parameter;
		parameter = newParameter;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.REST_PARAMETER_BINDING__PARAMETER, oldParameter, parameter));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ParameterBinding getBinding() {
		return binding;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBinding(ParameterBinding newBinding) {
		ParameterBinding oldBinding = binding;
		binding = newBinding == null ? BINDING_EDEFAULT : newBinding;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.REST_PARAMETER_BINDING__BINDING, oldBinding, binding));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getWireName() {
		return wireName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setWireName(String newWireName) {
		String oldWireName = wireName;
		wireName = newWireName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.REST_PARAMETER_BINDING__WIRE_NAME, oldWireName, wireName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.REST_PARAMETER_BINDING__PARAMETER:
				if (resolve) return getParameter();
				return basicGetParameter();
			case ServicesPackage.REST_PARAMETER_BINDING__BINDING:
				return getBinding();
			case ServicesPackage.REST_PARAMETER_BINDING__WIRE_NAME:
				return getWireName();
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
			case ServicesPackage.REST_PARAMETER_BINDING__PARAMETER:
				setParameter((Parameter)newValue);
				return;
			case ServicesPackage.REST_PARAMETER_BINDING__BINDING:
				setBinding((ParameterBinding)newValue);
				return;
			case ServicesPackage.REST_PARAMETER_BINDING__WIRE_NAME:
				setWireName((String)newValue);
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
			case ServicesPackage.REST_PARAMETER_BINDING__PARAMETER:
				setParameter((Parameter)null);
				return;
			case ServicesPackage.REST_PARAMETER_BINDING__BINDING:
				setBinding(BINDING_EDEFAULT);
				return;
			case ServicesPackage.REST_PARAMETER_BINDING__WIRE_NAME:
				setWireName(WIRE_NAME_EDEFAULT);
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
			case ServicesPackage.REST_PARAMETER_BINDING__PARAMETER:
				return parameter != null;
			case ServicesPackage.REST_PARAMETER_BINDING__BINDING:
				return binding != BINDING_EDEFAULT;
			case ServicesPackage.REST_PARAMETER_BINDING__WIRE_NAME:
				return WIRE_NAME_EDEFAULT == null ? wireName != null : !WIRE_NAME_EDEFAULT.equals(wireName);
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
		result.append(" (binding: ");
		result.append(binding);
		result.append(", wireName: ");
		result.append(wireName);
		result.append(')');
		return result.toString();
	}

} //RestParameterBindingImpl
