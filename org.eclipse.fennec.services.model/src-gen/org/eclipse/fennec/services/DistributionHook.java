/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Distribution Hook</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Callback for traffic between LocalServiceRegistry and RemoteServiceRegistry. Lets an integrator intervene at the federation boundary — e.g. mask sensitive properties before they cross into the broker, or drop inbound events that target unauthorized tenants.
 * <!-- end-model-doc -->
 *
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getDistributionHook()
 * @model interface="true" abstract="true"
 * @generated
 */
@ProviderType
public interface DistributionHook extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Called before the local registry forwards an event (REGISTERED / MODIFIED / UNREGISTERING) to the broker. Return OK to proceed; ERROR/CANCEL to suppress the outbound event.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic onOutbound(ServiceEvent event);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Called when the local registry receives an event from the broker (over the SSE stream or initial snapshot). Return OK to deliver to local listeners; ERROR/CANCEL to drop the event silently for this local.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic onInbound(ServiceEvent event);

} // DistributionHook
