/**
 */
package org.eclipse.fennec.tags.util;

import java.util.Map;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.ResourceLocator;

import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.EObjectValidator;

import org.eclipse.fennec.tags.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Validator</b> for the model.
 * <!-- end-user-doc -->
 * @see org.eclipse.fennec.tags.TagsPackage
 * @generated
 */
public class TagsValidator extends EObjectValidator {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final TagsValidator INSTANCE = new TagsValidator();

	/**
	 * A constant for the {@link org.eclipse.emf.common.util.Diagnostic#getSource() source} of diagnostic {@link org.eclipse.emf.common.util.Diagnostic#getCode() codes} from this package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.common.util.Diagnostic#getSource()
	 * @see org.eclipse.emf.common.util.Diagnostic#getCode()
	 * @generated
	 */
	public static final String DIAGNOSTIC_SOURCE = "org.eclipse.fennec.tags";

	/**
	 * A constant with a fixed name that can be used as the base value for additional hand written constants.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final int GENERATED_DIAGNOSTIC_CODE_COUNT = 0;

	/**
	 * A constant with a fixed name that can be used as the base value for additional hand written constants in a derived class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final int DIAGNOSTIC_CODE_COUNT = GENERATED_DIAGNOSTIC_CODE_COUNT;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TagsValidator() {
		super();
	}

	/**
	 * Returns the package of this validator switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EPackage getEPackage() {
	  return TagsPackage.eINSTANCE;
	}

	/**
	 * Calls <code>validateXXX</code> for the corresponding classifier of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected boolean validate(int classifierID, Object value, DiagnosticChain diagnostics, Map<Object, Object> context) {
		switch (classifierID) {
			case TagsPackage.TAG_REGISTRY:
				return validateTagRegistry((TagRegistry)value, diagnostics, context);
			case TagsPackage.TAG:
				return validateTag((Tag)value, diagnostics, context);
			case TagsPackage.TAG_TARGET:
				return validateTagTarget((TagTarget)value, diagnostics, context);
			case TagsPackage.TAG_ASSIGNMENT:
				return validateTagAssignment((TagAssignment)value, diagnostics, context);
			case TagsPackage.TAG_RULE:
				return validateTagRule((TagRule)value, diagnostics, context);
			case TagsPackage.REQUESTED_TAG:
				return validateRequestedTag((RequestedTag)value, diagnostics, context);
			case TagsPackage.EFFECTIVE_TAG:
				return validateEffectiveTag((EffectiveTag)value, diagnostics, context);
			case TagsPackage.FOUND_TARGETS:
				return validateFoundTargets((FoundTargets)value, diagnostics, context);
			case TagsPackage.TAGGED_TARGET:
				return validateTaggedTarget((TaggedTarget)value, diagnostics, context);
			case TagsPackage.TAG_CHANGE:
				return validateTagChange((TagChange)value, diagnostics, context);
			case TagsPackage.MATCH_MODE:
				return validateMatchMode((MatchMode)value, diagnostics, context);
			case TagsPackage.TAG_ORIGIN:
				return validateTagOrigin((TagOrigin)value, diagnostics, context);
			case TagsPackage.TAG_CHANGE_KIND:
				return validateTagChangeKind((TagChangeKind)value, diagnostics, context);
			default:
				return true;
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTagRegistry(TagRegistry tagRegistry, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(tagRegistry, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(tagRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(tagRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(tagRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(tagRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(tagRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(tagRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(tagRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(tagRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validateTagRegistry_rootIdsUnique(tagRegistry, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the rootIdsUnique constraint of '<em>Tag Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String TAG_REGISTRY__ROOT_IDS_UNIQUE__EEXPRESSION = "tags->isUnique(t | t.id)";

	/**
	 * Validates the rootIdsUnique constraint of '<em>Tag Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTagRegistry_rootIdsUnique(TagRegistry tagRegistry, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(TagsPackage.Literals.TAG_REGISTRY,
				 tagRegistry,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "rootIdsUnique",
				 TAG_REGISTRY__ROOT_IDS_UNIQUE__EEXPRESSION,
				 Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTag(Tag tag, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(tag, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(tag, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(tag, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(tag, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(tag, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(tag, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(tag, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(tag, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(tag, diagnostics, context);
		if (result || diagnostics != null) result &= validateTag_idHasNoSlash(tag, diagnostics, context);
		if (result || diagnostics != null) result &= validateTag_childIdsUnique(tag, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the idHasNoSlash constraint of '<em>Tag</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String TAG__ID_HAS_NO_SLASH__EEXPRESSION = "id <> null and id.matches('^[^/]+$')";

	/**
	 * Validates the idHasNoSlash constraint of '<em>Tag</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTag_idHasNoSlash(Tag tag, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(TagsPackage.Literals.TAG,
				 tag,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "idHasNoSlash",
				 TAG__ID_HAS_NO_SLASH__EEXPRESSION,
				 Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the childIdsUnique constraint of '<em>Tag</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String TAG__CHILD_IDS_UNIQUE__EEXPRESSION = "children->isUnique(c | c.id)";

	/**
	 * Validates the childIdsUnique constraint of '<em>Tag</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTag_childIdsUnique(Tag tag, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(TagsPackage.Literals.TAG,
				 tag,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "childIdsUnique",
				 TAG__CHILD_IDS_UNIQUE__EEXPRESSION,
				 Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTagTarget(TagTarget tagTarget, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(tagTarget, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTagAssignment(TagAssignment tagAssignment, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(tagAssignment, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTagRule(TagRule tagRule, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(tagRule, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRequestedTag(RequestedTag requestedTag, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(requestedTag, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEffectiveTag(EffectiveTag effectiveTag, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(effectiveTag, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateFoundTargets(FoundTargets foundTargets, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(foundTargets, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTaggedTarget(TaggedTarget taggedTarget, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(taggedTarget, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTagChange(TagChange tagChange, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(tagChange, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateMatchMode(MatchMode matchMode, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTagOrigin(TagOrigin tagOrigin, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTagChangeKind(TagChangeKind tagChangeKind, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * Returns the resource locator that will be used to fetch messages for this validator's diagnostics.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ResourceLocator getResourceLocator() {
		// TODO
		// Specialize this to return a resource locator for messages specific to this validator.
		// Ensure that you remove @generated or mark it @generated NOT
		return super.getResourceLocator();
	}

} //TagsValidator
