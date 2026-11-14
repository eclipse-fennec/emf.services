/*
 */
package org.eclipse.fennec.tags;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tag Registry</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Everything the tag service keeps, in one document: the tree, the administrator's assignments, the rules and the open requests. What is persisted, and what references into the tree resolve within.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.TagRegistry#getTags <em>Tags</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagRegistry#getAssignments <em>Assignments</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagRegistry#getRules <em>Rules</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagRegistry#getRequests <em>Requests</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getTagRegistry()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='rootIdsUnique'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 rootIdsUnique='tags-&gt;isUnique(t | t.id)'"
 * @generated
 */
@ProviderType
public interface TagRegistry extends EObject {
	/**
	 * Returns the value of the '<em><b>Tags</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.tags.Tag}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The roots of the tree. Their ids are unique among each other.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Tags</em>' containment reference list.
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagRegistry_Tags()
	 * @model containment="true"
	 * @generated
	 */
	EList<Tag> getTags();

	/**
	 * Returns the value of the '<em><b>Assignments</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.tags.TagAssignment}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Assignments</em>' containment reference list.
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagRegistry_Assignments()
	 * @model containment="true"
	 * @generated
	 */
	EList<TagAssignment> getAssignments();

	/**
	 * Returns the value of the '<em><b>Rules</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.tags.TagRule}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Rules</em>' containment reference list.
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagRegistry_Rules()
	 * @model containment="true"
	 * @generated
	 */
	EList<TagRule> getRules();

	/**
	 * Returns the value of the '<em><b>Requests</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.tags.RequestedTag}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Requests</em>' containment reference list.
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagRegistry_Requests()
	 * @model containment="true"
	 * @generated
	 */
	EList<RequestedTag> getRequests();

} // TagRegistry
