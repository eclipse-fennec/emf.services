/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Invocation Result</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * What a call answered. Either a value or a failure, and an operation that returns nothing answers with neither. The failure is a Diagnostic because that is how this registry reports failures everywhere - a transport turns it into a status where it has one.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceInvocationResult#getValue <em>Value</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceInvocationResult#getDiagnostic <em>Diagnostic</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInvocationResult()
 * @model
 * @generated
 */
@ProviderType
public interface ServiceInvocationResult extends EObject {
	/**
	 * Returns the value of the '<em><b>Value</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The result, in the Property that fits the operation's return type. Absent for an operation that returns nothing, and for a failure.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Value</em>' containment reference.
	 * @see #setValue(Property)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInvocationResult_Value()
	 * @model containment="true"
	 * @generated
	 */
	Property getValue();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceInvocationResult#getValue <em>Value</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Value</em>' containment reference.
	 * @see #getValue()
	 * @generated
	 */
	void setValue(Property value);

	/**
	 * Returns the value of the '<em><b>Diagnostic</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Why the call did not produce a value. Absent on success. Its code is what a declared error of the contract is recognised by.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Diagnostic</em>' containment reference.
	 * @see #setDiagnostic(Diagnostic)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInvocationResult_Diagnostic()
	 * @model containment="true"
	 * @generated
	 */
	Diagnostic getDiagnostic();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceInvocationResult#getDiagnostic <em>Diagnostic</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Diagnostic</em>' containment reference.
	 * @see #getDiagnostic()
	 * @generated
	 */
	void setDiagnostic(Diagnostic value);

} // ServiceInvocationResult
