/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Enumeration Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Closed list of permitted stringified values. The implementation compares the parameter's stringified form against allowedValues with case-sensitive equality.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.EnumerationConstraint#getAllowedValues <em>Allowed Values</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getEnumerationConstraint()
 * @model
 * @generated
 */
@ProviderType
public interface EnumerationConstraint extends ParameterConstraint {
	/**
	 * Returns the value of the '<em><b>Allowed Values</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Allowed stringified values. At least one entry required.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Allowed Values</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getEnumerationConstraint_AllowedValues()
	 * @model required="true"
	 * @generated
	 */
	EList<String> getAllowedValues();

} // EnumerationConstraint
