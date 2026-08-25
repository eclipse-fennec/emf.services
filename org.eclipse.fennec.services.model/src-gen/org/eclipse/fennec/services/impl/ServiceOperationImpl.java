/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.services.Invariant;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterConstraint;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Service Operation</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationImpl#getParameters <em>Parameters</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationImpl#getReturnType <em>Return Type</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationImpl#getReturnConstraints <em>Return Constraints</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationImpl#getExceptions <em>Exceptions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationImpl#getPreconditions <em>Preconditions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.ServiceOperationImpl#getPostconditions <em>Postconditions</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ServiceOperationImpl extends MinimalEObjectImpl.Container implements ServiceOperation {
	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected static final String DESCRIPTION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected String description = DESCRIPTION_EDEFAULT;

	/**
	 * The cached value of the '{@link #getParameters() <em>Parameters</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getParameters()
	 * @generated
	 * @ordered
	 */
	protected EList<Parameter> parameters;

	/**
	 * The default value of the '{@link #getReturnType() <em>Return Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReturnType()
	 * @generated
	 * @ordered
	 */
	protected static final String RETURN_TYPE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getReturnType() <em>Return Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReturnType()
	 * @generated
	 * @ordered
	 */
	protected String returnType = RETURN_TYPE_EDEFAULT;

	/**
	 * The cached value of the '{@link #getReturnConstraints() <em>Return Constraints</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReturnConstraints()
	 * @generated
	 * @ordered
	 */
	protected EList<ParameterConstraint> returnConstraints;

	/**
	 * The cached value of the '{@link #getExceptions() <em>Exceptions</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExceptions()
	 * @generated
	 * @ordered
	 */
	protected EList<ServiceException> exceptions;

	/**
	 * The cached value of the '{@link #getPreconditions() <em>Preconditions</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPreconditions()
	 * @generated
	 * @ordered
	 */
	protected EList<Invariant> preconditions;

	/**
	 * The cached value of the '{@link #getPostconditions() <em>Postconditions</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPostconditions()
	 * @generated
	 * @ordered
	 */
	protected EList<Invariant> postconditions;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServiceOperationImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.SERVICE_OPERATION;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_OPERATION__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDescription(String newDescription) {
		String oldDescription = description;
		description = newDescription;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_OPERATION__DESCRIPTION, oldDescription, description));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Parameter> getParameters() {
		if (parameters == null) {
			parameters = new EObjectContainmentEList<Parameter>(Parameter.class, this, ServicesPackage.SERVICE_OPERATION__PARAMETERS);
		}
		return parameters;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getReturnType() {
		return returnType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setReturnType(String newReturnType) {
		String oldReturnType = returnType;
		returnType = newReturnType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.SERVICE_OPERATION__RETURN_TYPE, oldReturnType, returnType));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ParameterConstraint> getReturnConstraints() {
		if (returnConstraints == null) {
			returnConstraints = new EObjectContainmentEList<ParameterConstraint>(ParameterConstraint.class, this, ServicesPackage.SERVICE_OPERATION__RETURN_CONSTRAINTS);
		}
		return returnConstraints;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ServiceException> getExceptions() {
		if (exceptions == null) {
			exceptions = new EObjectResolvingEList<ServiceException>(ServiceException.class, this, ServicesPackage.SERVICE_OPERATION__EXCEPTIONS);
		}
		return exceptions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Invariant> getPreconditions() {
		if (preconditions == null) {
			preconditions = new EObjectContainmentEList<Invariant>(Invariant.class, this, ServicesPackage.SERVICE_OPERATION__PRECONDITIONS);
		}
		return preconditions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Invariant> getPostconditions() {
		if (postconditions == null) {
			postconditions = new EObjectContainmentEList<Invariant>(Invariant.class, this, ServicesPackage.SERVICE_OPERATION__POSTCONDITIONS);
		}
		return postconditions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.SERVICE_OPERATION__PARAMETERS:
				return ((InternalEList<?>)getParameters()).basicRemove(otherEnd, msgs);
			case ServicesPackage.SERVICE_OPERATION__RETURN_CONSTRAINTS:
				return ((InternalEList<?>)getReturnConstraints()).basicRemove(otherEnd, msgs);
			case ServicesPackage.SERVICE_OPERATION__PRECONDITIONS:
				return ((InternalEList<?>)getPreconditions()).basicRemove(otherEnd, msgs);
			case ServicesPackage.SERVICE_OPERATION__POSTCONDITIONS:
				return ((InternalEList<?>)getPostconditions()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.SERVICE_OPERATION__NAME:
				return getName();
			case ServicesPackage.SERVICE_OPERATION__DESCRIPTION:
				return getDescription();
			case ServicesPackage.SERVICE_OPERATION__PARAMETERS:
				return getParameters();
			case ServicesPackage.SERVICE_OPERATION__RETURN_TYPE:
				return getReturnType();
			case ServicesPackage.SERVICE_OPERATION__RETURN_CONSTRAINTS:
				return getReturnConstraints();
			case ServicesPackage.SERVICE_OPERATION__EXCEPTIONS:
				return getExceptions();
			case ServicesPackage.SERVICE_OPERATION__PRECONDITIONS:
				return getPreconditions();
			case ServicesPackage.SERVICE_OPERATION__POSTCONDITIONS:
				return getPostconditions();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case ServicesPackage.SERVICE_OPERATION__NAME:
				setName((String)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION__DESCRIPTION:
				setDescription((String)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION__PARAMETERS:
				getParameters().clear();
				getParameters().addAll((Collection<? extends Parameter>)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION__RETURN_TYPE:
				setReturnType((String)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION__RETURN_CONSTRAINTS:
				getReturnConstraints().clear();
				getReturnConstraints().addAll((Collection<? extends ParameterConstraint>)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION__EXCEPTIONS:
				getExceptions().clear();
				getExceptions().addAll((Collection<? extends ServiceException>)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION__PRECONDITIONS:
				getPreconditions().clear();
				getPreconditions().addAll((Collection<? extends Invariant>)newValue);
				return;
			case ServicesPackage.SERVICE_OPERATION__POSTCONDITIONS:
				getPostconditions().clear();
				getPostconditions().addAll((Collection<? extends Invariant>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case ServicesPackage.SERVICE_OPERATION__NAME:
				setName(NAME_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_OPERATION__DESCRIPTION:
				setDescription(DESCRIPTION_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_OPERATION__PARAMETERS:
				getParameters().clear();
				return;
			case ServicesPackage.SERVICE_OPERATION__RETURN_TYPE:
				setReturnType(RETURN_TYPE_EDEFAULT);
				return;
			case ServicesPackage.SERVICE_OPERATION__RETURN_CONSTRAINTS:
				getReturnConstraints().clear();
				return;
			case ServicesPackage.SERVICE_OPERATION__EXCEPTIONS:
				getExceptions().clear();
				return;
			case ServicesPackage.SERVICE_OPERATION__PRECONDITIONS:
				getPreconditions().clear();
				return;
			case ServicesPackage.SERVICE_OPERATION__POSTCONDITIONS:
				getPostconditions().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case ServicesPackage.SERVICE_OPERATION__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case ServicesPackage.SERVICE_OPERATION__DESCRIPTION:
				return DESCRIPTION_EDEFAULT == null ? description != null : !DESCRIPTION_EDEFAULT.equals(description);
			case ServicesPackage.SERVICE_OPERATION__PARAMETERS:
				return parameters != null && !parameters.isEmpty();
			case ServicesPackage.SERVICE_OPERATION__RETURN_TYPE:
				return RETURN_TYPE_EDEFAULT == null ? returnType != null : !RETURN_TYPE_EDEFAULT.equals(returnType);
			case ServicesPackage.SERVICE_OPERATION__RETURN_CONSTRAINTS:
				return returnConstraints != null && !returnConstraints.isEmpty();
			case ServicesPackage.SERVICE_OPERATION__EXCEPTIONS:
				return exceptions != null && !exceptions.isEmpty();
			case ServicesPackage.SERVICE_OPERATION__PRECONDITIONS:
				return preconditions != null && !preconditions.isEmpty();
			case ServicesPackage.SERVICE_OPERATION__POSTCONDITIONS:
				return postconditions != null && !postconditions.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(", description: ");
		result.append(description);
		result.append(", returnType: ");
		result.append(returnType);
		result.append(')');
		return result.toString();
	}

} //ServiceOperationImpl
