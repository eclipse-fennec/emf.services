/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Principal</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * The WHO: an authenticated identity as the broker or a serving node established it from a verified credential — never taken from a request body, a path segment or a self-declared header. Contrast with the origin token (ClientOrigin, #125): that is an audit label a client chooses, and it stays one. A principal is runtime state like the registration or session it hangs on; it is in no catalog document and in no snapshot.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.Principal#getSubject <em>Subject</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Principal#getIssuer <em>Issuer</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Principal#getScopes <em>Scopes</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getPrincipal()
 * @model
 * @generated
 */
@ProviderType
public interface Principal extends EObject {
	/**
	 * Returns the value of the '<em><b>Subject</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Who: the credential's subject — the OIDC 'sub' of a bearer token, the subject of a client certificate. For a node this is its client identity, and it is what consumerId, the consumer segment of an MQTT response topic and a provider's claim of authorship are checked against.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Subject</em>' attribute.
	 * @see #setSubject(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getPrincipal_Subject()
	 * @model required="true"
	 * @generated
	 */
	String getSubject();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Principal#getSubject <em>Subject</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Subject</em>' attribute.
	 * @see #getSubject()
	 * @generated
	 */
	void setSubject(String value);

	/**
	 * Returns the value of the '<em><b>Issuer</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Who vouched for it: the token's 'iss' or the certificate issuer. Together with subject it is the identity; the same subject from another issuer is somebody else.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Issuer</em>' attribute.
	 * @see #setIssuer(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getPrincipal_Issuer()
	 * @model
	 * @generated
	 */
	String getIssuer();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Principal#getIssuer <em>Issuer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Issuer</em>' attribute.
	 * @see #getIssuer()
	 * @generated
	 */
	void setIssuer(String value);

	/**
	 * Returns the value of the '<em><b>Scopes</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The scopes the credential carried, as verified. Kept so that an authorization decision after the call — a drain, an event delivery — can be made without the credential still being at hand.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Scopes</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getPrincipal_Scopes()
	 * @model
	 * @generated
	 */
	EList<String> getScopes();

} // Principal
