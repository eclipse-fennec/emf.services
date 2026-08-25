/*
 */
package org.eclipse.fennec.services.impl;

import java.lang.reflect.InvocationTargetException;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.services.ComponentConfiguration;
import org.eclipse.fennec.services.ConnectionState;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceListener;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Local Service Registry</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl#getReferences <em>References</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl#getRegistrations <em>Registrations</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl#getSessions <em>Sessions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl#getConfigurations <em>Configurations</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl#getProviders <em>Providers</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl#getListeners <em>Listeners</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl#getRemote <em>Remote</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl#getConnectionState <em>Connection State</em>}</li>
 * </ul>
 *
 * @generated
 */
public class LocalServiceRegistryImpl extends ServiceRegistryImpl implements LocalServiceRegistry {
	/**
	 * The cached value of the '{@link #getReferences() <em>References</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReferences()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceReference> references;

	/**
	 * The cached value of the '{@link #getRegistrations() <em>Registrations</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRegistrations()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceRegistration> registrations;

	/**
	 * The cached value of the '{@link #getSessions() <em>Sessions</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSessions()
	 * @generated
	 * @ordered
	 */
	protected EList<ConsumerSession> sessions;

	/**
	 * The cached value of the '{@link #getConfigurations() <em>Configurations</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConfigurations()
	 * @generated
	 * @ordered
	 */
	protected EList<ComponentConfiguration> configurations;

	/**
	 * The cached value of the '{@link #getProviders() <em>Providers</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProviders()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceProvider> providers;

	/**
	 * The cached value of the '{@link #getListeners() <em>Listeners</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getListeners()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceListener> listeners;

	/**
	 * The cached value of the '{@link #getRemote() <em>Remote</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRemote()
	 * @generated
	 * @ordered
	 */
	protected RemoteServiceRegistry remote;

	/**
	 * The default value of the '{@link #getConnectionState() <em>Connection State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConnectionState()
	 * @generated
	 * @ordered
	 */
	protected static final ConnectionState CONNECTION_STATE_EDEFAULT = ConnectionState.OFFLINE;

