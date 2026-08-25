/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Discovery Hook</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Callback for consumer-side intervention. Consulted by the framework when a consumer looks up services or subscribes to events. Hook can veto, log, or shape the result set.
 * <!-- end-model-doc -->
 *
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getDiscoveryHook()
 * @model interface="true" abstract="true"
 * @generated
 */
@ProviderType
public interface DiscoveryHook extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Called before the registry runs a lookup. Return OK to proceed; ERROR/CANCEL to deny the lookup entirely (caller receives the diagnostic instead of references).
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic onLookup(String interfaceName, String filter, ConsumerCapability capability);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Called after the registry has assembled a candidate result list but before returning it to the consumer. Returns the (potentially) pruned list. Returning an empty list = no matches visible to this consumer.
	 * <!-- end-model-doc -->
	 * @model referencesMany="true"
	 * @generated
	 */
	EList<ServiceReference> filterReferences(ConsumerCapability capability, EList<ServiceReference> references);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Called before addServiceListener installs a listener. Can deny subscription based on listener.filter, consumer identity, or context.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic onSubscribe(ServiceListener listener);

} // DiscoveryHook
