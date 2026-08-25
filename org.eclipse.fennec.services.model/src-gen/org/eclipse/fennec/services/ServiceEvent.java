/*
 */
package org.eclipse.fennec.services;

import java.util.Date;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Event</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Notification about a service lifecycle change. Models org.osgi.framework.ServiceEvent. Delivered synchronously to local ServiceListeners; the Remote Registry sync mechanism may deliver an asynchronous echo across the federation (see REQUIREMENTS §8 Q1).
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceEvent#getType <em>Type</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceEvent#getReference <em>Reference</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceEvent#getTimestamp <em>Timestamp</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceEvent()
 * @model
 * @generated
 */
@ProviderType
public interface ServiceEvent extends EObject {
	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ServiceEventType}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Which lifecycle transition this event reports.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see org.eclipse.fennec.services.ServiceEventType
	 * @see #setType(ServiceEventType)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceEvent_Type()
	 * @model required="true"
	 * @generated
	 */
	ServiceEventType getType();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceEvent#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see org.eclipse.fennec.services.ServiceEventType
	 * @see #getType()
	 * @generated
	 */
	void setType(ServiceEventType value);

	/**
	 * Returns the value of the '<em><b>Reference</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The service the event is about.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Reference</em>' reference.
	 * @see #setReference(ServiceReference)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceEvent_Reference()
	 * @model required="true"
	 * @generated
	 */
	ServiceReference getReference();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceEvent#getReference <em>Reference</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Reference</em>' reference.
	 * @see #getReference()
	 * @generated
	 */
	void setReference(ServiceReference value);

	/**
	 * Returns the value of the '<em><b>Timestamp</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * When the event was generated. Optional in OSGi semantics; useful for audit trails and event-stream ordering across the federation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Timestamp</em>' attribute.
	 * @see #setTimestamp(Date)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceEvent_Timestamp()
	 * @model
	 * @generated
	 */
	Date getTimestamp();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceEvent#getTimestamp <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Timestamp</em>' attribute.
	 * @see #getTimestamp()
	 * @generated
	 */
	void setTimestamp(Date value);

} // ServiceEvent
