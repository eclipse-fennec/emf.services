/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.services.Capability;
import org.eclipse.fennec.services.ComponentDescription;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.UpdatePolicy;
import org.eclipse.fennec.services.VersionedElement;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Service Implementation</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getVersion <em>Version</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getImplementationId <em>Implementation Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getServiceInterfaces <em>Service Interfaces</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getFlavors <em>Flavors</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getComponentDescription <em>Component Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getUpdatePolicy <em>Update Policy</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getReplaces <em>Replaces</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getCutoverGraceMillis <em>Cutover Grace Millis</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl#getCapabilities <em>Capabilities</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ServiceImplementationImpl extends MinimalEObjectImpl.Container implements ServiceImplementation {
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
	 * The default value of the '{@link #getVersion() <em>Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getVersion()
	 * @generated
	 * @ordered
	 */
	protected static final String VERSION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getVersion() <em>Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getVersion()
	 * @generated
	 * @ordered
	 */
	protected String version = VERSION_EDEFAULT;

	/**
	 * The default value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected static final String DESCRIPTION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected String description = DESCRIPTION_EDEFAULT;

	/**
	 * The default value of the '{@link #getImplementationId() <em>Implementation Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getImplementationId()
	 * @generated
	 * @ordered
	 */
	protected static final String IMPLEMENTATION_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getImplementationId() <em>Implementation Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getImplementationId()
	 * @generated
	 * @ordered
	 */
	protected String implementationId = IMPLEMENTATION_ID_EDEFAULT;

	/**
	 * The cached value of the '{@link #getServiceInterfaces() <em>Service Interfaces</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getServiceInterfaces()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceInterface> serviceInterfaces;

	/**
	 * The cached value of the '{@link #getFlavors() <em>Flavors</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFlavors()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceFlavor> flavors;

	/**
	 * The cached value of the '{@link #getProperties() <em>Properties</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProperties()
	 * @generated
	 * @ordered
	 */
	protected EList<Property> properties;

	/**
	 * The cached value of the '{@link #getComponentDescription() <em>Component Description</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getComponentDescription()
	 * @generated
	 * @ordered
	 */
	protected ComponentDescription componentDescription;

	/**
	 * The default value of the '{@link #getUpdatePolicy() <em>Update Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUpdatePolicy()
	 * @generated
	 * @ordered
	 */
	protected static final UpdatePolicy UPDATE_POLICY_EDEFAULT = UpdatePolicy.UNSPECIFIED;

	/**
	 * The cached value of the '{@link #getUpdatePolicy() <em>Update Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUpdatePolicy()
	 * @generated
	 * @ordered
	 */
	protected UpdatePolicy updatePolicy = UPDATE_POLICY_EDEFAULT;

	/**
	 * The cached value of the '{@link #getReplaces() <em>Replaces</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReplaces()
	 * @generated
	 * @ordered
	 */
	protected ServiceImplementation replaces;

	/**
	 * The default value of the '{@link #getCutoverGraceMillis() <em>Cutover Grace Millis</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCutoverGraceMillis()
	 * @generated
	 * @ordered
	 */
	protected static final long CUTOVER_GRACE_MILLIS_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getCutoverGraceMillis() <em>Cutover Grace Millis</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCutoverGraceMillis()
	 * @generated
	 * @ordered
	 */
	protected long cutoverGraceMillis = CUTOVER_GRACE_MILLIS_EDEFAULT;

	/**
	 * The cached value of the '{@link #getCapabilities() <em>Capabilities</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCapabilities()
	 * @generated
	 * @ordered
	 */
	protected EList<Capability> capabilities;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServiceImplementationImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.SERVICE_IMPLEMENTATION;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_IMPLEMENTATION__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getVersion() {
		return version;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setVersion(String newVersion) {
		String oldVersion = version;
		version = newVersion;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_IMPLEMENTATION__VERSION, oldVersion, version));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDescription(String newDescription) {
		String oldDescription = description;
		description = newDescription;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_IMPLEMENTATION__DESCRIPTION, oldDescription, description));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getImplementationId() {
		return implementationId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setImplementationId(String newImplementationId) {
		String oldImplementationId = implementationId;
		implementationId = newImplementationId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_IMPLEMENTATION__IMPLEMENTATION_ID, oldImplementationId, implementationId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceInterface> getServiceInterfaces() {
		if (serviceInterfaces == null) {
			serviceInterfaces = new EObjectResolvingEList<ServiceInterface>(ServiceInterface.class, this, ServicesPackage.SERVICE_IMPLEMENTATION__SERVICE_INTERFACES);
		}
		return serviceInterfaces;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceFlavor> getFlavors() {
		if (flavors == null) {
			flavors = new EObjectContainmentEList<ServiceFlavor>(ServiceFlavor.class, this, ServicesPackage.SERVICE_IMPLEMENTATION__FLAVORS);
		}
		return flavors;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Property> getProperties() {
		if (properties == null) {
			properties = new EObjectContainmentEList<Property>(Property.class, this, ServicesPackage.SERVICE_IMPLEMENTATION__PROPERTIES);
		}
		return properties;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ComponentDescription getComponentDescription() {
		if (componentDescription != null && componentDescription.eIsProxy()) {
			InternalEObject oldComponentDescription = (InternalEObject)componentDescription;
			componentDescription = (ComponentDescription)eResolveProxy(oldComponentDescription);
			if (componentDescription != oldComponentDescription) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.SERVICE_IMPLEMENTATION__COMPONENT_DESCRIPTION, oldComponentDescription, componentDescription));
			}
		}
		return componentDescription;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComponentDescription basicGetComponentDescription() {
		return componentDescription;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setComponentDescription(ComponentDescription newComponentDescription) {
		ComponentDescription oldComponentDescription = componentDescription;
		componentDescription = newComponentDescription;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_IMPLEMENTATION__COMPONENT_DESCRIPTION, oldComponentDescription, componentDescription));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public UpdatePolicy getUpdatePolicy() {
		return updatePolicy;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setUpdatePolicy(UpdatePolicy newUpdatePolicy) {
		UpdatePolicy oldUpdatePolicy = updatePolicy;
		updatePolicy = newUpdatePolicy == null ? UPDATE_POLICY_EDEFAULT : newUpdatePolicy;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_IMPLEMENTATION__UPDATE_POLICY, oldUpdatePolicy, updatePolicy));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceImplementation getReplaces() {
		if (replaces != null && replaces.eIsProxy()) {
			InternalEObject oldReplaces = (InternalEObject)replaces;
			replaces = (ServiceImplementation)eResolveProxy(oldReplaces);
			if (replaces != oldReplaces) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.SERVICE_IMPLEMENTATION__REPLACES, oldReplaces, replaces));
			}
		}
		return replaces;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServiceImplementation basicGetReplaces() {
		return replaces;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setReplaces(ServiceImplementation newReplaces) {
		ServiceImplementation oldReplaces = replaces;
		replaces = newReplaces;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_IMPLEMENTATION__REPLACES, oldReplaces, replaces));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public long getCutoverGraceMillis() {
		return cutoverGraceMillis;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCutoverGraceMillis(long newCutoverGraceMillis) {
		long oldCutoverGraceMillis = cutoverGraceMillis;
		cutoverGraceMillis = newCutoverGraceMillis;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_IMPLEMENTATION__CUTOVER_GRACE_MILLIS, oldCutoverGraceMillis, cutoverGraceMillis));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Capability> getCapabilities() {
		if (capabilities == null) {
			capabilities = new EObjectContainmentEList<Capability>(Capability.class, this, ServicesPackage.SERVICE_IMPLEMENTATION__CAPABILITIES);
		}
		return capabilities;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.SERVICE_IMPLEMENTATION__FLAVORS:
				return ((InternalEList<?>)getFlavors()).basicRemove(otherEnd, msgs);
			case ServicesPackage.SERVICE_IMPLEMENTATION__PROPERTIES:
				return ((InternalEList<?>)getProperties()).basicRemove(otherEnd, msgs);
			case ServicesPackage.SERVICE_IMPLEMENTATION__CAPABILITIES:
				return ((InternalEList<?>)getCapabilities()).basicRemove(otherEnd, msgs);
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
			case ServicesPackage.SERVICE_IMPLEMENTATION__NAME:
				return getName();
			case ServicesPackage.SERVICE_IMPLEMENTATION__VERSION:
				return getVersion();
			case ServicesPackage.SERVICE_IMPLEMENTATION__DESCRIPTION:
				return getDescription();
			case ServicesPackage.SERVICE_IMPLEMENTATION__IMPLEMENTATION_ID:
				return getImplementationId();
			case ServicesPackage.SERVICE_IMPLEMENTATION__SERVICE_INTERFACES:
				return getServiceInterfaces();
			case ServicesPackage.SERVICE_IMPLEMENTATION__FLAVORS:
				return getFlavors();
			case ServicesPackage.SERVICE_IMPLEMENTATION__PROPERTIES:
				return getProperties();
			case ServicesPackage.SERVICE_IMPLEMENTATION__COMPONENT_DESCRIPTION:
				if (resolve) return getComponentDescription();
				return basicGetComponentDescription();
			case ServicesPackage.SERVICE_IMPLEMENTATION__UPDATE_POLICY:
				return getUpdatePolicy();
			case ServicesPackage.SERVICE_IMPLEMENTATION__REPLACES:
				if (resolve) return getReplaces();
				return basicGetReplaces();
			case ServicesPackage.SERVICE_IMPLEMENTATION__CUTOVER_GRACE_MILLIS:
				return getCutoverGraceMillis();
			case ServicesPackage.SERVICE_IMPLEMENTATION__CAPABILITIES:
				return getCapabilities();
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
			case ServicesPackage.SERVICE_IMPLEMENTATION__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__VERSION:
				setVersion((String)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__DESCRIPTION:
				setDescription((String)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__IMPLEMENTATION_ID:
				setImplementationId((String)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__SERVICE_INTERFACES:
				getServiceInterfaces().clear();
				getServiceInterfaces().addAll((Collection<? extends ServiceInterface>)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__FLAVORS:
				getFlavors().clear();
				getFlavors().addAll((Collection<? extends ServiceFlavor>)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__PROPERTIES:
				getProperties().clear();
				getProperties().addAll((Collection<? extends Property>)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__COMPONENT_DESCRIPTION:
				setComponentDescription((ComponentDescription)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__UPDATE_POLICY:
				setUpdatePolicy((UpdatePolicy)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__REPLACES:
				setReplaces((ServiceImplementation)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__CUTOVER_GRACE_MILLIS:
				setCutoverGraceMillis((Long)newValue);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__CAPABILITIES:
				getCapabilities().clear();
				getCapabilities().addAll((Collection<? extends Capability>)newValue);
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
			case ServicesPackage.SERVICE_IMPLEMENTATION__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__VERSION:
				setVersion(VERSION_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__DESCRIPTION:
				setDescription(DESCRIPTION_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__IMPLEMENTATION_ID:
				setImplementationId(IMPLEMENTATION_ID_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__SERVICE_INTERFACES:
				getServiceInterfaces().clear();
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__FLAVORS:
				getFlavors().clear();
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__PROPERTIES:
				getProperties().clear();
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__COMPONENT_DESCRIPTION:
				setComponentDescription((ComponentDescription)null);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__UPDATE_POLICY:
				setUpdatePolicy(UPDATE_POLICY_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__REPLACES:
				setReplaces((ServiceImplementation)null);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__CUTOVER_GRACE_MILLIS:
				setCutoverGraceMillis(CUTOVER_GRACE_MILLIS_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_IMPLEMENTATION__CAPABILITIES:
				getCapabilities().clear();
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
			case ServicesPackage.SERVICE_IMPLEMENTATION__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.SERVICE_IMPLEMENTATION__VERSION:
				return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
			case ServicesPackage.SERVICE_IMPLEMENTATION__DESCRIPTION:
				return DESCRIPTION_EDEFAULT == null ? description != null : !DESCRIPTION_EDEFAULT.equals(description);
			case ServicesPackage.SERVICE_IMPLEMENTATION__IMPLEMENTATION_ID:
				return IMPLEMENTATION_ID_EDEFAULT == null ? implementationId != null : !IMPLEMENTATION_ID_EDEFAULT.equals(implementationId);
			case ServicesPackage.SERVICE_IMPLEMENTATION__SERVICE_INTERFACES:
				return serviceInterfaces != null && !serviceInterfaces.isEmpty();
			case ServicesPackage.SERVICE_IMPLEMENTATION__FLAVORS:
				return flavors != null && !flavors.isEmpty();
			case ServicesPackage.SERVICE_IMPLEMENTATION__PROPERTIES:
				return properties != null && !properties.isEmpty();
			case ServicesPackage.SERVICE_IMPLEMENTATION__COMPONENT_DESCRIPTION:
				return componentDescription != null;
			case ServicesPackage.SERVICE_IMPLEMENTATION__UPDATE_POLICY:
				return updatePolicy != UPDATE_POLICY_EDEFAULT;
			case ServicesPackage.SERVICE_IMPLEMENTATION__REPLACES:
				return replaces != null;
			case ServicesPackage.SERVICE_IMPLEMENTATION__CUTOVER_GRACE_MILLIS:
				return cutoverGraceMillis != CUTOVER_GRACE_MILLIS_EDEFAULT;
			case ServicesPackage.SERVICE_IMPLEMENTATION__CAPABILITIES:
				return capabilities != null && !capabilities.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int eBaseStructuralFeatureID(int derivedFeatureID, Class<?> baseClass) {
		if (baseClass == VersionedElement.class) {
			switch (derivedFeatureID) {
				case ServicesPackage.SERVICE_IMPLEMENTATION__VERSION: return ServicesPackage.VERSIONED_ELEMENT__VERSION;
				default: return -1;
			}
		}
		return super.eBaseStructuralFeatureID(derivedFeatureID, baseClass);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int eDerivedStructuralFeatureID(int baseFeatureID, Class<?> baseClass) {
		if (baseClass == VersionedElement.class) {
			switch (baseFeatureID) {
				case ServicesPackage.VERSIONED_ELEMENT__VERSION: return ServicesPackage.SERVICE_IMPLEMENTATION__VERSION;
				default: return -1;
			}
		}
		return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
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
		result.append(", version: ");
		result.append(version);
		result.append(", description: ");
		result.append(description);
		result.append(", implementationId: ");
		result.append(implementationId);
		result.append(", updatePolicy: ");
		result.append(updatePolicy);
		result.append(", cutoverGraceMillis: ");
		result.append(cutoverGraceMillis);
		result.append(')');
		return result.toString();
	}

} //ServiceImplementationImpl
