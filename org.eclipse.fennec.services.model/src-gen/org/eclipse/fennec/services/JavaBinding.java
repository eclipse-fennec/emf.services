/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Java Binding</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Java rendering: targetPackage is the Java package, and the generated interface is named after the ServiceInterface.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.JavaBinding#getApiType <em>Api Type</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getJavaBinding()
 * @model
 * @generated
 */
@ProviderType
public interface JavaBinding extends LanguageBinding {
	/**
	 * Returns the value of the '<em><b>Api Type</b></em>' attribute.
	 * The default value is <code>"PROVIDER"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.ApiType}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Which OSGi API annotation the generated interface carries.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Api Type</em>' attribute.
	 * @see org.eclipse.fennec.services.ApiType
	 * @see #setApiType(ApiType)
	 * @see org.eclipse.fennec.services.ServicesPackage#getJavaBinding_ApiType()
	 * @model default="PROVIDER" required="true"
	 * @generated
	 */
	ApiType getApiType();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.JavaBinding#getApiType <em>Api Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Api Type</em>' attribute.
	 * @see org.eclipse.fennec.services.ApiType
	 * @see #getApiType()
	 * @generated
	 */
	void setApiType(ApiType value);

} // JavaBinding
