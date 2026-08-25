/*
 */
package org.eclipse.fennec.services.impl;

import java.lang.reflect.InvocationTargetException;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectWithInverseResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Service Registration</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistrationImpl#getReference <em>Reference</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistrationImpl#isUnregistered <em>Unregistered</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistrationImpl#getProvider <em>Provider</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistrationImpl#getImplementation <em>Implementation</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistrationImpl#getUsingSessions <em>Using Sessions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistrationImpl#getConsumerCount <em>Consumer Count</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ServiceRegistrationImpl extends MinimalEObjectImpl.Container implements ServiceRegistration {
	/**
	 * The cached value of the '{@link #getReference() <em>Reference</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReference()
	 * @generated
	 * @ordered
	 */
	protected ServiceReference reference;

	/**
	 * The default value of the '{@link #isUnregistered() <em>Unregistered</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isUnregistered()
	 * @generated
	 * @ordered
	 */
	protected static final boolean UNREGISTERED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isUnregistered() <em>Unregistered</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isUnregistered()
	 * @generated
	 * @ordered
	 */
	protected boolean unregistered = UNREGISTERED_EDEFAULT;

	/**
	 * The cached value of the '{@link #getProvider() <em>Provider</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProvider()
	 * @generated
	 * @ordered
	 */
	protected ServiceProvider provider;

	/**
	 * The cached value of the '{@link #getImplementation() <em>Implementation</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getImplementation()
	 * @generated
	 * @ordered
	 */
	protected ServiceImplementation implementation;

	/**
	 * The cached value of the '{@link #getUsingSessions() <em>Using Sessions</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUsingSessions()
	 * @generated
	 * @ordered
	 */
	protected EList<ConsumerSession> usingSessions;

	/**
	 * The cached setting delegate for the '{@link #getConsumerCount() <em>Consumer Count</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConsumerCount()
	 * @generated
	 * @ordered
	 */
	protected EStructuralFeature.Internal.SettingDelegate CONSUMER_COUNT__ESETTING_DELEGATE = ((EStructuralFeature.Internal)ServicesPackage.Literals.SERVICE_REGISTRATION__CONSUMER_COUNT).getSettingDelegate();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServiceRegistrationImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.SERVICE_REGISTRATION;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceReference getReference() {
		if (reference != null && reference.eIsProxy()) {
			InternalEObject oldReference = (InternalEObject)reference;
			reference = (ServiceReference)eResolveProxy(oldReference);
			if (reference != oldReference) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.SERVICE_REGISTRATION__REFERENCE, oldReference, reference));
			}
		}
		return reference;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServiceReference basicGetReference() {
		return reference;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetReference(ServiceReference newReference, NotificationChain msgs) {
		ServiceReference oldReference = reference;
		reference = newReference;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_REGISTRATION__REFERENCE, oldReference, newReference);
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
	public void setReference(ServiceReference newReference) {
		if (newReference != reference) {
			NotificationChain msgs = null;
			if (reference != null)
				msgs = ((InternalEObject)reference).eInverseRemove(this, ServicesPackage.SERVICE_REFERENCE__REGISTRATION, ServiceReference.class, msgs);
			if (newReference != null)
				msgs = ((InternalEObject)newReference).eInverseAdd(this, ServicesPackage.SERVICE_REFERENCE__REGISTRATION, ServiceReference.class, msgs);
			msgs = basicSetReference(newReference, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_REGISTRATION__REFERENCE, newReference, newReference));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isUnregistered() {
		return unregistered;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setUnregistered(boolean newUnregistered) {
		boolean oldUnregistered = unregistered;
		unregistered = newUnregistered;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_REGISTRATION__UNREGISTERED, oldUnregistered, unregistered));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceProvider getProvider() {
		if (provider != null && provider.eIsProxy()) {
			InternalEObject oldProvider = (InternalEObject)provider;
			provider = (ServiceProvider)eResolveProxy(oldProvider);
			if (provider != oldProvider) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.SERVICE_REGISTRATION__PROVIDER, oldProvider, provider));
			}
		}
		return provider;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServiceProvider basicGetProvider() {
		return provider;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setProvider(ServiceProvider newProvider) {
		ServiceProvider oldProvider = provider;
		provider = newProvider;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_REGISTRATION__PROVIDER, oldProvider, provider));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceImplementation getImplementation() {
		if (implementation != null && implementation.eIsProxy()) {
			InternalEObject oldImplementation = (InternalEObject)implementation;
			implementation = (ServiceImplementation)eResolveProxy(oldImplementation);
			if (implementation != oldImplementation) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.SERVICE_REGISTRATION__IMPLEMENTATION, oldImplementation, implementation));
			}
		}
		return implementation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServiceImplementation basicGetImplementation() {
		return implementation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setImplementation(ServiceImplementation newImplementation) {
		ServiceImplementation oldImplementation = implementation;
		implementation = newImplementation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_REGISTRATION__IMPLEMENTATION, oldImplementation, implementation));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ConsumerSession> getUsingSessions() {
		if (usingSessions == null) {
			usingSessions = new EObjectWithInverseResolvingEList.ManyInverse<ConsumerSession>(ConsumerSession.class, this, ServicesPackage.SERVICE_REGISTRATION__USING_SESSIONS, ServicesPackage.CONSUMER_SESSION__ACQUISITIONS);
		}
		return usingSessions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getConsumerCount() {
		return (Integer)CONSUMER_COUNT__ESETTING_DELEGATE.dynamicGet(this, null, 0, true, false);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void unregister() {
		// TODO: implement this method
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setProperties(EList<Property> props) {
		// TODO: implement this method
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
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
			case ServicesPackage.SERVICE_REGISTRATION__REFERENCE:
				if (reference != null)
					msgs = ((InternalEObject)reference).eInverseRemove(this, ServicesPackage.SERVICE_REFERENCE__REGISTRATION, ServiceReference.class, msgs);
				return basicSetReference((ServiceReference)otherEnd, msgs);
			case ServicesPackage.SERVICE_REGISTRATION__USING_SESSIONS:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getUsingSessions()).basicAdd(otherEnd, msgs);
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
			case ServicesPackage.SERVICE_REGISTRATION__REFERENCE:
				return basicSetReference(null, msgs);
			case ServicesPackage.SERVICE_REGISTRATION__USING_SESSIONS:
				return ((InternalEList<?>)getUsingSessions()).basicRemove(otherEnd, msgs);
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
			case ServicesPackage.SERVICE_REGISTRATION__REFERENCE:
				if (resolve) return getReference();
				return basicGetReference();
			case ServicesPackage.SERVICE_REGISTRATION__UNREGISTERED:
				return isUnregistered();
			case ServicesPackage.SERVICE_REGISTRATION__PROVIDER:
				if (resolve) return getProvider();
				return basicGetProvider();
			case ServicesPackage.SERVICE_REGISTRATION__IMPLEMENTATION:
				if (resolve) return getImplementation();
				return basicGetImplementation();
			case ServicesPackage.SERVICE_REGISTRATION__USING_SESSIONS:
				return getUsingSessions();
			case ServicesPackage.SERVICE_REGISTRATION__CONSUMER_COUNT:
				return getConsumerCount();
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
			case ServicesPackage.SERVICE_REGISTRATION__REFERENCE:
				setReference((ServiceReference)newValue);
				return;
			case ServicesPackage.SERVICE_REGISTRATION__UNREGISTERED:
				setUnregistered((Boolean)newValue);
				return;
			case ServicesPackage.SERVICE_REGISTRATION__PROVIDER:
				setProvider((ServiceProvider)newValue);
				return;
			case ServicesPackage.SERVICE_REGISTRATION__IMPLEMENTATION:
				setImplementation((ServiceImplementation)newValue);
				return;
			case ServicesPackage.SERVICE_REGISTRATION__USING_SESSIONS:
				getUsingSessions().clear();
				getUsingSessions().addAll((Collection<? extends ConsumerSession>)newValue);
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
			case ServicesPackage.SERVICE_REGISTRATION__REFERENCE:
				setReference((ServiceReference)null);
				return;
			case ServicesPackage.SERVICE_REGISTRATION__UNREGISTERED:
				setUnregistered(UNREGISTERED_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_REGISTRATION__PROVIDER:
				setProvider((ServiceProvider)null);
				return;
			case ServicesPackage.SERVICE_REGISTRATION__IMPLEMENTATION:
				setImplementation((ServiceImplementation)null);
				return;
			case ServicesPackage.SERVICE_REGISTRATION__USING_SESSIONS:
				getUsingSessions().clear();
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
			case ServicesPackage.SERVICE_REGISTRATION__REFERENCE:
				return reference != null;
			case ServicesPackage.SERVICE_REGISTRATION__UNREGISTERED:
				return unregistered != UNREGISTERED_EDEFAULT;
			case ServicesPackage.SERVICE_REGISTRATION__PROVIDER:
				return provider != null;
			case ServicesPackage.SERVICE_REGISTRATION__IMPLEMENTATION:
				return implementation != null;
			case ServicesPackage.SERVICE_REGISTRATION__USING_SESSIONS:
				return usingSessions != null && !usingSessions.isEmpty();
			case ServicesPackage.SERVICE_REGISTRATION__CONSUMER_COUNT:
				return CONSUMER_COUNT__ESETTING_DELEGATE.dynamicIsSet(this, null, 0);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Object eInvoke(int operationID, EList<?> arguments) throws InvocationTargetException {
		switch (operationID) {
			case ServicesPackage.SERVICE_REGISTRATION___UNREGISTER:
				unregister();
				return null;
			case ServicesPackage.SERVICE_REGISTRATION___SET_PROPERTIES__ELIST:
				setProperties((EList<Property>)arguments.get(0));
				return null;
		}
		return super.eInvoke(operationID, arguments);
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
		result.append(" (unregistered: ");
		result.append(unregistered);
		result.append(')');
		return result.toString();
	}

} //ServiceRegistrationImpl
