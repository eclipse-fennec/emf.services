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

import org.eclipse.fennec.tags.Tag;
import org.eclipse.fennec.tags.TagAssignment;
import org.eclipse.fennec.tags.TagTarget;
import org.eclipse.fennec.tags.TagsPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Tag Assignment</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagAssignmentImpl#getTag <em>Tag</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagAssignmentImpl#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagAssignmentImpl#getAssignedBy <em>Assigned By</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagAssignmentImpl#getAssignedAt <em>Assigned At</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TagAssignmentImpl extends MinimalEObjectImpl.Container implements TagAssignment {
	/**
	 * The cached value of the '{@link #getTag() <em>Tag</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTag()
	 * @generated
	 * @ordered
	 */
	protected Tag tag;

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
	 * The default value of the '{@link #getAssignedBy() <em>Assigned By</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAssignedBy()
	 * @generated
	 * @ordered
	 */
	protected static final String ASSIGNED_BY_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getAssignedBy() <em>Assigned By</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAssignedBy()
	 * @generated
	 * @ordered
	 */
	protected String assignedBy = ASSIGNED_BY_EDEFAULT;

	/**
	 * The default value of the '{@link #getAssignedAt() <em>Assigned At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAssignedAt()
	 * @generated
	 * @ordered
	 */
	protected static final Date ASSIGNED_AT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getAssignedAt() <em>Assigned At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAssignedAt()
	 * @generated
	 * @ordered
	 */
	protected Date assignedAt = ASSIGNED_AT_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TagAssignmentImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return TagsPackage.Literals.TAG_ASSIGNMENT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Tag getTag() {
		if (tag != null && tag.eIsProxy()) {
			InternalEObject oldTag = (InternalEObject)tag;
			tag = (Tag)eResolveProxy(oldTag);
			if (tag != oldTag) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, TagsPackage.TAG_ASSIGNMENT__TAG, oldTag, tag));
			}
		}
		return tag;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Tag basicGetTag() {
		return tag;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTag(Tag newTag) {
		Tag oldTag = tag;
		tag = newTag;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_ASSIGNMENT__TAG, oldTag, tag));
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
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_ASSIGNMENT__TARGET, oldTarget, newTarget);
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
				msgs = ((InternalEObject)target).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - TagsPackage.TAG_ASSIGNMENT__TARGET, null, msgs);
			if (newTarget != null)
				msgs = ((InternalEObject)newTarget).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - TagsPackage.TAG_ASSIGNMENT__TARGET, null, msgs);
			msgs = basicSetTarget(newTarget, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_ASSIGNMENT__TARGET, newTarget, newTarget));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getAssignedBy() {
		return assignedBy;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAssignedBy(String newAssignedBy) {
		String oldAssignedBy = assignedBy;
		assignedBy = newAssignedBy;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_ASSIGNMENT__ASSIGNED_BY, oldAssignedBy, assignedBy));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Date getAssignedAt() {
		return assignedAt;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAssignedAt(Date newAssignedAt) {
		Date oldAssignedAt = assignedAt;
		assignedAt = newAssignedAt;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_ASSIGNMENT__ASSIGNED_AT, oldAssignedAt, assignedAt));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case TagsPackage.TAG_ASSIGNMENT__TARGET:
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
			case TagsPackage.TAG_ASSIGNMENT__TAG:
				if (resolve) return getTag();
				return basicGetTag();
			case TagsPackage.TAG_ASSIGNMENT__TARGET:
				return getTarget();
			case TagsPackage.TAG_ASSIGNMENT__ASSIGNED_BY:
				return getAssignedBy();
			case TagsPackage.TAG_ASSIGNMENT__ASSIGNED_AT:
				return getAssignedAt();
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
			case TagsPackage.TAG_ASSIGNMENT__TAG:
				setTag((Tag)newValue);
				return;
			case TagsPackage.TAG_ASSIGNMENT__TARGET:
				setTarget((TagTarget)newValue);
				return;
			case TagsPackage.TAG_ASSIGNMENT__ASSIGNED_BY:
				setAssignedBy((String)newValue);
				return;
			case TagsPackage.TAG_ASSIGNMENT__ASSIGNED_AT:
				setAssignedAt((Date)newValue);
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
			case TagsPackage.TAG_ASSIGNMENT__TAG:
				setTag((Tag)null);
				return;
			case TagsPackage.TAG_ASSIGNMENT__TARGET:
				setTarget((TagTarget)null);
				return;
			case TagsPackage.TAG_ASSIGNMENT__ASSIGNED_BY:
				setAssignedBy(ASSIGNED_BY_EDEFAULT);
				return;
			case TagsPackage.TAG_ASSIGNMENT__ASSIGNED_AT:
				setAssignedAt(ASSIGNED_AT_EDEFAULT);
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
			case TagsPackage.TAG_ASSIGNMENT__TAG:
				return tag != null;
			case TagsPackage.TAG_ASSIGNMENT__TARGET:
				return target != null;
			case TagsPackage.TAG_ASSIGNMENT__ASSIGNED_BY:
				return ASSIGNED_BY_EDEFAULT == null ? assignedBy != null : !ASSIGNED_BY_EDEFAULT.equals(assignedBy);
			case TagsPackage.TAG_ASSIGNMENT__ASSIGNED_AT:
				return ASSIGNED_AT_EDEFAULT == null ? assignedAt != null : !ASSIGNED_AT_EDEFAULT.equals(assignedAt);
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
		result.append(" (assignedBy: ");
		result.append(assignedBy);
		result.append(", assignedAt: ");
		result.append(assignedAt);
		result.append(')');
		return result.toString();
	}

} //TagAssignmentImpl
