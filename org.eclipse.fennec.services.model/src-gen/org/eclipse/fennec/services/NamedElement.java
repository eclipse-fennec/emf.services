/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Named Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Mixin interface for anything carrying an identifying name. Names identify within their containment scope, not globally — multiple objects with the same name in the same XMI document are legal (e.g. a ServiceOperation 'listCatalog' on the interface and a RestOperationFlavor 'listCatalog' on a binding). Cross-references resolve via positional URI fragments, not by name.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.NamedElement#getName <em>Name</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getNamedElement()
 * @model interface="true" abstract="true"
 * @generated
 */
@ProviderType
public interface NamedElement extends EObject {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Identifying name within the containment scope. Convention: symbolic, language-neutral (e.g. 'PaymentService', 'com.example.payment.PaymentService'), not a display label. Not an XMI iD — see the class docstring.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getNamedElement_Name()
	 * @model required="true"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.NamedElement#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

} // NamedElement
