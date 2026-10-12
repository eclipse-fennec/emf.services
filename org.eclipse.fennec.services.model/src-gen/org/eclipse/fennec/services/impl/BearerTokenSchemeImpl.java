/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Bearer Token Scheme</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.BearerTokenSchemeImpl#getIssuer <em>Issuer</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.BearerTokenSchemeImpl#getAudiences <em>Audiences</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.BearerTokenSchemeImpl#getJwksUri <em>Jwks Uri</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.BearerTokenSchemeImpl#getScopeClaim <em>Scope Claim</em>}</li>
 * </ul>
 *
 * @generated
 */
public class BearerTokenSchemeImpl extends SecuritySchemeImpl implements BearerTokenScheme {
	/**
	 * The default value of the '{@link #getIssuer() <em>Issuer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIssuer()
	 * @generated
	 * @ordered
	 */
	protected static final String ISSUER_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getIssuer() <em>Issuer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIssuer()
	 * @generated
	 * @ordered
	 */
	protected String issuer = ISSUER_EDEFAULT;

	/**
	 * The cached value of the '{@link #getAudiences() <em>Audiences</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAudiences()
	 * @generated
	 * @ordered
	 */
	protected EList<String> audiences;

	/**
	 * The default value of the '{@link #getJwksUri() <em>Jwks Uri</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getJwksUri()
	 * @generated
	 * @ordered
	 */
	protected static final String JWKS_URI_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getJwksUri() <em>Jwks Uri</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getJwksUri()
	 * @generated
	 * @ordered
	 */
	protected String jwksUri = JWKS_URI_EDEFAULT;

	/**
	 * The default value of the '{@link #getScopeClaim() <em>Scope Claim</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getScopeClaim()
	 * @generated
	 * @ordered
	 */
	protected static final String SCOPE_CLAIM_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getScopeClaim() <em>Scope Claim</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getScopeClaim()
	 * @generated
	 * @ordered
	 */
	protected String scopeClaim = SCOPE_CLAIM_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected BearerTokenSchemeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.BEARER_TOKEN_SCHEME;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getIssuer() {
		return issuer;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setIssuer(String newIssuer) {
		String oldIssuer = issuer;
		issuer = newIssuer;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.BEARER_TOKEN_SCHEME__ISSUER, oldIssuer, issuer));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getAudiences() {
		if (audiences == null) {
			audiences = new EDataTypeUniqueEList<String>(String.class, this, ServicesPackage.BEARER_TOKEN_SCHEME__AUDIENCES);
		}
		return audiences;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getJwksUri() {
		return jwksUri;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setJwksUri(String newJwksUri) {
		String oldJwksUri = jwksUri;
		jwksUri = newJwksUri;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.BEARER_TOKEN_SCHEME__JWKS_URI, oldJwksUri, jwksUri));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getScopeClaim() {
		return scopeClaim;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setScopeClaim(String newScopeClaim) {
		String oldScopeClaim = scopeClaim;
		scopeClaim = newScopeClaim;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.BEARER_TOKEN_SCHEME__SCOPE_CLAIM, oldScopeClaim, scopeClaim));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.BEARER_TOKEN_SCHEME__ISSUER:
				return getIssuer();
			case ServicesPackage.BEARER_TOKEN_SCHEME__AUDIENCES:
				return getAudiences();
			case ServicesPackage.BEARER_TOKEN_SCHEME__JWKS_URI:
				return getJwksUri();
			case ServicesPackage.BEARER_TOKEN_SCHEME__SCOPE_CLAIM:
				return getScopeClaim();
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
			case ServicesPackage.BEARER_TOKEN_SCHEME__ISSUER:
				setIssuer((String)newValue);
				return;
			case ServicesPackage.BEARER_TOKEN_SCHEME__AUDIENCES:
				getAudiences().clear();
				getAudiences().addAll((Collection<? extends String>)newValue);
				return;
			case ServicesPackage.BEARER_TOKEN_SCHEME__JWKS_URI:
				setJwksUri((String)newValue);
				return;
			case ServicesPackage.BEARER_TOKEN_SCHEME__SCOPE_CLAIM:
				setScopeClaim((String)newValue);
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
			case ServicesPackage.BEARER_TOKEN_SCHEME__ISSUER:
				setIssuer(ISSUER_EDEFAULT);
				return;
			case ServicesPackage.BEARER_TOKEN_SCHEME__AUDIENCES:
				getAudiences().clear();
				return;
			case ServicesPackage.BEARER_TOKEN_SCHEME__JWKS_URI:
				setJwksUri(JWKS_URI_EDEFAULT);
				return;
			case ServicesPackage.BEARER_TOKEN_SCHEME__SCOPE_CLAIM:
				setScopeClaim(SCOPE_CLAIM_EDEFAULT);
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
			case ServicesPackage.BEARER_TOKEN_SCHEME__ISSUER:
				return ISSUER_EDEFAULT == null ? issuer != null : !ISSUER_EDEFAULT.equals(issuer);
			case ServicesPackage.BEARER_TOKEN_SCHEME__AUDIENCES:
				return audiences != null && !audiences.isEmpty();
			case ServicesPackage.BEARER_TOKEN_SCHEME__JWKS_URI:
				return JWKS_URI_EDEFAULT == null ? jwksUri != null : !JWKS_URI_EDEFAULT.equals(jwksUri);
			case ServicesPackage.BEARER_TOKEN_SCHEME__SCOPE_CLAIM:
				return SCOPE_CLAIM_EDEFAULT == null ? scopeClaim != null : !SCOPE_CLAIM_EDEFAULT.equals(scopeClaim);
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
		result.append(" (issuer: ");
		result.append(issuer);
		result.append(", audiences: ");
		result.append(audiences);
		result.append(", jwksUri: ");
		result.append(jwksUri);
		result.append(", scopeClaim: ");
		result.append(scopeClaim);
		result.append(')');
		return result.toString();
	}

} //BearerTokenSchemeImpl