	/**
	 * The cached value of the '{@link #getConnectionState() <em>Connection State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConnectionState()
	 * @generated
	 * @ordered
	 */
	protected ConnectionState connectionState = CONNECTION_STATE_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected LocalServiceRegistryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.LOCAL_SERVICE_REGISTRY;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceReference> getReferences() {
		if (references == null) {
			references = new EObjectContainmentEList<ServiceReference>(ServiceReference.class, this, ServicesPackage.LOCAL_SERVICE_REGISTRY__REFERENCES);
		}
		return references;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceRegistration> getRegistrations() {
		if (registrations == null) {
			registrations = new EObjectContainmentEList<ServiceRegistration>(ServiceRegistration.class, this, ServicesPackage.LOCAL_SERVICE_REGISTRY__REGISTRATIONS);
		}
		return registrations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ConsumerSession> getSessions() {
		if (sessions == null) {
			sessions = new EObjectContainmentEList<ConsumerSession>(ConsumerSession.class, this, ServicesPackage.LOCAL_SERVICE_REGISTRY__SESSIONS);
		}
		return sessions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ComponentConfiguration> getConfigurations() {
		if (configurations == null) {
			configurations = new EObjectContainmentEList<ComponentConfiguration>(ComponentConfiguration.class, this, ServicesPackage.LOCAL_SERVICE_REGISTRY__CONFIGURATIONS);
		}
		return configurations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceProvider> getProviders() {
		if (providers == null) {
			providers = new EObjectContainmentEList<ServiceProvider>(ServiceProvider.class, this, ServicesPackage.LOCAL_SERVICE_REGISTRY__PROVIDERS);
		}
		return providers;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceListener> getListeners() {
		if (listeners == null) {
			listeners = new EObjectResolvingEList<ServiceListener>(ServiceListener.class, this, ServicesPackage.LOCAL_SERVICE_REGISTRY__LISTENERS);
		}
		return listeners;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public RemoteServiceRegistry getRemote() {
		if (remote != null && remote.eIsProxy()) {
			InternalEObject oldRemote = (InternalEObject)remote;
			remote = (RemoteServiceRegistry)eResolveProxy(oldRemote);
			if (remote != oldRemote) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.LOCAL_SERVICE_REGISTRY__REMOTE, oldRemote, remote));
			}
		}
		return remote;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RemoteServiceRegistry basicGetRemote() {
		return remote;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRemote(RemoteServiceRegistry newRemote) {
		RemoteServiceRegistry oldRemote = remote;
		remote = newRemote;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.LOCAL_SERVICE_REGISTRY__REMOTE, oldRemote, remote));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ConnectionState getConnectionState() {
		return connectionState;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setConnectionState(ConnectionState newConnectionState) {
		ConnectionState oldConnectionState = connectionState;
		connectionState = newConnectionState == null ? CONNECTION_STATE_EDEFAULT : newConnectionState;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.LOCAL_SERVICE_REGISTRY__CONNECTION_STATE, oldConnectionState, connectionState));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation, EList<Property> props) {
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
	public void fireServiceEvent(ServiceEvent event) {
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
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REFERENCES:
				return ((InternalEList<?>)getReferences()).basicRemove(otherEnd, msgs);
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REGISTRATIONS:
				return ((InternalEList<?>)getRegistrations()).basicRemove(otherEnd, msgs);
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__SESSIONS:
				return ((InternalEList<?>)getSessions()).basicRemove(otherEnd, msgs);
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__CONFIGURATIONS:
				return ((InternalEList<?>)getConfigurations()).basicRemove(otherEnd, msgs);
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__PROVIDERS:
				return ((InternalEList<?>)getProviders()).basicRemove(otherEnd, msgs);
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
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REFERENCES:
				return getReferences();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REGISTRATIONS:
				return getRegistrations();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__SESSIONS:
				return getSessions();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__CONFIGURATIONS:
				return getConfigurations();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__PROVIDERS:
				return getProviders();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__LISTENERS:
				return getListeners();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REMOTE:
				if (resolve) return getRemote();
				return basicGetRemote();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__CONNECTION_STATE:
				return getConnectionState();
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
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REFERENCES:
				getReferences().clear();
				getReferences().addAll((Collection<? extends ServiceReference>)newValue);
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REGISTRATIONS:
				getRegistrations().clear();
				getRegistrations().addAll((Collection<? extends ServiceRegistration>)newValue);
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__SESSIONS:
				getSessions().clear();
				getSessions().addAll((Collection<? extends ConsumerSession>)newValue);
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__CONFIGURATIONS:
				getConfigurations().clear();
				getConfigurations().addAll((Collection<? extends ComponentConfiguration>)newValue);
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__PROVIDERS:
				getProviders().clear();
				getProviders().addAll((Collection<? extends ServiceProvider>)newValue);
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__LISTENERS:
				getListeners().clear();
				getListeners().addAll((Collection<? extends ServiceListener>)newValue);
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REMOTE:
				setRemote((RemoteServiceRegistry)newValue);
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__CONNECTION_STATE:
				setConnectionState((ConnectionState)newValue);
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
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REFERENCES:
				getReferences().clear();
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REGISTRATIONS:
				getRegistrations().clear();
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__SESSIONS:
				getSessions().clear();
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__CONFIGURATIONS:
				getConfigurations().clear();
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__PROVIDERS:
				getProviders().clear();
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__LISTENERS:
				getListeners().clear();
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REMOTE:
				setRemote((RemoteServiceRegistry)null);
				return;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__CONNECTION_STATE:
				setConnectionState(CONNECTION_STATE_EDEFAULT);
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
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REFERENCES:
				return references != null && !references.isEmpty();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REGISTRATIONS:
				return registrations != null && !registrations.isEmpty();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__SESSIONS:
				return sessions != null && !sessions.isEmpty();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__CONFIGURATIONS:
				return configurations != null && !configurations.isEmpty();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__PROVIDERS:
				return providers != null && !providers.isEmpty();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__LISTENERS:
				return listeners != null && !listeners.isEmpty();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__REMOTE:
				return remote != null;
			case ServicesPackage.LOCAL_SERVICE_REGISTRY__CONNECTION_STATE:
				return connectionState != CONNECTION_STATE_EDEFAULT;
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
			case ServicesPackage.LOCAL_SERVICE_REGISTRY___REGISTER_SERVICE__SERVICEPROVIDER_SERVICEIMPLEMENTATION_ELIST:
				return registerService((ServiceProvider)arguments.get(0), (ServiceImplementation)arguments.get(1), (EList<Property>)arguments.get(2));
			case ServicesPackage.LOCAL_SERVICE_REGISTRY___FIRE_SERVICE_EVENT__SERVICEEVENT:
				fireServiceEvent((ServiceEvent)arguments.get(0));
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
		result.append(" (connectionState: ");
		result.append(connectionState);
		result.append(')');
		return result.toString();
	}

} //LocalServiceRegistryImpl
