/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Versioned Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Mixin for elements that carry a semver version (ServiceInterface, ServiceProvider, ServiceImplementation, ServiceException). Together with NamedElement, (name, version) forms a stable identity across catalog releases.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.VersionedElement#getVersion <em>Version</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getVersionedElement()
 * @model interface="true" abstract="true"
 *        annotation="http://www.eclipse.org/emf/2002/Ecore constraints='validSemver'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 validSemver='version = null or version.matches(\'^\\\\d+\\\\.\\\\d+\\\\.\\\\d+(-[0-9A-Za-z.-]+)?$\')'"
 * @generated
 */
@ProviderType
public interface VersionedElement extends EObject {
	/**
	 * Returns the value of the '<em><b>Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Semver string, e.g. '1.4.2' or '2.0.0-rc1'. Validated against the major.minor.micro[-qualifier] grammar via the validSemver invariant. Code Publisher derives artifact semver from this field (see REQUIREMENTS FR-CodeDist-Versioning).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Version</em>' attribute.
	 * @see #setVersion(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getVersionedElement_Version()
	 * @model
	 * @generated
	 */
	String getVersion();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.VersionedElement#getVersion <em>Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Version</em>' attribute.
	 * @see #getVersion()
	 * @generated
	 */
	void setVersion(String value);

} // VersionedElement
