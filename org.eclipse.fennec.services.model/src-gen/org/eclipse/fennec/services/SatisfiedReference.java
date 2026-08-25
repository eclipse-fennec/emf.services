/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Satisfied Reference</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Snapshot of a satisfied ComponentReference at runtime: the declared reference name plus the actual ServiceReferences currently bound.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.SatisfiedReference#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.SatisfiedReference#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.SatisfiedReference#getBoundServices <em>Bound Services</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getSatisfiedReference()
 * @model
 * @generated
 */
@ProviderType
public interface SatisfiedReference extends EObject {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ComponentReference.name this snapshot belongs to.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getSatisfiedReference_Name()
	 * @model required="true"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.SatisfiedReference#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Target</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * LDAP target filter that was effective at binding time.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Target</em>' attribute.
	 * @see #setTarget(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getSatisfiedReference_Target()
	 * @model
	 * @generated
	 */
	String getTarget();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.SatisfiedReference#getTarget <em>Target</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' attribute.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(String value);

	/**
	 * Returns the value of the '<em><b>Bound Services</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceReference}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Currently bound ServiceReferences.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Bound Services</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getSatisfiedReference_BoundServices()
	 * @model
	 * @generated
	 */
	EList<ServiceReference> getBoundServices();

} // SatisfiedReference
