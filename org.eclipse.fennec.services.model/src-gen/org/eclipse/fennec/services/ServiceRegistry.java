/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Registry</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Abstract registry base. Holds the shared lookup operations that work the same for local and remote registries. Concrete subclasses are LocalServiceRegistry (in-process, OSGi-like) and RemoteServiceRegistry (the central broker holding the API catalog and global implementation index).
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistry#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistry#getPublishHooks <em>Publish Hooks</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistry#getDiscoveryHooks <em>Discovery Hooks</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistry#getDistributionHooks <em>Distribution Hooks</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistry()
 * @model abstract="true"
 * @generated
 */
@ProviderType
public interface ServiceRegistry extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Kind</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.RegistryKind}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Discriminator. Duplicates the subclass relationship but simplifies filtering of heterogeneous registry lists.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.RegistryKind
	 * @see #setKind(RegistryKind)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistry_Kind()
	 * @model required="true"
	 * @generated
	 */
	RegistryKind getKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceRegistry#getKind <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.RegistryKind
	 * @see #getKind()
	 * @generated
	 */
	void setKind(RegistryKind value);

	/**
	 * Returns the value of the '<em><b>Publish Hooks</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.PublishHook}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Plugged-in PublishHook implementations. Consulted in order before publishImplementation / withdrawImplementation accepts a request; if any hook returns a Diagnostic of severity ERROR or CANCEL, the action is rejected and the diagnostic is propagated. Non-containment because hook implementations are usually owned by the integrator's adapter component.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Publish Hooks</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistry_PublishHooks()
	 * @model
	 * @generated
	 */
	EList<PublishHook> getPublishHooks();

	/**
	 * Returns the value of the '<em><b>Discovery Hooks</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.DiscoveryHook}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Plugged-in DiscoveryHook implementations. Consulted in order before lookup / subscribe operations and to filter result lists.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Discovery Hooks</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistry_DiscoveryHooks()
	 * @model
	 * @generated
	 */
	EList<DiscoveryHook> getDiscoveryHooks();

	/**
	 * Returns the value of the '<em><b>Distribution Hooks</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.DistributionHook}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Plugged-in DistributionHook implementations. Consulted in order whenever an event crosses the local↔remote boundary (outbound from local, inbound from broker).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Distribution Hooks</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistry_DistributionHooks()
	 * @model
	 * @generated
	 */
	EList<DistributionHook> getDistributionHooks();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Returns one ServiceReference for the given interface, picking by service.ranking (descending) and service.id (ascending) on tie. Null if no match.
	 * @param interfaceName ServiceInterface.name.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	ServiceReference getServiceReference(String interfaceName);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Returns all matching ServiceReferences ordered by ranking. Honors the consumer's flavor capability — references whose ServiceImplementation has no flavor in capability.supportedFlavors are filtered out.
	 * @param interfaceName ServiceInterface.name.
	 * @param filter LDAP filter over service properties. Null = no filter.
	 * @param capability Consumer's capability bag (supported flavors etc.). For purely in-process lookup on a LocalServiceRegistry this can be null (no flavor filtering needed).
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	EList<ServiceReference> getServiceReferences(String interfaceName, String filter, ConsumerCapability capability);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Like getServiceReferences but ignores ServiceReference visibility constraints (returns even references the caller would not normally see, e.g. across PDP boundaries). Use for administrative tooling and audit.
	 * @param interfaceName ServiceInterface.name.
	 * @param filter LDAP filter. Null = no filter.
	 * @param capability Consumer's capability bag. Null permitted in administrative context.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	EList<ServiceReference> getAllServiceReferences(String interfaceName, String filter, ConsumerCapability capability);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Registers a listener. Idempotent: adding the same listener twice is a no-op.
	 * @param listener Listener to add. Non-null.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	void addServiceListener(ServiceListener listener);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Removes a listener. No-op if not registered.
	 * @param listener Listener to remove. Non-null.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	void removeServiceListener(ServiceListener listener);

} // ServiceRegistry
