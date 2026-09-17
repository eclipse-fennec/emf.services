/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Operation</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A single language-neutral operation on a ServiceInterface: parameters in order, optional return type with constraints, declared exceptions. The Code Publisher uses ServiceOperations to emit typed method signatures in Java/TS/Python stub artifacts.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceOperation#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceOperation#getParameters <em>Parameters</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceOperation#getReturnValue <em>Return Value</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceOperation#getExceptions <em>Exceptions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceOperation#getPreconditions <em>Preconditions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceOperation#getPostconditions <em>Postconditions</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperation()
 * @model
 * @generated
 */
@ProviderType
public interface ServiceOperation extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Human-readable description; rendered as JavaDoc / TSDoc / Python docstring on the generated stub.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperation_Description()
	 * @model
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceOperation#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Parameters</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Parameter}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Ordered list of input parameters. Order is the visible order in the generated stub signature and is determined by Parameter.index.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Parameters</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperation_Parameters()
	 * @model containment="true"
	 * @generated
	 */
	EList<Parameter> getParameters();

	/**
	 * Returns the value of the '<em><b>Return Value</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The operation's return slot, expressed as a Parameter: type, multiplicity and constraints in one place. Unset = void/None/no return value. 'index' is meaningless here and stays 0, 'optional' says the result may be null, 'defaultValue' has no meaning. Replaces the former 'returnType' string and 'returnConstraints' — a Parameter carries its own constraints.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Return Value</em>' containment reference.
	 * @see #setReturnValue(Parameter)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperation_ReturnValue()
	 * @model containment="true"
	 * @generated
	 */
	Parameter getReturnValue();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceOperation#getReturnValue <em>Return Value</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Return Value</em>' containment reference.
	 * @see #getReturnValue()
	 * @generated
	 */
	void setReturnValue(Parameter value);

	/**
	 * Returns the value of the '<em><b>Exceptions</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceException}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ServiceExceptions this operation may raise. Non-containment: the actual ServiceException objects are owned by the enclosing ServiceInterface.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Exceptions</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperation_Exceptions()
	 * @model
	 * @generated
	 */
	EList<ServiceException> getExceptions();

	/**
	 * Returns the value of the '<em><b>Preconditions</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Invariant}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Conditions that MUST hold when the operation is called. Evaluation context: 'self' = receiver, 'params' = map of incoming parameter values by name, 'op' = this ServiceOperation. Caller-side violations cause the framework to reject the call without invoking the implementation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Preconditions</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperation_Preconditions()
	 * @model containment="true"
	 * @generated
	 */
	EList<Invariant> getPreconditions();

	/**
	 * Returns the value of the '<em><b>Postconditions</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Invariant}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Conditions that MUST hold after the operation returns successfully. Evaluation context: as preconditions, plus 'result' = the return value (null if the operation has no return). Violations are framework bugs / implementation-contract breaches and are surfaced as ERROR diagnostics.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Postconditions</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperation_Postconditions()
	 * @model containment="true"
	 * @generated
	 */
	EList<Invariant> getPostconditions();

} // ServiceOperation
