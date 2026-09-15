/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Rest Parameter Binding</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Wire placement of one operation Parameter inside a RestOperationFlavor. Owned by the flavor, so it is implementation-specific (im1 layer) and never touches the catalog contract (sd1).
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.RestParameterBinding#getParameter <em>Parameter</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestParameterBinding#getBinding <em>Binding</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.RestParameterBinding#getWireName <em>Wire Name</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getRestParameterBinding()
 * @model
 * @generated
 */
@ProviderType
public interface RestParameterBinding extends EObject {
	/**
	 * Returns the value of the '<em><b>Parameter</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The Parameter of the bound ServiceOperation. Non-containment: parameters are owned by the catalog operation, same resolution rule as ServiceOperationFlavor.operation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Parameter</em>' reference.
	 * @see #setParameter(Parameter)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestParameterBinding_Parameter()
	 * @model required="true"
	 * @generated
	 */
	Parameter getParameter();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RestParameterBinding#getParameter <em>Parameter</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Parameter</em>' reference.
	 * @see #getParameter()
	 * @generated
	 */
	void setParameter(Parameter value);

	/**
	 * Returns the value of the '<em><b>Binding</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ParameterBinding}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * BODY / QUERY / HEADER / PATH. PATH requires a matching '{parameterName}' segment in RestOperationFlavor.path; the broker checks that on publish (the static OCL only checks that a path exists).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Binding</em>' attribute.
	 * @see org.eclipse.fennec.services.ParameterBinding
	 * @see #setBinding(ParameterBinding)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestParameterBinding_Binding()
	 * @model required="true"
	 * @generated
	 */
	ParameterBinding getBinding();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RestParameterBinding#getBinding <em>Binding</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Binding</em>' attribute.
	 * @see org.eclipse.fennec.services.ParameterBinding
	 * @see #getBinding()
	 * @generated
	 */
	void setBinding(ParameterBinding value);

	/**
	 * Returns the value of the '<em><b>Wire Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Name used on the wire when it differs from the Parameter name: query key, header name ('X-Tenant') or path template variable. Null = the Parameter name.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Wire Name</em>' attribute.
	 * @see #setWireName(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getRestParameterBinding_WireName()
	 * @model
	 * @generated
	 */
	String getWireName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.RestParameterBinding#getWireName <em>Wire Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Wire Name</em>' attribute.
	 * @see #getWireName()
	 * @generated
	 */
	void setWireName(String value);

} // RestParameterBinding
