/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Publish Hook</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Callback for provider-side intervention. Consulted by the framework before a publishImplementation or withdrawImplementation succeeds. Hook can veto by returning a Diagnostic with severity ERROR or CANCEL.
 * <!-- end-model-doc -->
 *
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getPublishHook()
 * @model interface="true" abstract="true"
 * @generated
 */
@ProviderType
public interface PublishHook extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Called before the broker accepts a new ServiceImplementation for publication. Return OK to proceed. Return ERROR/CANCEL to abort; the broker propagates the returned Diagnostic to the caller of publishImplementation.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic onPublish(ServiceProvider provider, ServiceImplementation implementation);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Called before the broker withdraws an existing ServiceImplementation. Same semantics as onPublish.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Diagnostic onWithdraw(ServiceProvider provider, ServiceImplementation implementation);

} // PublishHook
