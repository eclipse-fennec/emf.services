/*
 */
package org.eclipse.fennec.services.examples.model.ddsrexample;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Contact</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.Contact#getType <em>Type</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.Contact#getValue <em>Value</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getContact()
 * @model
 * @generated
 */
@ProviderType
public interface Contact extends EObject {
	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.examples.model.ddsrexample.ContactType}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.ContactType
	 * @see #setType(ContactType)
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getContact_Type()
	 * @model
	 * @generated
	 */
	ContactType getType();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.examples.model.ddsrexample.Contact#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.ContactType
	 * @see #getType()
	 * @generated
	 */
	void setType(ContactType value);

	/**
	 * Returns the value of the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Value</em>' attribute.
	 * @see #setValue(String)
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getContact_Value()
	 * @model required="true"
	 * @generated
	 */
	String getValue();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.examples.model.ddsrexample.Contact#getValue <em>Value</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Value</em>' attribute.
	 * @see #getValue()
	 * @generated
	 */
	void setValue(String value);

} // Contact
