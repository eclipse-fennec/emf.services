/*
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.fennec.services.NumericRangeConstraint;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Numeric Range Constraint</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.NumericRangeConstraintImpl#getMin <em>Min</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.NumericRangeConstraintImpl#getMax <em>Max</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.NumericRangeConstraintImpl#isInclusiveMin <em>Inclusive Min</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.NumericRangeConstraintImpl#isInclusiveMax <em>Inclusive Max</em>}</li>
 * </ul>
 *
 * @generated
 */
public class NumericRangeConstraintImpl extends ParameterConstraintImpl implements NumericRangeConstraint {
	/**
	 * The default value of the '{@link #getMin() <em>Min</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMin()
	 * @generated
	 * @ordered
	 */
	protected static final double MIN_EDEFAULT = 0.0;

	/**
	 * The cached value of the '{@link #getMin() <em>Min</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMin()
	 * @generated
	 * @ordered
	 */
	protected double min = MIN_EDEFAULT;

	/**
	 * The default value of the '{@link #getMax() <em>Max</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMax()
	 * @generated
	 * @ordered
	 */
	protected static final double MAX_EDEFAULT = 0.0;

	/**
	 * The cached value of the '{@link #getMax() <em>Max</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMax()
	 * @generated
	 * @ordered
	 */
	protected double max = MAX_EDEFAULT;

	/**
	 * The default value of the '{@link #isInclusiveMin() <em>Inclusive Min</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isInclusiveMin()
	 * @generated
	 * @ordered
	 */
	protected static final boolean INCLUSIVE_MIN_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isInclusiveMin() <em>Inclusive Min</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isInclusiveMin()
	 * @generated
	 * @ordered
	 */
	protected boolean inclusiveMin = INCLUSIVE_MIN_EDEFAULT;

	/**
	 * The default value of the '{@link #isInclusiveMax() <em>Inclusive Max</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isInclusiveMax()
	 * @generated
	 * @ordered
	 */
	protected static final boolean INCLUSIVE_MAX_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isInclusiveMax() <em>Inclusive Max</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isInclusiveMax()
	 * @generated
	 * @ordered
	 */
	protected boolean inclusiveMax = INCLUSIVE_MAX_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected NumericRangeConstraintImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.NUMERIC_RANGE_CONSTRAINT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public double getMin() {
		return min;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMin(double newMin) {
		double oldMin = min;
		min = newMin;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MIN, oldMin, min));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public double getMax() {
		return max;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMax(double newMax) {
		double oldMax = max;
		max = newMax;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MAX, oldMax, max));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isInclusiveMin() {
		return inclusiveMin;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setInclusiveMin(boolean newInclusiveMin) {
		boolean oldInclusiveMin = inclusiveMin;
		inclusiveMin = newInclusiveMin;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MIN, oldInclusiveMin, inclusiveMin));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isInclusiveMax() {
		return inclusiveMax;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setInclusiveMax(boolean newInclusiveMax) {
		boolean oldInclusiveMax = inclusiveMax;
		inclusiveMax = newInclusiveMax;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MAX, oldInclusiveMax, inclusiveMax));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MIN:
				return getMin();
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MAX:
				return getMax();
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MIN:
				return isInclusiveMin();
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MAX:
				return isInclusiveMax();
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
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MIN:
				setMin((Double)newValue);
				return;
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MAX:
				setMax((Double)newValue);
				return;
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MIN:
				setInclusiveMin((Boolean)newValue);
				return;
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MAX:
				setInclusiveMax((Boolean)newValue);
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
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MIN:
				setMin(MIN_EDEFAULT);
				return;
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MAX:
				setMax(MAX_EDEFAULT);
				return;
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MIN:
				setInclusiveMin(INCLUSIVE_MIN_EDEFAULT);
				return;
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MAX:
				setInclusiveMax(INCLUSIVE_MAX_EDEFAULT);
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
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MIN:
				return min != MIN_EDEFAULT;
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__MAX:
				return max != MAX_EDEFAULT;
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MIN:
				return inclusiveMin != INCLUSIVE_MIN_EDEFAULT;
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MAX:
				return inclusiveMax != INCLUSIVE_MAX_EDEFAULT;
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
		result.append(" (min: ");
		result.append(min);
		result.append(", max: ");
		result.append(max);
		result.append(", inclusiveMin: ");
		result.append(inclusiveMin);
		result.append(", inclusiveMax: ");
		result.append(inclusiveMax);
		result.append(')');
		return result.toString();
	}

} //NumericRangeConstraintImpl
