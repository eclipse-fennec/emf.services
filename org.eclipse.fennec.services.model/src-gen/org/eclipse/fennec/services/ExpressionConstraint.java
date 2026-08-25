/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Expression Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Open-ended constraint expressed as an expression in a given language (OCL in the prototype). Use when the typed constraints (Required, NumericRange, StringPattern, Enumeration, CollectionSize) do not fit — e.g. cross-parameter checks ('amount <= account.balance'), conditional rules ('if currency = EUR then amount <= 1000'), or domain logic. Has identity (NamedElement) so engines can report violations by name. Evaluation context: 'self' is the runtime value of the parameter/return value; 'op' is the enclosing ServiceOperation; 'params' is a map from parameter name to runtime value (only available where multiple parameters are in scope — i.e. preconditions/postconditions).
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ExpressionConstraint#getLanguage <em>Language</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ExpressionConstraint#getExpression <em>Expression</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ExpressionConstraint#getMessage <em>Message</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getExpressionConstraint()
 * @model
 * @generated
 */
@ProviderType
public interface ExpressionConstraint extends ParameterConstraint, NamedElement {
	/**
	 * Returns the value of the '<em><b>Language</b></em>' attribute.
	 * The default value is <code>"OCL"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ExpressionLanguage}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Expression language. Only OCL ships with the prototype, but the discriminator is in place so the model can later carry alternative languages without structural migration.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Language</em>' attribute.
	 * @see org.eclipse.fennec.services.ExpressionLanguage
	 * @see #setLanguage(ExpressionLanguage)
	 * @see org.eclipse.fennec.services.ServicesPackage#getExpressionConstraint_Language()
	 * @model default="OCL" required="true"
	 * @generated
	 */
	ExpressionLanguage getLanguage();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ExpressionConstraint#getLanguage <em>Language</em>}' attribute.
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
	 * The expression text. For OCL: a boolean expression returning true if the constraint is satisfied. Multi-line allowed.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Expression</em>' attribute.
	 * @see #setExpression(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getExpressionConstraint_Expression()
	 * @model required="true"
	 * @generated
	 */
	String getExpression();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ExpressionConstraint#getExpression <em>Expression</em>}' attribute.
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
	 * Optional human-readable explanation shown when the constraint is violated. May reference parameter values via implementation-defined placeholders (e.g. '{amount}' — exact placeholder syntax is an implementation concern).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Message</em>' attribute.
	 * @see #setMessage(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getExpressionConstraint_Message()
	 * @model
	 * @generated
	 */
	String getMessage();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ExpressionConstraint#getMessage <em>Message</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Message</em>' attribute.
	 * @see #getMessage()
	 * @generated
	 */
	void setMessage(String value);

} // ExpressionConstraint
