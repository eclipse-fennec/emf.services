/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Capability</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Something a ServiceImplementation or ServiceFlavor offers, modelled like an OSGi Provide-Capability: a namespace plus typed attributes. Matched against Requirement entries of a ConsumerCapability at lookup time (docs/WIRE_CHANNELS.md §6): a Requirement is satisfied when a Capability with the same namespace exists whose attributes match the Requirement's LDAP filter. The effective capability set of an implementation is its own capabilities plus those of its flavors. Namespace conventions (proposal, extensible): 'services.transport' with attributes kind (REST|MQTT) and version; 'services.contentType' with type; 'services.encoding' with format (XMI|JSON). Providers may add their own namespaces ('acme.region').
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.Capability#getNamespace <em>Namespace</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Capability#getAttributes <em>Attributes</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getCapability()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='attributeNamesUnique'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 attributeNamesUnique='attributes-&gt;isUnique(a | a.name)'"
 * @generated
 */
@ProviderType
public interface Capability extends EObject {
	/**
	 * Returns the value of the '<em><b>Namespace</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Capability namespace, dotted lower-case ('services.transport'). Matching is exact and case-sensitive.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Namespace</em>' attribute.
	 * @see #setNamespace(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getCapability_Namespace()
	 * @model required="true"
	 * @generated
	 */
	String getNamespace();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Capability#getNamespace <em>Namespace</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Namespace</em>' attribute.
	 * @see #getNamespace()
	 * @generated
	 */
	void setNamespace(String value);

	/**
	 * Returns the value of the '<em><b>Attributes</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Property}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Typed attributes of the capability (the same Property hierarchy the registry already evaluates LDAP filters against). Property.name is the attribute key; keys are unique within one capability. An empty list means 'present, no further detail' and satisfies any Requirement in the namespace that carries no filter.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Attributes</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getCapability_Attributes()
	 * @model containment="true"
	 * @generated
	 */
	EList<Property> getAttributes();

} // Capability
