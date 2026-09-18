/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Rest Exception Binding</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * The HTTP status one declared ServiceException of an operation travels as. Owned by the flavor, so it is implementation-specific (im1 layer) and never touches the catalog contract (sd1) — the same layering as RestParameterBinding. Without such a binding an error is a 500.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.RestExceptionBinding#getException <em>Exception</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestExceptionBinding#getStatus <em>Status</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getRestExceptionBinding()
 * @model
 * @generated
 */
@ProviderType
public interface RestExceptionBinding extends EObject {
	/**
	 * Returns the value of the '<em><b>Exception</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The declared error of the bound ServiceOperation. Non-containment: exceptions are owned by the catalog ServiceInterface, same resolution rule as RestParameterBinding.parameter.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Exception</em>' reference.
	 * @see #setException(ServiceException)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestExceptionBinding_Exception()
	 * @model required="true"
	 * @generated
	 */
	ServiceException getException();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RestExceptionBinding#getException <em>Exception</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Exception</em>' reference.
	 * @see #getException()
	 * @generated
	 */
	void setException(ServiceException value);

	/**
	 * Returns the value of the '<em><b>Status</b></em>' attribute.
	 * The default value is <code>"500"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * HTTP status this error is answered with, e.g. 409 for a conflict or 404 for something not found. A plain int rather than an enum: the set of codes in use is not ours to close, and a provider that answers with 418 is stating a fact, not asking permission.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Status</em>' attribute.
	 * @see #setStatus(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestExceptionBinding_Status()
	 * @model default="500" required="true"
	 * @generated
	 */
	int getStatus();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RestExceptionBinding#getStatus <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Status</em>' attribute.
	 * @see #getStatus()
	 * @generated
	 */
	void setStatus(int value);

} // RestExceptionBinding
