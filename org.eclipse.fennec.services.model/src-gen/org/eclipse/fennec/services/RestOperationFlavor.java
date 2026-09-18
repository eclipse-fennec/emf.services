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
 * REST binding for one operation: HTTP method + path under the RestFlavor.basePath + expected success status codes + optional per-parameter wire placement (parameterBindings) + the status a declared error travels as (exceptionBindings).
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.RestOperationFlavor#getMethod <em>Method</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestOperationFlavor#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestOperationFlavor#getReturnCodes <em>Return Codes</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestOperationFlavor#getParameterBindings <em>Parameter Bindings</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestOperationFlavor#getExceptionBindings <em>Exception Bindings</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getRestOperationFlavor()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='bindingsReferenceOperationParameters oneBindingPerParameter pathBindingsNeedPath exceptionBindingsReferenceOperationExceptions oneBindingPerException'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 bindingsReferenceOperationParameters='parameterBindings-&gt;forAll(b | operation.parameters-&gt;includes(b.parameter))' oneBindingPerParameter='parameterBindings-&gt;isUnique(b | b.parameter)' pathBindingsNeedPath='parameterBindings-&gt;forAll(b | b.binding.toString() &lt;&gt; \'PATH\' or path &lt;&gt; null)' exceptionBindingsReferenceOperationExceptions='exceptionBindings-&gt;forAll(b | operation.exceptions-&gt;includes(b.exception))' oneBindingPerException='exceptionBindings-&gt;isUnique(b | b.exception)'"
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

	/**
	 * Returns the value of the '<em><b>Parameter Bindings</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.RestParameterBinding}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Where each Parameter of the operation travels on the wire. Lets a REST binding express GET /payments/{id}?currency=EUR with a tenant header instead of forcing everything into the body (OPEN_ISSUES W3). A parameter without an entry keeps the convention the SDKs used before the bindings were driven: a single EObject is the payload, anything else a query parameter. The model names BODY as the default, but there is no encoding for several primitive arguments in one payload, so an undeclared parameter travels the older way until that wire shape exists.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Parameter Bindings</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestOperationFlavor_ParameterBindings()
	 * @model containment="true"
	 * @generated
	 */
	EList<RestParameterBinding> getParameterBindings();

	/**
	 * Returns the value of the '<em><b>Exception Bindings</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.RestExceptionBinding}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Which HTTP status each declared error of the operation travels as. returnCodes says which statuses mean success; this says what the others mean. An exception without an entry is a 500 — it is still the exception it is, just without a status of its own. Transport-specific by design, like ParameterBinding: it lives in the flavor layer, so a status change never moves a contract address, and an MQTT flavor answers with a reason code instead.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Exception Bindings</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestOperationFlavor_ExceptionBindings()
	 * @model containment="true"
	 * @generated
	 */
	EList<RestExceptionBinding> getExceptionBindings();

} // RestOperationFlavor
