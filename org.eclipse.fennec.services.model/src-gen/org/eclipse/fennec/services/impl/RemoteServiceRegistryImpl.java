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

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Remote Service Registry</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.RemoteServiceRegistryImpl#getEndpoint <em>Endpoint</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.RemoteServiceRegistryImpl#getCatalog <em>Catalog</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.RemoteServiceRegistryImpl#getImplementations <em>Implementations</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.RemoteServiceRegistryImpl#getProviders <em>Providers</em>}</li>
 * </ul>
 *
 * @generated
 */
public class RemoteServiceRegistryImpl extends ServiceRegistryImpl implements RemoteServiceRegistry {
	/**
	 * The default value of the '{@link #getEndpoint() <em>Endpoint</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEndpoint()
	 * @generated
	 * @ordered
	 */
	protected static final String ENDPOINT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getEndpoint() <em>Endpoint</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEndpoint()
	 * @generated
	 * @ordered
	 */
	protected String endpoint = ENDPOINT_EDEFAULT;

	/**
	 * The cached value of the '{@link #getCatalog() <em>Catalog</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCatalog()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceInterface> catalog;

	/**
	 * The cached value of the '{@link #getImplementations() <em>Implementations</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getImplementations()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceImplementation> implementations;

	/**
	 * The cached value of the '{@link #getProviders() <em>Providers</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProviders()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceProvider> providers;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected RemoteServiceRegistryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.REMOTE_SERVICE_REGISTRY;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setEndpoint(String newEndpoint) {
		String oldEndpoint = endpoint;
		endpoint = newEndpoint;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.REMOTE_SERVICE_REGISTRY__ENDPOINT, oldEndpoint, endpoint));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceInterface> getCatalog() {
		if (catalog == null) {
			catalog = new EObjectContainmentEList<ServiceInterface>(ServiceInterface.class, this, ServicesPackage.REMOTE_SERVICE_REGISTRY__CATALOG);
		}
		return catalog;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceImplementation> getImplementations() {
		if (implementations == null) {
			implementations = new EObjectResolvingEList<ServiceImplementation>(ServiceImplementation.class, this, ServicesPackage.REMOTE_SERVICE_REGISTRY__IMPLEMENTATIONS);
		}
		return implementations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceProvider> getProviders() {
		if (providers == null) {
			providers = new EObjectResolvingEList<ServiceProvider>(ServiceProvider.class, this, ServicesPackage.REMOTE_SERVICE_REGISTRY__PROVIDERS);
		}
		return providers;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
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
	public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
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
	public Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor) {
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
	public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
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
	public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
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
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__CATALOG:
				return ((InternalEList<?>)getCatalog()).basicRemove(otherEnd, msgs);
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
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__ENDPOINT:
				return getEndpoint();
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__CATALOG:
				return getCatalog();
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__IMPLEMENTATIONS:
				return getImplementations();
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__PROVIDERS:
				return getProviders();
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
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__ENDPOINT:
				setEndpoint((String)newValue);
				return;
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__CATALOG:
				getCatalog().clear();
				getCatalog().addAll((Collection<? extends ServiceInterface>)newValue);
				return;
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__IMPLEMENTATIONS:
				getImplementations().clear();
				getImplementations().addAll((Collection<? extends ServiceImplementation>)newValue);
				return;
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__PROVIDERS:
				getProviders().clear();
				getProviders().addAll((Collection<? extends ServiceProvider>)newValue);
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
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__ENDPOINT:
				setEndpoint(ENDPOINT_EDEFAULT);
				return;
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__CATALOG:
				getCatalog().clear();
				return;
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__IMPLEMENTATIONS:
				getImplementations().clear();
				return;
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__PROVIDERS:
				getProviders().clear();
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
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__ENDPOINT:
				return ENDPOINT_EDEFAULT == null ? endpoint != null : !ENDPOINT_EDEFAULT.equals(endpoint);
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__CATALOG:
				return catalog != null && !catalog.isEmpty();
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__IMPLEMENTATIONS:
				return implementations != null && !implementations.isEmpty();
			case ServicesPackage.REMOTE_SERVICE_REGISTRY__PROVIDERS:
				return providers != null && !providers.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eInvoke(int operationID, EList<?> arguments) throws InvocationTargetException {
		switch (operationID) {
			case ServicesPackage.REMOTE_SERVICE_REGISTRY___PUBLISH_IMPLEMENTATION__SERVICEPROVIDER_SERVICEIMPLEMENTATION:
				return publishImplementation((ServiceProvider)arguments.get(0), (ServiceImplementation)arguments.get(1));
			case ServicesPackage.REMOTE_SERVICE_REGISTRY___WITHDRAW_IMPLEMENTATION__SERVICEPROVIDER_SERVICEIMPLEMENTATION:
				return withdrawImplementation((ServiceProvider)arguments.get(0), (ServiceImplementation)arguments.get(1));
			case ServicesPackage.REMOTE_SERVICE_REGISTRY___ADD_CATALOG_ENTRY__SERVICEINTERFACE_STRING:
				return addCatalogEntry((ServiceInterface)arguments.get(0), (String)arguments.get(1));
			case ServicesPackage.REMOTE_SERVICE_REGISTRY___DEPRECATE_CATALOG_ENTRY__SERVICEINTERFACE_STRING:
				return deprecateCatalogEntry((ServiceInterface)arguments.get(0), (String)arguments.get(1));
			case ServicesPackage.REMOTE_SERVICE_REGISTRY___REMOVE_CATALOG_ENTRY__SERVICEINTERFACE_STRING:
				return removeCatalogEntry((ServiceInterface)arguments.get(0), (String)arguments.get(1));
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
		result.append(" (endpoint: ");
		result.append(endpoint);
		result.append(')');
		return result.toString();
	}

} //RemoteServiceRegistryImpl
