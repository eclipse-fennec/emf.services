/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Registration</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Provider-side handle to a registered service, equivalent to org.osgi.framework.ServiceRegistration. The provider holds onto this to unregister later or to modify properties.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistration#getReference <em>Reference</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistration#isUnregistered <em>Unregistered</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistration#getProvider <em>Provider</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistration#getImplementation <em>Implementation</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistration#getUsingSessions <em>Using Sessions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceRegistration#getConsumerCount <em>Consumer Count</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistration()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='unregisteredNotInRegistry'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 unregisteredNotInRegistry='not unregistered or LocalServiceRegistry.allInstances()-&gt;forAll(r | not r.registrations-&gt;includes(self))'"
 * @generated
 */
@ProviderType
public interface ServiceRegistration extends EObject {
	/**
	 * Returns the value of the '<em><b>Reference</b></em>' reference.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.services.ServiceReference#getRegistration <em>Registration</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Consumer-side view of the same service, paired via eOpposite. Both Registration and Reference are owned (containment) by the LocalServiceRegistry, not by each other.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Reference</em>' reference.
	 * @see #setReference(ServiceReference)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistration_Reference()
	 * @see org.eclipse.fennec.services.ServiceReference#getRegistration
	 * @model opposite="registration" required="true" transient="true"
	 * @generated
	 */
	ServiceReference getReference();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceRegistration#getReference <em>Reference</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Reference</em>' reference.
	 * @see #getReference()
	 * @generated
	 */
	void setReference(ServiceReference value);

	/**
	 * Returns the value of the '<em><b>Unregistered</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Becomes true after unregister() has run. Operations on an unregistered registration are no-ops (OCL TODO: forbid in references list of the registry once true).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Unregistered</em>' attribute.
	 * @see #setUnregistered(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistration_Unregistered()
	 * @model default="false" required="true"
	 * @generated
	 */
	boolean isUnregistered();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceRegistration#isUnregistered <em>Unregistered</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Unregistered</em>' attribute.
	 * @see #isUnregistered()
	 * @generated
	 */
	void setUnregistered(boolean value);

	/**
	 * Returns the value of the '<em><b>Provider</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The provider that materialised this registration by publishing the implementation (ACQUISITION.md par.8). Non-containment: the registration records the fact, it does not own the provider. Replaces the broker-internal implByRegistration side-map.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Provider</em>' reference.
	 * @see #setProvider(ServiceProvider)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistration_Provider()
	 * @model required="true"
	 * @generated
	 */
	ServiceProvider getProvider();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceRegistration#getProvider <em>Provider</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Provider</em>' reference.
	 * @see #getProvider()
	 * @generated
	 */
	void setProvider(ServiceProvider value);

	/**
	 * Returns the value of the '<em><b>Implementation</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The published implementation this registration stands for. Non-containment — the implementation stays contained in its provider.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Implementation</em>' reference.
	 * @see #setImplementation(ServiceImplementation)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistration_Implementation()
	 * @model required="true"
	 * @generated
	 */
	ServiceImplementation getImplementation();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceRegistration#getImplementation <em>Implementation</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Implementation</em>' reference.
	 * @see #getImplementation()
	 * @generated
	 */
	void setImplementation(ServiceImplementation value);

	/**
	 * Returns the value of the '<em><b>Using Sessions</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ConsumerSession}.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.services.ConsumerSession#getAcquisitions <em>Acquisitions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Derived view of the acquisition relation (ACQUISITION.md par.3): the sessions currently holding a lease on this registration. The OWNING side is ConsumerSession.acquisitions — the lease lifecycle follows the consumer. The usage count is a query (usingSessions size), never stored: stored counters drift on consumer crash. TRANSIENT by design: sessions are runtime state outside the persisted resource — a serialized link would tear every registry save/copy apart (not contained in a resource).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Using Sessions</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistration_UsingSessions()
	 * @see org.eclipse.fennec.services.ConsumerSession#getAcquisitions
	 * @model opposite="acquisitions" transient="true"
	 * @generated
	 */
	EList<ConsumerSession> getUsingSessions();

	/**
	 * Returns the value of the '<em><b>Consumer Count</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Derived usage count (ACQUISITION.md par.9): the number of sessions currently holding a lease. Computed via the OCL setting delegate — never stored (stored counters drift on consumer crash). Runtime-only like the relation it derives from: volatile/transient, requires the Fennec OCL engine to be present; without a registered delegate factory the getter fails rather than lying.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Consumer Count</em>' attribute.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceRegistration_ConsumerCount()
	 * @model transient="true" changeable="false" volatile="true" derived="true"
	 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 derivation='self.usingSessions-&gt;size()'"
	 * @generated
	 */
	int getConsumerCount();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Removes the service from the registry. Fires UNREGISTERING BEFORE removal (OSGi semantics, consumers get one last chance to release the service). Then removes from registry and propagates withdrawal to the Remote Registry asynchronously.
	 * <!-- end-model-doc -->
	 * @model
	 * @generated
	 */
	void unregister();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Replaces the property set on the registration. Fires MODIFIED to listeners whose filter still matches, MODIFIED_ENDMATCH to listeners whose filter no longer matches.
	 * @param props The new property set. Empty list = clear all provider-supplied properties (framework properties remain).
	 * <!-- end-model-doc -->
	 * @model propsMany="true"
	 * @generated
	 */
	void setProperties(EList<Property> props);

} // ServiceRegistration
