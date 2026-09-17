/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Type Script Binding</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * TypeScript rendering: targetPackage is the module path. Carries no language specifics yet — it exists so a template can dispatch on the binding type, and gains attributes when the TypeScript generator does.
 * <!-- end-model-doc -->
 *
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getTypeScriptBinding()
 * @model
 * @generated
 */
@ProviderType
public interface TypeScriptBinding extends LanguageBinding {
} // TypeScriptBinding
