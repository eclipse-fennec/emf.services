/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Bearer Token Scheme</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * An OAuth 2.0 / OpenID Connect bearer token (RFC 6750), the backend-to-backend case being a client-credentials token of the calling node. Over REST it travels in the Authorization header; over MQTT 5 as a user property of the request message, the connection's own authentication being the messaging broker's business (ConnectionScheme). The serving node verifies signature, issuer, audience and lifetime itself on every call, whether or not something in front of it already did (defense in depth). The principal is the token's subject.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.BearerTokenScheme#getIssuer <em>Issuer</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.BearerTokenScheme#getAudiences <em>Audiences</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.BearerTokenScheme#getJwksUri <em>Jwks Uri</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.BearerTokenScheme#getScopeClaim <em>Scope Claim</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getBearerTokenScheme()
 * @model
 * @generated
 */
@ProviderType
public interface BearerTokenScheme extends SecurityScheme {
	/**
	 * Returns the value of the '<em><b>Issuer</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The expected 'iss' claim — the identity provider (realm) the serving node trusts. Required: a scheme without an issuer cannot be verified and is invalid.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Issuer</em>' attribute.
	 * @see #setIssuer(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getBearerTokenScheme_Issuer()
	 * @model required="true"
	 * @generated
	 */
	String getIssuer();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.BearerTokenScheme#getIssuer <em>Issuer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Issuer</em>' attribute.
	 * @see #getIssuer()
	 * @generated
	 */
	void setIssuer(String value);

	/**
	 * Returns the value of the '<em><b>Audiences</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Accepted 'aud' values; the token must carry at least one of them. This is what stops a token minted for the broker from being replayed against a provider, or one tenant's token against another's node. Empty = the serving node's configured audience applies.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Audiences</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getBearerTokenScheme_Audiences()
	 * @model
	 * @generated
	 */
	EList<String> getAudiences();

	/**
	 * Returns the value of the '<em><b>Jwks Uri</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Where the verifier fetches the signing keys. Null = discovered from the issuer's well-known configuration.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Jwks Uri</em>' attribute.
	 * @see #setJwksUri(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getBearerTokenScheme_JwksUri()
	 * @model
	 * @generated
	 */
	String getJwksUri();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.BearerTokenScheme#getJwksUri <em>Jwks Uri</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Jwks Uri</em>' attribute.
	 * @see #getJwksUri()
	 * @generated
	 */
	void setJwksUri(String value);

	/**
	 * Returns the value of the '<em><b>Scope Claim</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The claim that carries the scopes of SecurityRequirement.scopes, as the identity provider spells it: 'scope' (space-separated, the OAuth default) or e.g. 'roles'. Null = 'scope'.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Scope Claim</em>' attribute.
	 * @see #setScopeClaim(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getBearerTokenScheme_ScopeClaim()
	 * @model
	 * @generated
	 */
	String getScopeClaim();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.BearerTokenScheme#getScopeClaim <em>Scope Claim</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Scope Claim</em>' attribute.
	 * @see #getScopeClaim()
	 * @generated
	 */
	void setScopeClaim(String value);

} // BearerTokenScheme
