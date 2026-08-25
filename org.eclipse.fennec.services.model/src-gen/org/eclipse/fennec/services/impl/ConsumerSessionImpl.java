/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;
import java.util.Date;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectWithInverseResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Consumer Session</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ConsumerSessionImpl#getConsumerId <em>Consumer Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ConsumerSessionImpl#getLastRenewal <em>Last Renewal</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ConsumerSessionImpl#getCapabilities <em>Capabilities</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ConsumerSessionImpl#getAcquisitions <em>Acquisitions</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ConsumerSessionImpl extends MinimalEObjectImpl.Container implements ConsumerSession {
	/**
	 * The default value of the '{@link #getConsumerId() <em>Consumer Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConsumerId()
	 * @generated
	 * @ordered
	 */
	protected static final String CONSUMER_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getConsumerId() <em>Consumer Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConsumerId()
	 * @generated
	 * @ordered
	 */
	protected String consumerId = CONSUMER_ID_EDEFAULT;

	/**
	 * The default value of the '{@link #getLastRenewal() <em>Last Renewal</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLastRenewal()
	 * @generated
	 * @ordered
	 */
	protected static final Date LAST_RENEWAL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getLastRenewal() <em>Last Renewal</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLastRenewal()
	 * @generated
	 * @ordered
	 */
	protected Date lastRenewal = LAST_RENEWAL_EDEFAULT;

	/**
	 * The cached value of the '{@link #getCapabilities() <em>Capabilities</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCapabilities()
	 * @generated
	 * @ordered
	 */
	protected ConsumerCapability capabilities;

	/**
	 * The cached value of the '{@link #getAcquisitions() <em>Acquisitions</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAcquisitions()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceRegistration> acquisitions;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ConsumerSessionImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.CONSUMER_SESSION;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getConsumerId() {
		return consumerId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setConsumerId(String newConsumerId) {
		String oldConsumerId = consumerId;
		consumerId = newConsumerId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.CONSUMER_SESSION__CONSUMER_ID, oldConsumerId, consumerId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Date getLastRenewal() {
		return lastRenewal;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLastRenewal(Date newLastRenewal) {
		Date oldLastRenewal = lastRenewal;
		lastRenewal = newLastRenewal;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.CONSUMER_SESSION__LAST_RENEWAL, oldLastRenewal, lastRenewal));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ConsumerCapability getCapabilities() {
		return capabilities;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetCapabilities(ConsumerCapability newCapabilities, NotificationChain msgs) {
		ConsumerCapability oldCapabilities = capabilities;
		capabilities = newCapabilities;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ServicesPackage.CONSUMER_SESSION__CAPABILITIES, oldCapabilities, newCapabilities);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCapabilities(ConsumerCapability newCapabilities) {
		if (newCapabilities != capabilities) {
			NotificationChain msgs = null;
			if (capabilities != null)
				msgs = ((InternalEObject)capabilities).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ServicesPackage.CONSUMER_SESSION__CAPABILITIES, null, msgs);
			if (newCapabilities != null)
				msgs = ((InternalEObject)newCapabilities).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ServicesPackage.CONSUMER_SESSION__CAPABILITIES, null, msgs);
			msgs = basicSetCapabilities(newCapabilities, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.CONSUMER_SESSION__CAPABILITIES, newCapabilities, newCapabilities));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceRegistration> getAcquisitions() {
		if (acquisitions == null) {
			acquisitions = new EObjectWithInverseResolvingEList.ManyInverse<ServiceRegistration>(ServiceRegistration.class, this, ServicesPackage.CONSUMER_SESSION__ACQUISITIONS, ServicesPackage.SERVICE_REGISTRATION__USING_SESSIONS);
		}
		return acquisitions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.CONSUMER_SESSION__ACQUISITIONS:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getAcquisitions()).basicAdd(otherEnd, msgs);
		}
		return super.eInverseAdd(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.CONSUMER_SESSION__CAPABILITIES:
				return basicSetCapabilities(null, msgs);
			case ServicesPackage.CONSUMER_SESSION__ACQUISITIONS:
				return ((InternalEList<?>)getAcquisitions()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.CONSUMER_SESSION__CONSUMER_ID:
				return getConsumerId();
			case ServicesPackage.CONSUMER_SESSION__LAST_RENEWAL:
				return getLastRenewal();
			case ServicesPackage.CONSUMER_SESSION__CAPABILITIES:
				return getCapabilities();
			case ServicesPackage.CONSUMER_SESSION__ACQUISITIONS:
				return getAcquisitions();
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
			case ServicesPackage.CONSUMER_SESSION__CONSUMER_ID:
				setConsumerId((String)newValue);
				return;
			case ServicesPackage.CONSUMER_SESSION__LAST_RENEWAL:
				setLastRenewal((Date)newValue);
				return;
			case ServicesPackage.CONSUMER_SESSION__CAPABILITIES:
				setCapabilities((ConsumerCapability)newValue);
				return;
			case ServicesPackage.CONSUMER_SESSION__ACQUISITIONS:
				getAcquisitions().clear();
				getAcquisitions().addAll((Collection<? extends ServiceRegistration>)newValue);
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
			case ServicesPackage.CONSUMER_SESSION__CONSUMER_ID:
				setConsumerId(CONSUMER_ID_EDEFAULT);
				return;
			case ServicesPackage.CONSUMER_SESSION__LAST_RENEWAL:
				setLastRenewal(LAST_RENEWAL_EDEFAULT);
				return;
			case ServicesPackage.CONSUMER_SESSION__CAPABILITIES:
				setCapabilities((ConsumerCapability)null);
				return;
			case ServicesPackage.CONSUMER_SESSION__ACQUISITIONS:
				getAcquisitions().clear();
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
			case ServicesPackage.CONSUMER_SESSION__CONSUMER_ID:
				return CONSUMER_ID_EDEFAULT == null ? consumerId != null : !CONSUMER_ID_EDEFAULT.equals(consumerId);
			case ServicesPackage.CONSUMER_SESSION__LAST_RENEWAL:
				return LAST_RENEWAL_EDEFAULT == null ? lastRenewal != null : !LAST_RENEWAL_EDEFAULT.equals(lastRenewal);
			case ServicesPackage.CONSUMER_SESSION__CAPABILITIES:
				return capabilities != null;
			case ServicesPackage.CONSUMER_SESSION__ACQUISITIONS:
				return acquisitions != null && !acquisitions.isEmpty();
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
		result.append(" (consumerId: ");
		result.append(consumerId);
		result.append(", lastRenewal: ");
		result.append(lastRenewal);
		result.append(')');
		return result.toString();
	}

} //ConsumerSessionImpl
