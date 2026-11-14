/*
 */
package org.eclipse.fennec.tags;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Effective Tag</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * One tag a target carries, as a query reports it: the current path and where it comes from. A target may carry the same tag for several reasons; each is reported.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.EffectiveTag#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.EffectiveTag#getOrigin <em>Origin</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getEffectiveTag()
 * @model
 * @generated
 */
@ProviderType
public interface EffectiveTag extends EObject {
	/**
	 * Returns the value of the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Path</em>' attribute.
	 * @see #setPath(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getEffectiveTag_Path()
	 * @model required="true"
	 * @generated
	 */
	String getPath();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.EffectiveTag#getPath <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Path</em>' attribute.
	 * @see #getPath()
	 * @generated
	 */
	void setPath(String value);

	/**
	 * Returns the value of the '<em><b>Origin</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.tags.TagOrigin}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Origin</em>' attribute.
	 * @see org.eclipse.fennec.tags.TagOrigin
	 * @see #setOrigin(TagOrigin)
	 * @see org.eclipse.fennec.tags.TagsPackage#getEffectiveTag_Origin()
	 * @model required="true"
	 * @generated
	 */
	TagOrigin getOrigin();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.EffectiveTag#getOrigin <em>Origin</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Origin</em>' attribute.
	 * @see org.eclipse.fennec.tags.TagOrigin
	 * @see #getOrigin()
	 * @generated
	 */
	void setOrigin(TagOrigin value);

} // EffectiveTag
