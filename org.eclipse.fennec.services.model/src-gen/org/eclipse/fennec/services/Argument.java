/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Argument</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * One value of a call, and the parameter it fills. By reference and not by name: a name would be a convention the two ends could read differently, which is exactly what the parameter bindings stopped doing for the REST wire. The contract has to be resolvable for this to be readable - the message says which one through its envelope.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.Argument#getParameter <em>Parameter</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Argument#getValue <em>Value</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getArgument()
 * @model
 * @generated
 */
@ProviderType
public interface Argument extends EObject {
	/**
	 * Returns the value of the '<em><b>Parameter</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The parameter this value is for.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Parameter</em>' reference.
	 * @see #setParameter(Parameter)
	 * @see org.eclipse.fennec.services.ServicesPackage#getArgument_Parameter()
	 * @model required="true"
	 * @generated
	 */
	Parameter getParameter();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Argument#getParameter <em>Parameter</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Parameter</em>' reference.
	 * @see #getParameter()
	 * @generated
	 */
	void setParameter(Parameter value);

	/**
	 * Returns the value of the '<em><b>Value</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The value itself, in the Property that fits its type. Absent means the argument was not given - which is only allowed where the parameter is optional.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Value</em>' containment reference.
	 * @see #setValue(Property)
	 * @see org.eclipse.fennec.services.ServicesPackage#getArgument_Value()
	 * @model containment="true"
	 * @generated
	 */
	Property getValue();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Argument#getValue <em>Value</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Value</em>' containment reference.
	 * @see #getValue()
	 * @generated
	 */
	void setValue(Property value);

} // Argument
