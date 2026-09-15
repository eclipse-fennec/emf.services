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
 *   <li>{@link org.eclipse.fennec.services.ServiceEvent#getReasonCode <em>Reason Code</em>}</li>
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

	/**
	 * Returns the value of the '<em><b>Reason Code</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Why the transition happened; lets a consumer tell 'gone for good' from 'replaced' from 'parked'. Short stable upper-case token, null for plain REGISTERED / MODIFIED. Broker vocabulary: WITHDRAWN (provider withdrew), REPLACED (same (name, version) republished, or DEPRECATE_AND_DRAIN drain finished), CUTOVER (HARD_CUTOVER grace elapsed), COLDIFIED (idle sweep moved the entry to the cold cache, it is still discoverable and rehydrates on demand), SESSION_EXPIRED, PROVIDER_LOST. Free-form so transports and future policies can add tokens without a model change.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Reason Code</em>' attribute.
	 * @see #setReasonCode(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceEvent_ReasonCode()
	 * @model
	 * @generated
	 */
	String getReasonCode();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceEvent#getReasonCode <em>Reason Code</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Reason Code</em>' attribute.
	 * @see #getReasonCode()
	 * @generated
	 */
	void setReasonCode(String value);

} // ServiceEvent
