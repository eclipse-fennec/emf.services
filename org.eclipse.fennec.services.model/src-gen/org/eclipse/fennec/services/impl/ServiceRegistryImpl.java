/*
 */
package org.eclipse.fennec.services.impl;

import java.lang.reflect.InvocationTargetException;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectResolvingEList;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.DiscoveryHook;
import org.eclipse.fennec.services.DistributionHook;
import org.eclipse.fennec.services.PublishHook;
import org.eclipse.fennec.services.RegistryKind;
import org.eclipse.fennec.services.ServiceListener;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistry;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Service Registry</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistryImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistryImpl#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistryImpl#getPublishHooks <em>Publish Hooks</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistryImpl#getDiscoveryHooks <em>Discovery Hooks</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceRegistryImpl#getDistributionHooks <em>Distribution Hooks</em>}</li>
 * </ul>
 *
 * @generated
 */
public abstract class ServiceRegistryImpl extends MinimalEObjectImpl.Container implements ServiceRegistry {
	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getKind() <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getKind()
	 * @generated
	 * @ordered
	 */
	protected static final RegistryKind KIND_EDEFAULT = RegistryKind.LOCAL;

	/**
	 * The cached value of the '{@link #getKind() <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getKind()
	 * @generated
	 * @ordered
	 */
	protected RegistryKind kind = KIND_EDEFAULT;

	/**
	 * The cached value of the '{@link #getPublishHooks() <em>Publish Hooks</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPublishHooks()
	 * @generated
	 * @ordered
	 */
	protected EList<PublishHook> publishHooks;

	/**
	 * The cached value of the '{@link #getDiscoveryHooks() <em>Discovery Hooks</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDiscoveryHooks()
	 * @generated
	 * @ordered
	 */
	protected EList<DiscoveryHook> discoveryHooks;

	/**
	 * The cached value of the '{@link #getDistributionHooks() <em>Distribution Hooks</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDistributionHooks()
	 * @generated
	 * @ordered
	 */
	protected EList<DistributionHook> distributionHooks;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServiceRegistryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.SERVICE_REGISTRY;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_REGISTRY__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public RegistryKind getKind() {
		return kind;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setKind(RegistryKind newKind) {
		RegistryKind oldKind = kind;
		kind = newKind == null ? KIND_EDEFAULT : newKind;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_REGISTRY__KIND, oldKind, kind));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<PublishHook> getPublishHooks() {
		if (publishHooks == null) {
			publishHooks = new EObjectResolvingEList<PublishHook>(PublishHook.class, this, ServicesPackage.SERVICE_REGISTRY__PUBLISH_HOOKS);
		}
		return publishHooks;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<DiscoveryHook> getDiscoveryHooks() {
		if (discoveryHooks == null) {
			discoveryHooks = new EObjectResolvingEList<DiscoveryHook>(DiscoveryHook.class, this, ServicesPackage.SERVICE_REGISTRY__DISCOVERY_HOOKS);
		}
		return discoveryHooks;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<DistributionHook> getDistributionHooks() {
		if (distributionHooks == null) {
			distributionHooks = new EObjectResolvingEList<DistributionHook>(DistributionHook.class, this, ServicesPackage.SERVICE_REGISTRY__DISTRIBUTION_HOOKS);
		}
		return distributionHooks;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceReference getServiceReference(String interfaceName) {
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
	public EList<ServiceReference> getServiceReferences(String interfaceName, String filter, ConsumerCapability capability) {
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
	public EList<ServiceReference> getAllServiceReferences(String interfaceName, String filter, ConsumerCapability capability) {
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
	public void addServiceListener(ServiceListener listener) {
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
	public void removeServiceListener(ServiceListener listener) {
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
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.SERVICE_REGISTRY__NAME:
				return getName();
			case ServicesPackage.SERVICE_REGISTRY__KIND:
				return getKind();
			case ServicesPackage.SERVICE_REGISTRY__PUBLISH_HOOKS:
				return getPublishHooks();
			case ServicesPackage.SERVICE_REGISTRY__DISCOVERY_HOOKS:
				return getDiscoveryHooks();
			case ServicesPackage.SERVICE_REGISTRY__DISTRIBUTION_HOOKS:
				return getDistributionHooks();
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
			case ServicesPackage.SERVICE_REGISTRY__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.SERVICE_REGISTRY__KIND:
				setKind((RegistryKind)newValue);
				return;
			case ServicesPackage.SERVICE_REGISTRY__PUBLISH_HOOKS:
				getPublishHooks().clear();
				getPublishHooks().addAll((Collection<? extends PublishHook>)newValue);
				return;
			case ServicesPackage.SERVICE_REGISTRY__DISCOVERY_HOOKS:
				getDiscoveryHooks().clear();
				getDiscoveryHooks().addAll((Collection<? extends DiscoveryHook>)newValue);
				return;
			case ServicesPackage.SERVICE_REGISTRY__DISTRIBUTION_HOOKS:
				getDistributionHooks().clear();
				getDistributionHooks().addAll((Collection<? extends DistributionHook>)newValue);
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
			case ServicesPackage.SERVICE_REGISTRY__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_REGISTRY__KIND:
				setKind(KIND_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_REGISTRY__PUBLISH_HOOKS:
				getPublishHooks().clear();
				return;
			case ServicesPackage.SERVICE_REGISTRY__DISCOVERY_HOOKS:
				getDiscoveryHooks().clear();
				return;
			case ServicesPackage.SERVICE_REGISTRY__DISTRIBUTION_HOOKS:
				getDistributionHooks().clear();
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
			case ServicesPackage.SERVICE_REGISTRY__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.SERVICE_REGISTRY__KIND:
				return kind != KIND_EDEFAULT;
			case ServicesPackage.SERVICE_REGISTRY__PUBLISH_HOOKS:
				return publishHooks != null && !publishHooks.isEmpty();
			case ServicesPackage.SERVICE_REGISTRY__DISCOVERY_HOOKS:
				return discoveryHooks != null && !discoveryHooks.isEmpty();
			case ServicesPackage.SERVICE_REGISTRY__DISTRIBUTION_HOOKS:
				return distributionHooks != null && !distributionHooks.isEmpty();
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
			case ServicesPackage.SERVICE_REGISTRY___GET_SERVICE_REFERENCE__STRING:
				return getServiceReference((String)arguments.get(0));
			case ServicesPackage.SERVICE_REGISTRY___GET_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY:
				return getServiceReferences((String)arguments.get(0), (String)arguments.get(1), (ConsumerCapability)arguments.get(2));
			case ServicesPackage.SERVICE_REGISTRY___GET_ALL_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY:
				return getAllServiceReferences((String)arguments.get(0), (String)arguments.get(1), (ConsumerCapability)arguments.get(2));
			case ServicesPackage.SERVICE_REGISTRY___ADD_SERVICE_LISTENER__SERVICELISTENER:
				addServiceListener((ServiceListener)arguments.get(0));
				return null;
			case ServicesPackage.SERVICE_REGISTRY___REMOVE_SERVICE_LISTENER__SERVICELISTENER:
				removeServiceListener((ServiceListener)arguments.get(0));
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
		result.append(" (name: ");
		result.append(name);
		result.append(", kind: ");
		result.append(kind);
		result.append(')');
		return result.toString();
	}

} //ServiceRegistryImpl
