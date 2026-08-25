/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Parameter Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Abstract base for validity constraints on Parameter values or ServiceOperation return values. Constraints are positional under their parameter (not separately named), so they do NOT mix in NamedElement.
 * <!-- end-model-doc -->
 *
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getParameterConstraint()
 * @model abstract="true"
 * @generated
 */
@ProviderType
public interface ParameterConstraint extends EObject {
} // ParameterConstraint
