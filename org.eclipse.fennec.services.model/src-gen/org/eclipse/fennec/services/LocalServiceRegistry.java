/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Local Service Registry</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * In-process registry. Owns local registrations and listeners, runs the synchronous lifecycle/event semantics, and (when 'remote' is set) propagates registrations to the Remote Registry asynchronously.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.LocalServiceRegistry#getReferences <em>References</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LocalServiceRegistry#getRegistrations <em>Registrations</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LocalServiceRegistry#getSessions <em>Sessions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LocalServiceRegistry#getConfigurations <em>Configurations</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LocalServiceRegistry#getProviders <em>Providers</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LocalServiceRegistry#getListeners <em>Listeners</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LocalServiceRegistry#getRemote <em>Remote</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LocalServiceRegistry#getConnectionState <em>Connection State</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getLocalServiceRegistry()
 * @model
 * @generated
 */
@ProviderType
public interface LocalServiceRegistry extends ServiceRegistry {
	/**
	 * Returns the value of the '<em><b>References</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceReference}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * All locally registered service references.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>References</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getLocalServiceRegistry_References()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServiceReference> getReferences();

	/**
	 * Returns the value of the '<em><b>Registrations</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceRegistration}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Provider-side handles paired with each reference via eOpposite.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Registrations</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getLocalServiceRegistry_Registrations()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServiceRegistration> getRegistrations();

	/**
	 * Returns the value of the '<em><b>Sessions</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ConsumerSession}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Consumer sessions holding acquisition leases (ACQUISITION.md par.3/par.4). Runtime state by design: sessions are deliberately NOT persisted to the broker snapshot — after a broker restart consumers rebuild them via their regular session PUTs (FR-Sync-Reconnect philosophy).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Sessions</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getLocalServiceRegistry_Sessions()
	 * @model containment="true"
	 * @generated
	 */
	EList<ConsumerSession> getSessions();

	/**
	 * Returns the value of the '<em><b>Configurations</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ComponentConfiguration}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Active component configurations (instances of declared components).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Configurations</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getLocalServiceRegistry_Configurations()
	 * @model containment="true"
	 * @generated
	 */
	EList<ComponentConfiguration> getConfigurations();

	/**
	 * Returns the value of the '<em><b>Providers</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceProvider}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Providers known to this local registry.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Providers</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getLocalServiceRegistry_Providers()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServiceProvider> getProviders();

	/**
	 * Returns the value of the '<em><b>Listeners</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceListener}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Registered listeners (non-containment because listeners are typically owned by the consumer that wrote them).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Listeners</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getLocalServiceRegistry_Listeners()
	 * @model
	 * @generated
	 */
	EList<ServiceListener> getListeners();

	/**
	 * Returns the value of the '<em><b>Remote</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The Remote Registry this local registry is connected to, if any. Null = standalone (no federation, no catalog lookup, no remote publication).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Remote</em>' reference.
	 * @see #setRemote(RemoteServiceRegistry)
	 * @see org.eclipse.fennec.services.ServicesPackage#getLocalServiceRegistry_Remote()
	 * @model
	 * @generated
	 */
	RemoteServiceRegistry getRemote();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.LocalServiceRegistry#getRemote <em>Remote</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Remote</em>' reference.
	 * @see #getRemote()
	 * @generated
	 */
	void setRemote(RemoteServiceRegistry value);

	/**
	 * Returns the value of the '<em><b>Connection State</b></em>' attribute.
	 * The default value is <code>"OFFLINE"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ConnectionState}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Health of the link to the Remote Registry. Consumers can read this to know whether lookup results are live (CONNECTED) or possibly stale (DEGRADED / OFFLINE). While DEGRADED or OFFLINE, reads are served from the last known snapshot+event-replay and writes (publishImplementation, withdrawImplementation, catalog mutations) are rejected with a Diagnostic. On reconnect, the registry pulls a fresh snapshot and synthesises ServiceEvents for the diff so local listeners see a consistent transition.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Connection State</em>' attribute.
	 * @see org.eclipse.fennec.services.ConnectionState
	 * @see #setConnectionState(ConnectionState)
	 * @see org.eclipse.fennec.services.ServicesPackage#getLocalServiceRegistry_ConnectionState()
	 * @model default="OFFLINE" required="true"
	 * @generated
	 */
	ConnectionState getConnectionState();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.LocalServiceRegistry#getConnectionState <em>Connection State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Connection State</em>' attribute.
	 * @see org.eclipse.fennec.services.ConnectionState
	 * @see #getConnectionState()
	 * @generated
	 */
	void setConnectionState(ConnectionState value);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Provider-facing registration entry point. Step order (REQUIREMENTS FR-Lifecycle-Register): (1) validate against catalog if remote is non-null; (2) allocate service.id and create ServiceReference; (3) add to local references/registrations synchronously; (4) deliver REGISTERED to matching local listeners synchronously, before returning; (5) hand off to async worker that publishes to remote (with retry).
	 * @param provider The provider that owns the registration. Used for PDP subject and as the back-link from ServiceReference.provider.
	 * @param implementation The implementation being registered. Its serviceInterfaces and flavors are advertised in the resulting ServiceReference.
	 * @param props Additional properties at registration time, merged on top of implementation.properties.
	 * <!-- end-model-doc -->
	 * @model propsMany="true"
	 * @generated
	 */
	ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation, EList<Property> props);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Delivers an event to local listeners that match its filter. Synchronous; returns when all listeners have been notified.
	 * @param event The event to deliver.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	void fireServiceEvent(ServiceEvent event);

} // LocalServiceRegistry
