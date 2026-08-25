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

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.services.ComponentDescription;
import org.eclipse.fennec.services.ComponentReference;
import org.eclipse.fennec.services.ConfigurationPolicy;
import org.eclipse.fennec.services.LifecycleHook;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceScope;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Component Description</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getFactory <em>Factory</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getScope <em>Scope</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getImplementationId <em>Implementation Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#isDefaultEnabled <em>Default Enabled</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#isImmediate <em>Immediate</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getConfigurationPolicy <em>Configuration Policy</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getConfigurationPid <em>Configuration Pid</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getServiceInterfaces <em>Service Interfaces</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getFactoryProperties <em>Factory Properties</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getReferences <em>References</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getLifecycleHooks <em>Lifecycle Hooks</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl#getProvider <em>Provider</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ComponentDescriptionImpl extends MinimalEObjectImpl.Container implements ComponentDescription {
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
	 * The default value of the '{@link #getFactory() <em>Factory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFactory()
	 * @generated
	 * @ordered
	 */
	protected static final String FACTORY_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getFactory() <em>Factory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFactory()
	 * @generated
	 * @ordered
	 */
	protected String factory = FACTORY_EDEFAULT;

	/**
	 * The default value of the '{@link #getScope() <em>Scope</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getScope()
	 * @generated
	 * @ordered
	 */
	protected static final ServiceScope SCOPE_EDEFAULT = ServiceScope.SINGLETON;

	/**
	 * The cached value of the '{@link #getScope() <em>Scope</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getScope()
	 * @generated
	 * @ordered
	 */
	protected ServiceScope scope = SCOPE_EDEFAULT;

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
	 * The default value of the '{@link #isDefaultEnabled() <em>Default Enabled</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDefaultEnabled()
	 * @generated
	 * @ordered
	 */
	protected static final boolean DEFAULT_ENABLED_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isDefaultEnabled() <em>Default Enabled</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDefaultEnabled()
	 * @generated
	 * @ordered
	 */
	protected boolean defaultEnabled = DEFAULT_ENABLED_EDEFAULT;

	/**
	 * The default value of the '{@link #isImmediate() <em>Immediate</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isImmediate()
	 * @generated
	 * @ordered
	 */
	protected static final boolean IMMEDIATE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isImmediate() <em>Immediate</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isImmediate()
	 * @generated
	 * @ordered
	 */
	protected boolean immediate = IMMEDIATE_EDEFAULT;

	/**
	 * The default value of the '{@link #getConfigurationPolicy() <em>Configuration Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConfigurationPolicy()
	 * @generated
	 * @ordered
	 */
	protected static final ConfigurationPolicy CONFIGURATION_POLICY_EDEFAULT = ConfigurationPolicy.OPTIONAL;

	/**
	 * The cached value of the '{@link #getConfigurationPolicy() <em>Configuration Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConfigurationPolicy()
	 * @generated
	 * @ordered
	 */
	protected ConfigurationPolicy configurationPolicy = CONFIGURATION_POLICY_EDEFAULT;

	/**
	 * The cached value of the '{@link #getConfigurationPid() <em>Configuration Pid</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConfigurationPid()
	 * @generated
	 * @ordered
	 */
	protected EList<String> configurationPid;

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
	 * The cached value of the '{@link #getProperties() <em>Properties</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProperties()
	 * @generated
	 * @ordered
	 */
	protected EList<Property> properties;

	/**
	 * The cached value of the '{@link #getFactoryProperties() <em>Factory Properties</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFactoryProperties()
	 * @generated
	 * @ordered
	 */
	protected EList<Property> factoryProperties;

	/**
	 * The cached value of the '{@link #getReferences() <em>References</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReferences()
	 * @generated
	 * @ordered
	 */
	protected EList<ComponentReference> references;

	/**
	 * The cached value of the '{@link #getLifecycleHooks() <em>Lifecycle Hooks</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLifecycleHooks()
	 * @generated
	 * @ordered
	 */
	protected EList<LifecycleHook> lifecycleHooks;

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
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ComponentDescriptionImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.COMPONENT_DESCRIPTION;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_DESCRIPTION__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getFactory() {
		return factory;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFactory(String newFactory) {
		String oldFactory = factory;
		factory = newFactory;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_DESCRIPTION__FACTORY, oldFactory, factory));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceScope getScope() {
		return scope;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setScope(ServiceScope newScope) {
		ServiceScope oldScope = scope;
		scope = newScope == null ? SCOPE_EDEFAULT : newScope;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_DESCRIPTION__SCOPE, oldScope, scope));
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_DESCRIPTION__IMPLEMENTATION_ID, oldImplementationId, implementationId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isDefaultEnabled() {
		return defaultEnabled;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDefaultEnabled(boolean newDefaultEnabled) {
		boolean oldDefaultEnabled = defaultEnabled;
		defaultEnabled = newDefaultEnabled;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_DESCRIPTION__DEFAULT_ENABLED, oldDefaultEnabled, defaultEnabled));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isImmediate() {
		return immediate;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setImmediate(boolean newImmediate) {
		boolean oldImmediate = immediate;
		immediate = newImmediate;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_DESCRIPTION__IMMEDIATE, oldImmediate, immediate));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ConfigurationPolicy getConfigurationPolicy() {
		return configurationPolicy;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setConfigurationPolicy(ConfigurationPolicy newConfigurationPolicy) {
		ConfigurationPolicy oldConfigurationPolicy = configurationPolicy;
		configurationPolicy = newConfigurationPolicy == null ? CONFIGURATION_POLICY_EDEFAULT : newConfigurationPolicy;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_POLICY, oldConfigurationPolicy, configurationPolicy));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getConfigurationPid() {
		if (configurationPid == null) {
			configurationPid = new EDataTypeUniqueEList<String>(String.class, this, ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_PID);
		}
		return configurationPid;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceInterface> getServiceInterfaces() {
		if (serviceInterfaces == null) {
			serviceInterfaces = new EObjectResolvingEList<ServiceInterface>(ServiceInterface.class, this, ServicesPackage.COMPONENT_DESCRIPTION__SERVICE_INTERFACES);
		}
		return serviceInterfaces;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Property> getProperties() {
		if (properties == null) {
			properties = new EObjectContainmentEList<Property>(Property.class, this, ServicesPackage.COMPONENT_DESCRIPTION__PROPERTIES);
		}
		return properties;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Property> getFactoryProperties() {
		if (factoryProperties == null) {
			factoryProperties = new EObjectContainmentEList<Property>(Property.class, this, ServicesPackage.COMPONENT_DESCRIPTION__FACTORY_PROPERTIES);
		}
		return factoryProperties;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ComponentReference> getReferences() {
		if (references == null) {
			references = new EObjectContainmentEList<ComponentReference>(ComponentReference.class, this, ServicesPackage.COMPONENT_DESCRIPTION__REFERENCES);
		}
		return references;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<LifecycleHook> getLifecycleHooks() {
		if (lifecycleHooks == null) {
			lifecycleHooks = new EObjectContainmentEList<LifecycleHook>(LifecycleHook.class, this, ServicesPackage.COMPONENT_DESCRIPTION__LIFECYCLE_HOOKS);
		}
		return lifecycleHooks;
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
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.COMPONENT_DESCRIPTION__PROVIDER, oldProvider, provider));
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_DESCRIPTION__PROVIDER, oldProvider, provider));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.COMPONENT_DESCRIPTION__PROPERTIES:
				return ((InternalEList<?>)getProperties()).basicRemove(otherEnd, msgs);
			case ServicesPackage.COMPONENT_DESCRIPTION__FACTORY_PROPERTIES:
				return ((InternalEList<?>)getFactoryProperties()).basicRemove(otherEnd, msgs);
			case ServicesPackage.COMPONENT_DESCRIPTION__REFERENCES:
				return ((InternalEList<?>)getReferences()).basicRemove(otherEnd, msgs);
			case ServicesPackage.COMPONENT_DESCRIPTION__LIFECYCLE_HOOKS:
				return ((InternalEList<?>)getLifecycleHooks()).basicRemove(otherEnd, msgs);
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
			case ServicesPackage.COMPONENT_DESCRIPTION__NAME:
				return getName();
			case ServicesPackage.COMPONENT_DESCRIPTION__FACTORY:
				return getFactory();
			case ServicesPackage.COMPONENT_DESCRIPTION__SCOPE:
				return getScope();
			case ServicesPackage.COMPONENT_DESCRIPTION__IMPLEMENTATION_ID:
				return getImplementationId();
			case ServicesPackage.COMPONENT_DESCRIPTION__DEFAULT_ENABLED:
				return isDefaultEnabled();
			case ServicesPackage.COMPONENT_DESCRIPTION__IMMEDIATE:
				return isImmediate();
			case ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_POLICY:
				return getConfigurationPolicy();
			case ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_PID:
				return getConfigurationPid();
			case ServicesPackage.COMPONENT_DESCRIPTION__SERVICE_INTERFACES:
				return getServiceInterfaces();
			case ServicesPackage.COMPONENT_DESCRIPTION__PROPERTIES:
				return getProperties();
			case ServicesPackage.COMPONENT_DESCRIPTION__FACTORY_PROPERTIES:
				return getFactoryProperties();
			case ServicesPackage.COMPONENT_DESCRIPTION__REFERENCES:
				return getReferences();
			case ServicesPackage.COMPONENT_DESCRIPTION__LIFECYCLE_HOOKS:
				return getLifecycleHooks();
			case ServicesPackage.COMPONENT_DESCRIPTION__PROVIDER:
				if (resolve) return getProvider();
				return basicGetProvider();
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
			case ServicesPackage.COMPONENT_DESCRIPTION__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__FACTORY:
				setFactory((String)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__SCOPE:
				setScope((ServiceScope)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__IMPLEMENTATION_ID:
				setImplementationId((String)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__DEFAULT_ENABLED:
				setDefaultEnabled((Boolean)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__IMMEDIATE:
				setImmediate((Boolean)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_POLICY:
				setConfigurationPolicy((ConfigurationPolicy)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_PID:
				getConfigurationPid().clear();
				getConfigurationPid().addAll((Collection<? extends String>)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__SERVICE_INTERFACES:
				getServiceInterfaces().clear();
				getServiceInterfaces().addAll((Collection<? extends ServiceInterface>)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__PROPERTIES:
				getProperties().clear();
				getProperties().addAll((Collection<? extends Property>)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__FACTORY_PROPERTIES:
				getFactoryProperties().clear();
				getFactoryProperties().addAll((Collection<? extends Property>)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__REFERENCES:
				getReferences().clear();
				getReferences().addAll((Collection<? extends ComponentReference>)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__LIFECYCLE_HOOKS:
				getLifecycleHooks().clear();
				getLifecycleHooks().addAll((Collection<? extends LifecycleHook>)newValue);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__PROVIDER:
				setProvider((ServiceProvider)newValue);
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
			case ServicesPackage.COMPONENT_DESCRIPTION__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__FACTORY:
				setFactory(FACTORY_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__SCOPE:
				setScope(SCOPE_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__IMPLEMENTATION_ID:
				setImplementationId(IMPLEMENTATION_ID_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__DEFAULT_ENABLED:
				setDefaultEnabled(DEFAULT_ENABLED_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__IMMEDIATE:
				setImmediate(IMMEDIATE_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_POLICY:
				setConfigurationPolicy(CONFIGURATION_POLICY_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_PID:
				getConfigurationPid().clear();
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__SERVICE_INTERFACES:
				getServiceInterfaces().clear();
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__PROPERTIES:
				getProperties().clear();
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__FACTORY_PROPERTIES:
				getFactoryProperties().clear();
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__REFERENCES:
				getReferences().clear();
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__LIFECYCLE_HOOKS:
				getLifecycleHooks().clear();
				return;
			case ServicesPackage.COMPONENT_DESCRIPTION__PROVIDER:
				setProvider((ServiceProvider)null);
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
			case ServicesPackage.COMPONENT_DESCRIPTION__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.COMPONENT_DESCRIPTION__FACTORY:
				return FACTORY_EDEFAULT == null ? factory != null : !FACTORY_EDEFAULT.equals(factory);
			case ServicesPackage.COMPONENT_DESCRIPTION__SCOPE:
				return scope != SCOPE_EDEFAULT;
			case ServicesPackage.COMPONENT_DESCRIPTION__IMPLEMENTATION_ID:
				return IMPLEMENTATION_ID_EDEFAULT == null ? implementationId != null : !IMPLEMENTATION_ID_EDEFAULT.equals(implementationId);
			case ServicesPackage.COMPONENT_DESCRIPTION__DEFAULT_ENABLED:
				return defaultEnabled != DEFAULT_ENABLED_EDEFAULT;
			case ServicesPackage.COMPONENT_DESCRIPTION__IMMEDIATE:
				return immediate != IMMEDIATE_EDEFAULT;
			case ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_POLICY:
				return configurationPolicy != CONFIGURATION_POLICY_EDEFAULT;
			case ServicesPackage.COMPONENT_DESCRIPTION__CONFIGURATION_PID:
				return configurationPid != null && !configurationPid.isEmpty();
			case ServicesPackage.COMPONENT_DESCRIPTION__SERVICE_INTERFACES:
				return serviceInterfaces != null && !serviceInterfaces.isEmpty();
			case ServicesPackage.COMPONENT_DESCRIPTION__PROPERTIES:
				return properties != null && !properties.isEmpty();
			case ServicesPackage.COMPONENT_DESCRIPTION__FACTORY_PROPERTIES:
				return factoryProperties != null && !factoryProperties.isEmpty();
			case ServicesPackage.COMPONENT_DESCRIPTION__REFERENCES:
				return references != null && !references.isEmpty();
			case ServicesPackage.COMPONENT_DESCRIPTION__LIFECYCLE_HOOKS:
				return lifecycleHooks != null && !lifecycleHooks.isEmpty();
			case ServicesPackage.COMPONENT_DESCRIPTION__PROVIDER:
				return provider != null;
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
		result.append(" (name: ");
		result.append(name);
		result.append(", factory: ");
		result.append(factory);
		result.append(", scope: ");
		result.append(scope);
		result.append(", implementationId: ");
		result.append(implementationId);
		result.append(", defaultEnabled: ");
		result.append(defaultEnabled);
		result.append(", immediate: ");
		result.append(immediate);
		result.append(", configurationPolicy: ");
		result.append(configurationPolicy);
		result.append(", configurationPid: ");
		result.append(configurationPid);
		result.append(')');
		return result.toString();
	}

} //ComponentDescriptionImpl
