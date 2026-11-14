/**
 */
package org.eclipse.fennec.tags.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EValidator;

import org.eclipse.emf.ecore.impl.EPackageImpl;

import org.eclipse.fennec.tags.EffectiveTag;
import org.eclipse.fennec.tags.FoundTargets;
import org.eclipse.fennec.tags.MatchMode;
import org.eclipse.fennec.tags.RequestedTag;
import org.eclipse.fennec.tags.Tag;
import org.eclipse.fennec.tags.TagAssignment;
import org.eclipse.fennec.tags.TagOrigin;
import org.eclipse.fennec.tags.TagRegistry;
import org.eclipse.fennec.tags.TagRule;
import org.eclipse.fennec.tags.TagTarget;
import org.eclipse.fennec.tags.TaggedTarget;
import org.eclipse.fennec.tags.TagsFactory;
import org.eclipse.fennec.tags.TagsPackage;

import org.eclipse.fennec.tags.util.TagsValidator;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class TagsPackageImpl extends EPackageImpl implements TagsPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass tagRegistryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass tagEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass tagTargetEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass tagAssignmentEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass tagRuleEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass requestedTagEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass effectiveTagEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass foundTargetsEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass taggedTargetEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum matchModeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum tagOriginEEnum = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see org.eclipse.fennec.tags.TagsPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private TagsPackageImpl() {
		super(eNS_URI, TagsFactory.eINSTANCE);
	}
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 *
	 * <p>This method is used to initialize {@link TagsPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static TagsPackage init() {
		if (isInited) return (TagsPackage)EPackage.Registry.INSTANCE.getEPackage(TagsPackage.eNS_URI);

		// Obtain or create and register package
		Object registeredTagsPackage = EPackage.Registry.INSTANCE.get(eNS_URI);
		TagsPackageImpl theTagsPackage = registeredTagsPackage instanceof TagsPackageImpl ? (TagsPackageImpl)registeredTagsPackage : new TagsPackageImpl();

		isInited = true;

		// Create package meta-data objects
		theTagsPackage.createPackageContents();

		// Initialize created meta-data
		theTagsPackage.initializePackageContents();

		// Register package validator
		EValidator.Registry.INSTANCE.put
			(theTagsPackage,
			 new EValidator.Descriptor() {
				 @Override
				 public EValidator getEValidator() {
					 return TagsValidator.INSTANCE;
				 }
			 });

		// Mark meta-data to indicate it can't be changed
		theTagsPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(TagsPackage.eNS_URI, theTagsPackage);
		return theTagsPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTagRegistry() {
		return tagRegistryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTagRegistry_Tags() {
		return (EReference)tagRegistryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTagRegistry_Assignments() {
		return (EReference)tagRegistryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTagRegistry_Rules() {
		return (EReference)tagRegistryEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTagRegistry_Requests() {
		return (EReference)tagRegistryEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTag() {
		return tagEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTag_Id() {
		return (EAttribute)tagEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTag_Name() {
		return (EAttribute)tagEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTag_Description() {
		return (EAttribute)tagEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTag_Path() {
		return (EAttribute)tagEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTag_SelfAssignable() {
		return (EAttribute)tagEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTag_FormerPaths() {
		return (EAttribute)tagEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTag_Children() {
		return (EReference)tagEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTag_Parent() {
		return (EReference)tagEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTagTarget() {
		return tagTargetEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTagTarget_Kind() {
		return (EAttribute)tagTargetEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTagTarget_Key() {
		return (EAttribute)tagTargetEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTagAssignment() {
		return tagAssignmentEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTagAssignment_Tag() {
		return (EReference)tagAssignmentEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTagAssignment_Target() {
		return (EReference)tagAssignmentEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTagAssignment_AssignedBy() {
		return (EAttribute)tagAssignmentEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTagAssignment_AssignedAt() {
		return (EAttribute)tagAssignmentEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTagRule() {
		return tagRuleEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTagRule_Tag() {
		return (EReference)tagRuleEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTagRule_Kind() {
		return (EAttribute)tagRuleEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTagRule_Filter() {
		return (EAttribute)tagRuleEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getRequestedTag() {
		return requestedTagEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRequestedTag_Path() {
		return (EAttribute)requestedTagEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getRequestedTag_Target() {
		return (EReference)requestedTagEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRequestedTag_RequestedBy() {
		return (EAttribute)requestedTagEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRequestedTag_RequestedAt() {
		return (EAttribute)requestedTagEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getEffectiveTag() {
		return effectiveTagEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEffectiveTag_Path() {
		return (EAttribute)effectiveTagEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEffectiveTag_Origin() {
		return (EAttribute)effectiveTagEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getFoundTargets() {
		return foundTargetsEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFoundTargets_Kind() {
		return (EAttribute)foundTargetsEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFoundTargets_Keys() {
		return (EAttribute)foundTargetsEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTaggedTarget() {
		return taggedTargetEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTaggedTarget_Target() {
		return (EReference)taggedTargetEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTaggedTarget_Tags() {
		return (EReference)taggedTargetEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getMatchMode() {
		return matchModeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getTagOrigin() {
		return tagOriginEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TagsFactory getTagsFactory() {
		return (TagsFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		tagRegistryEClass = createEClass(TAG_REGISTRY);
		createEReference(tagRegistryEClass, TAG_REGISTRY__TAGS);
		createEReference(tagRegistryEClass, TAG_REGISTRY__ASSIGNMENTS);
		createEReference(tagRegistryEClass, TAG_REGISTRY__RULES);
		createEReference(tagRegistryEClass, TAG_REGISTRY__REQUESTS);

		tagEClass = createEClass(TAG);
		createEAttribute(tagEClass, TAG__ID);
		createEAttribute(tagEClass, TAG__NAME);
		createEAttribute(tagEClass, TAG__DESCRIPTION);
		createEAttribute(tagEClass, TAG__PATH);
		createEAttribute(tagEClass, TAG__SELF_ASSIGNABLE);
		createEAttribute(tagEClass, TAG__FORMER_PATHS);
		createEReference(tagEClass, TAG__CHILDREN);
		createEReference(tagEClass, TAG__PARENT);

		tagTargetEClass = createEClass(TAG_TARGET);
		createEAttribute(tagTargetEClass, TAG_TARGET__KIND);
		createEAttribute(tagTargetEClass, TAG_TARGET__KEY);

		tagAssignmentEClass = createEClass(TAG_ASSIGNMENT);
		createEReference(tagAssignmentEClass, TAG_ASSIGNMENT__TAG);
		createEReference(tagAssignmentEClass, TAG_ASSIGNMENT__TARGET);
		createEAttribute(tagAssignmentEClass, TAG_ASSIGNMENT__ASSIGNED_BY);
		createEAttribute(tagAssignmentEClass, TAG_ASSIGNMENT__ASSIGNED_AT);

		tagRuleEClass = createEClass(TAG_RULE);
		createEReference(tagRuleEClass, TAG_RULE__TAG);
		createEAttribute(tagRuleEClass, TAG_RULE__KIND);
		createEAttribute(tagRuleEClass, TAG_RULE__FILTER);

		requestedTagEClass = createEClass(REQUESTED_TAG);
		createEAttribute(requestedTagEClass, REQUESTED_TAG__PATH);
		createEReference(requestedTagEClass, REQUESTED_TAG__TARGET);
		createEAttribute(requestedTagEClass, REQUESTED_TAG__REQUESTED_BY);
		createEAttribute(requestedTagEClass, REQUESTED_TAG__REQUESTED_AT);

		effectiveTagEClass = createEClass(EFFECTIVE_TAG);
		createEAttribute(effectiveTagEClass, EFFECTIVE_TAG__PATH);
		createEAttribute(effectiveTagEClass, EFFECTIVE_TAG__ORIGIN);

		foundTargetsEClass = createEClass(FOUND_TARGETS);
		createEAttribute(foundTargetsEClass, FOUND_TARGETS__KIND);
		createEAttribute(foundTargetsEClass, FOUND_TARGETS__KEYS);

		taggedTargetEClass = createEClass(TAGGED_TARGET);
		createEReference(taggedTargetEClass, TAGGED_TARGET__TARGET);
		createEReference(taggedTargetEClass, TAGGED_TARGET__TAGS);

		// Create enums
		matchModeEEnum = createEEnum(MATCH_MODE);
		tagOriginEEnum = createEEnum(TAG_ORIGIN);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes

		// Initialize classes, features, and operations; add parameters
		initEClass(tagRegistryEClass, TagRegistry.class, "TagRegistry", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getTagRegistry_Tags(), this.getTag(), null, "tags", null, 0, -1, TagRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTagRegistry_Assignments(), this.getTagAssignment(), null, "assignments", null, 0, -1, TagRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTagRegistry_Rules(), this.getTagRule(), null, "rules", null, 0, -1, TagRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTagRegistry_Requests(), this.getRequestedTag(), null, "requests", null, 0, -1, TagRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(tagEClass, Tag.class, "Tag", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTag_Id(), ecorePackage.getEString(), "id", null, 1, 1, Tag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTag_Name(), ecorePackage.getEString(), "name", null, 0, 1, Tag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTag_Description(), ecorePackage.getEString(), "description", null, 0, 1, Tag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTag_Path(), ecorePackage.getEString(), "path", null, 0, 1, Tag.class, IS_TRANSIENT, IS_VOLATILE, !IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getTag_SelfAssignable(), ecorePackage.getEBoolean(), "selfAssignable", null, 0, 1, Tag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTag_FormerPaths(), ecorePackage.getEString(), "formerPaths", null, 0, -1, Tag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTag_Children(), this.getTag(), this.getTag_Parent(), "children", null, 0, -1, Tag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTag_Parent(), this.getTag(), this.getTag_Children(), "parent", null, 0, 1, Tag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(tagTargetEClass, TagTarget.class, "TagTarget", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTagTarget_Kind(), ecorePackage.getEString(), "kind", null, 1, 1, TagTarget.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTagTarget_Key(), ecorePackage.getEString(), "key", null, 1, 1, TagTarget.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(tagAssignmentEClass, TagAssignment.class, "TagAssignment", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getTagAssignment_Tag(), this.getTag(), null, "tag", null, 1, 1, TagAssignment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTagAssignment_Target(), this.getTagTarget(), null, "target", null, 1, 1, TagAssignment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTagAssignment_AssignedBy(), ecorePackage.getEString(), "assignedBy", null, 0, 1, TagAssignment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTagAssignment_AssignedAt(), ecorePackage.getEDate(), "assignedAt", null, 0, 1, TagAssignment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(tagRuleEClass, TagRule.class, "TagRule", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getTagRule_Tag(), this.getTag(), null, "tag", null, 1, 1, TagRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTagRule_Kind(), ecorePackage.getEString(), "kind", "implementation", 1, 1, TagRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTagRule_Filter(), ecorePackage.getEString(), "filter", null, 1, 1, TagRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(requestedTagEClass, RequestedTag.class, "RequestedTag", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRequestedTag_Path(), ecorePackage.getEString(), "path", null, 1, 1, RequestedTag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRequestedTag_Target(), this.getTagTarget(), null, "target", null, 1, 1, RequestedTag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRequestedTag_RequestedBy(), ecorePackage.getEString(), "requestedBy", null, 0, 1, RequestedTag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRequestedTag_RequestedAt(), ecorePackage.getEDate(), "requestedAt", null, 0, 1, RequestedTag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(effectiveTagEClass, EffectiveTag.class, "EffectiveTag", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEffectiveTag_Path(), ecorePackage.getEString(), "path", null, 1, 1, EffectiveTag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEffectiveTag_Origin(), this.getTagOrigin(), "origin", null, 1, 1, EffectiveTag.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(foundTargetsEClass, FoundTargets.class, "FoundTargets", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFoundTargets_Kind(), ecorePackage.getEString(), "kind", null, 0, 1, FoundTargets.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFoundTargets_Keys(), ecorePackage.getEString(), "keys", null, 0, -1, FoundTargets.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(taggedTargetEClass, TaggedTarget.class, "TaggedTarget", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getTaggedTarget_Target(), this.getTagTarget(), null, "target", null, 1, 1, TaggedTarget.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTaggedTarget_Tags(), this.getEffectiveTag(), null, "tags", null, 0, -1, TaggedTarget.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Initialize enums and add enum literals
		initEEnum(matchModeEEnum, MatchMode.class, "MatchMode");
		addEEnumLiteral(matchModeEEnum, MatchMode.ANY);
		addEEnumLiteral(matchModeEEnum, MatchMode.ALL);

		initEEnum(tagOriginEEnum, TagOrigin.class, "TagOrigin");
		addEEnumLiteral(tagOriginEEnum, TagOrigin.SOURCE);
		addEEnumLiteral(tagOriginEEnum, TagOrigin.ADMIN);
		addEEnumLiteral(tagOriginEEnum, TagOrigin.RULE);

		// Create resource
		createResource(eNS_URI);

		// Create annotations
		// Version
		createVersionAnnotations();
		// http://www.eclipse.org/emf/2002/GenModel
		createGenModelAnnotations();
		// http://www.eclipse.org/emf/2002/Ecore
		createEcoreAnnotations();
		// http://www.eclipse.org/fennec/m2x/ocl/1.0
		create_1Annotations();
	}

	/**
	 * Initializes the annotations for <b>Version</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void createVersionAnnotations() {
		String source = "Version";
		addAnnotation
		  (this,
		   source,
		   new String[] {
			   "value", "1.0"
		   });
	}

	/**
	 * Initializes the annotations for <b>http://www.eclipse.org/emf/2002/GenModel</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void createGenModelAnnotations() {
		String source = "http://www.eclipse.org/emf/2002/GenModel";
		addAnnotation
		  (this,
		   source,
		   new String[] {
			   "complianceLevel", "17.0",
			   "oSGiCompatible", "true",
			   "basePackage", "org.eclipse.fennec",
			   "resource", "XMI",
			   "documentation", "Tags as an overlay (#272): a hierarchical tree of categories, assigned to things that live elsewhere. The model knows no type of the things it tags \u2014 a target is a kind and a key \u2014 so the same model serves the services registry (contracts, implementations) and could serve the Model Atlas (models). What a target is, which properties it has and which tags it asks for come from the source of the targets, not from here."
		   });
		addAnnotation
		  (matchModeEEnum,
		   source,
		   new String[] {
			   "documentation", "How several tag paths in one query combine. ANY: a target carrying at least one of them matches. ALL: a target must carry every one. A tag on a target also counts for each of its ancestors, so asking for a parent finds what carries a child."
		   });
		addAnnotation
		  (tagOriginEEnum,
		   source,
		   new String[] {
			   "documentation", "Where a tag on a target comes from. SOURCE: the target asked for it itself (a provider at registration) and the tag allows that. ADMIN: an administrator assigned it. RULE: a rule over the target\'s properties matched."
		   });
		addAnnotation
		  (tagRegistryEClass,
		   source,
		   new String[] {
			   "documentation", "Everything the tag service keeps, in one document: the tree, the administrator\'s assignments, the rules and the open requests. What is persisted, and what references into the tree resolve within."
		   });
		addAnnotation
		  (getTagRegistry_Tags(),
		   source,
		   new String[] {
			   "documentation", "The roots of the tree. Their ids are unique among each other."
		   });
		addAnnotation
		  (tagEClass,
		   source,
		   new String[] {
			   "documentation", "One category. Its id is short and technical and unique among its siblings, not globally: iot/weather and marketing/weather may both exist. Its path is derived from the ids up the tree, so changing an id moves every descendant with it and nothing has to be rewritten. Whatever names a tag from inside this model references the object; whatever names it from outside uses the path, and formerPaths keeps such names valid after a rename."
		   });
		addAnnotation
		  (getTag_Id(),
		   source,
		   new String[] {
			   "documentation", "Technical, short and stable, e.g. \'iot\' or \'sensors\'. No \'/\', which separates the segments of a path. Unique among the siblings."
		   });
		addAnnotation
		  (getTag_Name(),
		   source,
		   new String[] {
			   "documentation", "Human-readable, may change at any time without breaking anything."
		   });
		addAnnotation
		  (getTag_Path(),
		   source,
		   new String[] {
			   "documentation", "The ids from the root down, joined by \'/\', e.g. \'iot/sensors\'. What filters, assignments from outside and a URI name a tag by. Derived, never stored: a document on the wire carries the tree, and a reader without an OCL engine joins the ids itself."
		   });
		addAnnotation
		  (getTag_SelfAssignable(),
		   source,
		   new String[] {
			   "documentation", "Whether a target may give itself this tag (a provider at registration). False: asking for it only files a request the administrator accepts or discards."
		   });
		addAnnotation
		  (getTag_FormerPaths(),
		   source,
		   new String[] {
			   "documentation", "Paths this tag had before it was renamed or moved. Resolving a path accepts them too, so a target that named the tag by its old path keeps it. Never offered to a policy: what decides access sees only current paths."
		   });
		addAnnotation
		  (tagTargetEClass,
		   source,
		   new String[] {
			   "documentation", "Something a tag can be on, named by the source that knows it. For the services registry: kind \'contract\' with the interface name as key, kind \'implementation\' with provider name and contract name. Neither is a type of this model, which is what lets it tag other things."
		   });
		addAnnotation
		  (getTagTarget_Kind(),
		   source,
		   new String[] {
			   "documentation", "What sort of thing this is, as its source names it, e.g. \'contract\', \'implementation\', later \'model\'. An open string, not an enum, so a new source needs no model change."
		   });
		addAnnotation
		  (getTagTarget_Key(),
		   source,
		   new String[] {
			   "documentation", "Which one, unique within its kind, as its source names it. Stable across changes of the thing itself \u2014 for a contract its name, not its fingerprint, so a tag survives a change of the contract."
		   });
		addAnnotation
		  (tagAssignmentEClass,
		   source,
		   new String[] {
			   "documentation", "An administrator put a tag on a target. References the tag object, so it follows a rename."
		   });
		addAnnotation
		  (tagRuleEClass,
		   source,
		   new String[] {
			   "documentation", "Every target of a kind whose properties match the filter carries the tag. Applied when a query asks, not when a target arrives, so a new rule covers what is already there. References the tag object, so it follows a rename."
		   });
		addAnnotation
		  (getTagRule_Kind(),
		   source,
		   new String[] {
			   "documentation", "Which kind of target the rule looks at. Only targets with properties can match: in the services registry that is \'implementation\'."
		   });
		addAnnotation
		  (getTagRule_Filter(),
		   source,
		   new String[] {
			   "documentation", "An LDAP filter (RFC 4515, as in OSGi) over the target\'s properties, e.g. \'(payment.scheme=card)\'. Evaluated by the tag service only."
		   });
		addAnnotation
		  (requestedTagEClass,
		   source,
		   new String[] {
			   "documentation", "A target asked for a tag it may not give itself \u2014 the tag does not exist yet or is not self-assignable. The administrator accepts the request (the tag is created where needed and assigned) or discards it. A discarded request comes back if the target asks again; nothing is blocked."
		   });
		addAnnotation
		  (effectiveTagEClass,
		   source,
		   new String[] {
			   "documentation", "One tag a target carries, as a query reports it: the current path and where it comes from. A target may carry the same tag for several reasons; each is reported."
		   });
		addAnnotation
		  (foundTargetsEClass,
		   source,
		   new String[] {
			   "documentation", "What a query by tags answers: the keys of the targets of one kind that carry them, sorted. Keys only \u2014 what a target looks like comes from its own source, so there is one place for it."
		   });
		addAnnotation
		  (taggedTargetEClass,
		   source,
		   new String[] {
			   "documentation", "One target with the tags it carries, each with where it comes from, sorted by path."
		   });
	}

	/**
	 * Initializes the annotations for <b>http://www.eclipse.org/emf/2002/Ecore</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void createEcoreAnnotations() {
		String source = "http://www.eclipse.org/emf/2002/Ecore";
		addAnnotation
		  (this,
		   source,
		   new String[] {
			   "settingDelegates", "http://www.eclipse.org/fennec/m2x/ocl/1.0",
			   "validationDelegates", "http://www.eclipse.org/fennec/m2x/ocl/1.0"
		   });
		addAnnotation
		  (tagRegistryEClass,
		   source,
		   new String[] {
			   "constraints", "rootIdsUnique"
		   });
		addAnnotation
		  (tagEClass,
		   source,
		   new String[] {
			   "constraints", "idHasNoSlash childIdsUnique"
		   });
	}

	/**
	 * Initializes the annotations for <b>http://www.eclipse.org/fennec/m2x/ocl/1.0</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void create_1Annotations() {
		String source = "http://www.eclipse.org/fennec/m2x/ocl/1.0";
		addAnnotation
		  (tagRegistryEClass,
		   source,
		   new String[] {
			   "rootIdsUnique", "tags->isUnique(t | t.id)"
		   });
		addAnnotation
		  (tagEClass,
		   source,
		   new String[] {
			   "idHasNoSlash", "id <> null and id.matches(\'^[^/]+$\')",
			   "childIdsUnique", "children->isUnique(c | c.id)"
		   });
		addAnnotation
		  (getTag_Path(),
		   source,
		   new String[] {
			   "derivation", "if parent = null then id else parent.path + \'/\' + id endif"
		   });
	}

} //TagsPackageImpl
