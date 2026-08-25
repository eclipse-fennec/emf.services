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

import org.eclipse.fennec.services.CollectionType;
import org.eclipse.fennec.services.ComponentReference;
import org.eclipse.fennec.services.ReferenceBinding;
import org.eclipse.fennec.services.ReferenceCardinality;
import org.eclipse.fennec.services.ReferencePolicy;
import org.eclipse.fennec.services.ReferencePolicyOption;
import org.eclipse.fennec.services.ServiceScope;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Component Reference</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getInterfaceName <em>Interface Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getCardinality <em>Cardinality</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getPolicy <em>Policy</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getPolicyOption <em>Policy Option</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getScope <em>Scope</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getCollectionType <em>Collection Type</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getParameter <em>Parameter</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl#getBindings <em>Bindings</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ComponentReferenceImpl extends MinimalEObjectImpl.Container implements ComponentReference {
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
	 * The default value of the '{@link #getInterfaceName() <em>Interface Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInterfaceName()
	 * @generated
	 * @ordered
	 */
	protected static final String INTERFACE_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getInterfaceName() <em>Interface Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInterfaceName()
	 * @generated
	 * @ordered
	 */
	protected String interfaceName = INTERFACE_NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getCardinality() <em>Cardinality</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCardinality()
	 * @generated
	 * @ordered
	 */
	protected static final ReferenceCardinality CARDINALITY_EDEFAULT = ReferenceCardinality.ONE;

	/**
	 * The cached value of the '{@link #getCardinality() <em>Cardinality</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCardinality()
	 * @generated
	 * @ordered
	 */
	protected ReferenceCardinality cardinality = CARDINALITY_EDEFAULT;

	/**
	 * The default value of the '{@link #getPolicy() <em>Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPolicy()
	 * @generated
	 * @ordered
	 */
	protected static final ReferencePolicy POLICY_EDEFAULT = ReferencePolicy.STATIC;

	/**
	 * The cached value of the '{@link #getPolicy() <em>Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPolicy()
	 * @generated
	 * @ordered
	 */
	protected ReferencePolicy policy = POLICY_EDEFAULT;

	/**
	 * The default value of the '{@link #getPolicyOption() <em>Policy Option</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPolicyOption()
	 * @generated
	 * @ordered
	 */
	protected static final ReferencePolicyOption POLICY_OPTION_EDEFAULT = ReferencePolicyOption.RELUCTANT;

	/**
	 * The cached value of the '{@link #getPolicyOption() <em>Policy Option</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPolicyOption()
	 * @generated
	 * @ordered
	 */
	protected ReferencePolicyOption policyOption = POLICY_OPTION_EDEFAULT;

	/**
	 * The default value of the '{@link #getTarget() <em>Target</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTarget()
	 * @generated
	 * @ordered
	 */
	protected static final String TARGET_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTarget() <em>Target</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTarget()
	 * @generated
	 * @ordered
	 */
	protected String target = TARGET_EDEFAULT;

	/**
	 * The default value of the '{@link #getScope() <em>Scope</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getScope()
	 * @generated
	 * @ordered
	 */
	protected static final ServiceScope SCOPE_EDEFAULT = ServiceScope.BUNDLE;

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
	 * The default value of the '{@link #getCollectionType() <em>Collection Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCollectionType()
	 * @generated
	 * @ordered
	 */
	protected static final CollectionType COLLECTION_TYPE_EDEFAULT = CollectionType.SERVICE;

	/**
	 * The cached value of the '{@link #getCollectionType() <em>Collection Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCollectionType()
	 * @generated
	 * @ordered
	 */
	protected CollectionType collectionType = COLLECTION_TYPE_EDEFAULT;

	/**
	 * The default value of the '{@link #getParameter() <em>Parameter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getParameter()
	 * @generated
	 * @ordered
	 */
	protected static final int PARAMETER_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getParameter() <em>Parameter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getParameter()
	 * @generated
	 * @ordered
	 */
	protected int parameter = PARAMETER_EDEFAULT;

	/**
	 * The cached value of the '{@link #getBindings() <em>Bindings</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBindings()
	 * @generated
	 * @ordered
	 */
	protected EList<ReferenceBinding> bindings;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ComponentReferenceImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.COMPONENT_REFERENCE;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_REFERENCE__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getInterfaceName() {
		return interfaceName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setInterfaceName(String newInterfaceName) {
		String oldInterfaceName = interfaceName;
		interfaceName = newInterfaceName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_REFERENCE__INTERFACE_NAME, oldInterfaceName, interfaceName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ReferenceCardinality getCardinality() {
		return cardinality;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCardinality(ReferenceCardinality newCardinality) {
		ReferenceCardinality oldCardinality = cardinality;
		cardinality = newCardinality == null ? CARDINALITY_EDEFAULT : newCardinality;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_REFERENCE__CARDINALITY, oldCardinality, cardinality));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ReferencePolicy getPolicy() {
		return policy;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPolicy(ReferencePolicy newPolicy) {
		ReferencePolicy oldPolicy = policy;
		policy = newPolicy == null ? POLICY_EDEFAULT : newPolicy;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_REFERENCE__POLICY, oldPolicy, policy));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ReferencePolicyOption getPolicyOption() {
		return policyOption;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPolicyOption(ReferencePolicyOption newPolicyOption) {
		ReferencePolicyOption oldPolicyOption = policyOption;
		policyOption = newPolicyOption == null ? POLICY_OPTION_EDEFAULT : newPolicyOption;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_REFERENCE__POLICY_OPTION, oldPolicyOption, policyOption));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTarget() {
		return target;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTarget(String newTarget) {
		String oldTarget = target;
		target = newTarget;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_REFERENCE__TARGET, oldTarget, target));
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_REFERENCE__SCOPE, oldScope, scope));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public CollectionType getCollectionType() {
		return collectionType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCollectionType(CollectionType newCollectionType) {
		CollectionType oldCollectionType = collectionType;
		collectionType = newCollectionType == null ? COLLECTION_TYPE_EDEFAULT : newCollectionType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_REFERENCE__COLLECTION_TYPE, oldCollectionType, collectionType));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getParameter() {
		return parameter;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setParameter(int newParameter) {
		int oldParameter = parameter;
		parameter = newParameter;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.COMPONENT_REFERENCE__PARAMETER, oldParameter, parameter));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ReferenceBinding> getBindings() {
		if (bindings == null) {
			bindings = new EObjectContainmentEList<ReferenceBinding>(ReferenceBinding.class, this, ServicesPackage.COMPONENT_REFERENCE__BINDINGS);
		}
		return bindings;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.COMPONENT_REFERENCE__BINDINGS:
				return ((InternalEList<?>)getBindings()).basicRemove(otherEnd, msgs);
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
			case ServicesPackage.COMPONENT_REFERENCE__NAME:
				return getName();
			case ServicesPackage.COMPONENT_REFERENCE__INTERFACE_NAME:
				return getInterfaceName();
			case ServicesPackage.COMPONENT_REFERENCE__CARDINALITY:
				return getCardinality();
			case ServicesPackage.COMPONENT_REFERENCE__POLICY:
				return getPolicy();
			case ServicesPackage.COMPONENT_REFERENCE__POLICY_OPTION:
				return getPolicyOption();
			case ServicesPackage.COMPONENT_REFERENCE__TARGET:
				return getTarget();
			case ServicesPackage.COMPONENT_REFERENCE__SCOPE:
				return getScope();
			case ServicesPackage.COMPONENT_REFERENCE__COLLECTION_TYPE:
				return getCollectionType();
			case ServicesPackage.COMPONENT_REFERENCE__PARAMETER:
				return getParameter();
			case ServicesPackage.COMPONENT_REFERENCE__BINDINGS:
				return getBindings();
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
			case ServicesPackage.COMPONENT_REFERENCE__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__INTERFACE_NAME:
				setInterfaceName((String)newValue);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__CARDINALITY:
				setCardinality((ReferenceCardinality)newValue);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__POLICY:
				setPolicy((ReferencePolicy)newValue);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__POLICY_OPTION:
				setPolicyOption((ReferencePolicyOption)newValue);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__TARGET:
				setTarget((String)newValue);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__SCOPE:
				setScope((ServiceScope)newValue);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__COLLECTION_TYPE:
				setCollectionType((CollectionType)newValue);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__PARAMETER:
				setParameter((Integer)newValue);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__BINDINGS:
				getBindings().clear();
				getBindings().addAll((Collection<? extends ReferenceBinding>)newValue);
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
			case ServicesPackage.COMPONENT_REFERENCE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__INTERFACE_NAME:
				setInterfaceName(INTERFACE_NAME_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__CARDINALITY:
				setCardinality(CARDINALITY_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__POLICY:
				setPolicy(POLICY_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__POLICY_OPTION:
				setPolicyOption(POLICY_OPTION_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__TARGET:
				setTarget(TARGET_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__SCOPE:
				setScope(SCOPE_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__COLLECTION_TYPE:
				setCollectionType(COLLECTION_TYPE_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__PARAMETER:
				setParameter(PARAMETER_EDEFAULT);
				return;
			case ServicesPackage.COMPONENT_REFERENCE__BINDINGS:
				getBindings().clear();
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
			case ServicesPackage.COMPONENT_REFERENCE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.COMPONENT_REFERENCE__INTERFACE_NAME:
				return INTERFACE_NAME_EDEFAULT == null ? interfaceName != null : !INTERFACE_NAME_EDEFAULT.equals(interfaceName);
			case ServicesPackage.COMPONENT_REFERENCE__CARDINALITY:
				return cardinality != CARDINALITY_EDEFAULT;
			case ServicesPackage.COMPONENT_REFERENCE__POLICY:
				return policy != POLICY_EDEFAULT;
			case ServicesPackage.COMPONENT_REFERENCE__POLICY_OPTION:
				return policyOption != POLICY_OPTION_EDEFAULT;
			case ServicesPackage.COMPONENT_REFERENCE__TARGET:
				return TARGET_EDEFAULT == null ? target != null : !TARGET_EDEFAULT.equals(target);
			case ServicesPackage.COMPONENT_REFERENCE__SCOPE:
				return scope != SCOPE_EDEFAULT;
			case ServicesPackage.COMPONENT_REFERENCE__COLLECTION_TYPE:
				return collectionType != COLLECTION_TYPE_EDEFAULT;
			case ServicesPackage.COMPONENT_REFERENCE__PARAMETER:
				return parameter != PARAMETER_EDEFAULT;
			case ServicesPackage.COMPONENT_REFERENCE__BINDINGS:
				return bindings != null && !bindings.isEmpty();
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
		result.append(", interfaceName: ");
		result.append(interfaceName);
		result.append(", cardinality: ");
		result.append(cardinality);
		result.append(", policy: ");
		result.append(policy);
		result.append(", policyOption: ");
		result.append(policyOption);
		result.append(", target: ");
		result.append(target);
		result.append(", scope: ");
		result.append(scope);
		result.append(", collectionType: ");
		result.append(collectionType);
		result.append(", parameter: ");
		result.append(parameter);
		result.append(')');
		return result.toString();
	}

} //ComponentReferenceImpl
