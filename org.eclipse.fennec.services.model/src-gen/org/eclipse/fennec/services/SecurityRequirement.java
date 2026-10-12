/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Security Requirement</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * The WHAT of security: what a caller must present before an operation runs. Transport-neutral and part of the contract (ServiceInterface.security, ServiceOperation.security), unlike a SecurityScheme, which is the HOW and lives on the flavor. Scopes are the contract's vocabulary, as in OpenAPI: a name the caller's credential must carry. Mapping a scope to roles, groups or clients is the identity provider's business and never touches the document. What this class cannot say, by design: object-level rules such as 'only the registration you published' or 'only your own session' — those stay with the serving code, the way row-level rules stay with a database.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.SecurityRequirement#isAuthenticated <em>Authenticated</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.SecurityRequirement#getScopes <em>Scopes</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getSecurityRequirement()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='scopesImplyAuthenticated'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 scopesImplyAuthenticated='scopes-&gt;isEmpty() or authenticated'"
 * @generated
 */
@ProviderType
public interface SecurityRequirement extends EObject {
	/**
	 * Returns the value of the '<em><b>Authenticated</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * True = the caller must be an authenticated principal; an anonymous call is refused before the operation runs. False with scopes present is a contradiction and invalid.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Authenticated</em>' attribute.
	 * @see #setAuthenticated(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getSecurityRequirement_Authenticated()
	 * @model
	 * @generated
	 */
	boolean isAuthenticated();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.SecurityRequirement#isAuthenticated <em>Authenticated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Authenticated</em>' attribute.
	 * @see #isAuthenticated()
	 * @generated
	 */
	void setAuthenticated(boolean value);

	/**
	 * Returns the value of the '<em><b>Scopes</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Scope names the caller's credential must carry, ALL of them (AND). Alternatives are expressed as separate operations or separate contracts, not here. Empty with authenticated = true means 'any authenticated principal'. Spelled the way the identity provider will see them, e.g. 'payment:write', so the document is what a scope-to-role mapping is generated from.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Scopes</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getSecurityRequirement_Scopes()
	 * @model
	 * @generated
	 */
	EList<String> getScopes();

} // SecurityRequirement
