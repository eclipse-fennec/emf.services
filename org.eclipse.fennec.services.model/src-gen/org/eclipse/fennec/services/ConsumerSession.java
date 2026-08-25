/*
 */
package org.eclipse.fennec.services;

import java.util.Date;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Consumer Session</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Consumer-side counterpart of ServiceRegistration (ACQUISITION.md): the session OWNS the acquisition leases, because their lifecycle follows the consumer — one heartbeat renews all of them, one shutdown or crash releases all of them. Maintained by an idempotent full-replace (PUT /consumers/{consumerId}); a lease expires after 2x the renewal interval. Acquisition is a cooperative protocol, not enforcement: invocation runs peer-to-peer past the broker.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ConsumerSession#getConsumerId <em>Consumer Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ConsumerSession#getLastRenewal <em>Last Renewal</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ConsumerSession#getCapabilities <em>Capabilities</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ConsumerSession#getAcquisitions <em>Acquisitions</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerSession()
 * @model
 * @generated
 */
@ProviderType
public interface ConsumerSession extends EObject {
	/**
	 * Returns the value of the '<em><b>Consumer Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Identity of the consumer. Unauthenticated for now (S2) — once broker AuthN exists, the session binds to the authenticated identity and this id is checked against it.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Consumer Id</em>' attribute.
	 * @see #setConsumerId(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerSession_ConsumerId()
	 * @model required="true"
	 * @generated
	 */
	String getConsumerId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ConsumerSession#getConsumerId <em>Consumer Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Consumer Id</em>' attribute.
	 * @see #getConsumerId()
	 * @generated
	 */
	void setConsumerId(String value);

	/**
	 * Returns the value of the '<em><b>Last Renewal</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Broker-side timestamp of the last session PUT. The lease of every acquisition expires after 2x the renewal interval without one.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Last Renewal</em>' attribute.
	 * @see #setLastRenewal(Date)
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerSession_LastRenewal()
	 * @model
	 * @generated
	 */
	Date getLastRenewal();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ConsumerSession#getLastRenewal <em>Last Renewal</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Last Renewal</em>' attribute.
	 * @see #getLastRenewal()
	 * @generated
	 */
	void setLastRenewal(Date value);

	/**
	 * Returns the value of the '<em><b>Capabilities</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * What this consumer can speak (flavors, greediness, ...). Owned by the session so lookups and drain decisions can consult it without a separate transport.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Capabilities</em>' containment reference.
	 * @see #setCapabilities(ConsumerCapability)
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerSession_Capabilities()
	 * @model containment="true"
	 * @generated
	 */
	ConsumerCapability getCapabilities();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ConsumerSession#getCapabilities <em>Capabilities</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Capabilities</em>' containment reference.
	 * @see #getCapabilities()
	 * @generated
	 */
	void setCapabilities(ConsumerCapability value);

	/**
	 * Returns the value of the '<em><b>Acquisitions</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceRegistration}.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.services.ServiceRegistration#getUsingSessions <em>Using Sessions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The registrations this consumer claims to be using — the OWNING side of the acquisition relation. Points at the stable ServiceRegistration, not at the ServiceReference: references are the wire artefact and their ids regenerate on broker restart. Over-claiming is harmless (delays drain), under-claiming only hurts the consumer itself (loses drain protection).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Acquisitions</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerSession_Acquisitions()
	 * @see org.eclipse.fennec.services.ServiceRegistration#getUsingSessions
	 * @model opposite="usingSessions"
	 * @generated
	 */
	EList<ServiceRegistration> getAcquisitions();

} // ConsumerSession
