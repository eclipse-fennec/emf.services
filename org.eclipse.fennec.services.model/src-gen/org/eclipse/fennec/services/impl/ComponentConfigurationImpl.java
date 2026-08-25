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
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.services.ComponentConfiguration;
import org.eclipse.fennec.services.ComponentDescription;
import org.eclipse.fennec.services.ComponentState;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.SatisfiedReference;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.UnsatisfiedReference;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Component Configuration</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl#getId <em>Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl#getState <em>State</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl#getSatisfiedReferences <em>Satisfied References</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl#getUnsatisfiedReferences <em>Unsatisfied References</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl#getFailure <em>Failure</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl#getService <em>Service</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ComponentConfigurationImpl extends MinimalEObjectImpl.Container implements ComponentConfiguration {
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
	 * The default value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected static final String ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected String id = ID_EDEFAULT;

	/**
	 * The cached value of the '{@link #getDescription() <em>Description</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected ComponentDescription description;

	/**
	 * The default value of the '{@link #getState() <em>State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getState()
	 * @generated
	 * @ordered
	 */
	protected static final ComponentState STATE_EDEFAULT = ComponentState.UNSATISFIED_CONFIGURATION;

	/**
	 * The cached value of the '{@link #getState() <em>State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getState()
	 * @generated
	 * @ordered
	 */
	protected ComponentState state = STATE_EDEFAULT;

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
	 * The cached value of the '{@link #getSatisfiedReferences() <em>Satisfied References</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSatisfiedReferences()
	 * @generated
	 * @ordered
	 */
	protected EList<SatisfiedReference> satisfiedReferences;

	/**
	 * The cached value of the '{@link #getUnsatisfiedReferences() <em>Unsatisfied References</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUnsatisfiedReferences()
	 * @generated
	 * @ordered
	 */
	protected EList<UnsatisfiedReference> unsatisfiedReferences;

	/**
	 * The cached value of the '{@link #getFailure() <em>Failure</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFailure()
	 * @generated
	 * @ordered
	 */
	protected Diagnostic failure;

	/**
	 * The cached value of the '{@link #getService() <em>Service</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getService()
	 * @generated
	 * @ordered
	 */
	protected ServiceReference service;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ComponentConfigurationImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.COMPONENT_CONFIGURATION;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_CONFIGURATION__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setId(String newId) {
		String oldId = id;
		id = newId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_CONFIGURATION__ID, oldId, id));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ComponentDescription getDescription() {
		if (description != null && description.eIsProxy()) {
			InternalEObject oldDescription = (InternalEObject)description;
			description = (ComponentDescription)eResolveProxy(oldDescription);
			if (description != oldDescription) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.COMPONENT_CONFIGURATION__DESCRIPTION, oldDescription, description));
			}
		}
		return description;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComponentDescription basicGetDescription() {
		return description;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDescription(ComponentDescription newDescription) {
		ComponentDescription oldDescription = description;
		description = newDescription;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_CONFIGURATION__DESCRIPTION, oldDescription, description));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ComponentState getState() {
		return state;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setState(ComponentState newState) {
		ComponentState oldState = state;
		state = newState == null ? STATE_EDEFAULT : newState;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_CONFIGURATION__STATE, oldState, state));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Property> getProperties() {
		if (properties == null) {
			properties = new EObjectContainmentEList<Property>(Property.class, this, ServicesPackage.COMPONENT_CONFIGURATION__PROPERTIES);
		}
		return properties;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<SatisfiedReference> getSatisfiedReferences() {
		if (satisfiedReferences == null) {
			satisfiedReferences = new EObjectContainmentEList<SatisfiedReference>(SatisfiedReference.class, this, ServicesPackage.COMPONENT_CONFIGURATION__SATISFIED_REFERENCES);
		}
		return satisfiedReferences;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<UnsatisfiedReference> getUnsatisfiedReferences() {
		if (unsatisfiedReferences == null) {
			unsatisfiedReferences = new EObjectContainmentEList<UnsatisfiedReference>(UnsatisfiedReference.class, this, ServicesPackage.COMPONENT_CONFIGURATION__UNSATISFIED_REFERENCES);
		}
		return unsatisfiedReferences;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Diagnostic getFailure() {
		return failure;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetFailure(Diagnostic newFailure, NotificationChain msgs) {
		Diagnostic oldFailure = failure;
		failure = newFailure;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_CONFIGURATION__FAILURE, oldFailure, newFailure);
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
	public void setFailure(Diagnostic newFailure) {
		if (newFailure != failure) {
			NotificationChain msgs = null;
			if (failure != null)
				msgs = ((InternalEObject)failure).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ServicesPackage.COMPONENT_CONFIGURATION__FAILURE, null, msgs);
			if (newFailure != null)
				msgs = ((InternalEObject)newFailure).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ServicesPackage.COMPONENT_CONFIGURATION__FAILURE, null, msgs);
			msgs = basicSetFailure(newFailure, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_CONFIGURATION__FAILURE, newFailure, newFailure));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceReference getService() {
		if (service != null && service.eIsProxy()) {
			InternalEObject oldService = (InternalEObject)service;
			service = (ServiceReference)eResolveProxy(oldService);
			if (service != oldService) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.COMPONENT_CONFIGURATION__SERVICE, oldService, service));
			}
		}
		return service;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServiceReference basicGetService() {
		return service;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setService(ServiceReference newService) {
		ServiceReference oldService = service;
		service = newService;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_CONFIGURATION__SERVICE, oldService, service));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.COMPONENT_CONFIGURATION__PROPERTIES:
				return ((InternalEList<?>)getProperties()).basicRemove(otherEnd, msgs);
			case ServicesPackage.COMPONENT_CONFIGURATION__SATISFIED_REFERENCES:
				return ((InternalEList<?>)getSatisfiedReferences()).basicRemove(otherEnd, msgs);
			case ServicesPackage.COMPONENT_CONFIGURATION__UNSATISFIED_REFERENCES:
				return ((InternalEList<?>)getUnsatisfiedReferences()).basicRemove(otherEnd, msgs);
			case ServicesPackage.COMPONENT_CONFIGURATION__FAILURE:
				return basicSetFailure(null, msgs);
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
			case ServicesPackage.COMPONENT_CONFIGURATION__NAME:
				return getName();
			case ServicesPackage.COMPONENT_CONFIGURATION__ID:
				return getId();
			case ServicesPackage.COMPONENT_CONFIGURATION__DESCRIPTION:
				if (resolve) return getDescription();
				return basicGetDescription();
			case ServicesPackage.COMPONENT_CONFIGURATION__STATE:
				return getState();
			case ServicesPackage.COMPONENT_CONFIGURATION__PROPERTIES:
				return getProperties();
			case ServicesPackage.COMPONENT_CONFIGURATION__SATISFIED_REFERENCES:
				return getSatisfiedReferences();
			case ServicesPackage.COMPONENT_CONFIGURATION__UNSATISFIED_REFERENCES:
				return getUnsatisfiedReferences();
			case ServicesPackage.COMPONENT_CONFIGURATION__FAILURE:
				return getFailure();
			case ServicesPackage.COMPONENT_CONFIGURATION__SERVICE:
				if (resolve) return getService();
				return basicGetService();
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
			case ServicesPackage.COMPONENT_CONFIGURATION__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__ID:
				setId((String)newValue);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__DESCRIPTION:
				setDescription((ComponentDescription)newValue);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__STATE:
				setState((ComponentState)newValue);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__PROPERTIES:
				getProperties().clear();
				getProperties().addAll((Collection<? extends Property>)newValue);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__SATISFIED_REFERENCES:
				getSatisfiedReferences().clear();
				getSatisfiedReferences().addAll((Collection<? extends SatisfiedReference>)newValue);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__UNSATISFIED_REFERENCES:
				getUnsatisfiedReferences().clear();
				getUnsatisfiedReferences().addAll((Collection<? extends UnsatisfiedReference>)newValue);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__FAILURE:
				setFailure((Diagnostic)newValue);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__SERVICE:
				setService((ServiceReference)newValue);
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
			case ServicesPackage.COMPONENT_CONFIGURATION__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__ID:
				setId(ID_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__DESCRIPTION:
				setDescription((ComponentDescription)null);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__STATE:
				setState(STATE_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__PROPERTIES:
				getProperties().clear();
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__SATISFIED_REFERENCES:
				getSatisfiedReferences().clear();
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__UNSATISFIED_REFERENCES:
				getUnsatisfiedReferences().clear();
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__FAILURE:
				setFailure((Diagnostic)null);
				return;
			case ServicesPackage.COMPONENT_CONFIGURATION__SERVICE:
				setService((ServiceReference)null);
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
			case ServicesPackage.COMPONENT_CONFIGURATION__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.COMPONENT_CONFIGURATION__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case ServicesPackage.COMPONENT_CONFIGURATION__DESCRIPTION:
				return description != null;
			case ServicesPackage.COMPONENT_CONFIGURATION__STATE:
				return state != STATE_EDEFAULT;
			case ServicesPackage.COMPONENT_CONFIGURATION__PROPERTIES:
				return properties != null && !properties.isEmpty();
			case ServicesPackage.COMPONENT_CONFIGURATION__SATISFIED_REFERENCES:
				return satisfiedReferences != null && !satisfiedReferences.isEmpty();
			case ServicesPackage.COMPONENT_CONFIGURATION__UNSATISFIED_REFERENCES:
				return unsatisfiedReferences != null && !unsatisfiedReferences.isEmpty();
			case ServicesPackage.COMPONENT_CONFIGURATION__FAILURE:
				return failure != null;
			case ServicesPackage.COMPONENT_CONFIGURATION__SERVICE:
				return service != null;
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
		result.append(", id: ");
		result.append(id);
		result.append(", state: ");
		result.append(state);
		result.append(')');
		return result.toString();
	}

} //ComponentConfigurationImpl
