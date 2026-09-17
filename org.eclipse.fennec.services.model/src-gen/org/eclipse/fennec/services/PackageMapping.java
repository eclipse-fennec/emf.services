/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Package Mapping</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * One metamodel and the package/module its generated types live in, in the target language. Positional under its binding.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.PackageMapping#getNsURI <em>Ns URI</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.PackageMapping#getTarget <em>Target</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getPackageMapping()
 * @model
 * @generated
 */
@ProviderType
public interface PackageMapping extends EObject {
	/**
	 * Returns the value of the '<em><b>Ns URI</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Namespace URI of the EPackage, as Parameter.eType names it.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Ns URI</em>' attribute.
	 * @see #setNsURI(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getPackageMapping_NsURI()
	 * @model required="true"
	 * @generated
	 */
	String getNsURI();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.PackageMapping#getNsURI <em>Ns URI</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ns URI</em>' attribute.
	 * @see #getNsURI()
	 * @generated
	 */
	void setNsURI(String value);

	/**
	 * Returns the value of the '<em><b>Target</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Package or module prefix for that metamodel's types; the classifier name is appended.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Target</em>' attribute.
	 * @see #setTarget(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getPackageMapping_Target()
	 * @model required="true"
	 * @generated
	 */
	String getTarget();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.PackageMapping#getTarget <em>Target</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' attribute.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(String value);

} // PackageMapping
