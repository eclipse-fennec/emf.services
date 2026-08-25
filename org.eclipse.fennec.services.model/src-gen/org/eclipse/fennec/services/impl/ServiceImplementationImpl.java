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

import org.eclipse.fennec.services.ComponentDescription;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesPackage;
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
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.SERVICE_IMPLEMENTATION__FLAVORS:
				return ((InternalEList<?>)getFlavors()).basicRemove(otherEnd, msgs);
			case ServicesPackage.SERVICE_IMPLEMENTATION__PROPERTIES:
				return ((InternalEList<?>)getProperties()).basicRemove(otherEnd, msgs);
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
		result.append(')');
		return result.toString();
	}

} //ServiceImplementationImpl
