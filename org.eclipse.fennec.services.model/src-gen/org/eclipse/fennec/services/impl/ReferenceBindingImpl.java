/*
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.fennec.services.FieldOption;
import org.eclipse.fennec.services.ReferenceBinding;
import org.eclipse.fennec.services.ReferenceBindingKind;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Reference Binding</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ReferenceBindingImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ReferenceBindingImpl#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ReferenceBindingImpl#getFieldOption <em>Field Option</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ReferenceBindingImpl extends MinimalEObjectImpl.Container implements ReferenceBinding {
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
	protected static final ReferenceBindingKind KIND_EDEFAULT = ReferenceBindingKind.BIND;

	/**
	 * The cached value of the '{@link #getKind() <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getKind()
	 * @generated
	 * @ordered
	 */
	protected ReferenceBindingKind kind = KIND_EDEFAULT;

	/**
	 * The default value of the '{@link #getFieldOption() <em>Field Option</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFieldOption()
	 * @generated
	 * @ordered
	 */
	protected static final FieldOption FIELD_OPTION_EDEFAULT = FieldOption.REPLACE;

	/**
	 * The cached value of the '{@link #getFieldOption() <em>Field Option</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFieldOption()
	 * @generated
	 * @ordered
	 */
	protected FieldOption fieldOption = FIELD_OPTION_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ReferenceBindingImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.REFERENCE_BINDING;
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.REFERENCE_BINDING__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ReferenceBindingKind getKind() {
		return kind;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setKind(ReferenceBindingKind newKind) {
		ReferenceBindingKind oldKind = kind;
		kind = newKind == null ? KIND_EDEFAULT : newKind;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.REFERENCE_BINDING__KIND, oldKind, kind));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public FieldOption getFieldOption() {
		return fieldOption;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFieldOption(FieldOption newFieldOption) {
		FieldOption oldFieldOption = fieldOption;
		fieldOption = newFieldOption == null ? FIELD_OPTION_EDEFAULT : newFieldOption;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.REFERENCE_BINDING__FIELD_OPTION, oldFieldOption, fieldOption));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.REFERENCE_BINDING__NAME:
				return getName();
			case ServicesPackage.REFERENCE_BINDING__KIND:
				return getKind();
			case ServicesPackage.REFERENCE_BINDING__FIELD_OPTION:
				return getFieldOption();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case ServicesPackage.REFERENCE_BINDING__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.REFERENCE_BINDING__KIND:
				setKind((ReferenceBindingKind)newValue);
				return;
			case ServicesPackage.REFERENCE_BINDING__FIELD_OPTION:
				setFieldOption((FieldOption)newValue);
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
			case ServicesPackage.REFERENCE_BINDING__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.REFERENCE_BINDING__KIND:
				setKind(KIND_EDEFAULT);
				return;
			case ServicesPackage.REFERENCE_BINDING__FIELD_OPTION:
				setFieldOption(FIELD_OPTION_EDEFAULT);
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
			case ServicesPackage.REFERENCE_BINDING__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.REFERENCE_BINDING__KIND:
				return kind != KIND_EDEFAULT;
			case ServicesPackage.REFERENCE_BINDING__FIELD_OPTION:
				return fieldOption != FIELD_OPTION_EDEFAULT;
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
		result.append(", kind: ");
		result.append(kind);
		result.append(", fieldOption: ");
		result.append(fieldOption);
		result.append(')');
		return result.toString();
	}

} //ReferenceBindingImpl
