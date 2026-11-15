/*
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.fennec.services.ApiKeyLocation;
import org.eclipse.fennec.services.ApiKeyScheme;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Api Key Scheme</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ApiKeySchemeImpl#getIn <em>In</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ApiKeySchemeImpl#getParameterName <em>Parameter Name</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ApiKeySchemeImpl extends SecuritySchemeImpl implements ApiKeyScheme {
	/**
	 * The default value of the '{@link #getIn() <em>In</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIn()
	 * @generated
	 * @ordered
	 */
	protected static final ApiKeyLocation IN_EDEFAULT = ApiKeyLocation.HEADER;

	/**
	 * The cached value of the '{@link #getIn() <em>In</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIn()
	 * @generated
	 * @ordered
	 */
	protected ApiKeyLocation in = IN_EDEFAULT;

	/**
	 * The default value of the '{@link #getParameterName() <em>Parameter Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getParameterName()
	 * @generated
	 * @ordered
	 */
	protected static final String PARAMETER_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getParameterName() <em>Parameter Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getParameterName()
	 * @generated
	 * @ordered
	 */
	protected String parameterName = PARAMETER_NAME_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ApiKeySchemeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.API_KEY_SCHEME;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ApiKeyLocation getIn() {
		return in;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setIn(ApiKeyLocation newIn) {
		ApiKeyLocation oldIn = in;
		in = newIn == null ? IN_EDEFAULT : newIn;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.API_KEY_SCHEME__IN, oldIn, in));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getParameterName() {
		return parameterName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setParameterName(String newParameterName) {
		String oldParameterName = parameterName;
		parameterName = newParameterName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.API_KEY_SCHEME__PARAMETER_NAME, oldParameterName, parameterName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.API_KEY_SCHEME__IN:
				return getIn();
			case ServicesPackage.API_KEY_SCHEME__PARAMETER_NAME:
				return getParameterName();
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
			case ServicesPackage.API_KEY_SCHEME__IN:
				setIn((ApiKeyLocation)newValue);
				return;
			case ServicesPackage.API_KEY_SCHEME__PARAMETER_NAME:
				setParameterName((String)newValue);
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
			case ServicesPackage.API_KEY_SCHEME__IN:
				setIn(IN_EDEFAULT);
				return;
			case ServicesPackage.API_KEY_SCHEME__PARAMETER_NAME:
				setParameterName(PARAMETER_NAME_EDEFAULT);
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
			case ServicesPackage.API_KEY_SCHEME__IN:
				return in != IN_EDEFAULT;
			case ServicesPackage.API_KEY_SCHEME__PARAMETER_NAME:
				return PARAMETER_NAME_EDEFAULT == null ? parameterName != null : !PARAMETER_NAME_EDEFAULT.equals(parameterName);
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
		result.append(" (in: ");
		result.append(in);
		result.append(", parameterName: ");
		result.append(parameterName);
		result.append(')');
		return result.toString();
	}

} //ApiKeySchemeImpl
