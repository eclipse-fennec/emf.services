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
 *   <li>{@link org.eclipse.fennec.services.ConsumerSession#getOrigin <em>Origin</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ConsumerSession#getPrincipal <em>Principal</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ConsumerSession#getDomain <em>Domain</em>}</li>
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
	 * The identity the consumer calls itself by, and one name for three things: this session, the consumer segment of its MQTT response topics, and — on an authenticated broker — the subject of its principal. The broker refuses a PUT whose id is not the caller's own (ACQUISITION.md par.7), which closes the session-takeover hole of an unauthenticated id.
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
	 * Returns the value of the '<em><b>Origin</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Where the consumer behind this session came from, as the client origin token 'label/runtimeId' (issue #125). The consumerId says WHO claims to be calling and is chosen by the client; this says which deployment and which run of it the call actually reached the broker from. Session-scoped by construction: the runtime id half is the framework UUID, so it is new after every restart and identifies no person and no durable machine.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Origin</em>' attribute.
	 * @see #setOrigin(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerSession_Origin()
	 * @model
	 * @generated
	 */
	String getOrigin();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ConsumerSession#getOrigin <em>Origin</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Origin</em>' attribute.
	 * @see #getOrigin()
	 * @generated
	 */
	void setOrigin(String value);

	/**
	 * Returns the value of the '<em><b>Principal</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The verified identity behind this session (S2). The session is bound to it: only the same principal renews or deletes it, and what is served to it — lookups, events — is filtered by its domain. Null only on a broker that runs without authentication. TRANSIENT like acquisitions: never on the wire in either direction — a client cannot assert it in a document it sends, and a document returned to a client does not carry it. An auditor sees it, like publishedBy, only through a projection the broker chooses.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Principal</em>' containment reference.
	 * @see #setPrincipal(Principal)
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerSession_Principal()
	 * @model containment="true" transient="true"
	 * @generated
	 */
	Principal getPrincipal();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ConsumerSession#getPrincipal <em>Principal</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Principal</em>' containment reference.
	 * @see #getPrincipal()
	 * @generated
	 */
	void setPrincipal(Principal value);

	/**
	 * Returns the value of the '<em><b>Domain</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The tenant or topology this consumer acts in, taken from its verified credential; see ServiceRegistration.domain. Null = the broker's single default domain. TRANSIENT like acquisitions: never on the wire in either direction — a client cannot assert it in a document it sends, and a document returned to a client does not carry it. An auditor sees it, like publishedBy, only through a projection the broker chooses.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Domain</em>' attribute.
	 * @see #setDomain(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerSession_Domain()
	 * @model transient="true"
	 * @generated
	 */
	String getDomain();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ConsumerSession#getDomain <em>Domain</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Domain</em>' attribute.
	 * @see #getDomain()
	 * @generated
	 */
	void setDomain(String value);

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
	 * The registrations this consumer claims to be using — the OWNING side of the acquisition relation. Points at the stable ServiceRegistration, not at the ServiceReference: references are the wire artefact and their ids regenerate on broker restart. Over-claiming is harmless (delays drain), under-claiming only hurts the consumer itself (loses drain protection). TRANSIENT: the wire form of a session carries sibling ServiceReference id-stubs instead (same convention as publish).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Acquisitions</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerSession_Acquisitions()
	 * @see org.eclipse.fennec.services.ServiceRegistration#getUsingSessions
	 * @model opposite="usingSessions" transient="true"
	 * @generated
	 */
	EList<ServiceRegistration> getAcquisitions();

} // ConsumerSession
