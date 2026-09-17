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

import org.eclipse.fennec.services.LanguageBinding;
import org.eclipse.fennec.services.PackageMapping;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.TypeMapping;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Language Binding</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.LanguageBindingImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LanguageBindingImpl#getServiceInterfaces <em>Service Interfaces</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LanguageBindingImpl#getTargetPackage <em>Target Package</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LanguageBindingImpl#getTypeMappings <em>Type Mappings</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.LanguageBindingImpl#getPackageMappings <em>Package Mappings</em>}</li>
 * </ul>
 *
 * @generated
 */
public abstract class LanguageBindingImpl extends MinimalEObjectImpl.Container implements LanguageBinding {
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
	 * The cached value of the '{@link #getServiceInterfaces() <em>Service Interfaces</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getServiceInterfaces()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceInterface> serviceInterfaces;

	/**
	 * The default value of the '{@link #getTargetPackage() <em>Target Package</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetPackage()
	 * @generated
	 * @ordered
	 */
	protected static final String TARGET_PACKAGE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTargetPackage() <em>Target Package</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetPackage()
	 * @generated
	 * @ordered
	 */
	protected String targetPackage = TARGET_PACKAGE_EDEFAULT;

	/**
	 * The cached value of the '{@link #getTypeMappings() <em>Type Mappings</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTypeMappings()
	 * @generated
	 * @ordered
	 */
	protected EList<TypeMapping> typeMappings;

	/**
	 * The cached value of the '{@link #getPackageMappings() <em>Package Mappings</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPackageMappings()
	 * @generated
	 * @ordered
	 */
	protected EList<PackageMapping> packageMappings;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected LanguageBindingImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.LANGUAGE_BINDING;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.LANGUAGE_BINDING__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceInterface> getServiceInterfaces() {
		if (serviceInterfaces == null) {
			serviceInterfaces = new EObjectResolvingEList<ServiceInterface>(ServiceInterface.class, this, ServicesPackage.LANGUAGE_BINDING__SERVICE_INTERFACES);
		}
		return serviceInterfaces;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTargetPackage() {
		return targetPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTargetPackage(String newTargetPackage) {
		String oldTargetPackage = targetPackage;
		targetPackage = newTargetPackage;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.LANGUAGE_BINDING__TARGET_PACKAGE, oldTargetPackage, targetPackage));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<TypeMapping> getTypeMappings() {
		if (typeMappings == null) {
			typeMappings = new EObjectContainmentEList<TypeMapping>(TypeMapping.class, this, ServicesPackage.LANGUAGE_BINDING__TYPE_MAPPINGS);
		}
		return typeMappings;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<PackageMapping> getPackageMappings() {
		if (packageMappings == null) {
			packageMappings = new EObjectContainmentEList<PackageMapping>(PackageMapping.class, this, ServicesPackage.LANGUAGE_BINDING__PACKAGE_MAPPINGS);
		}
		return packageMappings;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.LANGUAGE_BINDING__TYPE_MAPPINGS:
				return ((InternalEList<?>)getTypeMappings()).basicRemove(otherEnd, msgs);
			case ServicesPackage.LANGUAGE_BINDING__PACKAGE_MAPPINGS:
				return ((InternalEList<?>)getPackageMappings()).basicRemove(otherEnd, msgs);
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
			case ServicesPackage.LANGUAGE_BINDING__NAME:
				return getName();
			case ServicesPackage.LANGUAGE_BINDING__SERVICE_INTERFACES:
				return getServiceInterfaces();
			case ServicesPackage.LANGUAGE_BINDING__TARGET_PACKAGE:
				return getTargetPackage();
			case ServicesPackage.LANGUAGE_BINDING__TYPE_MAPPINGS:
				return getTypeMappings();
			case ServicesPackage.LANGUAGE_BINDING__PACKAGE_MAPPINGS:
				return getPackageMappings();
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
			case ServicesPackage.LANGUAGE_BINDING__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.LANGUAGE_BINDING__SERVICE_INTERFACES:
				getServiceInterfaces().clear();
				getServiceInterfaces().addAll((Collection<? extends ServiceInterface>)newValue);
				return;
			case ServicesPackage.LANGUAGE_BINDING__TARGET_PACKAGE:
				setTargetPackage((String)newValue);
				return;
			case ServicesPackage.LANGUAGE_BINDING__TYPE_MAPPINGS:
				getTypeMappings().clear();
				getTypeMappings().addAll((Collection<? extends TypeMapping>)newValue);
				return;
			case ServicesPackage.LANGUAGE_BINDING__PACKAGE_MAPPINGS:
				getPackageMappings().clear();
				getPackageMappings().addAll((Collection<? extends PackageMapping>)newValue);
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
			case ServicesPackage.LANGUAGE_BINDING__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.LANGUAGE_BINDING__SERVICE_INTERFACES:
				getServiceInterfaces().clear();
				return;
			case ServicesPackage.LANGUAGE_BINDING__TARGET_PACKAGE:
				setTargetPackage(TARGET_PACKAGE_EDEFAULT);
				return;
			case ServicesPackage.LANGUAGE_BINDING__TYPE_MAPPINGS:
				getTypeMappings().clear();
				return;
			case ServicesPackage.LANGUAGE_BINDING__PACKAGE_MAPPINGS:
				getPackageMappings().clear();
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
			case ServicesPackage.LANGUAGE_BINDING__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.LANGUAGE_BINDING__SERVICE_INTERFACES:
				return serviceInterfaces != null && !serviceInterfaces.isEmpty();
			case ServicesPackage.LANGUAGE_BINDING__TARGET_PACKAGE:
				return TARGET_PACKAGE_EDEFAULT == null ? targetPackage != null : !TARGET_PACKAGE_EDEFAULT.equals(targetPackage);
			case ServicesPackage.LANGUAGE_BINDING__TYPE_MAPPINGS:
				return typeMappings != null && !typeMappings.isEmpty();
			case ServicesPackage.LANGUAGE_BINDING__PACKAGE_MAPPINGS:
				return packageMappings != null && !packageMappings.isEmpty();
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
		result.append(", targetPackage: ");
		result.append(targetPackage);
		result.append(')');
		return result.toString();
	}

} //LanguageBindingImpl
