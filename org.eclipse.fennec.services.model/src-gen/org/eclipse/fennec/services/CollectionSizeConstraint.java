/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Collection Size Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Bounds on collection-typed parameters (lists, arrays, sets). Implementation rejects values whose count is outside [minSize, maxSize].
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.CollectionSizeConstraint#getMinSize <em>Min Size</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.CollectionSizeConstraint#getMaxSize <em>Max Size</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getCollectionSizeConstraint()
 * @model annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 sizeBoundsNonNegative='(minSize = null or minSize &gt;= 0) and (maxSize = null or maxSize &gt;= 0)' sizeBoundsOrdered='minSize = null or maxSize = null or minSize &lt;= maxSize'"
 * @generated
 */
@ProviderType
public interface CollectionSizeConstraint extends ParameterConstraint {
	/**
	 * Returns the value of the '<em><b>Min Size</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Minimum element count, inclusive. Unset = 0.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Min Size</em>' attribute.
	 * @see #setMinSize(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getCollectionSizeConstraint_MinSize()
	 * @model
	 * @generated
	 */
	int getMinSize();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.CollectionSizeConstraint#getMinSize <em>Min Size</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Min Size</em>' attribute.
	 * @see #getMinSize()
	 * @generated
	 */
	void setMinSize(int value);

	/**
	 * Returns the value of the '<em><b>Max Size</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Maximum element count, inclusive. Unset = unbounded.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Max Size</em>' attribute.
	 * @see #setMaxSize(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getCollectionSizeConstraint_MaxSize()
	 * @model
	 * @generated
	 */
	int getMaxSize();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.CollectionSizeConstraint#getMaxSize <em>Max Size</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Size</em>' attribute.
	 * @see #getMaxSize()
	 * @generated
	 */
	void setMaxSize(int value);

} // CollectionSizeConstraint
