/*
 */
package org.eclipse.fennec.tags;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Found Targets</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * What a query by tags answers: the keys of the targets of one kind that carry them, sorted. Keys only — what a target looks like comes from its own source, so there is one place for it.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.FoundTargets#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.FoundTargets#getKeys <em>Keys</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getFoundTargets()
 * @model
 * @generated
 */
@ProviderType
public interface FoundTargets extends EObject {
	/**
	 * Returns the value of the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Kind</em>' attribute.
	 * @see #setKind(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getFoundTargets_Kind()
	 * @model
	 * @generated
	 */
	String getKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.FoundTargets#getKind <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Kind</em>' attribute.
	 * @see #getKind()
	 * @generated
	 */
	void setKind(String value);

	/**
	 * Returns the value of the '<em><b>Keys</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Keys</em>' attribute list.
	 * @see org.eclipse.fennec.tags.TagsPackage#getFoundTargets_Keys()
	 * @model
	 * @generated
	 */
	EList<String> getKeys();

} // FoundTargets
