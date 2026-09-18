/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Invocation</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A call, as a message. What the REST flavor spreads over path, query, header and body has to have one form where a transport carries nothing but messages - MQTT, AMQP - and this is it. It says which operation and the values, and nothing about how it travels: correlation and reply address belong to the envelope, not here.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceInvocation#getOperation <em>Operation</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceInvocation#getArguments <em>Arguments</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInvocation()
 * @model
 * @generated
 */
@ProviderType
public interface ServiceInvocation extends EObject {
	/**
	 * Returns the value of the '<em><b>Operation</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The operation being called. A reference rather than a name and a signature: a contract cannot have two operations of one name, so nothing else is needed to tell them apart.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Operation</em>' reference.
	 * @see #setOperation(ServiceOperation)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInvocation_Operation()
	 * @model required="true"
	 * @generated
	 */
	ServiceOperation getOperation();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceInvocation#getOperation <em>Operation</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Operation</em>' reference.
	 * @see #getOperation()
	 * @generated
	 */
	void setOperation(ServiceOperation value);

	/**
	 * Returns the value of the '<em><b>Arguments</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Argument}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The values, one per parameter that is given. Order does not matter - each one names the parameter it fills.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Arguments</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInvocation_Arguments()
	 * @model containment="true"
	 * @generated
	 */
	EList<Argument> getArguments();

} // ServiceInvocation
