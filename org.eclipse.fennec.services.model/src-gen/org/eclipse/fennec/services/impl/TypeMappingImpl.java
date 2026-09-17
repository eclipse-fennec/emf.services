/*
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.TypeMapping;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Type Mapping</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.TypeMappingImpl#getNeutralType <em>Neutral Type</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.TypeMappingImpl#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.TypeMappingImpl#isGenerated <em>Generated</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TypeMappingImpl extends MinimalEObjectImpl.Container implements TypeMapping {
	/**
	 * The default value of the '{@link #getNeutralType() <em>Neutral Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getNeutralType()
	 * @generated
	 * @ordered
	 */
	protected static final String NEUTRAL_TYPE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getNeutralType() <em>Neutral Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getNeutralType()
	 * @generated
	 * @ordered
	 */
	protected String neutralType = NEUTRAL_TYPE_EDEFAULT;

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
	 * The default value of the '{@link #isGenerated() <em>Generated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isGenerated()
	 * @generated
	 * @ordered
	 */
	protected static final boolean GENERATED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isGenerated() <em>Generated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isGenerated()
	 * @generated
	 * @ordered
	 */
	protected boolean generated = GENERATED_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TypeMappingImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.TYPE_MAPPING;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getNeutralType() {
		return neutralType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setNeutralType(String newNeutralType) {
		String oldNeutralType = neutralType;
		neutralType = newNeutralType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.TYPE_MAPPING__NEUTRAL_TYPE, oldNeutralType, neutralType));
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
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.TYPE_MAPPING__TARGET, oldTarget, target));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isGenerated() {
		return generated;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setGenerated(boolean newGenerated) {
		boolean oldGenerated = generated;
		generated = newGenerated;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.TYPE_MAPPING__GENERATED, oldGenerated, generated));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.TYPE_MAPPING__NEUTRAL_TYPE:
				return getNeutralType();
			case ServicesPackage.TYPE_MAPPING__TARGET:
				return getTarget();
			case ServicesPackage.TYPE_MAPPING__GENERATED:
				return isGenerated();
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
			case ServicesPackage.TYPE_MAPPING__NEUTRAL_TYPE:
				setNeutralType((String)newValue);
				return;
			case ServicesPackage.TYPE_MAPPING__TARGET:
				setTarget((String)newValue);
				return;
			case ServicesPackage.TYPE_MAPPING__GENERATED:
				setGenerated((Boolean)newValue);
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
			case ServicesPackage.TYPE_MAPPING__NEUTRAL_TYPE:
				setNeutralType(NEUTRAL_TYPE_EDEFAULT);
				return;
			case ServicesPackage.TYPE_MAPPING__TARGET:
				setTarget(TARGET_EDEFAULT);
				return;
			case ServicesPackage.TYPE_MAPPING__GENERATED:
				setGenerated(GENERATED_EDEFAULT);
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
			case ServicesPackage.TYPE_MAPPING__NEUTRAL_TYPE:
				return NEUTRAL_TYPE_EDEFAULT == null ? neutralType != null : !NEUTRAL_TYPE_EDEFAULT.equals(neutralType);
			case ServicesPackage.TYPE_MAPPING__TARGET:
				return TARGET_EDEFAULT == null ? target != null : !TARGET_EDEFAULT.equals(target);
			case ServicesPackage.TYPE_MAPPING__GENERATED:
				return generated != GENERATED_EDEFAULT;
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
		result.append(" (neutralType: ");
		result.append(neutralType);
		result.append(", target: ");
		result.append(target);
		result.append(", generated: ");
		result.append(generated);
		result.append(')');
		return result.toString();
	}

} //TypeMappingImpl
