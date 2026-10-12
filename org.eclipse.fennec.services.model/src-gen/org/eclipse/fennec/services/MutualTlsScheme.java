/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Mutual Tls Scheme</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * The caller proves itself with a client certificate on the TLS connection; the principal is the certificate's subject. Scopes of a SecurityRequirement are then taken from the serving node's mapping of certificate subjects, not from the wire.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.MutualTlsScheme#getTrustedIssuers <em>Trusted Issuers</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getMutualTlsScheme()
 * @model
 * @generated
 */
@ProviderType
public interface MutualTlsScheme extends SecurityScheme {
	/**
	 * Returns the value of the '<em><b>Trusted Issuers</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Distinguished names of the certificate authorities whose client certificates are accepted. Empty = the serving node's trust store decides.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Trusted Issuers</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getMutualTlsScheme_TrustedIssuers()
	 * @model
	 * @generated
	 */
	EList<String> getTrustedIssuers();

} // MutualTlsScheme
