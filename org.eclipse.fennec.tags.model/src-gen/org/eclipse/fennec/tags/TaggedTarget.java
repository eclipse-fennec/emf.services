/*
 */
package org.eclipse.fennec.tags;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tagged Target</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * One target with the tags it carries, each with where it comes from, sorted by path.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.TaggedTarget#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TaggedTarget#getTags <em>Tags</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getTaggedTarget()
 * @model
 * @generated
 */
@ProviderType
public interface TaggedTarget extends EObject {
	/**
	 * Returns the value of the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target</em>' containment reference.
	 * @see #setTarget(TagTarget)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTaggedTarget_Target()
	 * @model containment="true" required="true"
	 * @generated
	 */
	TagTarget getTarget();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TaggedTarget#getTarget <em>Target</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' containment reference.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(TagTarget value);

	/**
	 * Returns the value of the '<em><b>Tags</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.tags.EffectiveTag}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tags</em>' containment reference list.
	 * @see org.eclipse.fennec.tags.TagsPackage#getTaggedTarget_Tags()
	 * @model containment="true"
	 * @generated
	 */
	EList<EffectiveTag> getTags();

} // TaggedTarget
