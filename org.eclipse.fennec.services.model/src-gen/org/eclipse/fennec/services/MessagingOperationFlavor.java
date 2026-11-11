/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Messaging Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * How one operation is reached over a messaging broker. Each value overrides the MessagingFlavor default when set.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.MessagingOperationFlavor#getChannel <em>Channel</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MessagingOperationFlavor#getReplyChannel <em>Reply Channel</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.MessagingOperationFlavor#getQos <em>Qos</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingOperationFlavor()
 * @model
 * @generated
 */
@ProviderType
public interface MessagingOperationFlavor extends ServiceOperationFlavor {
	/**
	 * Returns the value of the '<em><b>Channel</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The full channel of this operation. Null = <MessagingFlavor.channel><separator><operation name>.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Channel</em>' attribute.
	 * @see #setChannel(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingOperationFlavor_Channel()
	 * @model
	 * @generated
	 */
	String getChannel();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MessagingOperationFlavor#getChannel <em>Channel</em>}' attribute.
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
	 * Overrides MessagingFlavor.replyChannel for this operation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Reply Channel</em>' attribute.
	 * @see #setReplyChannel(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingOperationFlavor_ReplyChannel()
	 * @model
	 * @generated
	 */
	String getReplyChannel();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MessagingOperationFlavor#getReplyChannel <em>Reply Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Reply Channel</em>' attribute.
	 * @see #getReplyChannel()
	 * @generated
	 */
	void setReplyChannel(String value);

	/**
	 * Returns the value of the '<em><b>Qos</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.MessagingQos}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Overrides MessagingFlavor.defaultQos for this operation. Unsettable, and that is what makes the override expressible at all (issue #81): without it EMF reports the enum's first literal for an operation that said nothing, and the flavor default could never apply.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Qos</em>' attribute.
	 * @see org.eclipse.fennec.services.MessagingQos
	 * @see #isSetQos()
	 * @see #unsetQos()
	 * @see #setQos(MessagingQos)
	 * @see org.eclipse.fennec.services.ServicesPackage#getMessagingOperationFlavor_Qos()
	 * @model unsettable="true"
	 * @generated
	 */
	MessagingQos getQos();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.MessagingOperationFlavor#getQos <em>Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Qos</em>' attribute.
	 * @see org.eclipse.fennec.services.MessagingQos
	 * @see #isSetQos()
	 * @see #unsetQos()
	 * @see #getQos()
	 * @generated
	 */
	void setQos(MessagingQos value);

	/**
	 * Unsets the value of the '{@link org.eclipse.fennec.services.MessagingOperationFlavor#getQos <em>Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetQos()
	 * @see #getQos()
	 * @see #setQos(MessagingQos)
	 * @generated
	 */
	void unsetQos();

	/**
	 * Returns whether the value of the '{@link org.eclipse.fennec.services.MessagingOperationFlavor#getQos <em>Qos</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Qos</em>' attribute is set.
	 * @see #unsetQos()
	 * @see #getQos()
	 * @see #setQos(MessagingQos)
	 * @generated
	 */
	boolean isSetQos();

} // MessagingOperationFlavor
