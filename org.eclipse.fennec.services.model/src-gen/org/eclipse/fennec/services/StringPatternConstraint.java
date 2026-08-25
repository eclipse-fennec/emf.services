/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>String Pattern Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * String-valued parameter constraint. Pattern uses ECMA-262 regex syntax (natively supported in TS, accepted by Java java.util.regex and Python re with the same minimal subset). Length bounds apply to the unicode code-point length.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.StringPatternConstraint#getPattern <em>Pattern</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.StringPatternConstraint#getMinLength <em>Min Length</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.StringPatternConstraint#getMaxLength <em>Max Length</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getStringPatternConstraint()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='lengthBoundsNonNegative lengthBoundsOrdered'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 lengthBoundsNonNegative='(minLength = null or minLength &gt;= 0) and (maxLength = null or maxLength &gt;= 0)' lengthBoundsOrdered='minLength = null or maxLength = null or minLength &lt;= maxLength'"
 * @generated
 */
@ProviderType
public interface StringPatternConstraint extends ParameterConstraint {
	/**
	 * Returns the value of the '<em><b>Pattern</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ECMA-262 regex. Anchors (^, $) are NOT implicit — to match the entire string, write them explicitly.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Pattern</em>' attribute.
	 * @see #setPattern(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getStringPatternConstraint_Pattern()
	 * @model required="true"
	 * @generated
	 */
	String getPattern();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.StringPatternConstraint#getPattern <em>Pattern</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Pattern</em>' attribute.
	 * @see #getPattern()
	 * @generated
	 */
	void setPattern(String value);

	/**
	 * Returns the value of the '<em><b>Min Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Minimum string length, inclusive. Unset = 0.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Min Length</em>' attribute.
	 * @see #setMinLength(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getStringPatternConstraint_MinLength()
	 * @model
	 * @generated
	 */
	int getMinLength();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.StringPatternConstraint#getMinLength <em>Min Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Min Length</em>' attribute.
	 * @see #getMinLength()
	 * @generated
	 */
	void setMinLength(int value);

	/**
	 * Returns the value of the '<em><b>Max Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Maximum string length, inclusive. Unset = unbounded.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Max Length</em>' attribute.
	 * @see #setMaxLength(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getStringPatternConstraint_MaxLength()
	 * @model
	 * @generated
	 */
	int getMaxLength();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.StringPatternConstraint#getMaxLength <em>Max Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Length</em>' attribute.
	 * @see #getMaxLength()
	 * @generated
	 */
	void setMaxLength(int value);

} // StringPatternConstraint
