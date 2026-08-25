/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Listener</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Interface a consumer implements to receive ServiceEvents. Equivalent to org.osgi.framework.ServiceListener.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceListener#getFilter <em>Filter</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceListener()
 * @model interface="true" abstract="true"
 * @generated
 */
@ProviderType
public interface ServiceListener extends EObject {
	/**
	 * Returns the value of the '<em><b>Filter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Optional LDAP filter limiting which services trigger serviceChanged. Null = receive events for all services.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Filter</em>' attribute.
	 * @see #setFilter(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceListener_Filter()
	 * @model
	 * @generated
	 */
	String getFilter();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceListener#getFilter <em>Filter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Filter</em>' attribute.
	 * @see #getFilter()
	 * @generated
	 */
	void setFilter(String value);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Called by the registry for each ServiceEvent that matches this listener's filter. The framework guarantees that this is called synchronously relative to the operation that produced the event (so consumers see REGISTERED before the registering operation returns).
	 * @param event The event being delivered. Non-null.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	void serviceChanged(ServiceEvent event);

} // ServiceListener
