/*
 */
package org.eclipse.fennec.tags;

import java.util.Date;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tag Change</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * One change to the tag registry, as the changes broadcast reports it — after it was kept. What it says depends on its kind: a tag by its path (and, moved, the path it had), an assignment or request with its target, a rule with its kind and filter.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.TagChange#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagChange#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagChange#getFormerPath <em>Former Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagChange#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagChange#getRuleKind <em>Rule Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagChange#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagChange#getAt <em>At</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getTagChange()
 * @model
 * @generated
 */
@ProviderType
public interface TagChange extends EObject {
	/**
	 * Returns the value of the '<em><b>Kind</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.tags.TagChangeKind}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.tags.TagChangeKind
	 * @see #setKind(TagChangeKind)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagChange_Kind()
	 * @model required="true"
	 * @generated
	 */
	TagChangeKind getKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagChange#getKind <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.tags.TagChangeKind
	 * @see #getKind()
	 * @generated
	 */
	void setKind(TagChangeKind value);

	/**
	 * Returns the value of the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The tag's path as it is after the change; for a removed tag, the path it had.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Path</em>' attribute.
	 * @see #setPath(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagChange_Path()
	 * @model
	 * @generated
	 */
	String getPath();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagChange#getPath <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Path</em>' attribute.
	 * @see #getPath()
	 * @generated
	 */
	void setPath(String value);

	/**
	 * Returns the value of the '<em><b>Former Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * TAG_MOVED: the path the tag had before.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Former Path</em>' attribute.
	 * @see #setFormerPath(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagChange_FormerPath()
	 * @model
	 * @generated
	 */
	String getFormerPath();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagChange#getFormerPath <em>Former Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Former Path</em>' attribute.
	 * @see #getFormerPath()
	 * @generated
	 */
	void setFormerPath(String value);

	/**
	 * Returns the value of the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target</em>' containment reference.
	 * @see #setTarget(TagTarget)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagChange_Target()
	 * @model containment="true"
	 * @generated
	 */
	TagTarget getTarget();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagChange#getTarget <em>Target</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' containment reference.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(TagTarget value);

	/**
	 * Returns the value of the '<em><b>Rule Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * RULE_PUT, RULE_REMOVED: the kind of target the rule looks at.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Rule Kind</em>' attribute.
	 * @see #setRuleKind(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagChange_RuleKind()
	 * @model
	 * @generated
	 */
	String getRuleKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagChange#getRuleKind <em>Rule Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Rule Kind</em>' attribute.
	 * @see #getRuleKind()
	 * @generated
	 */
	void setRuleKind(String value);

	/**
	 * Returns the value of the '<em><b>Filter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Filter</em>' attribute.
	 * @see #setFilter(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagChange_Filter()
	 * @model
	 * @generated
	 */
	String getFilter();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagChange#getFilter <em>Filter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Filter</em>' attribute.
	 * @see #getFilter()
	 * @generated
	 */
	void setFilter(String value);

	/**
	 * Returns the value of the '<em><b>At</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>At</em>' attribute.
	 * @see #setAt(Date)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagChange_At()
	 * @model
	 * @generated
	 */
	Date getAt();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagChange#getAt <em>At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>At</em>' attribute.
	 * @see #getAt()
	 * @generated
	 */
	void setAt(Date value);

} // TagChange
