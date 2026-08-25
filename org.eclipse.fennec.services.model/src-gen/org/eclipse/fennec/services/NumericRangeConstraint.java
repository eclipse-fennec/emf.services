/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Numeric Range Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Numeric range. EDouble is used for all numeric types — the implementation casts back into the parameter's actual numeric type for comparison. Unset min = no lower bound; unset max = no upper bound. At least one of min/max should be set (OCL invariant TODO).
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.NumericRangeConstraint#getMin <em>Min</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.NumericRangeConstraint#getMax <em>Max</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.NumericRangeConstraint#isInclusiveMin <em>Inclusive Min</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.NumericRangeConstraint#isInclusiveMax <em>Inclusive Max</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getNumericRangeConstraint()
 * @model annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 atLeastOneBound='min &lt;&gt; null or max &lt;&gt; null' rangeOrdered='min = null or max = null or min &lt;= max'"
 * @generated
 */
@ProviderType
public interface NumericRangeConstraint extends ParameterConstraint {
	/**
	 * Returns the value of the '<em><b>Min</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Lower bound. Unset = -∞.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Min</em>' attribute.
	 * @see #setMin(double)
	 * @see org.eclipse.fennec.services.ServicesPackage#getNumericRangeConstraint_Min()
	 * @model
	 * @generated
	 */
	double getMin();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.NumericRangeConstraint#getMin <em>Min</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Min</em>' attribute.
	 * @see #getMin()
	 * @generated
	 */
	void setMin(double value);

	/**
	 * Returns the value of the '<em><b>Max</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Upper bound. Unset = +∞.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Max</em>' attribute.
	 * @see #setMax(double)
	 * @see org.eclipse.fennec.services.ServicesPackage#getNumericRangeConstraint_Max()
	 * @model
	 * @generated
	 */
	double getMax();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.NumericRangeConstraint#getMax <em>Max</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max</em>' attribute.
	 * @see #getMax()
	 * @generated
	 */
	void setMax(double value);

	/**
	 * Returns the value of the '<em><b>Inclusive Min</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * true = value >= min allowed (default); false = strict greater-than.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Inclusive Min</em>' attribute.
	 * @see #setInclusiveMin(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getNumericRangeConstraint_InclusiveMin()
	 * @model default="true" required="true"
	 * @generated
	 */
	boolean isInclusiveMin();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.NumericRangeConstraint#isInclusiveMin <em>Inclusive Min</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Inclusive Min</em>' attribute.
	 * @see #isInclusiveMin()
	 * @generated
	 */
	void setInclusiveMin(boolean value);

	/**
	 * Returns the value of the '<em><b>Inclusive Max</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * true = value <= max allowed (default); false = strict less-than.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Inclusive Max</em>' attribute.
	 * @see #setInclusiveMax(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getNumericRangeConstraint_InclusiveMax()
	 * @model default="true" required="true"
	 * @generated
	 */
	boolean isInclusiveMax();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.NumericRangeConstraint#isInclusiveMax <em>Inclusive Max</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Inclusive Max</em>' attribute.
	 * @see #isInclusiveMax()
	 * @generated
	 */
	void setInclusiveMax(boolean value);

} // NumericRangeConstraint
