/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

import org.eclipse.fennec.services.MutualTlsScheme;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Mutual Tls Scheme</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.MutualTlsSchemeImpl#getTrustedIssuers <em>Trusted Issuers</em>}</li>
 * </ul>
 *
 * @generated
 */
public class MutualTlsSchemeImpl extends SecuritySchemeImpl implements MutualTlsScheme {
	/**
	 * The cached value of the '{@link #getTrustedIssuers() <em>Trusted Issuers</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTrustedIssuers()
	 * @generated
	 * @ordered
	 */
	protected EList<String> trustedIssuers;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MutualTlsSchemeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.MUTUAL_TLS_SCHEME;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getTrustedIssuers() {
		if (trustedIssuers == null) {
			trustedIssuers = new EDataTypeUniqueEList<String>(String.class, this, ServicesPackage.MUTUAL_TLS_SCHEME__TRUSTED_ISSUERS);
		}
		return trustedIssuers;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.MUTUAL_TLS_SCHEME__TRUSTED_ISSUERS:
				return getTrustedIssuers();
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
			case ServicesPackage.MUTUAL_TLS_SCHEME__TRUSTED_ISSUERS:
				getTrustedIssuers().clear();
				getTrustedIssuers().addAll((Collection<? extends String>)newValue);
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
			case ServicesPackage.MUTUAL_TLS_SCHEME__TRUSTED_ISSUERS:
				getTrustedIssuers().clear();
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
			case ServicesPackage.MUTUAL_TLS_SCHEME__TRUSTED_ISSUERS:
				return trustedIssuers != null && !trustedIssuers.isEmpty();
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
		result.append(" (trustedIssuers: ");
		result.append(trustedIssuers);
		result.append(')');
		return result.toString();
	}

} //MutualTlsSchemeImpl
