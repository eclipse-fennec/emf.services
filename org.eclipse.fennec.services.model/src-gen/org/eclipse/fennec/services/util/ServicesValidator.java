/**
 */
package org.eclipse.fennec.services.util;

import java.util.Map;

import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.ResourceLocator;

import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.EObjectValidator;

import org.eclipse.fennec.services.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Validator</b> for the model.
 * <!-- end-user-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage
 * @generated
 */
public class ServicesValidator extends EObjectValidator {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final ServicesValidator INSTANCE = new ServicesValidator();

	/**
	 * A constant for the {@link org.eclipse.emf.common.util.Diagnostic#getSource() source} of diagnostic {@link org.eclipse.emf.common.util.Diagnostic#getCode() codes} from this package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.common.util.Diagnostic#getSource()
	 * @see org.eclipse.emf.common.util.Diagnostic#getCode()
	 * @generated
	 */
	public static final String DIAGNOSTIC_SOURCE = "org.eclipse.fennec.services";

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
	public ServicesValidator() {
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
	  return ServicesPackage.eINSTANCE;
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
			case ServicesPackage.NAMED_ELEMENT:
				return validateNamedElement((NamedElement)value, diagnostics, context);
			case ServicesPackage.VERSIONED_ELEMENT:
				return validateVersionedElement((VersionedElement)value, diagnostics, context);
			case ServicesPackage.PROPERTY:
				return validateProperty((Property)value, diagnostics, context);
			case ServicesPackage.STRING_PROPERTY:
				return validateStringProperty((StringProperty)value, diagnostics, context);
			case ServicesPackage.INT_PROPERTY:
				return validateIntProperty((IntProperty)value, diagnostics, context);
			case ServicesPackage.LONG_PROPERTY:
				return validateLongProperty((LongProperty)value, diagnostics, context);
			case ServicesPackage.DOUBLE_PROPERTY:
				return validateDoubleProperty((DoubleProperty)value, diagnostics, context);
			case ServicesPackage.FLOAT_PROPERTY:
				return validateFloatProperty((FloatProperty)value, diagnostics, context);
			case ServicesPackage.SHORT_PROPERTY:
				return validateShortProperty((ShortProperty)value, diagnostics, context);
			case ServicesPackage.BOOL_PROPERTY:
				return validateBoolProperty((BoolProperty)value, diagnostics, context);
			case ServicesPackage.STRING_LIST_PROPERTY:
				return validateStringListProperty((StringListProperty)value, diagnostics, context);
			case ServicesPackage.SERVICE_OPERATION:
				return validateServiceOperation((ServiceOperation)value, diagnostics, context);
			case ServicesPackage.PARAMETER:
				return validateParameter((Parameter)value, diagnostics, context);
			case ServicesPackage.PARAMETER_CONSTRAINT:
				return validateParameterConstraint((ParameterConstraint)value, diagnostics, context);
			case ServicesPackage.REQUIRED_CONSTRAINT:
				return validateRequiredConstraint((RequiredConstraint)value, diagnostics, context);
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT:
				return validateNumericRangeConstraint((NumericRangeConstraint)value, diagnostics, context);
			case ServicesPackage.STRING_PATTERN_CONSTRAINT:
				return validateStringPatternConstraint((StringPatternConstraint)value, diagnostics, context);
			case ServicesPackage.ENUMERATION_CONSTRAINT:
				return validateEnumerationConstraint((EnumerationConstraint)value, diagnostics, context);
			case ServicesPackage.EXPRESSION_CONSTRAINT:
				return validateExpressionConstraint((ExpressionConstraint)value, diagnostics, context);
			case ServicesPackage.INVARIANT:
				return validateInvariant((Invariant)value, diagnostics, context);
			case ServicesPackage.COLLECTION_SIZE_CONSTRAINT:
				return validateCollectionSizeConstraint((CollectionSizeConstraint)value, diagnostics, context);
			case ServicesPackage.SERVICE_EXCEPTION:
				return validateServiceException((ServiceException)value, diagnostics, context);
			case ServicesPackage.SERVICE_INTERFACE:
				return validateServiceInterface((ServiceInterface)value, diagnostics, context);
			case ServicesPackage.LIFECYCLE_HOOK:
				return validateLifecycleHook((LifecycleHook)value, diagnostics, context);
			case ServicesPackage.REFERENCE_BINDING:
				return validateReferenceBinding((ReferenceBinding)value, diagnostics, context);
			case ServicesPackage.COMPONENT_REFERENCE:
				return validateComponentReference((ComponentReference)value, diagnostics, context);
			case ServicesPackage.COMPONENT_DESCRIPTION:
				return validateComponentDescription((ComponentDescription)value, diagnostics, context);
			case ServicesPackage.SERVICE_PROVIDER:
				return validateServiceProvider((ServiceProvider)value, diagnostics, context);
			case ServicesPackage.SERVICE_IMPLEMENTATION:
				return validateServiceImplementation((ServiceImplementation)value, diagnostics, context);
			case ServicesPackage.SERVICE_FLAVOR:
				return validateServiceFlavor((ServiceFlavor)value, diagnostics, context);
			case ServicesPackage.REST_FLAVOR:
				return validateRestFlavor((RestFlavor)value, diagnostics, context);
			case ServicesPackage.MQTT_FLAVOR:
				return validateMqttFlavor((MqttFlavor)value, diagnostics, context);
			case ServicesPackage.SERVICE_OPERATION_FLAVOR:
				return validateServiceOperationFlavor((ServiceOperationFlavor)value, diagnostics, context);
			case ServicesPackage.REST_OPERATION_FLAVOR:
				return validateRestOperationFlavor((RestOperationFlavor)value, diagnostics, context);
			case ServicesPackage.REST_PARAMETER_BINDING:
				return validateRestParameterBinding((RestParameterBinding)value, diagnostics, context);
			case ServicesPackage.MQTT_OPERATION_FLAVOR:
				return validateMqttOperationFlavor((MqttOperationFlavor)value, diagnostics, context);
			case ServicesPackage.SERVICE_REFERENCE:
				return validateServiceReference((ServiceReference)value, diagnostics, context);
			case ServicesPackage.SERVICE_REGISTRATION:
				return validateServiceRegistration((ServiceRegistration)value, diagnostics, context);
			case ServicesPackage.CONSUMER_SESSION:
				return validateConsumerSession((ConsumerSession)value, diagnostics, context);
			case ServicesPackage.COMPONENT_CONFIGURATION:
				return validateComponentConfiguration((ComponentConfiguration)value, diagnostics, context);
			case ServicesPackage.SATISFIED_REFERENCE:
				return validateSatisfiedReference((SatisfiedReference)value, diagnostics, context);
			case ServicesPackage.UNSATISFIED_REFERENCE:
				return validateUnsatisfiedReference((UnsatisfiedReference)value, diagnostics, context);
			case ServicesPackage.DIAGNOSTIC:
				return validateDiagnostic((Diagnostic)value, diagnostics, context);
			case ServicesPackage.SERVICE_EVENT:
				return validateServiceEvent((ServiceEvent)value, diagnostics, context);
			case ServicesPackage.SERVICE_LISTENER:
				return validateServiceListener((ServiceListener)value, diagnostics, context);
			case ServicesPackage.SERVICE_REGISTRY:
				return validateServiceRegistry((ServiceRegistry)value, diagnostics, context);
			case ServicesPackage.LOCAL_SERVICE_REGISTRY:
				return validateLocalServiceRegistry((LocalServiceRegistry)value, diagnostics, context);
			case ServicesPackage.REMOTE_SERVICE_REGISTRY:
				return validateRemoteServiceRegistry((RemoteServiceRegistry)value, diagnostics, context);
			case ServicesPackage.CAPABILITY:
				return validateCapability((Capability)value, diagnostics, context);
			case ServicesPackage.REQUIREMENT:
				return validateRequirement((Requirement)value, diagnostics, context);
			case ServicesPackage.CONSUMER_CAPABILITY:
				return validateConsumerCapability((ConsumerCapability)value, diagnostics, context);
			case ServicesPackage.PUBLISH_HOOK:
				return validatePublishHook((PublishHook)value, diagnostics, context);
			case ServicesPackage.DISCOVERY_HOOK:
				return validateDiscoveryHook((DiscoveryHook)value, diagnostics, context);
			case ServicesPackage.DISTRIBUTION_HOOK:
				return validateDistributionHook((DistributionHook)value, diagnostics, context);
			case ServicesPackage.LANGUAGE_BINDING:
				return validateLanguageBinding((LanguageBinding)value, diagnostics, context);
			case ServicesPackage.TYPE_MAPPING:
				return validateTypeMapping((TypeMapping)value, diagnostics, context);
			case ServicesPackage.PACKAGE_MAPPING:
				return validatePackageMapping((PackageMapping)value, diagnostics, context);
			case ServicesPackage.JAVA_BINDING:
				return validateJavaBinding((JavaBinding)value, diagnostics, context);
			case ServicesPackage.TYPE_SCRIPT_BINDING:
				return validateTypeScriptBinding((TypeScriptBinding)value, diagnostics, context);
			case ServicesPackage.PYTHON_BINDING:
				return validatePythonBinding((PythonBinding)value, diagnostics, context);
			case ServicesPackage.SERVICE_SCOPE:
				return validateServiceScope((ServiceScope)value, diagnostics, context);
			case ServicesPackage.REFERENCE_CARDINALITY:
				return validateReferenceCardinality((ReferenceCardinality)value, diagnostics, context);
			case ServicesPackage.REFERENCE_POLICY:
				return validateReferencePolicy((ReferencePolicy)value, diagnostics, context);
			case ServicesPackage.REFERENCE_POLICY_OPTION:
				return validateReferencePolicyOption((ReferencePolicyOption)value, diagnostics, context);
			case ServicesPackage.CONFIGURATION_POLICY:
				return validateConfigurationPolicy((ConfigurationPolicy)value, diagnostics, context);
			case ServicesPackage.COMPONENT_STATE:
				return validateComponentState((ComponentState)value, diagnostics, context);
			case ServicesPackage.SERVICE_EVENT_TYPE:
				return validateServiceEventType((ServiceEventType)value, diagnostics, context);
			case ServicesPackage.FIELD_OPTION:
				return validateFieldOption((FieldOption)value, diagnostics, context);
			case ServicesPackage.COLLECTION_TYPE:
				return validateCollectionType((CollectionType)value, diagnostics, context);
			case ServicesPackage.LIFECYCLE_HOOK_KIND:
				return validateLifecycleHookKind((LifecycleHookKind)value, diagnostics, context);
			case ServicesPackage.REFERENCE_BINDING_KIND:
				return validateReferenceBindingKind((ReferenceBindingKind)value, diagnostics, context);
			case ServicesPackage.DIAGNOSTIC_SEVERITY:
				return validateDiagnosticSeverity((DiagnosticSeverity)value, diagnostics, context);
			case ServicesPackage.FLAVOR_KIND:
				return validateFlavorKind((FlavorKind)value, diagnostics, context);
			case ServicesPackage.HTTP_METHOD:
				return validateHttpMethod((HttpMethod)value, diagnostics, context);
			case ServicesPackage.PARAMETER_BINDING:
				return validateParameterBinding((ParameterBinding)value, diagnostics, context);
			case ServicesPackage.MQTT_QOS:
				return validateMqttQos((MqttQos)value, diagnostics, context);
			case ServicesPackage.REGISTRY_KIND:
				return validateRegistryKind((RegistryKind)value, diagnostics, context);
			case ServicesPackage.EXPRESSION_LANGUAGE:
				return validateExpressionLanguage((ExpressionLanguage)value, diagnostics, context);
			case ServicesPackage.CATALOG_STATUS:
				return validateCatalogStatus((CatalogStatus)value, diagnostics, context);
			case ServicesPackage.UPDATE_POLICY:
				return validateUpdatePolicy((UpdatePolicy)value, diagnostics, context);
			case ServicesPackage.CONNECTION_STATE:
				return validateConnectionState((ConnectionState)value, diagnostics, context);
			case ServicesPackage.API_TYPE:
				return validateApiType((ApiType)value, diagnostics, context);
			default:
				return true;
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateNamedElement(NamedElement namedElement, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(namedElement, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateVersionedElement(VersionedElement versionedElement, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(versionedElement, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(versionedElement, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(versionedElement, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(versionedElement, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(versionedElement, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(versionedElement, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(versionedElement, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(versionedElement, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(versionedElement, diagnostics, context);
		if (result || diagnostics != null) result &= validateVersionedElement_validSemver(versionedElement, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the validSemver constraint of '<em>Versioned Element</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String VERSIONED_ELEMENT__VALID_SEMVER__EEXPRESSION = "version = null or version.matches('^\\\\d+\\\\.\\\\d+\\\\.\\\\d+(-[0-9A-Za-z.-]+)?$')";

	/**
	 * Validates the validSemver constraint of '<em>Versioned Element</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateVersionedElement_validSemver(VersionedElement versionedElement, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.VERSIONED_ELEMENT,
				 versionedElement,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "validSemver",
				 VERSIONED_ELEMENT__VALID_SEMVER__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateProperty(Property property, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(property, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateStringProperty(StringProperty stringProperty, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(stringProperty, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateIntProperty(IntProperty intProperty, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(intProperty, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateLongProperty(LongProperty longProperty, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(longProperty, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateDoubleProperty(DoubleProperty doubleProperty, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(doubleProperty, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateFloatProperty(FloatProperty floatProperty, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(floatProperty, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateShortProperty(ShortProperty shortProperty, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(shortProperty, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateBoolProperty(BoolProperty boolProperty, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(boolProperty, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateStringListProperty(StringListProperty stringListProperty, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(stringListProperty, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceOperation(ServiceOperation serviceOperation, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(serviceOperation, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateParameter(Parameter parameter, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(parameter, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validateParameter_typeOrEType(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validateParameter_boundsOrdered(parameter, diagnostics, context);
		if (result || diagnostics != null) result &= validateParameter_requiredSlotHasLowerBound(parameter, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the typeOrEType constraint of '<em>Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String PARAMETER__TYPE_OR_ETYPE__EEXPRESSION = "type <> null or eType <> null";

	/**
	 * Validates the typeOrEType constraint of '<em>Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateParameter_typeOrEType(Parameter parameter, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.PARAMETER,
				 parameter,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "typeOrEType",
				 PARAMETER__TYPE_OR_ETYPE__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the boundsOrdered constraint of '<em>Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String PARAMETER__BOUNDS_ORDERED__EEXPRESSION = "upperBound = -1 or upperBound >= lowerBound";

	/**
	 * Validates the boundsOrdered constraint of '<em>Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateParameter_boundsOrdered(Parameter parameter, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.PARAMETER,
				 parameter,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "boundsOrdered",
				 PARAMETER__BOUNDS_ORDERED__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the requiredSlotHasLowerBound constraint of '<em>Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String PARAMETER__REQUIRED_SLOT_HAS_LOWER_BOUND__EEXPRESSION = "optional or lowerBound >= 1";

	/**
	 * Validates the requiredSlotHasLowerBound constraint of '<em>Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateParameter_requiredSlotHasLowerBound(Parameter parameter, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.PARAMETER,
				 parameter,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "requiredSlotHasLowerBound",
				 PARAMETER__REQUIRED_SLOT_HAS_LOWER_BOUND__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateParameterConstraint(ParameterConstraint parameterConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(parameterConstraint, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRequiredConstraint(RequiredConstraint requiredConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(requiredConstraint, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateNumericRangeConstraint(NumericRangeConstraint numericRangeConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(numericRangeConstraint, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(numericRangeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(numericRangeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(numericRangeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(numericRangeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(numericRangeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(numericRangeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(numericRangeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(numericRangeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validateNumericRangeConstraint_atLeastOneBound(numericRangeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validateNumericRangeConstraint_rangeOrdered(numericRangeConstraint, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the atLeastOneBound constraint of '<em>Numeric Range Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String NUMERIC_RANGE_CONSTRAINT__AT_LEAST_ONE_BOUND__EEXPRESSION = "min <> null or max <> null";

	/**
	 * Validates the atLeastOneBound constraint of '<em>Numeric Range Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateNumericRangeConstraint_atLeastOneBound(NumericRangeConstraint numericRangeConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.NUMERIC_RANGE_CONSTRAINT,
				 numericRangeConstraint,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "atLeastOneBound",
				 NUMERIC_RANGE_CONSTRAINT__AT_LEAST_ONE_BOUND__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the rangeOrdered constraint of '<em>Numeric Range Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String NUMERIC_RANGE_CONSTRAINT__RANGE_ORDERED__EEXPRESSION = "min = null or max = null or min <= max";

	/**
	 * Validates the rangeOrdered constraint of '<em>Numeric Range Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateNumericRangeConstraint_rangeOrdered(NumericRangeConstraint numericRangeConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.NUMERIC_RANGE_CONSTRAINT,
				 numericRangeConstraint,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "rangeOrdered",
				 NUMERIC_RANGE_CONSTRAINT__RANGE_ORDERED__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateStringPatternConstraint(StringPatternConstraint stringPatternConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(stringPatternConstraint, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(stringPatternConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(stringPatternConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(stringPatternConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(stringPatternConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(stringPatternConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(stringPatternConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(stringPatternConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(stringPatternConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validateStringPatternConstraint_lengthBoundsNonNegative(stringPatternConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validateStringPatternConstraint_lengthBoundsOrdered(stringPatternConstraint, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the lengthBoundsNonNegative constraint of '<em>String Pattern Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String STRING_PATTERN_CONSTRAINT__LENGTH_BOUNDS_NON_NEGATIVE__EEXPRESSION = "(minLength = null or minLength >= 0) and (maxLength = null or maxLength >= 0)";

	/**
	 * Validates the lengthBoundsNonNegative constraint of '<em>String Pattern Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateStringPatternConstraint_lengthBoundsNonNegative(StringPatternConstraint stringPatternConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.STRING_PATTERN_CONSTRAINT,
				 stringPatternConstraint,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "lengthBoundsNonNegative",
				 STRING_PATTERN_CONSTRAINT__LENGTH_BOUNDS_NON_NEGATIVE__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the lengthBoundsOrdered constraint of '<em>String Pattern Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String STRING_PATTERN_CONSTRAINT__LENGTH_BOUNDS_ORDERED__EEXPRESSION = "minLength = null or maxLength = null or minLength <= maxLength";

	/**
	 * Validates the lengthBoundsOrdered constraint of '<em>String Pattern Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateStringPatternConstraint_lengthBoundsOrdered(StringPatternConstraint stringPatternConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.STRING_PATTERN_CONSTRAINT,
				 stringPatternConstraint,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "lengthBoundsOrdered",
				 STRING_PATTERN_CONSTRAINT__LENGTH_BOUNDS_ORDERED__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEnumerationConstraint(EnumerationConstraint enumerationConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(enumerationConstraint, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateExpressionConstraint(ExpressionConstraint expressionConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(expressionConstraint, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateInvariant(Invariant invariant, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(invariant, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCollectionSizeConstraint(CollectionSizeConstraint collectionSizeConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(collectionSizeConstraint, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(collectionSizeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(collectionSizeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(collectionSizeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(collectionSizeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(collectionSizeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(collectionSizeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(collectionSizeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(collectionSizeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validateCollectionSizeConstraint_sizeBoundsNonNegative(collectionSizeConstraint, diagnostics, context);
		if (result || diagnostics != null) result &= validateCollectionSizeConstraint_sizeBoundsOrdered(collectionSizeConstraint, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the sizeBoundsNonNegative constraint of '<em>Collection Size Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String COLLECTION_SIZE_CONSTRAINT__SIZE_BOUNDS_NON_NEGATIVE__EEXPRESSION = "(minSize = null or minSize >= 0) and (maxSize = null or maxSize >= 0)";

	/**
	 * Validates the sizeBoundsNonNegative constraint of '<em>Collection Size Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCollectionSizeConstraint_sizeBoundsNonNegative(CollectionSizeConstraint collectionSizeConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.COLLECTION_SIZE_CONSTRAINT,
				 collectionSizeConstraint,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "sizeBoundsNonNegative",
				 COLLECTION_SIZE_CONSTRAINT__SIZE_BOUNDS_NON_NEGATIVE__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the sizeBoundsOrdered constraint of '<em>Collection Size Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String COLLECTION_SIZE_CONSTRAINT__SIZE_BOUNDS_ORDERED__EEXPRESSION = "minSize = null or maxSize = null or minSize <= maxSize";

	/**
	 * Validates the sizeBoundsOrdered constraint of '<em>Collection Size Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCollectionSizeConstraint_sizeBoundsOrdered(CollectionSizeConstraint collectionSizeConstraint, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.COLLECTION_SIZE_CONSTRAINT,
				 collectionSizeConstraint,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "sizeBoundsOrdered",
				 COLLECTION_SIZE_CONSTRAINT__SIZE_BOUNDS_ORDERED__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceException(ServiceException serviceException, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(serviceException, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(serviceException, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(serviceException, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(serviceException, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(serviceException, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(serviceException, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(serviceException, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(serviceException, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(serviceException, diagnostics, context);
		if (result || diagnostics != null) result &= validateVersionedElement_validSemver(serviceException, diagnostics, context);
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceInterface(ServiceInterface serviceInterface, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(serviceInterface, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(serviceInterface, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(serviceInterface, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(serviceInterface, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(serviceInterface, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(serviceInterface, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(serviceInterface, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(serviceInterface, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(serviceInterface, diagnostics, context);
		if (result || diagnostics != null) result &= validateVersionedElement_validSemver(serviceInterface, diagnostics, context);
		if (result || diagnostics != null) result &= validateServiceInterface_replacedByIsDeprecated(serviceInterface, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the replacedByIsDeprecated constraint of '<em>Service Interface</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String SERVICE_INTERFACE__REPLACED_BY_IS_DEPRECATED__EEXPRESSION = "replacedBy = null or status.toString() = 'DEPRECATED'";

	/**
	 * Validates the replacedByIsDeprecated constraint of '<em>Service Interface</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceInterface_replacedByIsDeprecated(ServiceInterface serviceInterface, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.SERVICE_INTERFACE,
				 serviceInterface,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "replacedByIsDeprecated",
				 SERVICE_INTERFACE__REPLACED_BY_IS_DEPRECATED__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateLifecycleHook(LifecycleHook lifecycleHook, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(lifecycleHook, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateReferenceBinding(ReferenceBinding referenceBinding, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(referenceBinding, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateComponentReference(ComponentReference componentReference, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(componentReference, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateComponentDescription(ComponentDescription componentDescription, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(componentDescription, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceProvider(ServiceProvider serviceProvider, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(serviceProvider, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(serviceProvider, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(serviceProvider, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(serviceProvider, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(serviceProvider, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(serviceProvider, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(serviceProvider, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(serviceProvider, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(serviceProvider, diagnostics, context);
		if (result || diagnostics != null) result &= validateVersionedElement_validSemver(serviceProvider, diagnostics, context);
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceImplementation(ServiceImplementation serviceImplementation, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(serviceImplementation, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validateVersionedElement_validSemver(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validateServiceImplementation_atLeastOneInterface(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validateServiceImplementation_operationFlavorsCoverInterfaces(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validateServiceImplementation_replacesIsNotSelf(serviceImplementation, diagnostics, context);
		if (result || diagnostics != null) result &= validateServiceImplementation_cutoverGraceNonNegative(serviceImplementation, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the atLeastOneInterface constraint of '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String SERVICE_IMPLEMENTATION__AT_LEAST_ONE_INTERFACE__EEXPRESSION = "serviceInterfaces->notEmpty()";

	/**
	 * Validates the atLeastOneInterface constraint of '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceImplementation_atLeastOneInterface(ServiceImplementation serviceImplementation, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.SERVICE_IMPLEMENTATION,
				 serviceImplementation,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "atLeastOneInterface",
				 SERVICE_IMPLEMENTATION__AT_LEAST_ONE_INTERFACE__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the operationFlavorsCoverInterfaces constraint of '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String SERVICE_IMPLEMENTATION__OPERATION_FLAVORS_COVER_INTERFACES__EEXPRESSION = "flavors->forAll(f | f.operationFlavors->forAll(of | serviceInterfaces->exists(si | si.operations->includes(of.operation))))";

	/**
	 * Validates the operationFlavorsCoverInterfaces constraint of '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceImplementation_operationFlavorsCoverInterfaces(ServiceImplementation serviceImplementation, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.SERVICE_IMPLEMENTATION,
				 serviceImplementation,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "operationFlavorsCoverInterfaces",
				 SERVICE_IMPLEMENTATION__OPERATION_FLAVORS_COVER_INTERFACES__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the replacesIsNotSelf constraint of '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String SERVICE_IMPLEMENTATION__REPLACES_IS_NOT_SELF__EEXPRESSION = "replaces = null or replaces <> self";

	/**
	 * Validates the replacesIsNotSelf constraint of '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceImplementation_replacesIsNotSelf(ServiceImplementation serviceImplementation, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.SERVICE_IMPLEMENTATION,
				 serviceImplementation,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "replacesIsNotSelf",
				 SERVICE_IMPLEMENTATION__REPLACES_IS_NOT_SELF__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the cutoverGraceNonNegative constraint of '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String SERVICE_IMPLEMENTATION__CUTOVER_GRACE_NON_NEGATIVE__EEXPRESSION = "cutoverGraceMillis >= 0";

	/**
	 * Validates the cutoverGraceNonNegative constraint of '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceImplementation_cutoverGraceNonNegative(ServiceImplementation serviceImplementation, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.SERVICE_IMPLEMENTATION,
				 serviceImplementation,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "cutoverGraceNonNegative",
				 SERVICE_IMPLEMENTATION__CUTOVER_GRACE_NON_NEGATIVE__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceFlavor(ServiceFlavor serviceFlavor, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(serviceFlavor, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRestFlavor(RestFlavor restFlavor, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(restFlavor, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateMqttFlavor(MqttFlavor mqttFlavor, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(mqttFlavor, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceOperationFlavor(ServiceOperationFlavor serviceOperationFlavor, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(serviceOperationFlavor, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRestOperationFlavor(RestOperationFlavor restOperationFlavor, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(restOperationFlavor, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validateRestOperationFlavor_bindingsReferenceOperationParameters(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validateRestOperationFlavor_oneBindingPerParameter(restOperationFlavor, diagnostics, context);
		if (result || diagnostics != null) result &= validateRestOperationFlavor_pathBindingsNeedPath(restOperationFlavor, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the bindingsReferenceOperationParameters constraint of '<em>Rest Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String REST_OPERATION_FLAVOR__BINDINGS_REFERENCE_OPERATION_PARAMETERS__EEXPRESSION = "parameterBindings->forAll(b | operation.parameters->includes(b.parameter))";

	/**
	 * Validates the bindingsReferenceOperationParameters constraint of '<em>Rest Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRestOperationFlavor_bindingsReferenceOperationParameters(RestOperationFlavor restOperationFlavor, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.REST_OPERATION_FLAVOR,
				 restOperationFlavor,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "bindingsReferenceOperationParameters",
				 REST_OPERATION_FLAVOR__BINDINGS_REFERENCE_OPERATION_PARAMETERS__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the oneBindingPerParameter constraint of '<em>Rest Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String REST_OPERATION_FLAVOR__ONE_BINDING_PER_PARAMETER__EEXPRESSION = "parameterBindings->isUnique(b | b.parameter)";

	/**
	 * Validates the oneBindingPerParameter constraint of '<em>Rest Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRestOperationFlavor_oneBindingPerParameter(RestOperationFlavor restOperationFlavor, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.REST_OPERATION_FLAVOR,
				 restOperationFlavor,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "oneBindingPerParameter",
				 REST_OPERATION_FLAVOR__ONE_BINDING_PER_PARAMETER__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the pathBindingsNeedPath constraint of '<em>Rest Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String REST_OPERATION_FLAVOR__PATH_BINDINGS_NEED_PATH__EEXPRESSION = "parameterBindings->forAll(b | b.binding.toString() <> 'PATH' or path <> null)";

	/**
	 * Validates the pathBindingsNeedPath constraint of '<em>Rest Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRestOperationFlavor_pathBindingsNeedPath(RestOperationFlavor restOperationFlavor, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.REST_OPERATION_FLAVOR,
				 restOperationFlavor,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "pathBindingsNeedPath",
				 REST_OPERATION_FLAVOR__PATH_BINDINGS_NEED_PATH__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRestParameterBinding(RestParameterBinding restParameterBinding, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(restParameterBinding, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateMqttOperationFlavor(MqttOperationFlavor mqttOperationFlavor, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(mqttOperationFlavor, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceReference(ServiceReference serviceReference, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(serviceReference, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceRegistration(ServiceRegistration serviceRegistration, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(serviceRegistration, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(serviceRegistration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(serviceRegistration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(serviceRegistration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(serviceRegistration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(serviceRegistration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(serviceRegistration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(serviceRegistration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(serviceRegistration, diagnostics, context);
		if (result || diagnostics != null) result &= validateServiceRegistration_unregisteredNotInRegistry(serviceRegistration, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the unregisteredNotInRegistry constraint of '<em>Service Registration</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String SERVICE_REGISTRATION__UNREGISTERED_NOT_IN_REGISTRY__EEXPRESSION = "not unregistered or LocalServiceRegistry.allInstances()->forAll(r | not r.registrations->includes(self))";

	/**
	 * Validates the unregisteredNotInRegistry constraint of '<em>Service Registration</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceRegistration_unregisteredNotInRegistry(ServiceRegistration serviceRegistration, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.SERVICE_REGISTRATION,
				 serviceRegistration,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "unregisteredNotInRegistry",
				 SERVICE_REGISTRATION__UNREGISTERED_NOT_IN_REGISTRY__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateConsumerSession(ConsumerSession consumerSession, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(consumerSession, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateComponentConfiguration(ComponentConfiguration componentConfiguration, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(componentConfiguration, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(componentConfiguration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(componentConfiguration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(componentConfiguration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(componentConfiguration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(componentConfiguration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(componentConfiguration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(componentConfiguration, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(componentConfiguration, diagnostics, context);
		if (result || diagnostics != null) result &= validateComponentConfiguration_failureOnlyWhenFailed(componentConfiguration, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the failureOnlyWhenFailed constraint of '<em>Component Configuration</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String COMPONENT_CONFIGURATION__FAILURE_ONLY_WHEN_FAILED__EEXPRESSION = "(state.toString() = 'FAILED_ACTIVATION') = (failure <> null)";

	/**
	 * Validates the failureOnlyWhenFailed constraint of '<em>Component Configuration</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateComponentConfiguration_failureOnlyWhenFailed(ComponentConfiguration componentConfiguration, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.COMPONENT_CONFIGURATION,
				 componentConfiguration,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "failureOnlyWhenFailed",
				 COMPONENT_CONFIGURATION__FAILURE_ONLY_WHEN_FAILED__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateSatisfiedReference(SatisfiedReference satisfiedReference, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(satisfiedReference, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateUnsatisfiedReference(UnsatisfiedReference unsatisfiedReference, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(unsatisfiedReference, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateDiagnostic(Diagnostic diagnostic, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(diagnostic, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceEvent(ServiceEvent serviceEvent, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(serviceEvent, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceListener(ServiceListener serviceListener, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(serviceListener, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceRegistry(ServiceRegistry serviceRegistry, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(serviceRegistry, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateLocalServiceRegistry(LocalServiceRegistry localServiceRegistry, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(localServiceRegistry, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRemoteServiceRegistry(RemoteServiceRegistry remoteServiceRegistry, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(remoteServiceRegistry, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validateRemoteServiceRegistry_publishedImplsHaveFlavor(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validateRemoteServiceRegistry_publishedImplsReferenceCatalog(remoteServiceRegistry, diagnostics, context);
		if (result || diagnostics != null) result &= validateRemoteServiceRegistry_publishedImplsOwnedByListedProvider(remoteServiceRegistry, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the publishedImplsHaveFlavor constraint of '<em>Remote Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String REMOTE_SERVICE_REGISTRY__PUBLISHED_IMPLS_HAVE_FLAVOR__EEXPRESSION = "implementations->forAll(i | i.flavors->notEmpty())";

	/**
	 * Validates the publishedImplsHaveFlavor constraint of '<em>Remote Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRemoteServiceRegistry_publishedImplsHaveFlavor(RemoteServiceRegistry remoteServiceRegistry, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.REMOTE_SERVICE_REGISTRY,
				 remoteServiceRegistry,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "publishedImplsHaveFlavor",
				 REMOTE_SERVICE_REGISTRY__PUBLISHED_IMPLS_HAVE_FLAVOR__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the publishedImplsReferenceCatalog constraint of '<em>Remote Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String REMOTE_SERVICE_REGISTRY__PUBLISHED_IMPLS_REFERENCE_CATALOG__EEXPRESSION = "implementations->forAll(i | i.serviceInterfaces->forAll(si | catalog->includes(si)))";

	/**
	 * Validates the publishedImplsReferenceCatalog constraint of '<em>Remote Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRemoteServiceRegistry_publishedImplsReferenceCatalog(RemoteServiceRegistry remoteServiceRegistry, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.REMOTE_SERVICE_REGISTRY,
				 remoteServiceRegistry,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "publishedImplsReferenceCatalog",
				 REMOTE_SERVICE_REGISTRY__PUBLISHED_IMPLS_REFERENCE_CATALOG__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the publishedImplsOwnedByListedProvider constraint of '<em>Remote Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String REMOTE_SERVICE_REGISTRY__PUBLISHED_IMPLS_OWNED_BY_LISTED_PROVIDER__EEXPRESSION = "implementations->forAll(i | providers->exists(p | p.implementations->includes(i)))";

	/**
	 * Validates the publishedImplsOwnedByListedProvider constraint of '<em>Remote Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRemoteServiceRegistry_publishedImplsOwnedByListedProvider(RemoteServiceRegistry remoteServiceRegistry, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.REMOTE_SERVICE_REGISTRY,
				 remoteServiceRegistry,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "publishedImplsOwnedByListedProvider",
				 REMOTE_SERVICE_REGISTRY__PUBLISHED_IMPLS_OWNED_BY_LISTED_PROVIDER__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCapability(Capability capability, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(capability, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(capability, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(capability, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(capability, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(capability, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(capability, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(capability, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(capability, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(capability, diagnostics, context);
		if (result || diagnostics != null) result &= validateCapability_attributeNamesUnique(capability, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the attributeNamesUnique constraint of '<em>Capability</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String CAPABILITY__ATTRIBUTE_NAMES_UNIQUE__EEXPRESSION = "attributes->isUnique(a | a.name)";

	/**
	 * Validates the attributeNamesUnique constraint of '<em>Capability</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCapability_attributeNamesUnique(Capability capability, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.CAPABILITY,
				 capability,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "attributeNamesUnique",
				 CAPABILITY__ATTRIBUTE_NAMES_UNIQUE__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRequirement(Requirement requirement, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(requirement, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateConsumerCapability(ConsumerCapability consumerCapability, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(consumerCapability, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validatePublishHook(PublishHook publishHook, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(publishHook, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateDiscoveryHook(DiscoveryHook discoveryHook, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(discoveryHook, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateDistributionHook(DistributionHook distributionHook, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(distributionHook, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateLanguageBinding(LanguageBinding languageBinding, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(languageBinding, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(languageBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(languageBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(languageBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(languageBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(languageBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(languageBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(languageBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(languageBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validateLanguageBinding_uniqueTypeMappings(languageBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validateLanguageBinding_uniquePackageMappings(languageBinding, diagnostics, context);
		return result;
	}

	/**
	 * The cached validation expression for the uniqueTypeMappings constraint of '<em>Language Binding</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String LANGUAGE_BINDING__UNIQUE_TYPE_MAPPINGS__EEXPRESSION = "typeMappings->isUnique(neutralType)";

	/**
	 * Validates the uniqueTypeMappings constraint of '<em>Language Binding</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateLanguageBinding_uniqueTypeMappings(LanguageBinding languageBinding, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.LANGUAGE_BINDING,
				 languageBinding,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "uniqueTypeMappings",
				 LANGUAGE_BINDING__UNIQUE_TYPE_MAPPINGS__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * The cached validation expression for the uniquePackageMappings constraint of '<em>Language Binding</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final String LANGUAGE_BINDING__UNIQUE_PACKAGE_MAPPINGS__EEXPRESSION = "packageMappings->isUnique(nsURI)";

	/**
	 * Validates the uniquePackageMappings constraint of '<em>Language Binding</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateLanguageBinding_uniquePackageMappings(LanguageBinding languageBinding, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return
			validate
				(ServicesPackage.Literals.LANGUAGE_BINDING,
				 languageBinding,
				 diagnostics,
				 context,
				 "http://www.eclipse.org/fennec/m2x/ocl/1.0",
				 "uniquePackageMappings",
				 LANGUAGE_BINDING__UNIQUE_PACKAGE_MAPPINGS__EEXPRESSION,
				 org.eclipse.emf.common.util.Diagnostic.ERROR,
				 DIAGNOSTIC_SOURCE,
				 0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTypeMapping(TypeMapping typeMapping, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(typeMapping, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validatePackageMapping(PackageMapping packageMapping, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate_EveryDefaultConstraint(packageMapping, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateJavaBinding(JavaBinding javaBinding, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(javaBinding, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(javaBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(javaBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(javaBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(javaBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(javaBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(javaBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(javaBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(javaBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validateLanguageBinding_uniqueTypeMappings(javaBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validateLanguageBinding_uniquePackageMappings(javaBinding, diagnostics, context);
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTypeScriptBinding(TypeScriptBinding typeScriptBinding, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(typeScriptBinding, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(typeScriptBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(typeScriptBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(typeScriptBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(typeScriptBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(typeScriptBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(typeScriptBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(typeScriptBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(typeScriptBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validateLanguageBinding_uniqueTypeMappings(typeScriptBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validateLanguageBinding_uniquePackageMappings(typeScriptBinding, diagnostics, context);
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validatePythonBinding(PythonBinding pythonBinding, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (!validate_NoCircularContainment(pythonBinding, diagnostics, context)) return false;
		boolean result = validate_EveryMultiplicityConforms(pythonBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryDataValueConforms(pythonBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryReferenceIsContained(pythonBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryBidirectionalReferenceIsPaired(pythonBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryProxyResolves(pythonBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_UniqueID(pythonBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryKeyUnique(pythonBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validate_EveryMapEntryUnique(pythonBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validateLanguageBinding_uniqueTypeMappings(pythonBinding, diagnostics, context);
		if (result || diagnostics != null) result &= validateLanguageBinding_uniquePackageMappings(pythonBinding, diagnostics, context);
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceScope(ServiceScope serviceScope, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateReferenceCardinality(ReferenceCardinality referenceCardinality, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateReferencePolicy(ReferencePolicy referencePolicy, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateReferencePolicyOption(ReferencePolicyOption referencePolicyOption, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateConfigurationPolicy(ConfigurationPolicy configurationPolicy, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateComponentState(ComponentState componentState, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateServiceEventType(ServiceEventType serviceEventType, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateFieldOption(FieldOption fieldOption, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCollectionType(CollectionType collectionType, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateLifecycleHookKind(LifecycleHookKind lifecycleHookKind, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateReferenceBindingKind(ReferenceBindingKind referenceBindingKind, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateDiagnosticSeverity(DiagnosticSeverity diagnosticSeverity, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateFlavorKind(FlavorKind flavorKind, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateHttpMethod(HttpMethod httpMethod, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateParameterBinding(ParameterBinding parameterBinding, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateMqttQos(MqttQos mqttQos, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRegistryKind(RegistryKind registryKind, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateExpressionLanguage(ExpressionLanguage expressionLanguage, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCatalogStatus(CatalogStatus catalogStatus, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateUpdatePolicy(UpdatePolicy updatePolicy, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateConnectionState(ConnectionState connectionState, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateApiType(ApiType apiType, DiagnosticChain diagnostics, Map<Object, Object> context) {
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

} //ServicesValidator
