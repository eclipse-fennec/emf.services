/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Provider</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Language-neutral equivalent of an OSGi Bundle: a deployment unit that owns component descriptions and registers services. Identifies who has registered what in the registry; used by PDP for authorization (subject = provider).
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceProvider#getSymbolicName <em>Symbolic Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceProvider#getDescriptions <em>Descriptions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceProvider#getImplementations <em>Implementations</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceProvider()
 * @model
 * @generated
 */
@ProviderType
public interface ServiceProvider extends NamedElement, VersionedElement {
	/**
	 * Returns the value of the '<em><b>Symbolic Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * OSGi-style Bundle-SymbolicName, e.g. 'com.example.payments.rest'. Used as the wire-identifier for the provider.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Symbolic Name</em>' attribute.
	 * @see #setSymbolicName(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceProvider_SymbolicName()
	 * @model required="true"
	 * @generated
	 */
	String getSymbolicName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceProvider#getSymbolicName <em>Symbolic Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Symbolic Name</em>' attribute.
	 * @see #getSymbolicName()
	 * @generated
	 */
	void setSymbolicName(String value);

	/**
	 * Returns the value of the '<em><b>Descriptions</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ComponentDescription}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * DS component descriptions owned by this provider. Containment: descriptions live and die with the provider.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Descriptions</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceProvider_Descriptions()
	 * @model containment="true"
	 * @generated
	 */
	EList<ComponentDescription> getDescriptions();

	/**
	 * Returns the value of the '<em><b>Implementations</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceImplementation}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Concrete service implementations owned by this provider. Containment: ownership and authority for publish/withdraw flow from the provider that contains the implementation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Implementations</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceProvider_Implementations()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServiceImplementation> getImplementations();

} // ServiceProvider
