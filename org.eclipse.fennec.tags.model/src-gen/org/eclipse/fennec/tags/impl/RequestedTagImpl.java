/*
 */
package org.eclipse.fennec.tags.impl;

import java.util.Date;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.fennec.tags.RequestedTag;
import org.eclipse.fennec.tags.TagTarget;
import org.eclipse.fennec.tags.TagsPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Requested Tag</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.impl.RequestedTagImpl#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.RequestedTagImpl#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.RequestedTagImpl#getRequestedBy <em>Requested By</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.RequestedTagImpl#getRequestedAt <em>Requested At</em>}</li>
 * </ul>
 *
 * @generated
 */
public class RequestedTagImpl extends MinimalEObjectImpl.Container implements RequestedTag {
	/**
	 * The default value of the '{@link #getPath() <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPath()
	 * @generated
	 * @ordered
	 */
	protected static final String PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPath() <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPath()
	 * @generated
	 * @ordered
	 */
	protected String path = PATH_EDEFAULT;

	/**
	 * The cached value of the '{@link #getTarget() <em>Target</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTarget()
	 * @generated
	 * @ordered
	 */
	protected TagTarget target;

	/**
	 * The default value of the '{@link #getRequestedBy() <em>Requested By</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRequestedBy()
	 * @generated
	 * @ordered
	 */
	protected static final String REQUESTED_BY_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRequestedBy() <em>Requested By</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRequestedBy()
	 * @generated
	 * @ordered
	 */
	protected String requestedBy = REQUESTED_BY_EDEFAULT;

	/**
	 * The default value of the '{@link #getRequestedAt() <em>Requested At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRequestedAt()
	 * @generated
	 * @ordered
	 */
	protected static final Date REQUESTED_AT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRequestedAt() <em>Requested At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRequestedAt()
	 * @generated
	 * @ordered
	 */
	protected Date requestedAt = REQUESTED_AT_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected RequestedTagImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return TagsPackage.Literals.REQUESTED_TAG;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getPath() {
		return path;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPath(String newPath) {
		String oldPath = path;
		path = newPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.REQUESTED_TAG__PATH, oldPath, path));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TagTarget getTarget() {
		return target;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetTarget(TagTarget newTarget, NotificationChain msgs) {
		TagTarget oldTarget = target;
		target = newTarget;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, TagsPackage.REQUESTED_TAG__TARGET, oldTarget, newTarget);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTarget(TagTarget newTarget) {
		if (newTarget != target) {
			NotificationChain msgs = null;
			if (target != null)
				msgs = ((InternalEObject)target).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - TagsPackage.REQUESTED_TAG__TARGET, null, msgs);
			if (newTarget != null)
				msgs = ((InternalEObject)newTarget).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - TagsPackage.REQUESTED_TAG__TARGET, null, msgs);
			msgs = basicSetTarget(newTarget, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.REQUESTED_TAG__TARGET, newTarget, newTarget));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRequestedBy() {
		return requestedBy;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRequestedBy(String newRequestedBy) {
		String oldRequestedBy = requestedBy;
		requestedBy = newRequestedBy;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.REQUESTED_TAG__REQUESTED_BY, oldRequestedBy, requestedBy));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Date getRequestedAt() {
		return requestedAt;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRequestedAt(Date newRequestedAt) {
		Date oldRequestedAt = requestedAt;
		requestedAt = newRequestedAt;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.REQUESTED_TAG__REQUESTED_AT, oldRequestedAt, requestedAt));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case TagsPackage.REQUESTED_TAG__TARGET:
				return basicSetTarget(null, msgs);
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
			case TagsPackage.REQUESTED_TAG__PATH:
				return getPath();
			case TagsPackage.REQUESTED_TAG__TARGET:
				return getTarget();
			case TagsPackage.REQUESTED_TAG__REQUESTED_BY:
				return getRequestedBy();
			case TagsPackage.REQUESTED_TAG__REQUESTED_AT:
				return getRequestedAt();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case TagsPackage.REQUESTED_TAG__PATH:
				setPath((String)newValue);
				return;
			case TagsPackage.REQUESTED_TAG__TARGET:
				setTarget((TagTarget)newValue);
				return;
			case TagsPackage.REQUESTED_TAG__REQUESTED_BY:
				setRequestedBy((String)newValue);
				return;
			case TagsPackage.REQUESTED_TAG__REQUESTED_AT:
				setRequestedAt((Date)newValue);
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
			case TagsPackage.REQUESTED_TAG__PATH:
				setPath(PATH_EDEFAULT);
				return;
			case TagsPackage.REQUESTED_TAG__TARGET:
				setTarget((TagTarget)null);
				return;
			case TagsPackage.REQUESTED_TAG__REQUESTED_BY:
				setRequestedBy(REQUESTED_BY_EDEFAULT);
				return;
			case TagsPackage.REQUESTED_TAG__REQUESTED_AT:
				setRequestedAt(REQUESTED_AT_EDEFAULT);
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
			case TagsPackage.REQUESTED_TAG__PATH:
				return PATH_EDEFAULT == null ? path != null : !PATH_EDEFAULT.equals(path);
			case TagsPackage.REQUESTED_TAG__TARGET:
				return target != null;
			case TagsPackage.REQUESTED_TAG__REQUESTED_BY:
				return REQUESTED_BY_EDEFAULT == null ? requestedBy != null : !REQUESTED_BY_EDEFAULT.equals(requestedBy);
			case TagsPackage.REQUESTED_TAG__REQUESTED_AT:
				return REQUESTED_AT_EDEFAULT == null ? requestedAt != null : !REQUESTED_AT_EDEFAULT.equals(requestedAt);
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
		result.append(" (path: ");
		result.append(path);
		result.append(", requestedBy: ");
		result.append(requestedBy);
		result.append(", requestedAt: ");
		result.append(requestedAt);
		result.append(')');
		return result.toString();
	}

} //RequestedTagImpl
