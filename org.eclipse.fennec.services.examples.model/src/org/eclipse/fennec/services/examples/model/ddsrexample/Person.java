/*
 */
package org.eclipse.fennec.services.examples.model.ddsrexample;

import java.util.Date;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Person</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getId <em>Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getFirstName <em>First Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getLastName <em>Last Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getBirthDate <em>Birth Date</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getContacts <em>Contacts</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getAddress <em>Address</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getPerson()
 * @model
 * @generated
 */
@ProviderType
public interface Person extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getPerson_Id()
	 * @model id="true" required="true"
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>First Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>First Name</em>' attribute.
	 * @see #setFirstName(String)
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getPerson_FirstName()
	 * @model
	 * @generated
	 */
	String getFirstName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getFirstName <em>First Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>First Name</em>' attribute.
	 * @see #getFirstName()
	 * @generated
	 */
	void setFirstName(String value);

	/**
	 * Returns the value of the '<em><b>Last Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Last Name</em>' attribute.
	 * @see #setLastName(String)
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getPerson_LastName()
	 * @model
	 * @generated
	 */
	String getLastName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getLastName <em>Last Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Last Name</em>' attribute.
	 * @see #getLastName()
	 * @generated
	 */
	void setLastName(String value);

	/**
	 * Returns the value of the '<em><b>Birth Date</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Birth Date</em>' attribute.
	 * @see #setBirthDate(Date)
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getPerson_BirthDate()
	 * @model
	 * @generated
	 */
	Date getBirthDate();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getBirthDate <em>Birth Date</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Birth Date</em>' attribute.
	 * @see #getBirthDate()
	 * @generated
	 */
	void setBirthDate(Date value);

	/**
	 * Returns the value of the '<em><b>Contacts</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.examples.model.ddsrexample.Contact}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Contacts</em>' containment reference list.
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getPerson_Contacts()
	 * @model containment="true" keys="type value"
	 * @generated
	 */
	EList<Contact> getContacts();

	/**
	 * Returns the value of the '<em><b>Address</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Address</em>' reference.
	 * @see #setAddress(Address)
	 * @see org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExamplePackage#getPerson_Address()
	 * @model keys="id"
	 * @generated
	 */
	Address getAddress();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.examples.model.ddsrexample.Person#getAddress <em>Address</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Address</em>' reference.
	 * @see #getAddress()
	 * @generated
	 */
	void setAddress(Address value);

} // Person
