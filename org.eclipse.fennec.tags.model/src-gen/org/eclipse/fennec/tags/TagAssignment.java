/*
 */
package org.eclipse.fennec.tags;

import java.util.Date;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tag Assignment</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * An administrator put a tag on a target. References the tag object, so it follows a rename.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.TagAssignment#getTag <em>Tag</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagAssignment#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagAssignment#getAssignedBy <em>Assigned By</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagAssignment#getAssignedAt <em>Assigned At</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getTagAssignment()
 * @model
 * @generated
 */
@ProviderType
public interface TagAssignment extends EObject {
	/**
	 * Returns the value of the '<em><b>Tag</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tag</em>' reference.
	 * @see #setTag(Tag)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagAssignment_Tag()
	 * @model required="true"
	 * @generated
	 */
	Tag getTag();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagAssignment#getTag <em>Tag</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Tag</em>' reference.
	 * @see #getTag()
	 * @generated
	 */
	void setTag(Tag value);

	/**
	 * Returns the value of the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target</em>' containment reference.
	 * @see #setTarget(TagTarget)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagAssignment_Target()
	 * @model containment="true" required="true"
	 * @generated
	 */
	TagTarget getTarget();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagAssignment#getTarget <em>Target</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' containment reference.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(TagTarget value);

	/**
	 * Returns the value of the '<em><b>Assigned By</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Assigned By</em>' attribute.
	 * @see #setAssignedBy(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagAssignment_AssignedBy()
	 * @model
	 * @generated
	 */
	String getAssignedBy();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagAssignment#getAssignedBy <em>Assigned By</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Assigned By</em>' attribute.
	 * @see #getAssignedBy()
	 * @generated
	 */
	void setAssignedBy(String value);

	/**
	 * Returns the value of the '<em><b>Assigned At</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Assigned At</em>' attribute.
	 * @see #setAssignedAt(Date)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagAssignment_AssignedAt()
	 * @model
	 * @generated
	 */
	Date getAssignedAt();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagAssignment#getAssignedAt <em>Assigned At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Assigned At</em>' attribute.
	 * @see #getAssignedAt()
	 * @generated
	 */
	void setAssignedAt(Date value);

} // TagAssignment
