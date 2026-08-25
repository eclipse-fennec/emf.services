/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Abstract transport-binding refinement for a single operation. Concrete subclasses (RestOperationFlavor, MqttOperationFlavor) add transport-specific fields.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceOperationFlavor#getOperation <em>Operation</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceOperationFlavor#getConsumes <em>Consumes</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceOperationFlavor#getProduces <em>Produces</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperationFlavor()
 * @model abstract="true"
 * @generated
 */
@ProviderType
public interface ServiceOperationFlavor extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Operation</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The ServiceOperation this flavor binding applies to. Non-containment: the operation is owned by its ServiceInterface.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Operation</em>' reference.
	 * @see #setOperation(ServiceOperation)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperationFlavor_Operation()
	 * @model required="true"
	 * @generated
	 */
	ServiceOperation getOperation();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceOperationFlavor#getOperation <em>Operation</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Operation</em>' reference.
	 * @see #getOperation()
	 * @generated
	 */
	void setOperation(ServiceOperation value);

	/**
	 * Returns the value of the '<em><b>Consumes</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Content-Types of accepted request bodies. Overrides the flavor-level default for this operation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Consumes</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperationFlavor_Consumes()
	 * @model
	 * @generated
	 */
	EList<String> getConsumes();

	/**
	 * Returns the value of the '<em><b>Produces</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Content-Types of produced response bodies. Overrides the flavor-level default for this operation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Produces</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceOperationFlavor_Produces()
	 * @model
	 * @generated
	 */
	EList<String> getProduces();

} // ServiceOperationFlavor
