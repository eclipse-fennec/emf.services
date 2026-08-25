/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Component Configuration</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Runtime instance of a ComponentDescription with resolved configuration and references. Models org.osgi.service.component.runtime.dto.ComponentConfigurationDTO. The id attribute carries OSGi component.id; it is intentionally NOT marked iD=true because NamedElement.name is already the XMI identifier.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ComponentConfiguration#getId <em>Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentConfiguration#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentConfiguration#getState <em>State</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentConfiguration#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentConfiguration#getSatisfiedReferences <em>Satisfied References</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentConfiguration#getUnsatisfiedReferences <em>Unsatisfied References</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentConfiguration#getFailure <em>Failure</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentConfiguration#getService <em>Service</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getComponentConfiguration()
 * @model annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 failureOnlyWhenFailed='(state = ddsr::ComponentState::FAILED_ACTIVATION) = (failure &lt;&gt; null)'"
 * @generated
 */
@ProviderType
public interface ComponentConfiguration extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * OSGi component.id, runtime-assigned. Unique within the LocalServiceRegistry but not necessarily across the federation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentConfiguration_Id()
	 * @model required="true"
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentConfiguration#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Description</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The ComponentDescription this configuration is an instance of. Non-containment.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Description</em>' reference.
	 * @see #setDescription(ComponentDescription)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentConfiguration_Description()
	 * @model required="true"
	 * @generated
	 */
	ComponentDescription getDescription();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentConfiguration#getDescription <em>Description</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' reference.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(ComponentDescription value);

	/**
	 * Returns the value of the '<em><b>State</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ComponentState}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Current lifecycle state. The Java/TS DDSR runtime MUST agree on the same transitions for the same observable events (NFR-Behavioral-Parity).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>State</em>' attribute.
	 * @see org.eclipse.fennec.services.ComponentState
	 * @see #setState(ComponentState)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentConfiguration_State()
	 * @model required="true"
	 * @generated
	 */
	ComponentState getState();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentConfiguration#getState <em>State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>State</em>' attribute.
	 * @see org.eclipse.fennec.services.ComponentState
	 * @see #getState()
	 * @generated
	 */
	void setState(ComponentState value);

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Property}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Effective properties at this point in time (Description.properties merged with Configuration Admin overrides).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentConfiguration_Properties()
	 * @model containment="true"
	 * @generated
	 */
	EList<Property> getProperties();

	/**
	 * Returns the value of the '<em><b>Satisfied References</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.SatisfiedReference}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ComponentReferences whose cardinality is currently satisfied.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Satisfied References</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentConfiguration_SatisfiedReferences()
	 * @model containment="true"
	 * @generated
	 */
	EList<SatisfiedReference> getSatisfiedReferences();

	/**
	 * Returns the value of the '<em><b>Unsatisfied References</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.UnsatisfiedReference}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ComponentReferences whose cardinality is not currently met (the reason the configuration is UNSATISFIED_REFERENCE, if applicable).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Unsatisfied References</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentConfiguration_UnsatisfiedReferences()
	 * @model containment="true"
	 * @generated
	 */
	EList<UnsatisfiedReference> getUnsatisfiedReferences();

	/**
	 * Returns the value of the '<em><b>Failure</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Structured failure information when state = FAILED_ACTIVATION. Must be null in any other state (OCL invariant TODO).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Failure</em>' containment reference.
	 * @see #setFailure(Diagnostic)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentConfiguration_Failure()
	 * @model containment="true"
	 * @generated
	 */
	Diagnostic getFailure();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentConfiguration#getFailure <em>Failure</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Failure</em>' containment reference.
	 * @see #getFailure()
	 * @generated
	 */
	void setFailure(Diagnostic value);

	/**
	 * Returns the value of the '<em><b>Service</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The ServiceReference the configuration publishes when ACTIVE. Null if the component is not a service publisher (e.g. immediate=true with no provided service).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Service</em>' reference.
	 * @see #setService(ServiceReference)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentConfiguration_Service()
	 * @model
	 * @generated
	 */
	ServiceReference getService();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentConfiguration#getService <em>Service</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Service</em>' reference.
	 * @see #getService()
	 * @generated
	 */
	void setService(ServiceReference value);

} // ComponentConfiguration
