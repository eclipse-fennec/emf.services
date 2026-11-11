/*
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.fennec.services.MessagingOperationFlavor;
import org.eclipse.fennec.services.MessagingQos;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Messaging Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.MessagingOperationFlavorImpl#getChannel <em>Channel</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MessagingOperationFlavorImpl#getReplyChannel <em>Reply Channel</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MessagingOperationFlavorImpl#getQos <em>Qos</em>}</li>
 * </ul>
 *
 * @generated
 */
public class MessagingOperationFlavorImpl extends ServiceOperationFlavorImpl implements MessagingOperationFlavor {
	/**
	 * The default value of the '{@link #getChannel() <em>Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChannel()
	 * @generated
	 * @ordered
	 */
	protected static final String CHANNEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getChannel() <em>Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChannel()
	 * @generated
	 * @ordered
	 */
	protected String channel = CHANNEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getReplyChannel() <em>Reply Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReplyChannel()
	 * @generated
	 * @ordered
	 */
	protected static final String REPLY_CHANNEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getReplyChannel() <em>Reply Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReplyChannel()
	 * @generated
	 * @ordered
	 */
	protected String replyChannel = REPLY_CHANNEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getQos() <em>Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getQos()
	 * @generated
	 * @ordered
	 */
	protected static final MessagingQos QOS_EDEFAULT = MessagingQos.AT_MOST_ONCE;

	/**
	 * The cached value of the '{@link #getQos() <em>Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getQos()
	 * @generated
	 * @ordered
	 */
	protected MessagingQos qos = QOS_EDEFAULT;

	/**
	 * This is true if the Qos attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean qosESet;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MessagingOperationFlavorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.MESSAGING_OPERATION_FLAVOR;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getChannel() {
		return channel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setChannel(String newChannel) {
		String oldChannel = channel;
		channel = newChannel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MESSAGING_OPERATION_FLAVOR__CHANNEL, oldChannel, channel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getReplyChannel() {
		return replyChannel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setReplyChannel(String newReplyChannel) {
		String oldReplyChannel = replyChannel;
		replyChannel = newReplyChannel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MESSAGING_OPERATION_FLAVOR__REPLY_CHANNEL, oldReplyChannel, replyChannel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public MessagingQos getQos() {
		return qos;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setQos(MessagingQos newQos) {
		MessagingQos oldQos = qos;
		qos = newQos == null ? QOS_EDEFAULT : newQos;
		boolean oldQosESet = qosESet;
		qosESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MESSAGING_OPERATION_FLAVOR__QOS, oldQos, qos, !oldQosESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void unsetQos() {
		MessagingQos oldQos = qos;
		boolean oldQosESet = qosESet;
		qos = QOS_EDEFAULT;
		qosESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, ServicesPackage.MESSAGING_OPERATION_FLAVOR__QOS, oldQos, QOS_EDEFAULT, oldQosESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isSetQos() {
		return qosESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__CHANNEL:
				return getChannel();
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__REPLY_CHANNEL:
				return getReplyChannel();
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__QOS:
				return getQos();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__CHANNEL:
				setChannel((String)newValue);
				return;
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__REPLY_CHANNEL:
				setReplyChannel((String)newValue);
				return;
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__QOS:
				setQos((MessagingQos)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__CHANNEL:
				setChannel(CHANNEL_EDEFAULT);
				return;
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__REPLY_CHANNEL:
				setReplyChannel(REPLY_CHANNEL_EDEFAULT);
				return;
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__QOS:
				unsetQos();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__CHANNEL:
				return CHANNEL_EDEFAULT == null ? channel != null : !CHANNEL_EDEFAULT.equals(channel);
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__REPLY_CHANNEL:
				return REPLY_CHANNEL_EDEFAULT == null ? replyChannel != null : !REPLY_CHANNEL_EDEFAULT.equals(replyChannel);
			case ServicesPackage.MESSAGING_OPERATION_FLAVOR__QOS:
				return isSetQos();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (channel: ");
		result.append(channel);
		result.append(", replyChannel: ");
		result.append(replyChannel);
		result.append(", qos: ");
		if (qosESet) result.append(qos); else result.append("<unset>");
		result.append(')');
		return result.toString();
	}

} //MessagingOperationFlavorImpl
