/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Component Reference</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A declared dependency of a component on another service. Models org.osgi.service.component.runtime.dto.ReferenceDTO. Drives whether a ComponentConfiguration becomes SATISFIED.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ComponentReference#getInterfaceName <em>Interface Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentReference#getCardinality <em>Cardinality</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentReference#getPolicy <em>Policy</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentReference#getPolicyOption <em>Policy Option</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentReference#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentReference#getScope <em>Scope</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentReference#getCollectionType <em>Collection Type</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentReference#getParameter <em>Parameter</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentReference#getBindings <em>Bindings</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference()
 * @model
 * @generated
 */
@ProviderType
public interface ComponentReference extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Interface Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Name of the required ServiceInterface (matches ServiceInterface.name).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Interface Name</em>' attribute.
	 * @see #setInterfaceName(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference_InterfaceName()
	 * @model required="true"
	 * @generated
	 */
	String getInterfaceName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentReference#getInterfaceName <em>Interface Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Interface Name</em>' attribute.
	 * @see #getInterfaceName()
	 * @generated
	 */
	void setInterfaceName(String value);

	/**
	 * Returns the value of the '<em><b>Cardinality</b></em>' attribute.
	 * The default value is <code>"ONE"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ReferenceCardinality}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * How many targets are needed/allowed.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Cardinality</em>' attribute.
	 * @see org.eclipse.fennec.services.ReferenceCardinality
	 * @see #setCardinality(ReferenceCardinality)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference_Cardinality()
	 * @model default="ONE" required="true"
	 * @generated
	 */
	ReferenceCardinality getCardinality();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentReference#getCardinality <em>Cardinality</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Cardinality</em>' attribute.
	 * @see org.eclipse.fennec.services.ReferenceCardinality
	 * @see #getCardinality()
	 * @generated
	 */
	void setCardinality(ReferenceCardinality value);

	/**
	 * Returns the value of the '<em><b>Policy</b></em>' attribute.
	 * The default value is <code>"STATIC"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ReferencePolicy}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Rebinding strategy when targets change.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Policy</em>' attribute.
	 * @see org.eclipse.fennec.services.ReferencePolicy
	 * @see #setPolicy(ReferencePolicy)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference_Policy()
	 * @model default="STATIC" required="true"
	 * @generated
	 */
	ReferencePolicy getPolicy();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentReference#getPolicy <em>Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Policy</em>' attribute.
	 * @see org.eclipse.fennec.services.ReferencePolicy
	 * @see #getPolicy()
	 * @generated
	 */
	void setPolicy(ReferencePolicy value);

	/**
	 * Returns the value of the '<em><b>Policy Option</b></em>' attribute.
	 * The default value is <code>"RELUCTANT"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ReferencePolicyOption}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Whether to switch to a higher-ranked target when one appears.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Policy Option</em>' attribute.
	 * @see org.eclipse.fennec.services.ReferencePolicyOption
	 * @see #setPolicyOption(ReferencePolicyOption)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference_PolicyOption()
	 * @model default="RELUCTANT" required="true"
	 * @generated
	 */
	ReferencePolicyOption getPolicyOption();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentReference#getPolicyOption <em>Policy Option</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Policy Option</em>' attribute.
	 * @see org.eclipse.fennec.services.ReferencePolicyOption
	 * @see #getPolicyOption()
	 * @generated
	 */
	void setPolicyOption(ReferencePolicyOption value);

	/**
	 * Returns the value of the '<em><b>Target</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * LDAP filter narrowing the set of acceptable targets, e.g. '(currency=EUR)'. Empty/unset = any target of the named interface.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Target</em>' attribute.
	 * @see #setTarget(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference_Target()
	 * @model
	 * @generated
	 */
	String getTarget();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentReference#getTarget <em>Target</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' attribute.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(String value);

	/**
	 * Returns the value of the '<em><b>Scope</b></em>' attribute.
	 * The default value is <code>"BUNDLE"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ServiceScope}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Scope at which the target is obtained from a ServiceFactory/PrototypeServiceFactory.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Scope</em>' attribute.
	 * @see org.eclipse.fennec.services.ServiceScope
	 * @see #setScope(ServiceScope)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference_Scope()
	 * @model default="BUNDLE" required="true"
	 * @generated
	 */
	ServiceScope getScope();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentReference#getScope <em>Scope</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Scope</em>' attribute.
	 * @see org.eclipse.fennec.services.ServiceScope
	 * @see #getScope()
	 * @generated
	 */
	void setScope(ServiceScope value);

	/**
	 * Returns the value of the '<em><b>Collection Type</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.CollectionType}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Only meaningful for multi-cardinality references with field-injection: element type expected by the collection field.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Collection Type</em>' attribute.
	 * @see org.eclipse.fennec.services.CollectionType
	 * @see #setCollectionType(CollectionType)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference_CollectionType()
	 * @model
	 * @generated
	 */
	CollectionType getCollectionType();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentReference#getCollectionType <em>Collection Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Collection Type</em>' attribute.
	 * @see org.eclipse.fennec.services.CollectionType
	 * @see #getCollectionType()
	 * @generated
	 */
	void setCollectionType(CollectionType value);

	/**
	 * Returns the value of the '<em><b>Parameter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * DS 1.4: zero-based constructor parameter index when the reference is injected via the constructor.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Parameter</em>' attribute.
	 * @see #setParameter(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference_Parameter()
	 * @model
	 * @generated
	 */
	int getParameter();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentReference#getParameter <em>Parameter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Parameter</em>' attribute.
	 * @see #getParameter()
	 * @generated
	 */
	void setParameter(int value);

	/**
	 * Returns the value of the '<em><b>Bindings</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ReferenceBinding}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Callback / field hooks for this reference. Empty = the component is happy to be activated without explicit binding callbacks.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Bindings</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentReference_Bindings()
	 * @model containment="true"
	 * @generated
	 */
	EList<ReferenceBinding> getBindings();

} // ComponentReference
