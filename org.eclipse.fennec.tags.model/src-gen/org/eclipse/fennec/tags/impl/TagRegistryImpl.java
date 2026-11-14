/*
 */
package org.eclipse.fennec.tags.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.tags.RequestedTag;
import org.eclipse.fennec.tags.Tag;
import org.eclipse.fennec.tags.TagAssignment;
import org.eclipse.fennec.tags.TagRegistry;
import org.eclipse.fennec.tags.TagRule;
import org.eclipse.fennec.tags.TagsPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Tag Registry</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagRegistryImpl#getTags <em>Tags</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagRegistryImpl#getAssignments <em>Assignments</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagRegistryImpl#getRules <em>Rules</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.impl.TagRegistryImpl#getRequests <em>Requests</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TagRegistryImpl extends MinimalEObjectImpl.Container implements TagRegistry {
	/**
	 * The cached value of the '{@link #getTags() <em>Tags</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTags()
	 * @generated
	 * @ordered
	 */
	protected EList<Tag> tags;

	/**
	 * The cached value of the '{@link #getAssignments() <em>Assignments</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAssignments()
	 * @generated
	 * @ordered
	 */
	protected EList<TagAssignment> assignments;

	/**
	 * The cached value of the '{@link #getRules() <em>Rules</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRules()
	 * @generated
	 * @ordered
	 */
	protected EList<TagRule> rules;

	/**
	 * The cached value of the '{@link #getRequests() <em>Requests</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRequests()
	 * @generated
	 * @ordered
	 */
	protected EList<RequestedTag> requests;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TagRegistryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return TagsPackage.Literals.TAG_REGISTRY;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Tag> getTags() {
		if (tags == null) {
			tags = new EObjectContainmentEList<Tag>(Tag.class, this, TagsPackage.TAG_REGISTRY__TAGS);
		}
		return tags;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<TagAssignment> getAssignments() {
		if (assignments == null) {
			assignments = new EObjectContainmentEList<TagAssignment>(TagAssignment.class, this, TagsPackage.TAG_REGISTRY__ASSIGNMENTS);
		}
		return assignments;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<TagRule> getRules() {
		if (rules == null) {
			rules = new EObjectContainmentEList<TagRule>(TagRule.class, this, TagsPackage.TAG_REGISTRY__RULES);
		}
		return rules;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<RequestedTag> getRequests() {
		if (requests == null) {
			requests = new EObjectContainmentEList<RequestedTag>(RequestedTag.class, this, TagsPackage.TAG_REGISTRY__REQUESTS);
		}
		return requests;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case TagsPackage.TAG_REGISTRY__TAGS:
				return ((InternalEList<?>)getTags()).basicRemove(otherEnd, msgs);
			case TagsPackage.TAG_REGISTRY__ASSIGNMENTS:
				return ((InternalEList<?>)getAssignments()).basicRemove(otherEnd, msgs);
			case TagsPackage.TAG_REGISTRY__RULES:
				return ((InternalEList<?>)getRules()).basicRemove(otherEnd, msgs);
			case TagsPackage.TAG_REGISTRY__REQUESTS:
				return ((InternalEList<?>)getRequests()).basicRemove(otherEnd, msgs);
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
			case TagsPackage.TAG_REGISTRY__TAGS:
				return getTags();
			case TagsPackage.TAG_REGISTRY__ASSIGNMENTS:
				return getAssignments();
			case TagsPackage.TAG_REGISTRY__RULES:
				return getRules();
			case TagsPackage.TAG_REGISTRY__REQUESTS:
				return getRequests();
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
			case TagsPackage.TAG_REGISTRY__TAGS:
				getTags().clear();
				getTags().addAll((Collection<? extends Tag>)newValue);
				return;
			case TagsPackage.TAG_REGISTRY__ASSIGNMENTS:
				getAssignments().clear();
				getAssignments().addAll((Collection<? extends TagAssignment>)newValue);
				return;
			case TagsPackage.TAG_REGISTRY__RULES:
				getRules().clear();
				getRules().addAll((Collection<? extends TagRule>)newValue);
				return;
			case TagsPackage.TAG_REGISTRY__REQUESTS:
				getRequests().clear();
				getRequests().addAll((Collection<? extends RequestedTag>)newValue);
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
			case TagsPackage.TAG_REGISTRY__TAGS:
				getTags().clear();
				return;
			case TagsPackage.TAG_REGISTRY__ASSIGNMENTS:
				getAssignments().clear();
				return;
			case TagsPackage.TAG_REGISTRY__RULES:
				getRules().clear();
				return;
			case TagsPackage.TAG_REGISTRY__REQUESTS:
				getRequests().clear();
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
			case TagsPackage.TAG_REGISTRY__TAGS:
				return tags != null && !tags.isEmpty();
			case TagsPackage.TAG_REGISTRY__ASSIGNMENTS:
				return assignments != null && !assignments.isEmpty();
			case TagsPackage.TAG_REGISTRY__RULES:
				return rules != null && !rules.isEmpty();
			case TagsPackage.TAG_REGISTRY__REQUESTS:
				return requests != null && !requests.isEmpty();
		}
		return super.eIsSet(featureID);
	}

} //TagRegistryImpl
