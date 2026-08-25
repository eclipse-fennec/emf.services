/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Reference Binding</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Language-neutral handle on a reference callback or field-injection point. Replaces OSGi DS bind/unbind/updated/field annotations. The framework invokes the implementation method (or sets the field) whose name matches 'name' when the corresponding ComponentReference changes.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ReferenceBinding#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ReferenceBinding#getFieldOption <em>Field Option</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getReferenceBinding()
 * @model
 * @generated
 */
@ProviderType
public interface ReferenceBinding extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Kind</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ReferenceBindingKind}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Which reference event triggers this binding.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.ReferenceBindingKind
	 * @see #setKind(ReferenceBindingKind)
	 * @see org.eclipse.fennec.services.ServicesPackage#getReferenceBinding_Kind()
	 * @model required="true"
	 * @generated
	 */
	ReferenceBindingKind getKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ReferenceBinding#getKind <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.ReferenceBindingKind
	 * @see #getKind()
	 * @generated
	 */
	void setKind(ReferenceBindingKind value);

	/**
	 * Returns the value of the '<em><b>Field Option</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.FieldOption}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Only meaningful when kind = FIELD: how a collection field is mutated when the reference set changes.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Field Option</em>' attribute.
	 * @see org.eclipse.fennec.services.FieldOption
	 * @see #setFieldOption(FieldOption)
	 * @see org.eclipse.fennec.services.ServicesPackage#getReferenceBinding_FieldOption()
	 * @model
	 * @generated
	 */
	FieldOption getFieldOption();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ReferenceBinding#getFieldOption <em>Field Option</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Field Option</em>' attribute.
	 * @see org.eclipse.fennec.services.FieldOption
	 * @see #getFieldOption()
	 * @generated
	 */
	void setFieldOption(FieldOption value);

} // ReferenceBinding
