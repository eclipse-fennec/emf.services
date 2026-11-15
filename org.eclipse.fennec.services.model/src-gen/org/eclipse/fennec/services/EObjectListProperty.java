/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EObject List Property</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Property carrying an ordered list of models, for an argument or a result whose slot is model-typed and many-valued (upperBound other than 1). The twin of StringListProperty for models; containment for the reason EObjectProperty gives. Empty list = no values; a list keeps its order and may not hold the same object twice.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.EObjectListProperty#getValue <em>Value</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getEObjectListProperty()
 * @model
 * @generated
 */
@ProviderType
public interface EObjectListProperty extends Property {
	/**
	 * Returns the value of the '<em><b>Value</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.EObject}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The models this property carries, in order. What each is allowed to be is stated by the slot it fills - the Parameter's eType.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Value</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getEObjectListProperty_Value()
	 * @model containment="true"
	 * @generated
	 */
	EList<EObject> getValue();

} // EObjectListProperty
