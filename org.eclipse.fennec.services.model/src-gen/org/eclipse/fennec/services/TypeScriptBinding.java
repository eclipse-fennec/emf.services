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
 * TypeScript rendering: targetPackage is the npm name of the generated package. It has no attributes of its own and needs none — targetPackage, fileHeader, typeMappings and packageMappings from LanguageBinding are all the TypeScript generator reads. The class exists so that a binding document says which language it is for.
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
