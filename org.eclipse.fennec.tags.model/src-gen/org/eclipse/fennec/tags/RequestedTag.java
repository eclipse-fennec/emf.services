/*
 */
package org.eclipse.fennec.tags;

import java.util.Date;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Requested Tag</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A target asked for a tag it may not give itself — the tag does not exist yet or is not self-assignable. The administrator accepts the request (the tag is created where needed and assigned) or discards it. A discarded request comes back if the target asks again; nothing is blocked.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.RequestedTag#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.RequestedTag#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.RequestedTag#getRequestedBy <em>Requested By</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.RequestedTag#getRequestedAt <em>Requested At</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getRequestedTag()
 * @model
 * @generated
 */
@ProviderType
public interface RequestedTag extends EObject {
	/**
	 * Returns the value of the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Path</em>' attribute.
	 * @see #setPath(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getRequestedTag_Path()
	 * @model required="true"
	 * @generated
	 */
	String getPath();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.RequestedTag#getPath <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Path</em>' attribute.
	 * @see #getPath()
	 * @generated
	 */
	void setPath(String value);

	/**
	 * Returns the value of the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target</em>' containment reference.
	 * @see #setTarget(TagTarget)
	 * @see org.eclipse.fennec.tags.TagsPackage#getRequestedTag_Target()
	 * @model containment="true" required="true"
	 * @generated
	 */
	TagTarget getTarget();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.RequestedTag#getTarget <em>Target</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' containment reference.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(TagTarget value);

	/**
	 * Returns the value of the '<em><b>Requested By</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Requested By</em>' attribute.
	 * @see #setRequestedBy(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getRequestedTag_RequestedBy()
	 * @model
	 * @generated
	 */
	String getRequestedBy();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.RequestedTag#getRequestedBy <em>Requested By</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Requested By</em>' attribute.
	 * @see #getRequestedBy()
	 * @generated
	 */
	void setRequestedBy(String value);

	/**
	 * Returns the value of the '<em><b>Requested At</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Requested At</em>' attribute.
	 * @see #setRequestedAt(Date)
	 * @see org.eclipse.fennec.tags.TagsPackage#getRequestedTag_RequestedAt()
	 * @model
	 * @generated
	 */
	Date getRequestedAt();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.RequestedTag#getRequestedAt <em>Requested At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Requested At</em>' attribute.
	 * @see #getRequestedAt()
	 * @generated
	 */
	void setRequestedAt(Date value);

} // RequestedTag
