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

import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.Invariant;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.VersionedElement;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Service Interface</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl#getVersion <em>Version</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl#getOperations <em>Operations</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl#getExceptions <em>Exceptions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl#getInvariants <em>Invariants</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl#getStatus <em>Status</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl#getDeprecationReason <em>Deprecation Reason</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl#getReplacedBy <em>Replaced By</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ServiceInterfaceImpl extends MinimalEObjectImpl.Container implements ServiceInterface {
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
	 * The cached value of the '{@link #getOperations() <em>Operations</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOperations()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceOperation> operations;

	/**
	 * The cached value of the '{@link #getExceptions() <em>Exceptions</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExceptions()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceException> exceptions;

	/**
	 * The cached value of the '{@link #getInvariants() <em>Invariants</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInvariants()
	 * @generated
	 * @ordered
	 */
	protected EList<Invariant> invariants;

	/**
	 * The default value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected static final CatalogStatus STATUS_EDEFAULT = CatalogStatus.ACTIVE;

	/**
	 * The cached value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected CatalogStatus status = STATUS_EDEFAULT;

	/**
	 * The default value of the '{@link #getDeprecationReason() <em>Deprecation Reason</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDeprecationReason()
	 * @generated
	 * @ordered
	 */
	protected static final String DEPRECATION_REASON_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDeprecationReason() <em>Deprecation Reason</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDeprecationReason()
	 * @generated
	 * @ordered
	 */
	protected String deprecationReason = DEPRECATION_REASON_EDEFAULT;

	/**
	 * The cached value of the '{@link #getReplacedBy() <em>Replaced By</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReplacedBy()
	 * @generated
	 * @ordered
	 */
	protected ServiceInterface replacedBy;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServiceInterfaceImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.SERVICE_INTERFACE;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INTERFACE__NAME, oldName, name));
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INTERFACE__VERSION, oldVersion, version));
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INTERFACE__DESCRIPTION, oldDescription, description));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceOperation> getOperations() {
		if (operations == null) {
			operations = new EObjectContainmentEList<ServiceOperation>(ServiceOperation.class, this, ServicesPackage.SERVICE_INTERFACE__OPERATIONS);
		}
		return operations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceException> getExceptions() {
		if (exceptions == null) {
			exceptions = new EObjectContainmentEList<ServiceException>(ServiceException.class, this, ServicesPackage.SERVICE_INTERFACE__EXCEPTIONS);
		}
		return exceptions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Invariant> getInvariants() {
		if (invariants == null) {
			invariants = new EObjectContainmentEList<Invariant>(Invariant.class, this, ServicesPackage.SERVICE_INTERFACE__INVARIANTS);
		}
		return invariants;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public CatalogStatus getStatus() {
		return status;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStatus(CatalogStatus newStatus) {
		CatalogStatus oldStatus = status;
		status = newStatus == null ? STATUS_EDEFAULT : newStatus;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INTERFACE__STATUS, oldStatus, status));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getDeprecationReason() {
		return deprecationReason;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDeprecationReason(String newDeprecationReason) {
		String oldDeprecationReason = deprecationReason;
		deprecationReason = newDeprecationReason;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INTERFACE__DEPRECATION_REASON, oldDeprecationReason, deprecationReason));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceInterface getReplacedBy() {
		if (replacedBy != null && replacedBy.eIsProxy()) {
			InternalEObject oldReplacedBy = (InternalEObject)replacedBy;
			replacedBy = (ServiceInterface)eResolveProxy(oldReplacedBy);
			if (replacedBy != oldReplacedBy) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, ServicesPackage.SERVICE_INTERFACE__REPLACED_BY, oldReplacedBy, replacedBy));
			}
		}
		return replacedBy;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServiceInterface basicGetReplacedBy() {
		return replacedBy;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setReplacedBy(ServiceInterface newReplacedBy) {
		ServiceInterface oldReplacedBy = replacedBy;
		replacedBy = newReplacedBy;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_INTERFACE__REPLACED_BY, oldReplacedBy, replacedBy));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.SERVICE_INTERFACE__OPERATIONS:
				return ((InternalEList<?>)getOperations()).basicRemove(otherEnd, msgs);
			case ServicesPackage.SERVICE_INTERFACE__EXCEPTIONS:
				return ((InternalEList<?>)getExceptions()).basicRemove(otherEnd, msgs);
			case ServicesPackage.SERVICE_INTERFACE__INVARIANTS:
				return ((InternalEList<?>)getInvariants()).basicRemove(otherEnd, msgs);
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
			case ServicesPackage.SERVICE_INTERFACE__NAME:
				return getName();
			case ServicesPackage.SERVICE_INTERFACE__VERSION:
				return getVersion();
			case ServicesPackage.SERVICE_INTERFACE__DESCRIPTION:
				return getDescription();
			case ServicesPackage.SERVICE_INTERFACE__OPERATIONS:
				return getOperations();
			case ServicesPackage.SERVICE_INTERFACE__EXCEPTIONS:
				return getExceptions();
			case ServicesPackage.SERVICE_INTERFACE__INVARIANTS:
				return getInvariants();
			case ServicesPackage.SERVICE_INTERFACE__STATUS:
				return getStatus();
			case ServicesPackage.SERVICE_INTERFACE__DEPRECATION_REASON:
				return getDeprecationReason();
			case ServicesPackage.SERVICE_INTERFACE__REPLACED_BY:
				if (resolve) return getReplacedBy();
				return basicGetReplacedBy();
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
			case ServicesPackage.SERVICE_INTERFACE__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.SERVICE_INTERFACE__VERSION:
				setVersion((String)newValue);
				return;
			case ServicesPackage.SERVICE_INTERFACE__DESCRIPTION:
				setDescription((String)newValue);
				return;
			case ServicesPackage.SERVICE_INTERFACE__OPERATIONS:
				getOperations().clear();
				getOperations().addAll((Collection<? extends ServiceOperation>)newValue);
				return;
			case ServicesPackage.SERVICE_INTERFACE__EXCEPTIONS:
				getExceptions().clear();
				getExceptions().addAll((Collection<? extends ServiceException>)newValue);
				return;
			case ServicesPackage.SERVICE_INTERFACE__INVARIANTS:
				getInvariants().clear();
				getInvariants().addAll((Collection<? extends Invariant>)newValue);
				return;
			case ServicesPackage.SERVICE_INTERFACE__STATUS:
				setStatus((CatalogStatus)newValue);
				return;
			case ServicesPackage.SERVICE_INTERFACE__DEPRECATION_REASON:
				setDeprecationReason((String)newValue);
				return;
			case ServicesPackage.SERVICE_INTERFACE__REPLACED_BY:
				setReplacedBy((ServiceInterface)newValue);
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
			case ServicesPackage.SERVICE_INTERFACE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_INTERFACE__VERSION:
				setVersion(VERSION_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_INTERFACE__DESCRIPTION:
				setDescription(DESCRIPTION_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_INTERFACE__OPERATIONS:
				getOperations().clear();
				return;
			case ServicesPackage.SERVICE_INTERFACE__EXCEPTIONS:
				getExceptions().clear();
				return;
			case ServicesPackage.SERVICE_INTERFACE__INVARIANTS:
				getInvariants().clear();
				return;
			case ServicesPackage.SERVICE_INTERFACE__STATUS:
				setStatus(STATUS_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_INTERFACE__DEPRECATION_REASON:
				setDeprecationReason(DEPRECATION_REASON_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_INTERFACE__REPLACED_BY:
				setReplacedBy((ServiceInterface)null);
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
			case ServicesPackage.SERVICE_INTERFACE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.SERVICE_INTERFACE__VERSION:
				return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
			case ServicesPackage.SERVICE_INTERFACE__DESCRIPTION:
				return DESCRIPTION_EDEFAULT == null ? description != null : !DESCRIPTION_EDEFAULT.equals(description);
			case ServicesPackage.SERVICE_INTERFACE__OPERATIONS:
				return operations != null && !operations.isEmpty();
			case ServicesPackage.SERVICE_INTERFACE__EXCEPTIONS:
				return exceptions != null && !exceptions.isEmpty();
			case ServicesPackage.SERVICE_INTERFACE__INVARIANTS:
				return invariants != null && !invariants.isEmpty();
			case ServicesPackage.SERVICE_INTERFACE__STATUS:
				return status != STATUS_EDEFAULT;
			case ServicesPackage.SERVICE_INTERFACE__DEPRECATION_REASON:
				return DEPRECATION_REASON_EDEFAULT == null ? deprecationReason != null : !DEPRECATION_REASON_EDEFAULT.equals(deprecationReason);
			case ServicesPackage.SERVICE_INTERFACE__REPLACED_BY:
				return replacedBy != null;
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
				case ServicesPackage.SERVICE_INTERFACE__VERSION: return ServicesPackage.VERSIONED_ELEMENT__VERSION;
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
				case ServicesPackage.VERSIONED_ELEMENT__VERSION: return ServicesPackage.SERVICE_INTERFACE__VERSION;
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
		result.append(", status: ");
		result.append(status);
		result.append(", deprecationReason: ");
		result.append(deprecationReason);
		result.append(')');
		return result.toString();
	}

} //ServiceInterfaceImpl
