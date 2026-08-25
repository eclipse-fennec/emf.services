/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Component Description</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Declarative blueprint of a component, before it has been instantiated as a ComponentConfiguration. Models org.osgi.service.component.runtime.dto.ComponentDescriptionDTO. Owns the references the component needs and the services it can publish.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getFactory <em>Factory</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getScope <em>Scope</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getImplementationId <em>Implementation Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#isDefaultEnabled <em>Default Enabled</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#isImmediate <em>Immediate</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getConfigurationPolicy <em>Configuration Policy</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getConfigurationPid <em>Configuration Pid</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getServiceInterfaces <em>Service Interfaces</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getFactoryProperties <em>Factory Properties</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getReferences <em>References</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getLifecycleHooks <em>Lifecycle Hooks</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ComponentDescription#getProvider <em>Provider</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription()
 * @model
 * @generated
 */
@ProviderType
public interface ComponentDescription extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Factory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Factory name if this is a DS factory component; null otherwise.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Factory</em>' attribute.
	 * @see #setFactory(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_Factory()
	 * @model
	 * @generated
	 */
	String getFactory();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentDescription#getFactory <em>Factory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Factory</em>' attribute.
	 * @see #getFactory()
	 * @generated
	 */
	void setFactory(String value);

	/**
	 * Returns the value of the '<em><b>Scope</b></em>' attribute.
	 * The default value is <code>"SINGLETON"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ServiceScope}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Service scope at which instances of this component are handed to consumers.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Scope</em>' attribute.
	 * @see org.eclipse.fennec.services.ServiceScope
	 * @see #setScope(ServiceScope)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_Scope()
	 * @model default="SINGLETON" required="true"
	 * @generated
	 */
	ServiceScope getScope();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentDescription#getScope <em>Scope</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Scope</em>' attribute.
	 * @see org.eclipse.fennec.services.ServiceScope
	 * @see #getScope()
	 * @generated
	 */
	void setScope(ServiceScope value);

	/**
	 * Returns the value of the '<em><b>Implementation Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Language-neutral symbolic id of the implementation class/module/file. Replaces OSGi 'implementationClass: String' to avoid Java FQN assumptions.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Implementation Id</em>' attribute.
	 * @see #setImplementationId(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_ImplementationId()
	 * @model required="true"
	 * @generated
	 */
	String getImplementationId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentDescription#getImplementationId <em>Implementation Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Implementation Id</em>' attribute.
	 * @see #getImplementationId()
	 * @generated
	 */
	void setImplementationId(String value);

	/**
	 * Returns the value of the '<em><b>Default Enabled</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Whether the component is enabled at bundle start. Disable via the (future) ServiceComponentRuntime.disableComponent.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Default Enabled</em>' attribute.
	 * @see #setDefaultEnabled(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_DefaultEnabled()
	 * @model default="true" required="true"
	 * @generated
	 */
	boolean isDefaultEnabled();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentDescription#isDefaultEnabled <em>Default Enabled</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Default Enabled</em>' attribute.
	 * @see #isDefaultEnabled()
	 * @generated
	 */
	void setDefaultEnabled(boolean value);

	/**
	 * Returns the value of the '<em><b>Immediate</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * If true, activate as soon as satisfied; if false, only activate when a consumer first requests the service.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Immediate</em>' attribute.
	 * @see #setImmediate(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_Immediate()
	 * @model default="false" required="true"
	 * @generated
	 */
	boolean isImmediate();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentDescription#isImmediate <em>Immediate</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Immediate</em>' attribute.
	 * @see #isImmediate()
	 * @generated
	 */
	void setImmediate(boolean value);

	/**
	 * Returns the value of the '<em><b>Configuration Policy</b></em>' attribute.
	 * The default value is <code>"OPTIONAL"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ConfigurationPolicy}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Behavior when Configuration Admin has no matching configuration.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Configuration Policy</em>' attribute.
	 * @see org.eclipse.fennec.services.ConfigurationPolicy
	 * @see #setConfigurationPolicy(ConfigurationPolicy)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_ConfigurationPolicy()
	 * @model default="OPTIONAL" required="true"
	 * @generated
	 */
	ConfigurationPolicy getConfigurationPolicy();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentDescription#getConfigurationPolicy <em>Configuration Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Configuration Policy</em>' attribute.
	 * @see org.eclipse.fennec.services.ConfigurationPolicy
	 * @see #getConfigurationPolicy()
	 * @generated
	 */
	void setConfigurationPolicy(ConfigurationPolicy value);

	/**
	 * Returns the value of the '<em><b>Configuration Pid</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * PIDs this component consumes from Configuration Admin. Empty = component name is the default PID.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Configuration Pid</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_ConfigurationPid()
	 * @model
	 * @generated
	 */
	EList<String> getConfigurationPid();

	/**
	 * Returns the value of the '<em><b>Service Interfaces</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceInterface}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ServiceInterfaces this component publishes. Non-containment: shared with the catalog and other ComponentDescriptions / ServiceImplementations.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Service Interfaces</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_ServiceInterfaces()
	 * @model
	 * @generated
	 */
	EList<ServiceInterface> getServiceInterfaces();

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Property}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Declared component/service properties (service.ranking, custom properties, target attributes for references).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_Properties()
	 * @model containment="true"
	 * @generated
	 */
	EList<Property> getProperties();

	/**
	 * Returns the value of the '<em><b>Factory Properties</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Property}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Factory-specific properties (DS 1.4). Only meaningful if factory != null.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Factory Properties</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_FactoryProperties()
	 * @model containment="true"
	 * @generated
	 */
	EList<Property> getFactoryProperties();

	/**
	 * Returns the value of the '<em><b>References</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ComponentReference}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Other services this component needs.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>References</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_References()
	 * @model containment="true"
	 * @generated
	 */
	EList<ComponentReference> getReferences();

	/**
	 * Returns the value of the '<em><b>Lifecycle Hooks</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.LifecycleHook}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Activate/deactivate/modified callbacks plus DS 1.4 activation fields. Replaces the flat OSGi DS attributes (activate, deactivate, modified, activationFields, init).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Lifecycle Hooks</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_LifecycleHooks()
	 * @model containment="true"
	 * @generated
	 */
	EList<LifecycleHook> getLifecycleHooks();

	/**
	 * Returns the value of the '<em><b>Provider</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The provider that owns this description. Non-containment: the containing relation runs the other way via ServiceProvider.descriptions.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Provider</em>' reference.
	 * @see #setProvider(ServiceProvider)
	 * @see org.eclipse.fennec.services.ServicesPackage#getComponentDescription_Provider()
	 * @model
	 * @generated
	 */
	ServiceProvider getProvider();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ComponentDescription#getProvider <em>Provider</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Provider</em>' reference.
	 * @see #getProvider()
	 * @generated
	 */
	void setProvider(ServiceProvider value);

} // ComponentDescription
