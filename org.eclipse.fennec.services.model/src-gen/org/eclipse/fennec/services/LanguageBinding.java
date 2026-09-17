/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Language Binding</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * How the contracts of a catalog are rendered in one programming language. A binding is build-time configuration, NOT part of a contract: it REFERENCES the ServiceInterfaces it renders instead of being contained in them, it lives in its own document in the generating project, and it never travels on the wire. That keeps the catalog language-neutral and keeps every binding out of the sd1 fingerprint — two providers of the same contract in different languages still see the same contract. The binding classes live in this metamodel so a toolchain in any language needs nothing but services.ecore.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.LanguageBinding#getServiceInterfaces <em>Service Interfaces</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LanguageBinding#getTargetPackage <em>Target Package</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LanguageBinding#getTypeMappings <em>Type Mappings</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LanguageBinding#getPackageMappings <em>Package Mappings</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getLanguageBinding()
 * @model abstract="true"
 *        annotation="http://www.eclipse.org/emf/2002/Ecore constraints='uniqueTypeMappings uniquePackageMappings'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 uniqueTypeMappings='typeMappings-&gt;isUnique(neutralType)' uniquePackageMappings='packageMappings-&gt;isUnique(nsURI)'"
 * @generated
 */
@ProviderType
public interface LanguageBinding extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Service Interfaces</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceInterface}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The contracts this binding renders. Non-containment: the ServiceInterfaces are owned by the catalog, the binding only points at them — typically as a cross-document href into the published contract document.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Service Interfaces</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getLanguageBinding_ServiceInterfaces()
	 * @model required="true"
	 * @generated
	 */
	EList<ServiceInterface> getServiceInterfaces();

	/**
	 * Returns the value of the '<em><b>Target Package</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Where the generated code lives, in the naming of the target language: a package for Java ('org.acme.payment'), a module path for TypeScript ('@acme/payment'), a module for Python ('acme.payment'). The output DIRECTORY is not part of this — that is the build's business (bnd's 'output' attribute); the generator derives the path inside it from this name.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Target Package</em>' attribute.
	 * @see #setTargetPackage(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getLanguageBinding_TargetPackage()
	 * @model required="true"
	 * @generated
	 */
	String getTargetPackage();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.LanguageBinding#getTargetPackage <em>Target Package</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target Package</em>' attribute.
	 * @see #getTargetPackage()
	 * @generated
	 */
	void setTargetPackage(String value);

	/**
	 * Returns the value of the '<em><b>Type Mappings</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.TypeMapping}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Overrides for Parameter.type, the language-neutral type name. A generator knows the standard set ('string', 'int', 'double', …) on its own; only what it cannot know is stated here, e.g. 'money.Money' to 'com.acme.money.Money'.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Type Mappings</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getLanguageBinding_TypeMappings()
	 * @model containment="true"
	 * @generated
	 */
	EList<TypeMapping> getTypeMappings();

	/**
	 * Returns the value of the '<em><b>Package Mappings</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.PackageMapping}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Where the generated types of a referenced metamodel live in this language. Resolves Parameter.eType: the contract names an EClass by nsURI, and only the target language's own code generator knows the package it ended up in — for Java that is the genmodel's basePackage, which the .ecore does not carry.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Package Mappings</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getLanguageBinding_PackageMappings()
	 * @model containment="true"
	 * @generated
	 */
	EList<PackageMapping> getPackageMappings();

} // LanguageBinding
