/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Remote Service Registry</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * The central Broker. Holds the API catalog (ServiceInterface containments) and the global implementation index (non-containment references back to ServiceImplementations that live under their providers). Provider/consumer wire traffic does NOT flow through here — only directory traffic.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.RemoteServiceRegistry#getEndpoint <em>Endpoint</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RemoteServiceRegistry#getCatalog <em>Catalog</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RemoteServiceRegistry#getImplementations <em>Implementations</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RemoteServiceRegistry#getProviders <em>Providers</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getRemoteServiceRegistry()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='publishedImplsHaveFlavor publishedImplsReferenceCatalog publishedImplsOwnedByListedProvider'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 publishedImplsHaveFlavor='implementations-&gt;forAll(i | i.flavors-&gt;notEmpty())' publishedImplsReferenceCatalog='implementations-&gt;forAll(i | i.serviceInterfaces-&gt;forAll(si | catalog-&gt;includes(si)))' publishedImplsOwnedByListedProvider='implementations-&gt;forAll(i | providers-&gt;exists(p | p.implementations-&gt;includes(i)))'"
 * @generated
 */
@ProviderType
public interface RemoteServiceRegistry extends ServiceRegistry {
	/**
	 * Returns the value of the '<em><b>Endpoint</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Address clients connect to (e.g. cluster VIP). May be null on the server side instance.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Endpoint</em>' attribute.
	 * @see #setEndpoint(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRemoteServiceRegistry_Endpoint()
	 * @model
	 * @generated
	 */
	String getEndpoint();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RemoteServiceRegistry#getEndpoint <em>Endpoint</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Endpoint</em>' attribute.
	 * @see #getEndpoint()
	 * @generated
	 */
	void setEndpoint(String value);

	/**
	 * Returns the value of the '<em><b>Catalog</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceInterface}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The curated API catalog. Mutations go through addCatalogEntry / deprecateCatalogEntry / removeCatalogEntry, all of which flow through the PDP.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Catalog</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getRemoteServiceRegistry_Catalog()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServiceInterface> getCatalog();

	/**
	 * Returns the value of the '<em><b>Implementations</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceImplementation}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Index of currently published implementations. Non-containment: each implementation is owned (containment) by its ServiceProvider.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Implementations</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getRemoteServiceRegistry_Implementations()
	 * @model
	 * @generated
	 */
	EList<ServiceImplementation> getImplementations();

	/**
	 * Returns the value of the '<em><b>Providers</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceProvider}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Providers known to the broker. Non-containment: providers also live under their LocalServiceRegistry.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Providers</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getRemoteServiceRegistry_Providers()
	 * @model
	 * @generated
	 */
	EList<ServiceProvider> getProviders();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Adds an implementation to the global index. Validates that all referenced ServiceInterfaces exist in the catalog. Authorization flows through the PublishHook chain. If any of the referenced ServiceInterfaces has status = DEPRECATED, the call succeeds but the returned Diagnostic carries severity = WARNING with the deprecationReason (so the provider knows it is publishing against a deprecated interface). Returns OK (severity) on plain success.
	 * @param provider The owning provider. Used as PDP subject.
	 * @param implementation The implementation to publish. MUST be contained in 'provider.implementations'.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Symmetric to publishImplementation. The provider parameter is required so that PDP authorization, audit logging, and ownership checks (implementation must belong to provider) can run unambiguously, even though the implementation's container is reachable via eContainer().
	 * @param provider The provider that owns the implementation. Authority and audit subject.
	 * @param implementation The implementation to withdraw. MUST be contained in 'provider.implementations' (OCL invariant).
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Adds a new ServiceInterface to the catalog. Authorization flows through the PDP — typically only the Governance officer is permitted. On success, triggers the Code Publisher to emit JAR / npm / wheel artifacts (REQUIREMENTS FR-CodeDist-Publisher).
	 * @param serviceInterface The interface to add.
	 * @param requestor Identity that requested the change. Used as PDP subject.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Soft-deprecation. Sets status = DEPRECATED on the ServiceInterface; optionally records deprecationReason and replacedBy. Existing ServiceImplementations stay live, lookups continue to resolve, ServiceListeners continue to fire. Subsequent publishImplementation calls against this interface succeed but return a WARNING diagnostic that consumers can inspect. The transition is one-way: once DEPRECATED, the entry cannot return to ACTIVE — a revival is a new ServiceInterface entry.
	 * @param serviceInterface The interface to deprecate.
	 * @param requestor Identity that requested the change.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Strict-Reject removal. Refuses with Diagnostic(severity=ERROR, code=CATALOG_HAS_LIVE_IMPLS) as long as ANY ServiceImplementation in the registry references the interface. The governance officer must first ensure all providers have withdrawn their implementations (typically: deprecate the interface, wait for the migration window, then remove). Returns OK only when the catalog entry is gone.
	 * @param serviceInterface The interface to remove.
	 * @param requestor Identity that requested the change.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor);

} // RemoteServiceRegistry
