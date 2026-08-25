/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Rest Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * HTTP/REST transport. Operations map to (HTTP method, base path + operation path) tuples; bodies are encoded with the listed content types.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.RestFlavor#getHost <em>Host</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestFlavor#getBasePath <em>Base Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestFlavor#getContentTypes <em>Content Types</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getRestFlavor()
 * @model
 * @generated
 */
@ProviderType
public interface RestFlavor extends ServiceFlavor {
	/**
	 * Returns the value of the '<em><b>Host</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Optional host (scheme://host[:port]). When null, consumers receive the host from the ServiceReference at lookup time (allows the same model to describe many deployment endpoints).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Host</em>' attribute.
	 * @see #setHost(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestFlavor_Host()
	 * @model
	 * @generated
	 */
	String getHost();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RestFlavor#getHost <em>Host</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Host</em>' attribute.
	 * @see #getHost()
	 * @generated
	 */
	void setHost(String value);

	/**
	 * Returns the value of the '<em><b>Base Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Path prefix shared by all operations of this flavor, e.g. '/api/v1/payment'. Operations append their own path on top.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Base Path</em>' attribute.
	 * @see #setBasePath(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestFlavor_BasePath()
	 * @model required="true"
	 * @generated
	 */
	String getBasePath();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RestFlavor#getBasePath <em>Base Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Base Path</em>' attribute.
	 * @see #getBasePath()
	 * @generated
	 */
	void setBasePath(String value);

	/**
	 * Returns the value of the '<em><b>Content Types</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Default Accept / Content-Type values that operations inherit. Empty = 'application/json' assumed.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Content Types</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestFlavor_ContentTypes()
	 * @model
	 * @generated
	 */
	EList<String> getContentTypes();

} // RestFlavor
