/*
 */
package org.eclipse.fennec.tags;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tag</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * One category. Its id is short and technical and unique among its siblings, not globally: iot/weather and marketing/weather may both exist. Its path is derived from the ids up the tree, so changing an id moves every descendant with it and nothing has to be rewritten. Whatever names a tag from inside this model references the object; whatever names it from outside uses the path, and formerPaths keeps such names valid after a rename.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.Tag#getId <em>Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.Tag#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.Tag#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.Tag#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.Tag#isSelfAssignable <em>Self Assignable</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.Tag#getFormerPaths <em>Former Paths</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.Tag#getChildren <em>Children</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.Tag#getParent <em>Parent</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getTag()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='idHasNoSlash childIdsUnique'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 idHasNoSlash='id &lt;&gt; null and id.matches(\'^[^/]+$\')' childIdsUnique='children-&gt;isUnique(c | c.id)'"
 * @generated
 */
@ProviderType
public interface Tag extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Technical, short and stable, e.g. 'iot' or 'sensors'. No '/', which separates the segments of a path. Unique among the siblings.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTag_Id()
	 * @model required="true"
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.Tag#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Human-readable, may change at any time without breaking anything.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTag_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.Tag#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTag_Description()
	 * @model
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.Tag#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The ids from the root down, joined by '/', e.g. 'iot/sensors'. What filters, assignments from outside and a URI name a tag by. Derived, never stored: a document on the wire carries the tree, and a reader without an OCL engine joins the ids itself.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Path</em>' attribute.
	 * @see org.eclipse.fennec.tags.TagsPackage#getTag_Path()
	 * @model transient="true" changeable="false" volatile="true" derived="true"
	 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 derivation='if parent = null then id else parent.path + \'/\' + id endif'"
	 * @generated
	 */
	String getPath();

	/**
	 * Returns the value of the '<em><b>Self Assignable</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Whether a target may give itself this tag (a provider at registration). False: asking for it only files a request the administrator accepts or discards.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Self Assignable</em>' attribute.
	 * @see #setSelfAssignable(boolean)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTag_SelfAssignable()
	 * @model
	 * @generated
	 */
	boolean isSelfAssignable();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.Tag#isSelfAssignable <em>Self Assignable</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Self Assignable</em>' attribute.
	 * @see #isSelfAssignable()
	 * @generated
	 */
	void setSelfAssignable(boolean value);

	/**
	 * Returns the value of the '<em><b>Former Paths</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Paths this tag had before it was renamed or moved. Resolving a path accepts them too, so a target that named the tag by its old path keeps it. Never offered to a policy: what decides access sees only current paths.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Former Paths</em>' attribute list.
	 * @see org.eclipse.fennec.tags.TagsPackage#getTag_FormerPaths()
	 * @model
	 * @generated
	 */
	EList<String> getFormerPaths();

	/**
	 * Returns the value of the '<em><b>Children</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.tags.Tag}.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.tags.Tag#getParent <em>Parent</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Children</em>' containment reference list.
	 * @see org.eclipse.fennec.tags.TagsPackage#getTag_Children()
	 * @see org.eclipse.fennec.tags.Tag#getParent
	 * @model opposite="parent" containment="true"
	 * @generated
	 */
	EList<Tag> getChildren();

	/**
	 * Returns the value of the '<em><b>Parent</b></em>' container reference.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.tags.Tag#getChildren <em>Children</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Parent</em>' container reference.
	 * @see #setParent(Tag)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTag_Parent()
	 * @see org.eclipse.fennec.tags.Tag#getChildren
	 * @model opposite="children" transient="false"
	 * @generated
	 */
	Tag getParent();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.Tag#getParent <em>Parent</em>}' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Parent</em>' container reference.
	 * @see #getParent()
	 * @generated
	 */
	void setParent(Tag value);

} // Tag
