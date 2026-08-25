/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Rest Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * REST binding for one operation: HTTP method + path under the RestFlavor.basePath + expected success status codes.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.RestOperationFlavor#getMethod <em>Method</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestOperationFlavor#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestOperationFlavor#getReturnCodes <em>Return Codes</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getRestOperationFlavor()
 * @model
 * @generated
 */
@ProviderType
public interface RestOperationFlavor extends ServiceOperationFlavor {
	/**
	 * Returns the value of the '<em><b>Method</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.HttpMethod}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * HTTP method (GET, POST, …).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Method</em>' attribute.
	 * @see org.eclipse.fennec.services.HttpMethod
	 * @see #setMethod(HttpMethod)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestOperationFlavor_Method()
	 * @model required="true"
	 * @generated
	 */
	HttpMethod getMethod();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RestOperationFlavor#getMethod <em>Method</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Method</em>' attribute.
	 * @see org.eclipse.fennec.services.HttpMethod
	 * @see #getMethod()
	 * @generated
	 */
	void setMethod(HttpMethod value);

	/**
	 * Returns the value of the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Path appended to RestFlavor.basePath. Null = operation lives directly at basePath. Path templates ('/payments/{id}') are allowed; the framework substitutes path parameters by name from the operation's Parameters.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Path</em>' attribute.
	 * @see #setPath(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestOperationFlavor_Path()
	 * @model
	 * @generated
	 */
	String getPath();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RestOperationFlavor#getPath <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Path</em>' attribute.
	 * @see #getPath()
	 * @generated
	 */
	void setPath(String value);

	/**
	 * Returns the value of the '<em><b>Return Codes</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.Integer}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * HTTP status codes that count as a successful response (typically [200] or [200, 204]). Any other status is mapped to one of the operation's ServiceExceptions.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Return Codes</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestOperationFlavor_ReturnCodes()
	 * @model required="true"
	 * @generated
	 */
	EList<Integer> getReturnCodes();

} // RestOperationFlavor
