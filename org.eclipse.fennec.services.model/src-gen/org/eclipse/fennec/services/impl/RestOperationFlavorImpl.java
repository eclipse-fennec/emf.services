/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Rest Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.RestOperationFlavorImpl#getMethod <em>Method</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.RestOperationFlavorImpl#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.RestOperationFlavorImpl#getReturnCodes <em>Return Codes</em>}</li>
 * </ul>
 *
 * @generated
 */
public class RestOperationFlavorImpl extends ServiceOperationFlavorImpl implements RestOperationFlavor {
	/**
	 * The default value of the '{@link #getMethod() <em>Method</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMethod()
	 * @generated
	 * @ordered
	 */
	protected static final HttpMethod METHOD_EDEFAULT = HttpMethod.GET;

	/**
	 * The cached value of the '{@link #getMethod() <em>Method</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMethod()
	 * @generated
	 * @ordered
	 */
	protected HttpMethod method = METHOD_EDEFAULT;

	/**
	 * The default value of the '{@link #getPath() <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPath()
	 * @generated
	 * @ordered
	 */
	protected static final String PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPath() <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPath()
	 * @generated
	 * @ordered
	 */
	protected String path = PATH_EDEFAULT;

	/**
	 * The cached value of the '{@link #getReturnCodes() <em>Return Codes</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReturnCodes()
	 * @generated
	 * @ordered
	 */
	protected EList<Integer> returnCodes;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected RestOperationFlavorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.REST_OPERATION_FLAVOR;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public HttpMethod getMethod() {
		return method;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMethod(HttpMethod newMethod) {
		HttpMethod oldMethod = method;
		method = newMethod == null ? METHOD_EDEFAULT : newMethod;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.REST_OPERATION_FLAVOR__METHOD, oldMethod, method));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getPath() {
		return path;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPath(String newPath) {
		String oldPath = path;
		path = newPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.REST_OPERATION_FLAVOR__PATH, oldPath, path));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Integer> getReturnCodes() {
		if (returnCodes == null) {
			returnCodes = new EDataTypeUniqueEList<Integer>(Integer.class, this, ServicesPackage.REST_OPERATION_FLAVOR__RETURN_CODES);
		}
		return returnCodes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.REST_OPERATION_FLAVOR__METHOD:
				return getMethod();
			case ServicesPackage.REST_OPERATION_FLAVOR__PATH:
				return getPath();
			case ServicesPackage.REST_OPERATION_FLAVOR__RETURN_CODES:
				return getReturnCodes();
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
			case ServicesPackage.REST_OPERATION_FLAVOR__METHOD:
				setMethod((HttpMethod)newValue);
				return;
			case ServicesPackage.REST_OPERATION_FLAVOR__PATH:
				setPath((String)newValue);
				return;
			case ServicesPackage.REST_OPERATION_FLAVOR__RETURN_CODES:
				getReturnCodes().clear();
				getReturnCodes().addAll((Collection<? extends Integer>)newValue);
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
			case ServicesPackage.REST_OPERATION_FLAVOR__METHOD:
				setMethod(METHOD_EDEFAULT);
				return;
			case ServicesPackage.REST_OPERATION_FLAVOR__PATH:
				setPath(PATH_EDEFAULT);
				return;
			case ServicesPackage.REST_OPERATION_FLAVOR__RETURN_CODES:
				getReturnCodes().clear();
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
			case ServicesPackage.REST_OPERATION_FLAVOR__METHOD:
				return method != METHOD_EDEFAULT;
			case ServicesPackage.REST_OPERATION_FLAVOR__PATH:
				return PATH_EDEFAULT == null ? path != null : !PATH_EDEFAULT.equals(path);
			case ServicesPackage.REST_OPERATION_FLAVOR__RETURN_CODES:
				return returnCodes != null && !returnCodes.isEmpty();
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
		result.append(" (method: ");
		result.append(method);
		result.append(", path: ");
		result.append(path);
		result.append(", returnCodes: ");
		result.append(returnCodes);
		result.append(')');
		return result.toString();
	}

} //RestOperationFlavorImpl
