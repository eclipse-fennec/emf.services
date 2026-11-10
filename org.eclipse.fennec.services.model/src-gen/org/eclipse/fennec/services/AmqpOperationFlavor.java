/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Amqp Operation Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * How one operation is reached over AMQP.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.AmqpOperationFlavor#getRoutingKey <em>Routing Key</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getAmqpOperationFlavor()
 * @model
 * @generated
 */
@ProviderType
public interface AmqpOperationFlavor extends ServiceOperationFlavor {
	/**
	 * Returns the value of the '<em><b>Routing Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The full routing key of this operation. Null = <AmqpFlavor.requestRoutingKey>.<operation name>.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Routing Key</em>' attribute.
	 * @see #setRoutingKey(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getAmqpOperationFlavor_RoutingKey()
	 * @model
	 * @generated
	 */
	String getRoutingKey();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.AmqpOperationFlavor#getRoutingKey <em>Routing Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Routing Key</em>' attribute.
	 * @see #getRoutingKey()
	 * @generated
	 */
	void setRoutingKey(String value);

} // AmqpOperationFlavor
