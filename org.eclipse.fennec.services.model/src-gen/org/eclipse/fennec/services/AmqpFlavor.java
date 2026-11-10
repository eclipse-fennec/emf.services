/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Amqp Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * AMQP 0-9-1 transport. A call is published to a topic exchange under a routing key; the answer goes to the reply address the call names in the protocol's own reply-to property and is matched by its correlation id. Unlike MQTT there is no response address in the flavor: the consumer declares an exclusive queue of its own, so who may read an answer is enforced by the broker rather than by a naming convention.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.AmqpFlavor#getBrokers <em>Brokers</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.AmqpFlavor#getExchange <em>Exchange</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.AmqpFlavor#getRequestRoutingKey <em>Request Routing Key</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getAmqpFlavor()
 * @model
 * @generated
 */
@ProviderType
public interface AmqpFlavor extends ServiceFlavor {
	/**
	 * Returns the value of the '<em><b>Brokers</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * One or more broker URLs (amqp://… or amqps://…), including the virtual host. Multiple brokers indicate a high-availability cluster; the client picks one and fails over.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Brokers</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getAmqpFlavor_Brokers()
	 * @model required="true"
	 * @generated
	 */
	EList<String> getBrokers();

	/**
	 * Returns the value of the '<em><b>Exchange</b></em>' attribute.
	 * The default value is <code>"amq.topic"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The topic exchange calls are published to. The default is the topic exchange every AMQP 0-9-1 broker declares.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Exchange</em>' attribute.
	 * @see #setExchange(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getAmqpFlavor_Exchange()
	 * @model default="amq.topic" required="true"
	 * @generated
	 */
	String getExchange();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.AmqpFlavor#getExchange <em>Exchange</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Exchange</em>' attribute.
	 * @see #getExchange()
	 * @generated
	 */
	void setExchange(String value);

	/**
	 * Returns the value of the '<em><b>Request Routing Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Base routing key for incoming calls, segments separated by '.'. An operation is reached under <requestRoutingKey>.<operation name> unless its AmqpOperationFlavor.routingKey says otherwise.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Request Routing Key</em>' attribute.
	 * @see #setRequestRoutingKey(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getAmqpFlavor_RequestRoutingKey()
	 * @model required="true"
	 * @generated
	 */
	String getRequestRoutingKey();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.AmqpFlavor#getRequestRoutingKey <em>Request Routing Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Request Routing Key</em>' attribute.
	 * @see #getRequestRoutingKey()
	 * @generated
	 */
	void setRequestRoutingKey(String value);

} // AmqpFlavor
