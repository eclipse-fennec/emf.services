/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Flavor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Abstract base for a transport binding. Concrete subclasses (RestFlavor, MqttFlavor, …) add transport-specific defaults. Operation-level bindings live in operationFlavors. Extended by third-party flavor plugins.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceFlavor#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceFlavor#getOperationFlavors <em>Operation Flavors</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceFlavor()
 * @model abstract="true"
 * @generated
 */
@ProviderType
public interface ServiceFlavor extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Kind</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.FlavorKind}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Discriminator that matches ConsumerCapability.supportedFlavors. Redundant with the concrete subclass but useful when filtering heterogeneous flavor lists at the registry.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.FlavorKind
	 * @see #setKind(FlavorKind)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceFlavor_Kind()
	 * @model required="true"
	 * @generated
	 */
	FlavorKind getKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceFlavor#getKind <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.FlavorKind
	 * @see #getKind()
	 * @generated
	 */
	void setKind(FlavorKind value);

	/**
	 * Returns the value of the '<em><b>Operation Flavors</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceOperationFlavor}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Per-operation transport bindings (HTTP method+path for REST, request/response topic for MQTT, …). Empty = the flavor's interface-level defaults apply to every operation.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Operation Flavors</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceFlavor_OperationFlavors()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServiceOperationFlavor> getOperationFlavors();

} // ServiceFlavor
