/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Required Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Marker constraint: parameter MUST NOT be null/None/undefined. Redundant with Parameter.optional = false but explicit, useful for return-value constraints where Parameter.optional does not apply.
 * <!-- end-model-doc -->
 *
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getRequiredConstraint()
 * @model
 * @generated
 */
@ProviderType
public interface RequiredConstraint extends ParameterConstraint {
} // RequiredConstraint
