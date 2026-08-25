/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Implementation</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A concrete realization of one or more ServiceInterfaces by a single provider, optionally reachable over one or more transport flavors. Distinct from ComponentDescription because not all implementations are DS-driven (plain Java services, hand-wired TS modules, ad-hoc Python objects). When DS-driven, componentDescription points back to the description.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceImplementation#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceImplementation#getImplementationId <em>Implementation Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceImplementation#getServiceInterfaces <em>Service Interfaces</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceImplementation#getFlavors <em>Flavors</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceImplementation#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceImplementation#getComponentDescription <em>Component Description</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceImplementation()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='atLeastOneInterface operationFlavorsCoverInterfaces'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 atLeastOneInterface='serviceInterfaces-&gt;notEmpty()' operationFlavorsCoverInterfaces='flavors-&gt;forAll(f | f.operationFlavors-&gt;forAll(of | serviceInterfaces-&gt;exists(si | si.operations-&gt;includes(of.operation))))'"
 * @generated
 */
@ProviderType
public interface ServiceImplementation extends NamedElement, VersionedElement {
	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Doc text.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceImplementation_Description()
	 * @model
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceImplementation#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Implementation Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Language-neutral symbolic id of the backing class/module/file.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Implementation Id</em>' attribute.
	 * @see #setImplementationId(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceImplementation_ImplementationId()
	 * @model required="true"
	 * @generated
	 */
	String getImplementationId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceImplementation#getImplementationId <em>Implementation Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Implementation Id</em>' attribute.
	 * @see #getImplementationId()
	 * @generated
	 */
	void setImplementationId(String value);

	/**
	 * Returns the value of the '<em><b>Service Interfaces</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceInterface}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ServiceInterfaces this implementation satisfies. At least one. Non-containment: interfaces are owned by the catalog.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Service Interfaces</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceImplementation_ServiceInterfaces()
	 * @model required="true"
	 * @generated
	 */
	EList<ServiceInterface> getServiceInterfaces();

	/**
	 * Returns the value of the '<em><b>Flavors</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceFlavor}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Transport flavors over which this implementation is reachable. Empty = local-only (no remote consumers). When publishing to RemoteServiceRegistry, MUST be non-empty (OCL invariant TODO).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Flavors</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceImplementation_Flavors()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServiceFlavor> getFlavors();

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Property}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Implementation-specific properties (service.ranking, region, tenant, …). Visible to consumers via lookup filters.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceImplementation_Properties()
	 * @model containment="true"
	 * @generated
	 */
	EList<Property> getProperties();

	/**
	 * Returns the value of the '<em><b>Component Description</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Back-link to the DS ComponentDescription, if this implementation is DS-driven. Non-containment; can be null for plain (non-DS) implementations.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Component Description</em>' reference.
	 * @see #setComponentDescription(ComponentDescription)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceImplementation_ComponentDescription()
	 * @model
	 * @generated
	 */
	ComponentDescription getComponentDescription();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceImplementation#getComponentDescription <em>Component Description</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Component Description</em>' reference.
	 * @see #getComponentDescription()
	 * @generated
	 */
	void setComponentDescription(ComponentDescription value);

} // ServiceImplementation
