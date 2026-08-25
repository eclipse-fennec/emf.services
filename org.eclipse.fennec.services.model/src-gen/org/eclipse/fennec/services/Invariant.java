/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Invariant</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Operation-level pre/postcondition or interface-level always-true expression. Same expression shape as ExpressionConstraint but NOT a ParameterConstraint — invariants are not attached to a single parameter or return value, they describe a contract on the enclosing operation or interface. Evaluation context depends on host: in Operation.preconditions / postconditions, 'self' is the receiver service object and 'params'/'result' are the call's parameters/return value; in ServiceInterface.invariants, 'self' is any instance of the interface.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.Invariant#getLanguage <em>Language</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Invariant#getExpression <em>Expression</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Invariant#getMessage <em>Message</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getInvariant()
 * @model
 * @generated
 */
@ProviderType
public interface Invariant extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Language</b></em>' attribute.
	 * The default value is <code>"OCL"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ExpressionLanguage}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Expression language. See ExpressionConstraint.language.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Language</em>' attribute.
	 * @see org.eclipse.fennec.services.ExpressionLanguage
	 * @see #setLanguage(ExpressionLanguage)
	 * @see org.eclipse.fennec.services.ServicesPackage#getInvariant_Language()
	 * @model default="OCL" required="true"
	 * @generated
	 */
	ExpressionLanguage getLanguage();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Invariant#getLanguage <em>Language</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Language</em>' attribute.
	 * @see org.eclipse.fennec.services.ExpressionLanguage
	 * @see #getLanguage()
	 * @generated
	 */
	void setLanguage(ExpressionLanguage value);

	/**
	 * Returns the value of the '<em><b>Expression</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The expression text. Boolean. Multi-line allowed.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Expression</em>' attribute.
	 * @see #setExpression(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getInvariant_Expression()
	 * @model required="true"
	 * @generated
	 */
	String getExpression();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Invariant#getExpression <em>Expression</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Expression</em>' attribute.
	 * @see #getExpression()
	 * @generated
	 */
	void setExpression(String value);

	/**
	 * Returns the value of the '<em><b>Message</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Optional human-readable explanation shown when the invariant is violated.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Message</em>' attribute.
	 * @see #setMessage(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getInvariant_Message()
	 * @model
	 * @generated
	 */
	String getMessage();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Invariant#getMessage <em>Message</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Message</em>' attribute.
	 * @see #getMessage()
	 * @generated
	 */
	void setMessage(String value);

} // Invariant
