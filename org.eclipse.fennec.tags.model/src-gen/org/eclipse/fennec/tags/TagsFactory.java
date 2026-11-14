/*
 */
package org.eclipse.fennec.tags;

import org.eclipse.emf.ecore.EFactory;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.eclipse.fennec.tags.TagsPackage
 * @generated
 */
@ProviderType
public interface TagsFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	TagsFactory eINSTANCE = org.eclipse.fennec.tags.impl.TagsFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Tag Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Tag Registry</em>'.
	 * @generated
	 */
	TagRegistry createTagRegistry();

	/**
	 * Returns a new object of class '<em>Tag</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Tag</em>'.
	 * @generated
	 */
	Tag createTag();

	/**
	 * Returns a new object of class '<em>Tag Target</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Tag Target</em>'.
	 * @generated
	 */
	TagTarget createTagTarget();

	/**
	 * Returns a new object of class '<em>Tag Assignment</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Tag Assignment</em>'.
	 * @generated
	 */
	TagAssignment createTagAssignment();

	/**
	 * Returns a new object of class '<em>Tag Rule</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Tag Rule</em>'.
	 * @generated
	 */
	TagRule createTagRule();

	/**
	 * Returns a new object of class '<em>Requested Tag</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Requested Tag</em>'.
	 * @generated
	 */
	RequestedTag createRequestedTag();

	/**
	 * Returns a new object of class '<em>Effective Tag</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Effective Tag</em>'.
	 * @generated
	 */
	EffectiveTag createEffectiveTag();

	/**
	 * Returns a new object of class '<em>Found Targets</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Found Targets</em>'.
	 * @generated
	 */
	FoundTargets createFoundTargets();

	/**
	 * Returns a new object of class '<em>Tagged Target</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Tagged Target</em>'.
	 * @generated
	 */
	TaggedTarget createTaggedTarget();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	TagsPackage getTagsPackage();

} //TagsFactory
