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

import org.eclipse.fennec.tags.TagChange;
import org.eclipse.fennec.tags.TagChangeKind;
import org.eclipse.fennec.tags.TagTarget;
import org.eclipse.fennec.tags.TagsPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Tag Change</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagChangeImpl#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagChangeImpl#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagChangeImpl#getFormerPath <em>Former Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagChangeImpl#getTarget <em>Target</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagChangeImpl#getRuleKind <em>Rule Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagChangeImpl#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagChangeImpl#getAt <em>At</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TagChangeImpl extends MinimalEObjectImpl.Container implements TagChange {
	/**
	 * The default value of the '{@link #getKind() <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getKind()
	 * @generated
	 * @ordered
	 */
	protected static final TagChangeKind KIND_EDEFAULT = TagChangeKind.TAG_PUT;

	/**
	 * The cached value of the '{@link #getKind() <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getKind()
	 * @generated
	 * @ordered
	 */
	protected TagChangeKind kind = KIND_EDEFAULT;

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
	 * The default value of the '{@link #getFormerPath() <em>Former Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFormerPath()
	 * @generated
	 * @ordered
	 */
	protected static final String FORMER_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getFormerPath() <em>Former Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFormerPath()
	 * @generated
	 * @ordered
	 */
	protected String formerPath = FORMER_PATH_EDEFAULT;

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
	 * The default value of the '{@link #getRuleKind() <em>Rule Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRuleKind()
	 * @generated
	 * @ordered
	 */
	protected static final String RULE_KIND_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRuleKind() <em>Rule Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRuleKind()
	 * @generated
	 * @ordered
	 */
	protected String ruleKind = RULE_KIND_EDEFAULT;

	/**
	 * The default value of the '{@link #getFilter() <em>Filter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFilter()
	 * @generated
	 * @ordered
	 */
	protected static final String FILTER_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getFilter() <em>Filter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFilter()
	 * @generated
	 * @ordered
	 */
	protected String filter = FILTER_EDEFAULT;

	/**
	 * The default value of the '{@link #getAt() <em>At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAt()
	 * @generated
	 * @ordered
	 */
	protected static final Date AT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getAt() <em>At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAt()
	 * @generated
	 * @ordered
	 */
	protected Date at = AT_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TagChangeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return TagsPackage.Literals.TAG_CHANGE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TagChangeKind getKind() {
		return kind;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setKind(TagChangeKind newKind) {
		TagChangeKind oldKind = kind;
		kind = newKind == null ? KIND_EDEFAULT : newKind;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_CHANGE__KIND, oldKind, kind));
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
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_CHANGE__PATH, oldPath, path));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getFormerPath() {
		return formerPath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFormerPath(String newFormerPath) {
		String oldFormerPath = formerPath;
		formerPath = newFormerPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_CHANGE__FORMER_PATH, oldFormerPath, formerPath));
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
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_CHANGE__TARGET, oldTarget, newTarget);
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
				msgs = ((InternalEObject)target).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - TagsPackage.TAG_CHANGE__TARGET, null, msgs);
			if (newTarget != null)
				msgs = ((InternalEObject)newTarget).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - TagsPackage.TAG_CHANGE__TARGET, null, msgs);
			msgs = basicSetTarget(newTarget, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_CHANGE__TARGET, newTarget, newTarget));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRuleKind() {
		return ruleKind;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRuleKind(String newRuleKind) {
		String oldRuleKind = ruleKind;
		ruleKind = newRuleKind;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_CHANGE__RULE_KIND, oldRuleKind, ruleKind));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getFilter() {
		return filter;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFilter(String newFilter) {
		String oldFilter = filter;
		filter = newFilter;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_CHANGE__FILTER, oldFilter, filter));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Date getAt() {
		return at;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAt(Date newAt) {
		Date oldAt = at;
		at = newAt;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG_CHANGE__AT, oldAt, at));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case TagsPackage.TAG_CHANGE__TARGET:
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
			case TagsPackage.TAG_CHANGE__KIND:
				return getKind();
			case TagsPackage.TAG_CHANGE__PATH:
				return getPath();
			case TagsPackage.TAG_CHANGE__FORMER_PATH:
				return getFormerPath();
			case TagsPackage.TAG_CHANGE__TARGET:
				return getTarget();
			case TagsPackage.TAG_CHANGE__RULE_KIND:
				return getRuleKind();
			case TagsPackage.TAG_CHANGE__FILTER:
				return getFilter();
			case TagsPackage.TAG_CHANGE__AT:
				return getAt();
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
			case TagsPackage.TAG_CHANGE__KIND:
				setKind((TagChangeKind)newValue);
				return;
			case TagsPackage.TAG_CHANGE__PATH:
				setPath((String)newValue);
				return;
			case TagsPackage.TAG_CHANGE__FORMER_PATH:
				setFormerPath((String)newValue);
				return;
			case TagsPackage.TAG_CHANGE__TARGET:
				setTarget((TagTarget)newValue);
				return;
			case TagsPackage.TAG_CHANGE__RULE_KIND:
				setRuleKind((String)newValue);
				return;
			case TagsPackage.TAG_CHANGE__FILTER:
				setFilter((String)newValue);
				return;
			case TagsPackage.TAG_CHANGE__AT:
				setAt((Date)newValue);
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
			case TagsPackage.TAG_CHANGE__KIND:
				setKind(KIND_EDEFAULT);
				return;
			case TagsPackage.TAG_CHANGE__PATH:
				setPath(PATH_EDEFAULT);
				return;
			case TagsPackage.TAG_CHANGE__FORMER_PATH:
				setFormerPath(FORMER_PATH_EDEFAULT);
				return;
			case TagsPackage.TAG_CHANGE__TARGET:
				setTarget((TagTarget)null);
				return;
			case TagsPackage.TAG_CHANGE__RULE_KIND:
				setRuleKind(RULE_KIND_EDEFAULT);
				return;
			case TagsPackage.TAG_CHANGE__FILTER:
				setFilter(FILTER_EDEFAULT);
				return;
			case TagsPackage.TAG_CHANGE__AT:
				setAt(AT_EDEFAULT);
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
			case TagsPackage.TAG_CHANGE__KIND:
				return kind != KIND_EDEFAULT;
			case TagsPackage.TAG_CHANGE__PATH:
				return PATH_EDEFAULT == null ? path != null : !PATH_EDEFAULT.equals(path);
			case TagsPackage.TAG_CHANGE__FORMER_PATH:
				return FORMER_PATH_EDEFAULT == null ? formerPath != null : !FORMER_PATH_EDEFAULT.equals(formerPath);
			case TagsPackage.TAG_CHANGE__TARGET:
				return target != null;
			case TagsPackage.TAG_CHANGE__RULE_KIND:
				return RULE_KIND_EDEFAULT == null ? ruleKind != null : !RULE_KIND_EDEFAULT.equals(ruleKind);
			case TagsPackage.TAG_CHANGE__FILTER:
				return FILTER_EDEFAULT == null ? filter != null : !FILTER_EDEFAULT.equals(filter);
			case TagsPackage.TAG_CHANGE__AT:
				return AT_EDEFAULT == null ? at != null : !AT_EDEFAULT.equals(at);
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
		result.append(" (kind: ");
		result.append(kind);
		result.append(", path: ");
		result.append(path);
		result.append(", formerPath: ");
		result.append(formerPath);
		result.append(", ruleKind: ");
		result.append(ruleKind);
		result.append(", filter: ");
		result.append(filter);
		result.append(", at: ");
		result.append(at);
		result.append(')');
		return result.toString();
	}

} //TagChangeImpl
