/**
 */
package org.eclipse.fennec.tags.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.eclipse.fennec.tags.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class TagsFactoryImpl extends EFactoryImpl implements TagsFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static TagsFactory init() {
		try {
			TagsFactory theTagsFactory = (TagsFactory)EPackage.Registry.INSTANCE.getEFactory(TagsPackage.eNS_URI);
			if (theTagsFactory != null) {
				return theTagsFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new TagsFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TagsFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case TagsPackage.TAG_REGISTRY: return createTagRegistry();
			case TagsPackage.TAG: return createTag();
			case TagsPackage.TAG_TARGET: return createTagTarget();
			case TagsPackage.TAG_ASSIGNMENT: return createTagAssignment();
			case TagsPackage.TAG_RULE: return createTagRule();
			case TagsPackage.REQUESTED_TAG: return createRequestedTag();
			case TagsPackage.EFFECTIVE_TAG: return createEffectiveTag();
			case TagsPackage.FOUND_TARGETS: return createFoundTargets();
			case TagsPackage.TAGGED_TARGET: return createTaggedTarget();
			case TagsPackage.TAG_CHANGE: return createTagChange();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object createFromString(EDataType eDataType, String initialValue) {
		switch (eDataType.getClassifierID()) {
			case TagsPackage.MATCH_MODE:
				return createMatchModeFromString(eDataType, initialValue);
			case TagsPackage.TAG_ORIGIN:
				return createTagOriginFromString(eDataType, initialValue);
			case TagsPackage.TAG_CHANGE_KIND:
				return createTagChangeKindFromString(eDataType, initialValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String convertToString(EDataType eDataType, Object instanceValue) {
		switch (eDataType.getClassifierID()) {
			case TagsPackage.MATCH_MODE:
				return convertMatchModeToString(eDataType, instanceValue);
			case TagsPackage.TAG_ORIGIN:
				return convertTagOriginToString(eDataType, instanceValue);
			case TagsPackage.TAG_CHANGE_KIND:
				return convertTagChangeKindToString(eDataType, instanceValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TagRegistry createTagRegistry() {
		TagRegistryImpl tagRegistry = new TagRegistryImpl();
		return tagRegistry;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Tag createTag() {
		TagImpl tag = new TagImpl();
		return tag;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TagTarget createTagTarget() {
		TagTargetImpl tagTarget = new TagTargetImpl();
		return tagTarget;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TagAssignment createTagAssignment() {
		TagAssignmentImpl tagAssignment = new TagAssignmentImpl();
		return tagAssignment;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TagRule createTagRule() {
		TagRuleImpl tagRule = new TagRuleImpl();
		return tagRule;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public RequestedTag createRequestedTag() {
		RequestedTagImpl requestedTag = new RequestedTagImpl();
		return requestedTag;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EffectiveTag createEffectiveTag() {
		EffectiveTagImpl effectiveTag = new EffectiveTagImpl();
		return effectiveTag;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public FoundTargets createFoundTargets() {
		FoundTargetsImpl foundTargets = new FoundTargetsImpl();
		return foundTargets;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TaggedTarget createTaggedTarget() {
		TaggedTargetImpl taggedTarget = new TaggedTargetImpl();
		return taggedTarget;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TagChange createTagChange() {
		TagChangeImpl tagChange = new TagChangeImpl();
		return tagChange;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MatchMode createMatchModeFromString(EDataType eDataType, String initialValue) {
		MatchMode result = MatchMode.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertMatchModeToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TagOrigin createTagOriginFromString(EDataType eDataType, String initialValue) {
		TagOrigin result = TagOrigin.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertTagOriginToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TagChangeKind createTagChangeKindFromString(EDataType eDataType, String initialValue) {
		TagChangeKind result = TagChangeKind.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertTagChangeKindToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TagsPackage getTagsPackage() {
		return (TagsPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static TagsPackage getPackage() {
		return TagsPackage.eINSTANCE;
	}

} //TagsFactoryImpl
