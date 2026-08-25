/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Lifecycle Hook</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Language-neutral handle on a lifecycle callback. Replaces OSGi DS activate/deactivate/modified/activationFields/init annotations. The framework invokes the implementation method whose name matches 'name' at the appropriate lifecycle point.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.LifecycleHook#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.LifecycleHook#getParameter <em>Parameter</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getLifecycleHook()
 * @model
 * @generated
 */
@ProviderType
public interface LifecycleHook extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Kind</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.LifecycleHookKind}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Which lifecycle event triggers this hook.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.LifecycleHookKind
	 * @see #setKind(LifecycleHookKind)
	 * @see org.eclipse.fennec.services.ServicesPackage#getLifecycleHook_Kind()
	 * @model required="true"
	 * @generated
	 */
	LifecycleHookKind getKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.LifecycleHook#getKind <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Kind</em>' attribute.
	 * @see org.eclipse.fennec.services.LifecycleHookKind
	 * @see #getKind()
	 * @generated
	 */
	void setKind(LifecycleHookKind value);

	/**
	 * Returns the value of the '<em><b>Parameter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Zero-based constructor parameter index, only meaningful when kind is part of constructor injection (init in DS 1.4).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Parameter</em>' attribute.
	 * @see #setParameter(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getLifecycleHook_Parameter()
	 * @model
	 * @generated
	 */
	int getParameter();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.LifecycleHook#getParameter <em>Parameter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Parameter</em>' attribute.
	 * @see #getParameter()
	 * @generated
	 */
	void setParameter(int value);

} // LifecycleHook
