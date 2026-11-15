/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Api Key Scheme</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * The caller presents a key the endpoint issued to it, in a header, a query parameter or a cookie — OpenAPI's apiKey scheme (#299). Describes endpoints that are not Fennec Services nodes, such as those a bridge announces; a Fennec Services node never serves with it, and a flavor whose only scheme is an API key is refused rather than served open. Which key a caller presents is configuration of the calling node (ApiKeySupplier), never part of a document. A key identifies a caller and carries no scopes: a requirement that names scopes is not satisfiable by it alone, as with a ConnectionScheme.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ApiKeyScheme#getIn <em>In</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ApiKeyScheme#getParameterName <em>Parameter Name</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getApiKeyScheme()
 * @model
 * @generated
 */
@ProviderType
public interface ApiKeyScheme extends SecurityScheme {
	/**
	 * Returns the value of the '<em><b>In</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ApiKeyLocation}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Where the key travels.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>In</em>' attribute.
	 * @see org.eclipse.fennec.services.ApiKeyLocation
	 * @see #setIn(ApiKeyLocation)
	 * @see org.eclipse.fennec.services.ServicesPackage#getApiKeyScheme_In()
	 * @model required="true"
	 * @generated
	 */
	ApiKeyLocation getIn();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ApiKeyScheme#getIn <em>In</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>In</em>' attribute.
	 * @see org.eclipse.fennec.services.ApiKeyLocation
	 * @see #getIn()
	 * @generated
	 */
	void setIn(ApiKeyLocation value);

	/**
	 * Returns the value of the '<em><b>Parameter Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The name of the header, query parameter or cookie that carries the key, e.g. 'X-API-Key' or 'api_key'. Required: without it a caller cannot tell where to put the key.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Parameter Name</em>' attribute.
	 * @see #setParameterName(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getApiKeyScheme_ParameterName()
	 * @model required="true"
	 * @generated
	 */
	String getParameterName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ApiKeyScheme#getParameterName <em>Parameter Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Parameter Name</em>' attribute.
	 * @see #getParameterName()
	 * @generated
	 */
	void setParameterName(String value);

} // ApiKeyScheme
