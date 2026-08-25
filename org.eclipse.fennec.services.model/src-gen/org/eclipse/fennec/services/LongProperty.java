/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Long Property</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Property carrying a 64-bit integer value.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.LongProperty#getValue <em>Value</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getLongProperty()
 * @model
 * @generated
 */
@ProviderType
public interface LongProperty extends Property {
	/**
	 * Returns the value of the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Value</em>' attribute.
	 * @see #setValue(long)
	 * @see org.eclipse.fennec.services.ServicesPackage#getLongProperty_Value()
	 * @model
	 * @generated
	 */
	long getValue();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.LongProperty#getValue <em>Value</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Value</em>' attribute.
	 * @see #getValue()
	 * @generated
	 */
	void setValue(long value);

} // LongProperty
