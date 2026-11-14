/*
 */
package org.eclipse.fennec.tags;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tag Target</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Something a tag can be on, named by the source that knows it. For the services registry: kind 'contract' with the interface name as key, kind 'implementation' with provider name and contract name. Neither is a type of this model, which is what lets it tag other things.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.TagTarget#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagTarget#getKey <em>Key</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getTagTarget()
 * @model
 * @generated
 */
@ProviderType
public interface TagTarget extends EObject {
	/**
	 * Returns the value of the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * What sort of thing this is, as its source names it, e.g. 'contract', 'implementation', later 'model'. An open string, not an enum, so a new source needs no model change.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Kind</em>' attribute.
	 * @see #setKind(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagTarget_Kind()
	 * @model required="true"
	 * @generated
	 */
	String getKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagTarget#getKind <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Kind</em>' attribute.
	 * @see #getKind()
	 * @generated
	 */
	void setKind(String value);

	/**
	 * Returns the value of the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Which one, unique within its kind, as its source names it. Stable across changes of the thing itself — for a contract its name, not its fingerprint, so a tag survives a change of the contract.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Key</em>' attribute.
	 * @see #setKey(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagTarget_Key()
	 * @model required="true"
	 * @generated
	 */
	String getKey();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagTarget#getKey <em>Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Key</em>' attribute.
	 * @see #getKey()
	 * @generated
	 */
	void setKey(String value);

} // TagTarget
