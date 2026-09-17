/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Type Mapping</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * One language-neutral type name and what it is called in the target language — a Parameter.type, or the symbolic ServiceException.type. Positional under its binding, so it does NOT mix in NamedElement. Without a mapping a generator falls back to the last segment of the symbolic name, in the binding's targetPackage, and generates nothing: a contract names types symbolically, and only the binding knows what they are called in a language.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.TypeMapping#getNeutralType <em>Neutral Type</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.TypeMapping#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.TypeMapping#isGenerated <em>Generated</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getTypeMapping()
 * @model
 * @generated
 */
@ProviderType
public interface TypeMapping extends EObject {
	/**
	 * Returns the value of the '<em><b>Neutral Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The value of Parameter.type this entry matches.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Neutral Type</em>' attribute.
	 * @see #setNeutralType(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getTypeMapping_NeutralType()
	 * @model required="true"
	 * @generated
	 */
	String getNeutralType();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.TypeMapping#getNeutralType <em>Neutral Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Neutral Type</em>' attribute.
	 * @see #getNeutralType()
	 * @generated
	 */
	void setNeutralType(String value);

	/**
	 * Returns the value of the '<em><b>Target</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The type name emitted for it, qualified as the target language needs it.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Target</em>' attribute.
	 * @see #setTarget(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getTypeMapping_Target()
	 * @model required="true"
	 * @generated
	 */
	String getTarget();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.TypeMapping#getTarget <em>Target</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' attribute.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(String value);

	/**
	 * Returns the value of the '<em><b>Generated</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Whether the target type is written by the generator or already exists. False (the default) means the type is there — a hand-written class, or one another generator owns, such as the EMF class behind an eType; the generated code only refers to it. True means the contract owns it and the generator writes it, which is how a declared ServiceException becomes a class in the target language: name, version, doc and typed payload all come from the contract, so no hand-written copy can drift from it.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Generated</em>' attribute.
	 * @see #setGenerated(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getTypeMapping_Generated()
	 * @model default="false" required="true"
	 * @generated
	 */
	boolean isGenerated();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.TypeMapping#isGenerated <em>Generated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Generated</em>' attribute.
	 * @see #isGenerated()
	 * @generated
	 */
	void setGenerated(boolean value);

} // TypeMapping
