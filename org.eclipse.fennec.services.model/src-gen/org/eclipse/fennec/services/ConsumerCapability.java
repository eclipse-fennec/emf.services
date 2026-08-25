/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Consumer Capability</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Bag of capabilities a consumer attaches to a lookup. NOT persistent — created per request and passed through getServiceReferences / getAllServiceReferences. The registry uses supportedFlavors to filter implementations the consumer cannot actually invoke.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ConsumerCapability#getConsumerId <em>Consumer Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ConsumerCapability#getSupportedFlavors <em>Supported Flavors</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ConsumerCapability#getProperties <em>Properties</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerCapability()
 * @model
 * @generated
 */
@ProviderType
public interface ConsumerCapability extends EObject {
	/**
	 * Returns the value of the '<em><b>Consumer Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Optional consumer identity (auditing, policy). Null = anonymous.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Consumer Id</em>' attribute.
	 * @see #setConsumerId(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerCapability_ConsumerId()
	 * @model
	 * @generated
	 */
	String getConsumerId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ConsumerCapability#getConsumerId <em>Consumer Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Consumer Id</em>' attribute.
	 * @see #getConsumerId()
	 * @generated
	 */
	void setConsumerId(String value);

	/**
	 * Returns the value of the '<em><b>Supported Flavors</b></em>' attribute list.
	 * The list contents are of type {@link org.eclipse.fennec.services.FlavorKind}.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.FlavorKind}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Flavors the consumer can invoke. At least one — a consumer without any flavor client plugin cannot consume anything.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Supported Flavors</em>' attribute list.
	 * @see org.eclipse.fennec.services.FlavorKind
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerCapability_SupportedFlavors()
	 * @model required="true"
	 * @generated
	 */
	EList<FlavorKind> getSupportedFlavors();

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Property}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Additional capability hints (accepted content types, encoding preferences, …). Open-ended bag.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getConsumerCapability_Properties()
	 * @model containment="true"
	 * @generated
	 */
	EList<Property> getProperties();

} // ConsumerCapability
