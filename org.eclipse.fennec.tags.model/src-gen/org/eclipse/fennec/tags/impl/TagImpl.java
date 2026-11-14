/*
 */
package org.eclipse.fennec.tags.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.tags.Tag;
import org.eclipse.fennec.tags.TagsPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Tag</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagImpl#getId <em>Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagImpl#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagImpl#isSelfAssignable <em>Self Assignable</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagImpl#getFormerPaths <em>Former Paths</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagImpl#getChildren <em>Children</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagImpl#getParent <em>Parent</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TagImpl extends MinimalEObjectImpl.Container implements Tag {
	/**
	 * The default value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected static final String ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected String id = ID_EDEFAULT;

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
	 * The cached setting delegate for the '{@link #getPath() <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPath()
	 * @generated
	 * @ordered
	 */
	protected EStructuralFeature.Internal.SettingDelegate PATH__ESETTING_DELEGATE = ((EStructuralFeature.Internal)TagsPackage.Literals.TAG__PATH).getSettingDelegate();

	/**
	 * The default value of the '{@link #isSelfAssignable() <em>Self Assignable</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSelfAssignable()
	 * @generated
	 * @ordered
	 */
	protected static final boolean SELF_ASSIGNABLE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isSelfAssignable() <em>Self Assignable</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSelfAssignable()
	 * @generated
	 * @ordered
	 */
	protected boolean selfAssignable = SELF_ASSIGNABLE_EDEFAULT;

	/**
	 * The cached value of the '{@link #getFormerPaths() <em>Former Paths</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFormerPaths()
	 * @generated
	 * @ordered
	 */
	protected EList<String> formerPaths;

	/**
	 * The cached value of the '{@link #getChildren() <em>Children</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChildren()
	 * @generated
	 * @ordered
	 */
	protected EList<Tag> children;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TagImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return TagsPackage.Literals.TAG;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setId(String newId) {
		String oldId = id;
		id = newId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG__ID, oldId, id));
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
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG__NAME, oldName, name));
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
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG__DESCRIPTION, oldDescription, description));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getPath() {
		return (String)PATH__ESETTING_DELEGATE.dynamicGet(this, null, 0, true, false);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isSelfAssignable() {
		return selfAssignable;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSelfAssignable(boolean newSelfAssignable) {
		boolean oldSelfAssignable = selfAssignable;
		selfAssignable = newSelfAssignable;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG__SELF_ASSIGNABLE, oldSelfAssignable, selfAssignable));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getFormerPaths() {
		if (formerPaths == null) {
			formerPaths = new EDataTypeUniqueEList<String>(String.class, this, TagsPackage.TAG__FORMER_PATHS);
		}
		return formerPaths;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Tag> getChildren() {
		if (children == null) {
			children = new EObjectContainmentWithInverseEList<Tag>(Tag.class, this, TagsPackage.TAG__CHILDREN, TagsPackage.TAG__PARENT);
		}
		return children;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Tag getParent() {
		if (eContainerFeatureID() != TagsPackage.TAG__PARENT) return null;
		return (Tag)eInternalContainer();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetParent(Tag newParent, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newParent, TagsPackage.TAG__PARENT, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setParent(Tag newParent) {
		if (newParent != eInternalContainer() || (eContainerFeatureID() != TagsPackage.TAG__PARENT && newParent != null)) {
			if (EcoreUtil.isAncestor(this, newParent))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newParent != null)
				msgs = ((InternalEObject)newParent).eInverseAdd(this, TagsPackage.TAG__CHILDREN, Tag.class, msgs);
			msgs = basicSetParent(newParent, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TagsPackage.TAG__PARENT, newParent, newParent));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case TagsPackage.TAG__CHILDREN:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getChildren()).basicAdd(otherEnd, msgs);
			case TagsPackage.TAG__PARENT:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetParent((Tag)otherEnd, msgs);
		}
		return super.eInverseAdd(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case TagsPackage.TAG__CHILDREN:
				return ((InternalEList<?>)getChildren()).basicRemove(otherEnd, msgs);
			case TagsPackage.TAG__PARENT:
				return basicSetParent(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eBasicRemoveFromContainerFeature(NotificationChain msgs) {
		switch (eContainerFeatureID()) {
			case TagsPackage.TAG__PARENT:
				return eInternalContainer().eInverseRemove(this, TagsPackage.TAG__CHILDREN, Tag.class, msgs);
		}
		return super.eBasicRemoveFromContainerFeature(msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case TagsPackage.TAG__ID:
				return getId();
			case TagsPackage.TAG__NAME:
				return getName();
			case TagsPackage.TAG__DESCRIPTION:
				return getDescription();
			case TagsPackage.TAG__PATH:
				return getPath();
			case TagsPackage.TAG__SELF_ASSIGNABLE:
				return isSelfAssignable();
			case TagsPackage.TAG__FORMER_PATHS:
				return getFormerPaths();
			case TagsPackage.TAG__CHILDREN:
				return getChildren();
			case TagsPackage.TAG__PARENT:
				return getParent();
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
			case TagsPackage.TAG__ID:
				setId((String)newValue);
				return;
			case TagsPackage.TAG__NAME:
				setName((String)newValue);
				return;
			case TagsPackage.TAG__DESCRIPTION:
				setDescription((String)newValue);
				return;
			case TagsPackage.TAG__SELF_ASSIGNABLE:
				setSelfAssignable((Boolean)newValue);
				return;
			case TagsPackage.TAG__FORMER_PATHS:
				getFormerPaths().clear();
				getFormerPaths().addAll((Collection<? extends String>)newValue);
				return;
			case TagsPackage.TAG__CHILDREN:
				getChildren().clear();
				getChildren().addAll((Collection<? extends Tag>)newValue);
				return;
			case TagsPackage.TAG__PARENT:
				setParent((Tag)newValue);
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
			case TagsPackage.TAG__ID:
				setId(ID_EDEFAULT);
				return;
			case TagsPackage.TAG__NAME:
				setName(NAME_EDEFAULT);
				return;
			case TagsPackage.TAG__DESCRIPTION:
				setDescription(DESCRIPTION_EDEFAULT);
				return;
			case TagsPackage.TAG__SELF_ASSIGNABLE:
				setSelfAssignable(SELF_ASSIGNABLE_EDEFAULT);
				return;
			case TagsPackage.TAG__FORMER_PATHS:
				getFormerPaths().clear();
				return;
			case TagsPackage.TAG__CHILDREN:
				getChildren().clear();
				return;
			case TagsPackage.TAG__PARENT:
				setParent((Tag)null);
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
			case TagsPackage.TAG__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case TagsPackage.TAG__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case TagsPackage.TAG__DESCRIPTION:
				return DESCRIPTION_EDEFAULT == null ? description != null : !DESCRIPTION_EDEFAULT.equals(description);
			case TagsPackage.TAG__PATH:
				return PATH__ESETTING_DELEGATE.dynamicIsSet(this, null, 0);
			case TagsPackage.TAG__SELF_ASSIGNABLE:
				return selfAssignable != SELF_ASSIGNABLE_EDEFAULT;
			case TagsPackage.TAG__FORMER_PATHS:
				return formerPaths != null && !formerPaths.isEmpty();
			case TagsPackage.TAG__CHILDREN:
				return children != null && !children.isEmpty();
			case TagsPackage.TAG__PARENT:
				return getParent() != null;
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
		result.append(" (id: ");
		result.append(id);
		result.append(", name: ");
		result.append(name);
		result.append(", description: ");
		result.append(description);
		result.append(", selfAssignable: ");
		result.append(selfAssignable);
		result.append(", formerPaths: ");
		result.append(formerPaths);
		result.append(')');
		return result.toString();
	}

} //TagImpl
