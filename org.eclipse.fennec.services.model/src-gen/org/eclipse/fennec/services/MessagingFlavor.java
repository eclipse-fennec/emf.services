/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Messaging Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A transport over a messaging broker, whichever protocol the broker speaks. Over messaging the API is one — publish to a channel, subscribe to a channel, a request and its answer — so one flavor describes it: where the broker is, which channel each operation is reached on, and where answers go. The protocol is the flavor's kind (MQTT, AMQP) and the scheme of its broker URLs; it decides what a channel is called (a topic with '/' levels, a routing key with '.' segments) and nothing about the shape of this class.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.MessagingFlavor#getBrokers <em>Brokers</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MessagingFlavor#getChannel <em>Channel</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MessagingFlavor#getReplyChannel <em>Reply Channel</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MessagingFlavor#getExchange <em>Exchange</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MessagingFlavor#getDefaultQos <em>Default Qos</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingFlavor()
 * @model
 * @generated
 */
@ProviderType
public interface MessagingFlavor extends ServiceFlavor {
	/**
	 * Returns the value of the '<em><b>Brokers</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * One or more broker URLs — tcp://, ssl://, ws://, wss:// for MQTT, amqp:// or amqps:// (the virtual host as the path) for AMQP. Without user or password: the flavor is announced to every consumer. Multiple brokers indicate a high-availability cluster; the client picks one and fails over.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Brokers</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingFlavor_Brokers()
	 * @model required="true"
	 * @generated
	 */
	EList<String> getBrokers();

	/**
	 * Returns the value of the '<em><b>Channel</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Base channel for incoming calls: a topic over MQTT, a routing key over AMQP. An operation is reached on <channel><separator><operation name> — '/' over MQTT, '.' over AMQP — unless its MessagingOperationFlavor.channel says otherwise.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Channel</em>' attribute.
	 * @see #setChannel(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingFlavor_Channel()
	 * @model required="true"
	 * @generated
	 */
	String getChannel();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MessagingFlavor#getChannel <em>Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Channel</em>' attribute.
	 * @see #getChannel()
	 * @generated
	 */
	void setChannel(String value);

	/**
	 * Returns the value of the '<em><b>Reply Channel</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Base channel for answers. Set: a consumer listens on a subtree of its own name below it and names the exact channel in each call, which is what lets a broker's ACL say who may read which answers. Null: the flavor names no place for answers and the consumer's connection provides one — a reply channel only it reads, named in the call.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Reply Channel</em>' attribute.
	 * @see #setReplyChannel(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingFlavor_ReplyChannel()
	 * @model
	 * @generated
	 */
	String getReplyChannel();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MessagingFlavor#getReplyChannel <em>Reply Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Reply Channel</em>' attribute.
	 * @see #getReplyChannel()
	 * @generated
	 */
	void setReplyChannel(String value);

	/**
	 * Returns the value of the '<em><b>Exchange</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The exchange calls are published to, where the protocol has exchanges (AMQP). Null = the topic exchange every AMQP 0-9-1 broker declares, amq.topic. Meaningless over MQTT.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Exchange</em>' attribute.
	 * @see #setExchange(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingFlavor_Exchange()
	 * @model
	 * @generated
	 */
	String getExchange();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MessagingFlavor#getExchange <em>Exchange</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Exchange</em>' attribute.
	 * @see #getExchange()
	 * @generated
	 */
	void setExchange(String value);

	/**
	 * Returns the value of the '<em><b>Default Qos</b></em>' attribute.
	 * The default value is <code>"AT_LEAST_ONCE"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.MessagingQos}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Default QoS for operations. Per-operation override possible via MessagingOperationFlavor.qos.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Default Qos</em>' attribute.
	 * @see org.eclipse.fennec.services.MessagingQos
	 * @see #setDefaultQos(MessagingQos)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingFlavor_DefaultQos()
	 * @model default="AT_LEAST_ONCE" required="true"
	 * @generated
	 */
	MessagingQos getDefaultQos();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MessagingFlavor#getDefaultQos <em>Default Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Default Qos</em>' attribute.
	 * @see org.eclipse.fennec.services.MessagingQos
	 * @see #getDefaultQos()
	 * @generated
	 */
	void setDefaultQos(MessagingQos value);

} // MessagingFlavor
