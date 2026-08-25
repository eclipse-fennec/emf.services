/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Reference</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Consumer-side handle to a registered service, equivalent to org.osgi.framework.ServiceReference. The id is the language-neutral counterpart of OSGi service.id (UUID rather than long). Properties are typed (Property containments) instead of an untyped map.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceReference#getId <em>Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceReference#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceReference#getProvider <em>Provider</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceReference#getRegistration <em>Registration</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceReference()
 * @model
 * @generated
 */
@ProviderType
public interface ServiceReference extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * UUID assigned by the registry at registration time. Replaces OSGi 'service.id: long' to remain stable across processes.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceReference_Id()
	 * @model id="true" required="true"
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceReference#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Property}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Service properties at the time of registration / last setProperties call. Includes both framework properties (service.id, service.ranking, …) and provider-supplied properties.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceReference_Properties()
	 * @model containment="true"
	 * @generated
	 */
	EList<Property> getProperties();

	/**
	 * Returns the value of the '<em><b>Provider</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Provider that registered the service. Replaces OSGi 'bundle: long'.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Provider</em>' reference.
	 * @see #setProvider(ServiceProvider)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceReference_Provider()
	 * @model required="true"
	 * @generated
	 */
	ServiceProvider getProvider();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceReference#getProvider <em>Provider</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Provider</em>' reference.
	 * @see #getProvider()
	 * @generated
	 */
	void setProvider(ServiceProvider value);

	/**
	 * Returns the value of the '<em><b>Registration</b></em>' reference.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.services.ServiceRegistration#getReference <em>Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Provider-side handle on the same service, paired via eOpposite. Null only during the brief window between unregister() and removal. TRANSIENT: broker-side registrations are runtime handles without a containment home in the persisted registry — a serialized link would tear every snapshot apart.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Registration</em>' reference.
	 * @see #setRegistration(ServiceRegistration)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceReference_Registration()
	 * @see org.eclipse.fennec.services.ServiceRegistration#getReference
	 * @model opposite="reference" transient="true"
	 * @generated
	 */
	ServiceRegistration getRegistration();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceReference#getRegistration <em>Registration</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Registration</em>' reference.
	 * @see #getRegistration()
	 * @generated
	 */
	void setRegistration(ServiceRegistration value);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Looks up a property value by key. Returns null when no property with that name exists. Return type is EJavaObject because the concrete value type depends on the Property subclass.
	 * @param key Property name. Case-sensitive.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	Object getProperty(String key);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Returns the names of all defined properties, in undefined order. Empty list if no properties are set.
	 * <!-- end-model-doc -->
	 * @model kind="operation"
	 * @generated
	 */
	EList<String> getPropertyKeys();

} // ServiceReference
