/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Security Scheme</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * The HOW of security: one way a caller proves a principal on one transport. Abstract base extended like ServiceFlavor — concrete subclasses here, further ones by third parties. Contained by the flavor (ServiceFlavor.securitySchemes), because proof is a property of the wire: the same contract may be served with a bearer token over REST and with the connection's identity over MQTT. The name identifies the scheme within the flavor, like an OpenAPI securitySchemes key. Which credentials a CALLER uses to satisfy a scheme is configuration of the calling node and never part of any document.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.SecurityScheme#getKind <em>Kind</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getSecurityScheme()
 * @model abstract="true"
 * @generated
 */
@ProviderType
public interface SecurityScheme extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Kind</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.SecuritySchemeKind}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Discriminator, redundant with the concrete subclass; this is what the implied 'services.security' capability carries and what a consumer filters on.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.SecuritySchemeKind
	 * @see #setKind(SecuritySchemeKind)
	 * @see org.eclipse.fennec.services.ServicesPackage#getSecurityScheme_Kind()
	 * @model required="true"
	 * @generated
	 */
	SecuritySchemeKind getKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.SecurityScheme#getKind <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.SecuritySchemeKind
	 * @see #getKind()
	 * @generated
	 */
	void setKind(SecuritySchemeKind value);

} // SecurityScheme
