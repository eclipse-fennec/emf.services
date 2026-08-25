/*
 */
package org.eclipse.fennec.services.examples.model.ddsrexample;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Address Book</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.AddressBook#getAddresses <em>Addresses</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.AddressBook#getPersons <em>Persons</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getAddressBook()
 * @model
 * @generated
 */
@ProviderType
public interface AddressBook extends EObject {
	/**
	 * Returns the value of the '<em><b>Addresses</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.examples.model.ddsrexample.Address}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Addresses</em>' containment reference list.
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getAddressBook_Addresses()
	 * @model containment="true"
	 * @generated
	 */
	EList<Address> getAddresses();

	/**
	 * Returns the value of the '<em><b>Persons</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.examples.model.ddsrexample.Person}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Persons</em>' containment reference list.
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getAddressBook_Persons()
	 * @model containment="true"
	 * @generated
	 */
	EList<Person> getPersons();

} // AddressBook
