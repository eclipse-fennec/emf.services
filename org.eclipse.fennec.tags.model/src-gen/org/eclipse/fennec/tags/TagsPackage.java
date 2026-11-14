/*
 */
package org.eclipse.fennec.tags;


import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.fennec.emf.osgi.annotation.provide.EPackage;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each operation of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Tags as an overlay (#272): a hierarchical tree of categories, assigned to things that live elsewhere. The model knows no type of the things it tags — a target is a kind and a key — so the same model serves the services registry (contracts, implementations) and could serve the Model Atlas (models). What a target is, which properties it has and which tags it asks for come from the source of the targets, not from here.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.tags.TagsFactory
 * @model kind="package"
 *        annotation="Version value='1.0'"
 *        annotation="http://www.eclipse.org/emf/2002/GenModel complianceLevel='17.0' oSGiCompatible='true' basePackage='org.eclipse.fennec' resource='XMI'"
 *        annotation="http://www.eclipse.org/emf/2002/Ecore settingDelegates='http://www.eclipse.org/fennec/m2x/ocl/1.0' validationDelegates='http://www.eclipse.org/fennec/m2x/ocl/1.0'"
 * @generated
 */
@ProviderType
@EPackage(uri = TagsPackage.eNS_URI, fingerprint = "fp1:6ab5492f72b5630c552be839b73e983d1776da2766e125cebf1a075ca8d5f20d", genModel = "/model/tags.genmodel", genModelSourceLocations = {"model/tags.genmodel","org.eclipse.fennec.tags.model/model/tags.genmodel"}, ecore = "/model/tags.ecore", ecoreSourceLocations = "/model/tags.ecore")
public interface TagsPackage extends org.eclipse.emf.ecore.EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "tags";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://eclipse.org/fennec/tags/1.0";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "tags";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	TagsPackage eINSTANCE = org.eclipse.fennec.tags.impl.TagsPackageImpl.init();

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.impl.TagRegistryImpl <em>Tag Registry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.impl.TagRegistryImpl
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagRegistry()
	 * @generated
	 */
	int TAG_REGISTRY = 0;

	/**
	 * The feature id for the '<em><b>Tags</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_REGISTRY__TAGS = 0;

	/**
	 * The feature id for the '<em><b>Assignments</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_REGISTRY__ASSIGNMENTS = 1;

	/**
	 * The feature id for the '<em><b>Rules</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_REGISTRY__RULES = 2;

	/**
	 * The feature id for the '<em><b>Requests</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_REGISTRY__REQUESTS = 3;

	/**
	 * The number of structural features of the '<em>Tag Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_REGISTRY_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Tag Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_REGISTRY_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.impl.TagImpl <em>Tag</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.impl.TagImpl
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTag()
	 * @generated
	 */
	int TAG = 1;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG__ID = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG__NAME = 1;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG__DESCRIPTION = 2;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG__PATH = 3;

	/**
	 * The feature id for the '<em><b>Self Assignable</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG__SELF_ASSIGNABLE = 4;

	/**
	 * The feature id for the '<em><b>Former Paths</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG__FORMER_PATHS = 5;

	/**
	 * The feature id for the '<em><b>Children</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG__CHILDREN = 6;

	/**
	 * The feature id for the '<em><b>Parent</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG__PARENT = 7;

	/**
	 * The number of structural features of the '<em>Tag</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_FEATURE_COUNT = 8;

	/**
	 * The number of operations of the '<em>Tag</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.impl.TagTargetImpl <em>Tag Target</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.impl.TagTargetImpl
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagTarget()
	 * @generated
	 */
	int TAG_TARGET = 2;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_TARGET__KIND = 0;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_TARGET__KEY = 1;

	/**
	 * The number of structural features of the '<em>Tag Target</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_TARGET_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Tag Target</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_TARGET_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.impl.TagAssignmentImpl <em>Tag Assignment</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.impl.TagAssignmentImpl
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagAssignment()
	 * @generated
	 */
	int TAG_ASSIGNMENT = 3;

	/**
	 * The feature id for the '<em><b>Tag</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_ASSIGNMENT__TAG = 0;

	/**
	 * The feature id for the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_ASSIGNMENT__TARGET = 1;

	/**
	 * The feature id for the '<em><b>Assigned By</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_ASSIGNMENT__ASSIGNED_BY = 2;

	/**
	 * The feature id for the '<em><b>Assigned At</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_ASSIGNMENT__ASSIGNED_AT = 3;

	/**
	 * The number of structural features of the '<em>Tag Assignment</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_ASSIGNMENT_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Tag Assignment</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_ASSIGNMENT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.impl.TagRuleImpl <em>Tag Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.impl.TagRuleImpl
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagRule()
	 * @generated
	 */
	int TAG_RULE = 4;

	/**
	 * The feature id for the '<em><b>Tag</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_RULE__TAG = 0;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_RULE__KIND = 1;

	/**
	 * The feature id for the '<em><b>Filter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_RULE__FILTER = 2;

	/**
	 * The number of structural features of the '<em>Tag Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_RULE_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Tag Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAG_RULE_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.impl.RequestedTagImpl <em>Requested Tag</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.impl.RequestedTagImpl
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getRequestedTag()
	 * @generated
	 */
	int REQUESTED_TAG = 5;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUESTED_TAG__PATH = 0;

	/**
	 * The feature id for the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUESTED_TAG__TARGET = 1;

	/**
	 * The feature id for the '<em><b>Requested By</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUESTED_TAG__REQUESTED_BY = 2;

	/**
	 * The feature id for the '<em><b>Requested At</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUESTED_TAG__REQUESTED_AT = 3;

	/**
	 * The number of structural features of the '<em>Requested Tag</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUESTED_TAG_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Requested Tag</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUESTED_TAG_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.impl.EffectiveTagImpl <em>Effective Tag</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.impl.EffectiveTagImpl
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getEffectiveTag()
	 * @generated
	 */
	int EFFECTIVE_TAG = 6;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EFFECTIVE_TAG__PATH = 0;

	/**
	 * The feature id for the '<em><b>Origin</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EFFECTIVE_TAG__ORIGIN = 1;

	/**
	 * The number of structural features of the '<em>Effective Tag</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EFFECTIVE_TAG_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Effective Tag</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EFFECTIVE_TAG_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.impl.FoundTargetsImpl <em>Found Targets</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.impl.FoundTargetsImpl
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getFoundTargets()
	 * @generated
	 */
	int FOUND_TARGETS = 7;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FOUND_TARGETS__KIND = 0;

	/**
	 * The feature id for the '<em><b>Keys</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FOUND_TARGETS__KEYS = 1;

	/**
	 * The number of structural features of the '<em>Found Targets</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FOUND_TARGETS_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Found Targets</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FOUND_TARGETS_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.impl.TaggedTargetImpl <em>Tagged Target</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.impl.TaggedTargetImpl
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTaggedTarget()
	 * @generated
	 */
	int TAGGED_TARGET = 8;

	/**
	 * The feature id for the '<em><b>Target</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAGGED_TARGET__TARGET = 0;

	/**
	 * The feature id for the '<em><b>Tags</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAGGED_TARGET__TAGS = 1;

	/**
	 * The number of structural features of the '<em>Tagged Target</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAGGED_TARGET_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Tagged Target</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TAGGED_TARGET_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.MatchMode <em>Match Mode</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.MatchMode
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getMatchMode()
	 * @generated
	 */
	int MATCH_MODE = 9;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.tags.TagOrigin <em>Tag Origin</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.tags.TagOrigin
	 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagOrigin()
	 * @generated
	 */
	int TAG_ORIGIN = 10;


	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.tags.TagRegistry <em>Tag Registry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Tag Registry</em>'.
	 * @see org.eclipse.fennec.tags.TagRegistry
	 * @generated
	 */
	EClass getTagRegistry();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.tags.TagRegistry#getTags <em>Tags</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Tags</em>'.
	 * @see org.eclipse.fennec.tags.TagRegistry#getTags()
	 * @see #getTagRegistry()
	 * @generated
	 */
	EReference getTagRegistry_Tags();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.tags.TagRegistry#getAssignments <em>Assignments</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Assignments</em>'.
	 * @see org.eclipse.fennec.tags.TagRegistry#getAssignments()
	 * @see #getTagRegistry()
	 * @generated
	 */
	EReference getTagRegistry_Assignments();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.tags.TagRegistry#getRules <em>Rules</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Rules</em>'.
	 * @see org.eclipse.fennec.tags.TagRegistry#getRules()
	 * @see #getTagRegistry()
	 * @generated
	 */
	EReference getTagRegistry_Rules();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.tags.TagRegistry#getRequests <em>Requests</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Requests</em>'.
	 * @see org.eclipse.fennec.tags.TagRegistry#getRequests()
	 * @see #getTagRegistry()
	 * @generated
	 */
	EReference getTagRegistry_Requests();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.tags.Tag <em>Tag</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Tag</em>'.
	 * @see org.eclipse.fennec.tags.Tag
	 * @generated
	 */
	EClass getTag();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.Tag#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see org.eclipse.fennec.tags.Tag#getId()
	 * @see #getTag()
	 * @generated
	 */
	EAttribute getTag_Id();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.Tag#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.eclipse.fennec.tags.Tag#getName()
	 * @see #getTag()
	 * @generated
	 */
	EAttribute getTag_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.Tag#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.eclipse.fennec.tags.Tag#getDescription()
	 * @see #getTag()
	 * @generated
	 */
	EAttribute getTag_Description();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.Tag#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see org.eclipse.fennec.tags.Tag#getPath()
	 * @see #getTag()
	 * @generated
	 */
	EAttribute getTag_Path();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.Tag#isSelfAssignable <em>Self Assignable</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Self Assignable</em>'.
	 * @see org.eclipse.fennec.tags.Tag#isSelfAssignable()
	 * @see #getTag()
	 * @generated
	 */
	EAttribute getTag_SelfAssignable();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.tags.Tag#getFormerPaths <em>Former Paths</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Former Paths</em>'.
	 * @see org.eclipse.fennec.tags.Tag#getFormerPaths()
	 * @see #getTag()
	 * @generated
	 */
	EAttribute getTag_FormerPaths();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.tags.Tag#getChildren <em>Children</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Children</em>'.
	 * @see org.eclipse.fennec.tags.Tag#getChildren()
	 * @see #getTag()
	 * @generated
	 */
	EReference getTag_Children();

	/**
	 * Returns the meta object for the container reference '{@link org.eclipse.fennec.tags.Tag#getParent <em>Parent</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Parent</em>'.
	 * @see org.eclipse.fennec.tags.Tag#getParent()
	 * @see #getTag()
	 * @generated
	 */
	EReference getTag_Parent();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.tags.TagTarget <em>Tag Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Tag Target</em>'.
	 * @see org.eclipse.fennec.tags.TagTarget
	 * @generated
	 */
	EClass getTagTarget();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.TagTarget#getKind <em>Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Kind</em>'.
	 * @see org.eclipse.fennec.tags.TagTarget#getKind()
	 * @see #getTagTarget()
	 * @generated
	 */
	EAttribute getTagTarget_Kind();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.TagTarget#getKey <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see org.eclipse.fennec.tags.TagTarget#getKey()
	 * @see #getTagTarget()
	 * @generated
	 */
	EAttribute getTagTarget_Key();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.tags.TagAssignment <em>Tag Assignment</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Tag Assignment</em>'.
	 * @see org.eclipse.fennec.tags.TagAssignment
	 * @generated
	 */
	EClass getTagAssignment();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.tags.TagAssignment#getTag <em>Tag</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Tag</em>'.
	 * @see org.eclipse.fennec.tags.TagAssignment#getTag()
	 * @see #getTagAssignment()
	 * @generated
	 */
	EReference getTagAssignment_Tag();

	/**
	 * Returns the meta object for the containment reference '{@link org.eclipse.fennec.tags.TagAssignment#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Target</em>'.
	 * @see org.eclipse.fennec.tags.TagAssignment#getTarget()
	 * @see #getTagAssignment()
	 * @generated
	 */
	EReference getTagAssignment_Target();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.TagAssignment#getAssignedBy <em>Assigned By</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Assigned By</em>'.
	 * @see org.eclipse.fennec.tags.TagAssignment#getAssignedBy()
	 * @see #getTagAssignment()
	 * @generated
	 */
	EAttribute getTagAssignment_AssignedBy();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.TagAssignment#getAssignedAt <em>Assigned At</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Assigned At</em>'.
	 * @see org.eclipse.fennec.tags.TagAssignment#getAssignedAt()
	 * @see #getTagAssignment()
	 * @generated
	 */
	EAttribute getTagAssignment_AssignedAt();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.tags.TagRule <em>Tag Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Tag Rule</em>'.
	 * @see org.eclipse.fennec.tags.TagRule
	 * @generated
	 */
	EClass getTagRule();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.tags.TagRule#getTag <em>Tag</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Tag</em>'.
	 * @see org.eclipse.fennec.tags.TagRule#getTag()
	 * @see #getTagRule()
	 * @generated
	 */
	EReference getTagRule_Tag();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.TagRule#getKind <em>Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Kind</em>'.
	 * @see org.eclipse.fennec.tags.TagRule#getKind()
	 * @see #getTagRule()
	 * @generated
	 */
	EAttribute getTagRule_Kind();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.TagRule#getFilter <em>Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Filter</em>'.
	 * @see org.eclipse.fennec.tags.TagRule#getFilter()
	 * @see #getTagRule()
	 * @generated
	 */
	EAttribute getTagRule_Filter();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.tags.RequestedTag <em>Requested Tag</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Requested Tag</em>'.
	 * @see org.eclipse.fennec.tags.RequestedTag
	 * @generated
	 */
	EClass getRequestedTag();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.RequestedTag#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see org.eclipse.fennec.tags.RequestedTag#getPath()
	 * @see #getRequestedTag()
	 * @generated
	 */
	EAttribute getRequestedTag_Path();

	/**
	 * Returns the meta object for the containment reference '{@link org.eclipse.fennec.tags.RequestedTag#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Target</em>'.
	 * @see org.eclipse.fennec.tags.RequestedTag#getTarget()
	 * @see #getRequestedTag()
	 * @generated
	 */
	EReference getRequestedTag_Target();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.RequestedTag#getRequestedBy <em>Requested By</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Requested By</em>'.
	 * @see org.eclipse.fennec.tags.RequestedTag#getRequestedBy()
	 * @see #getRequestedTag()
	 * @generated
	 */
	EAttribute getRequestedTag_RequestedBy();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.RequestedTag#getRequestedAt <em>Requested At</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Requested At</em>'.
	 * @see org.eclipse.fennec.tags.RequestedTag#getRequestedAt()
	 * @see #getRequestedTag()
	 * @generated
	 */
	EAttribute getRequestedTag_RequestedAt();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.tags.EffectiveTag <em>Effective Tag</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Effective Tag</em>'.
	 * @see org.eclipse.fennec.tags.EffectiveTag
	 * @generated
	 */
	EClass getEffectiveTag();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.EffectiveTag#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see org.eclipse.fennec.tags.EffectiveTag#getPath()
	 * @see #getEffectiveTag()
	 * @generated
	 */
	EAttribute getEffectiveTag_Path();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.EffectiveTag#getOrigin <em>Origin</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Origin</em>'.
	 * @see org.eclipse.fennec.tags.EffectiveTag#getOrigin()
	 * @see #getEffectiveTag()
	 * @generated
	 */
	EAttribute getEffectiveTag_Origin();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.tags.FoundTargets <em>Found Targets</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Found Targets</em>'.
	 * @see org.eclipse.fennec.tags.FoundTargets
	 * @generated
	 */
	EClass getFoundTargets();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.tags.FoundTargets#getKind <em>Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Kind</em>'.
	 * @see org.eclipse.fennec.tags.FoundTargets#getKind()
	 * @see #getFoundTargets()
	 * @generated
	 */
	EAttribute getFoundTargets_Kind();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.tags.FoundTargets#getKeys <em>Keys</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Keys</em>'.
	 * @see org.eclipse.fennec.tags.FoundTargets#getKeys()
	 * @see #getFoundTargets()
	 * @generated
	 */
	EAttribute getFoundTargets_Keys();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.tags.TaggedTarget <em>Tagged Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Tagged Target</em>'.
	 * @see org.eclipse.fennec.tags.TaggedTarget
	 * @generated
	 */
	EClass getTaggedTarget();

	/**
	 * Returns the meta object for the containment reference '{@link org.eclipse.fennec.tags.TaggedTarget#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Target</em>'.
	 * @see org.eclipse.fennec.tags.TaggedTarget#getTarget()
	 * @see #getTaggedTarget()
	 * @generated
	 */
	EReference getTaggedTarget_Target();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.tags.TaggedTarget#getTags <em>Tags</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Tags</em>'.
	 * @see org.eclipse.fennec.tags.TaggedTarget#getTags()
	 * @see #getTaggedTarget()
	 * @generated
	 */
	EReference getTaggedTarget_Tags();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.tags.MatchMode <em>Match Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Match Mode</em>'.
	 * @see org.eclipse.fennec.tags.MatchMode
	 * @generated
	 */
	EEnum getMatchMode();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.tags.TagOrigin <em>Tag Origin</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Tag Origin</em>'.
	 * @see org.eclipse.fennec.tags.TagOrigin
	 * @generated
	 */
	EEnum getTagOrigin();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	TagsFactory getTagsFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each operation of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.impl.TagRegistryImpl <em>Tag Registry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.impl.TagRegistryImpl
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagRegistry()
		 * @generated
		 */
		EClass TAG_REGISTRY = eINSTANCE.getTagRegistry();

		/**
		 * The meta object literal for the '<em><b>Tags</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAG_REGISTRY__TAGS = eINSTANCE.getTagRegistry_Tags();

		/**
		 * The meta object literal for the '<em><b>Assignments</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAG_REGISTRY__ASSIGNMENTS = eINSTANCE.getTagRegistry_Assignments();

		/**
		 * The meta object literal for the '<em><b>Rules</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAG_REGISTRY__RULES = eINSTANCE.getTagRegistry_Rules();

		/**
		 * The meta object literal for the '<em><b>Requests</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAG_REGISTRY__REQUESTS = eINSTANCE.getTagRegistry_Requests();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.impl.TagImpl <em>Tag</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.impl.TagImpl
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTag()
		 * @generated
		 */
		EClass TAG = eINSTANCE.getTag();

		/**
		 * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG__ID = eINSTANCE.getTag_Id();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG__NAME = eINSTANCE.getTag_Name();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG__DESCRIPTION = eINSTANCE.getTag_Description();

		/**
		 * The meta object literal for the '<em><b>Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG__PATH = eINSTANCE.getTag_Path();

		/**
		 * The meta object literal for the '<em><b>Self Assignable</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG__SELF_ASSIGNABLE = eINSTANCE.getTag_SelfAssignable();

		/**
		 * The meta object literal for the '<em><b>Former Paths</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG__FORMER_PATHS = eINSTANCE.getTag_FormerPaths();

		/**
		 * The meta object literal for the '<em><b>Children</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAG__CHILDREN = eINSTANCE.getTag_Children();

		/**
		 * The meta object literal for the '<em><b>Parent</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAG__PARENT = eINSTANCE.getTag_Parent();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.impl.TagTargetImpl <em>Tag Target</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.impl.TagTargetImpl
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagTarget()
		 * @generated
		 */
		EClass TAG_TARGET = eINSTANCE.getTagTarget();

		/**
		 * The meta object literal for the '<em><b>Kind</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG_TARGET__KIND = eINSTANCE.getTagTarget_Kind();

		/**
		 * The meta object literal for the '<em><b>Key</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG_TARGET__KEY = eINSTANCE.getTagTarget_Key();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.impl.TagAssignmentImpl <em>Tag Assignment</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.impl.TagAssignmentImpl
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagAssignment()
		 * @generated
		 */
		EClass TAG_ASSIGNMENT = eINSTANCE.getTagAssignment();

		/**
		 * The meta object literal for the '<em><b>Tag</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAG_ASSIGNMENT__TAG = eINSTANCE.getTagAssignment_Tag();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAG_ASSIGNMENT__TARGET = eINSTANCE.getTagAssignment_Target();

		/**
		 * The meta object literal for the '<em><b>Assigned By</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG_ASSIGNMENT__ASSIGNED_BY = eINSTANCE.getTagAssignment_AssignedBy();

		/**
		 * The meta object literal for the '<em><b>Assigned At</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG_ASSIGNMENT__ASSIGNED_AT = eINSTANCE.getTagAssignment_AssignedAt();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.impl.TagRuleImpl <em>Tag Rule</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.impl.TagRuleImpl
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagRule()
		 * @generated
		 */
		EClass TAG_RULE = eINSTANCE.getTagRule();

		/**
		 * The meta object literal for the '<em><b>Tag</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAG_RULE__TAG = eINSTANCE.getTagRule_Tag();

		/**
		 * The meta object literal for the '<em><b>Kind</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG_RULE__KIND = eINSTANCE.getTagRule_Kind();

		/**
		 * The meta object literal for the '<em><b>Filter</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TAG_RULE__FILTER = eINSTANCE.getTagRule_Filter();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.impl.RequestedTagImpl <em>Requested Tag</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.impl.RequestedTagImpl
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getRequestedTag()
		 * @generated
		 */
		EClass REQUESTED_TAG = eINSTANCE.getRequestedTag();

		/**
		 * The meta object literal for the '<em><b>Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REQUESTED_TAG__PATH = eINSTANCE.getRequestedTag_Path();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference REQUESTED_TAG__TARGET = eINSTANCE.getRequestedTag_Target();

		/**
		 * The meta object literal for the '<em><b>Requested By</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REQUESTED_TAG__REQUESTED_BY = eINSTANCE.getRequestedTag_RequestedBy();

		/**
		 * The meta object literal for the '<em><b>Requested At</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REQUESTED_TAG__REQUESTED_AT = eINSTANCE.getRequestedTag_RequestedAt();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.impl.EffectiveTagImpl <em>Effective Tag</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.impl.EffectiveTagImpl
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getEffectiveTag()
		 * @generated
		 */
		EClass EFFECTIVE_TAG = eINSTANCE.getEffectiveTag();

		/**
		 * The meta object literal for the '<em><b>Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EFFECTIVE_TAG__PATH = eINSTANCE.getEffectiveTag_Path();

		/**
		 * The meta object literal for the '<em><b>Origin</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EFFECTIVE_TAG__ORIGIN = eINSTANCE.getEffectiveTag_Origin();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.impl.FoundTargetsImpl <em>Found Targets</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.impl.FoundTargetsImpl
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getFoundTargets()
		 * @generated
		 */
		EClass FOUND_TARGETS = eINSTANCE.getFoundTargets();

		/**
		 * The meta object literal for the '<em><b>Kind</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FOUND_TARGETS__KIND = eINSTANCE.getFoundTargets_Kind();

		/**
		 * The meta object literal for the '<em><b>Keys</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FOUND_TARGETS__KEYS = eINSTANCE.getFoundTargets_Keys();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.impl.TaggedTargetImpl <em>Tagged Target</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.impl.TaggedTargetImpl
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTaggedTarget()
		 * @generated
		 */
		EClass TAGGED_TARGET = eINSTANCE.getTaggedTarget();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAGGED_TARGET__TARGET = eINSTANCE.getTaggedTarget_Target();

		/**
		 * The meta object literal for the '<em><b>Tags</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TAGGED_TARGET__TAGS = eINSTANCE.getTaggedTarget_Tags();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.MatchMode <em>Match Mode</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.MatchMode
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getMatchMode()
		 * @generated
		 */
		EEnum MATCH_MODE = eINSTANCE.getMatchMode();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.tags.TagOrigin <em>Tag Origin</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.tags.TagOrigin
		 * @see org.eclipse.fennec.tags.impl.TagsPackageImpl#getTagOrigin()
		 * @generated
		 */
		EEnum TAG_ORIGIN = eINSTANCE.getTagOrigin();

	}

} //TagsPackage
