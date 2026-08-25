/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Exception</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A declared error a ServiceOperation can raise. Defined once per ServiceInterface and referenced by the operations that may raise it. Carries typed properties for the exception payload, so consumers can extract structured data instead of parsing a message.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceException#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceException#getType <em>Type</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceException#getProperties <em>Properties</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceException()
 * @model
 * @generated
 */
@ProviderType
public interface ServiceException extends NamedElement, VersionedElement {
	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Doc text; rendered as @throws JavaDoc / TSDoc tag / Python docstring entry.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceException_Description()
	 * @model
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceException#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Symbolic exception type (FQN-style), e.g. 'com.example.payment.InsufficientFundsException'. Distinct from name: 'name' is the catalog identifier, 'type' is what consumers/providers see on the wire.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see #setType(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceException_Type()
	 * @model required="true"
	 * @generated
	 */
	String getType();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceException#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see #getType()
	 * @generated
	 */
	void setType(String value);

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Property}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Typed payload fields (e.g. errorCode: int, retryable: bool, accountId: string). Empty = no structured payload, only a message.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceException_Properties()
	 * @model containment="true"
	 * @generated
	 */
	EList<Property> getProperties();

} // ServiceException
