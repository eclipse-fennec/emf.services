/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

import org.eclipse.fennec.services.MessagingFlavor;
import org.eclipse.fennec.services.MessagingQos;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Messaging Flavor</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.MessagingFlavorImpl#getBrokers <em>Brokers</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MessagingFlavorImpl#getChannel <em>Channel</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MessagingFlavorImpl#getReplyChannel <em>Reply Channel</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MessagingFlavorImpl#getExchange <em>Exchange</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.MessagingFlavorImpl#getDefaultQos <em>Default Qos</em>}</li>
 * </ul>
 *
 * @generated
 */
public class MessagingFlavorImpl extends ServiceFlavorImpl implements MessagingFlavor {
	/**
	 * The cached value of the '{@link #getBrokers() <em>Brokers</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBrokers()
	 * @generated
	 * @ordered
	 */
	protected EList<String> brokers;

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
	 * The default value of the '{@link #getExchange() <em>Exchange</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExchange()
	 * @generated
	 * @ordered
	 */
	protected static final String EXCHANGE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getExchange() <em>Exchange</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExchange()
	 * @generated
	 * @ordered
	 */
	protected String exchange = EXCHANGE_EDEFAULT;

	/**
	 * The default value of the '{@link #getDefaultQos() <em>Default Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDefaultQos()
	 * @generated
	 * @ordered
	 */
	protected static final MessagingQos DEFAULT_QOS_EDEFAULT = MessagingQos.AT_LEAST_ONCE;

	/**
	 * The cached value of the '{@link #getDefaultQos() <em>Default Qos</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDefaultQos()
	 * @generated
	 * @ordered
	 */
	protected MessagingQos defaultQos = DEFAULT_QOS_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MessagingFlavorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.MESSAGING_FLAVOR;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getBrokers() {
		if (brokers == null) {
			brokers = new EDataTypeUniqueEList<String>(String.class, this, ServicesPackage.MESSAGING_FLAVOR__BROKERS);
		}
		return brokers;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MESSAGING_FLAVOR__CHANNEL, oldChannel, channel));
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MESSAGING_FLAVOR__REPLY_CHANNEL, oldReplyChannel, replyChannel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getExchange() {
		return exchange;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setExchange(String newExchange) {
		String oldExchange = exchange;
		exchange = newExchange;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MESSAGING_FLAVOR__EXCHANGE, oldExchange, exchange));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public MessagingQos getDefaultQos() {
		return defaultQos;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDefaultQos(MessagingQos newDefaultQos) {
		MessagingQos oldDefaultQos = defaultQos;
		defaultQos = newDefaultQos == null ? DEFAULT_QOS_EDEFAULT : newDefaultQos;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.MESSAGING_FLAVOR__DEFAULT_QOS, oldDefaultQos, defaultQos));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.MESSAGING_FLAVOR__BROKERS:
				return getBrokers();
			case ServicesPackage.MESSAGING_FLAVOR__CHANNEL:
				return getChannel();
			case ServicesPackage.MESSAGING_FLAVOR__REPLY_CHANNEL:
				return getReplyChannel();
			case ServicesPackage.MESSAGING_FLAVOR__EXCHANGE:
				return getExchange();
			case ServicesPackage.MESSAGING_FLAVOR__DEFAULT_QOS:
				return getDefaultQos();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case ServicesPackage.MESSAGING_FLAVOR__BROKERS:
				getBrokers().clear();
				getBrokers().addAll((Collection<? extends String>)newValue);
				return;
			case ServicesPackage.MESSAGING_FLAVOR__CHANNEL:
				setChannel((String)newValue);
				return;
			case ServicesPackage.MESSAGING_FLAVOR__REPLY_CHANNEL:
				setReplyChannel((String)newValue);
				return;
			case ServicesPackage.MESSAGING_FLAVOR__EXCHANGE:
				setExchange((String)newValue);
				return;
			case ServicesPackage.MESSAGING_FLAVOR__DEFAULT_QOS:
				setDefaultQos((MessagingQos)newValue);
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
			case ServicesPackage.MESSAGING_FLAVOR__BROKERS:
				getBrokers().clear();
				return;
			case ServicesPackage.MESSAGING_FLAVOR__CHANNEL:
				setChannel(CHANNEL_EDEFAULT);
				return;
			case ServicesPackage.MESSAGING_FLAVOR__REPLY_CHANNEL:
				setReplyChannel(REPLY_CHANNEL_EDEFAULT);
				return;
			case ServicesPackage.MESSAGING_FLAVOR__EXCHANGE:
				setExchange(EXCHANGE_EDEFAULT);
				return;
			case ServicesPackage.MESSAGING_FLAVOR__DEFAULT_QOS:
				setDefaultQos(DEFAULT_QOS_EDEFAULT);
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
			case ServicesPackage.MESSAGING_FLAVOR__BROKERS:
				return brokers != null && !brokers.isEmpty();
			case ServicesPackage.MESSAGING_FLAVOR__CHANNEL:
				return CHANNEL_EDEFAULT == null ? channel != null : !CHANNEL_EDEFAULT.equals(channel);
			case ServicesPackage.MESSAGING_FLAVOR__REPLY_CHANNEL:
				return REPLY_CHANNEL_EDEFAULT == null ? replyChannel != null : !REPLY_CHANNEL_EDEFAULT.equals(replyChannel);
			case ServicesPackage.MESSAGING_FLAVOR__EXCHANGE:
				return EXCHANGE_EDEFAULT == null ? exchange != null : !EXCHANGE_EDEFAULT.equals(exchange);
			case ServicesPackage.MESSAGING_FLAVOR__DEFAULT_QOS:
				return defaultQos != DEFAULT_QOS_EDEFAULT;
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
		result.append(" (brokers: ");
		result.append(brokers);
		result.append(", channel: ");
		result.append(channel);
		result.append(", replyChannel: ");
		result.append(replyChannel);
		result.append(", exchange: ");
		result.append(exchange);
		result.append(", defaultQos: ");
		result.append(defaultQos);
		result.append(')');
		return result.toString();
	}

} //MessagingFlavorImpl
