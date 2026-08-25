/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Unsatisfied Reference</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Snapshot of a ComponentReference that does not meet its cardinality. Lists candidate target services (may be empty) so operators can see which targets exist but were rejected, e.g. by a target filter.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.UnsatisfiedReference#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.UnsatisfiedReference#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.UnsatisfiedReference#getTargetServices <em>Target Services</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getUnsatisfiedReference()
 * @model
 * @generated
 */
@ProviderType
public interface UnsatisfiedReference extends EObject {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ComponentReference.name this snapshot belongs to.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getUnsatisfiedReference_Name()
	 * @model required="true"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.UnsatisfiedReference#getName <em>Name</em>}' attribute.
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
	 * LDAP target filter in effect.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Target</em>' attribute.
	 * @see #setTarget(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getUnsatisfiedReference_Target()
	 * @model
	 * @generated
	 */
	String getTarget();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.UnsatisfiedReference#getTarget <em>Target</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' attribute.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(String value);

	/**
	 * Returns the value of the '<em><b>Target Services</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceReference}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Services that match the interface but did not satisfy the cardinality / filter / scope. May be empty.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Target Services</em>' reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getUnsatisfiedReference_TargetServices()
	 * @model
	 * @generated
	 */
	EList<ServiceReference> getTargetServices();

} // UnsatisfiedReference
