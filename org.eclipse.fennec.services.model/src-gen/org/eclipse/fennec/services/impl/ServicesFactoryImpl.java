/**
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.eclipse.fennec.services.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class ServicesFactoryImpl extends EFactoryImpl implements ServicesFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static ServicesFactory init() {
		try {
			ServicesFactory theServicesFactory = (ServicesFactory)EPackage.Registry.INSTANCE.getEFactory(ServicesPackage.eNS_URI);
			if (theServicesFactory != null) {
				return theServicesFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new ServicesFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServicesFactoryImpl() {
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
			case ServicesPackage.STRING_PROPERTY: return createStringProperty();
			case ServicesPackage.INT_PROPERTY: return createIntProperty();
			case ServicesPackage.LONG_PROPERTY: return createLongProperty();
			case ServicesPackage.DOUBLE_PROPERTY: return createDoubleProperty();
			case ServicesPackage.FLOAT_PROPERTY: return createFloatProperty();
			case ServicesPackage.SHORT_PROPERTY: return createShortProperty();
			case ServicesPackage.BOOL_PROPERTY: return createBoolProperty();
			case ServicesPackage.STRING_LIST_PROPERTY: return createStringListProperty();
			case ServicesPackage.SERVICE_OPERATION: return createServiceOperation();
			case ServicesPackage.PARAMETER: return createParameter();
			case ServicesPackage.REQUIRED_CONSTRAINT: return createRequiredConstraint();
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT: return createNumericRangeConstraint();
			case ServicesPackage.STRING_PATTERN_CONSTRAINT: return createStringPatternConstraint();
			case ServicesPackage.ENUMERATION_CONSTRAINT: return createEnumerationConstraint();
			case ServicesPackage.EXPRESSION_CONSTRAINT: return createExpressionConstraint();
			case ServicesPackage.INVARIANT: return createInvariant();
			case ServicesPackage.COLLECTION_SIZE_CONSTRAINT: return createCollectionSizeConstraint();
			case ServicesPackage.SERVICE_EXCEPTION: return createServiceException();
			case ServicesPackage.SERVICE_INTERFACE: return createServiceInterface();
			case ServicesPackage.LIFECYCLE_HOOK: return createLifecycleHook();
			case ServicesPackage.REFERENCE_BINDING: return createReferenceBinding();
			case ServicesPackage.COMPONENT_REFERENCE: return createComponentReference();
			case ServicesPackage.COMPONENT_DESCRIPTION: return createComponentDescription();
			case ServicesPackage.SERVICE_PROVIDER: return createServiceProvider();
			case ServicesPackage.SERVICE_IMPLEMENTATION: return createServiceImplementation();
			case ServicesPackage.REST_FLAVOR: return createRestFlavor();
			case ServicesPackage.MQTT_FLAVOR: return createMqttFlavor();
			case ServicesPackage.REST_OPERATION_FLAVOR: return createRestOperationFlavor();
			case ServicesPackage.MQTT_OPERATION_FLAVOR: return createMqttOperationFlavor();
			case ServicesPackage.SERVICE_REFERENCE: return createServiceReference();
			case ServicesPackage.SERVICE_REGISTRATION: return createServiceRegistration();
			case ServicesPackage.CONSUMER_SESSION: return createConsumerSession();
			case ServicesPackage.COMPONENT_CONFIGURATION: return createComponentConfiguration();
			case ServicesPackage.SATISFIED_REFERENCE: return createSatisfiedReference();
			case ServicesPackage.UNSATISFIED_REFERENCE: return createUnsatisfiedReference();
			case ServicesPackage.DIAGNOSTIC: return createDiagnostic();
			case ServicesPackage.SERVICE_EVENT: return createServiceEvent();
			case ServicesPackage.LOCAL_SERVICE_REGISTRY: return createLocalServiceRegistry();
			case ServicesPackage.REMOTE_SERVICE_REGISTRY: return createRemoteServiceRegistry();
			case ServicesPackage.CONSUMER_CAPABILITY: return createConsumerCapability();
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
			case ServicesPackage.SERVICE_SCOPE:
				return createServiceScopeFromString(eDataType, initialValue);
			case ServicesPackage.REFERENCE_CARDINALITY:
				return createReferenceCardinalityFromString(eDataType, initialValue);
			case ServicesPackage.REFERENCE_POLICY:
				return createReferencePolicyFromString(eDataType, initialValue);
			case ServicesPackage.REFERENCE_POLICY_OPTION:
				return createReferencePolicyOptionFromString(eDataType, initialValue);
			case ServicesPackage.CONFIGURATION_POLICY:
				return createConfigurationPolicyFromString(eDataType, initialValue);
			case ServicesPackage.COMPONENT_STATE:
				return createComponentStateFromString(eDataType, initialValue);
			case ServicesPackage.SERVICE_EVENT_TYPE:
				return createServiceEventTypeFromString(eDataType, initialValue);
			case ServicesPackage.FIELD_OPTION:
				return createFieldOptionFromString(eDataType, initialValue);
			case ServicesPackage.COLLECTION_TYPE:
				return createCollectionTypeFromString(eDataType, initialValue);
			case ServicesPackage.LIFECYCLE_HOOK_KIND:
				return createLifecycleHookKindFromString(eDataType, initialValue);
			case ServicesPackage.REFERENCE_BINDING_KIND:
				return createReferenceBindingKindFromString(eDataType, initialValue);
			case ServicesPackage.DIAGNOSTIC_SEVERITY:
				return createDiagnosticSeverityFromString(eDataType, initialValue);
			case ServicesPackage.FLAVOR_KIND:
				return createFlavorKindFromString(eDataType, initialValue);
			case ServicesPackage.HTTP_METHOD:
				return createHttpMethodFromString(eDataType, initialValue);
			case ServicesPackage.MQTT_QOS:
				return createMqttQosFromString(eDataType, initialValue);
			case ServicesPackage.REGISTRY_KIND:
				return createRegistryKindFromString(eDataType, initialValue);
			case ServicesPackage.EXPRESSION_LANGUAGE:
				return createExpressionLanguageFromString(eDataType, initialValue);
			case ServicesPackage.CATALOG_STATUS:
				return createCatalogStatusFromString(eDataType, initialValue);
			case ServicesPackage.CONNECTION_STATE:
				return createConnectionStateFromString(eDataType, initialValue);
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
			case ServicesPackage.SERVICE_SCOPE:
				return convertServiceScopeToString(eDataType, instanceValue);
			case ServicesPackage.REFERENCE_CARDINALITY:
				return convertReferenceCardinalityToString(eDataType, instanceValue);
			case ServicesPackage.REFERENCE_POLICY:
				return convertReferencePolicyToString(eDataType, instanceValue);
			case ServicesPackage.REFERENCE_POLICY_OPTION:
				return convertReferencePolicyOptionToString(eDataType, instanceValue);
			case ServicesPackage.CONFIGURATION_POLICY:
				return convertConfigurationPolicyToString(eDataType, instanceValue);
			case ServicesPackage.COMPONENT_STATE:
				return convertComponentStateToString(eDataType, instanceValue);
			case ServicesPackage.SERVICE_EVENT_TYPE:
				return convertServiceEventTypeToString(eDataType, instanceValue);
			case ServicesPackage.FIELD_OPTION:
				return convertFieldOptionToString(eDataType, instanceValue);
			case ServicesPackage.COLLECTION_TYPE:
				return convertCollectionTypeToString(eDataType, instanceValue);
			case ServicesPackage.LIFECYCLE_HOOK_KIND:
				return convertLifecycleHookKindToString(eDataType, instanceValue);
			case ServicesPackage.REFERENCE_BINDING_KIND:
				return convertReferenceBindingKindToString(eDataType, instanceValue);
			case ServicesPackage.DIAGNOSTIC_SEVERITY:
				return convertDiagnosticSeverityToString(eDataType, instanceValue);
			case ServicesPackage.FLAVOR_KIND:
				return convertFlavorKindToString(eDataType, instanceValue);
			case ServicesPackage.HTTP_METHOD:
				return convertHttpMethodToString(eDataType, instanceValue);
			case ServicesPackage.MQTT_QOS:
				return convertMqttQosToString(eDataType, instanceValue);
			case ServicesPackage.REGISTRY_KIND:
				return convertRegistryKindToString(eDataType, instanceValue);
			case ServicesPackage.EXPRESSION_LANGUAGE:
				return convertExpressionLanguageToString(eDataType, instanceValue);
			case ServicesPackage.CATALOG_STATUS:
				return convertCatalogStatusToString(eDataType, instanceValue);
			case ServicesPackage.CONNECTION_STATE:
				return convertConnectionStateToString(eDataType, instanceValue);
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
	public StringProperty createStringProperty() {
		StringPropertyImpl stringProperty = new StringPropertyImpl();
		return stringProperty;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public IntProperty createIntProperty() {
		IntPropertyImpl intProperty = new IntPropertyImpl();
		return intProperty;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public LongProperty createLongProperty() {
		LongPropertyImpl longProperty = new LongPropertyImpl();
		return longProperty;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public DoubleProperty createDoubleProperty() {
		DoublePropertyImpl doubleProperty = new DoublePropertyImpl();
		return doubleProperty;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public FloatProperty createFloatProperty() {
		FloatPropertyImpl floatProperty = new FloatPropertyImpl();
		return floatProperty;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ShortProperty createShortProperty() {
		ShortPropertyImpl shortProperty = new ShortPropertyImpl();
		return shortProperty;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public BoolProperty createBoolProperty() {
		BoolPropertyImpl boolProperty = new BoolPropertyImpl();
		return boolProperty;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public StringListProperty createStringListProperty() {
		StringListPropertyImpl stringListProperty = new StringListPropertyImpl();
		return stringListProperty;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceOperation createServiceOperation() {
		ServiceOperationImpl serviceOperation = new ServiceOperationImpl();
		return serviceOperation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Parameter createParameter() {
		ParameterImpl parameter = new ParameterImpl();
		return parameter;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public RequiredConstraint createRequiredConstraint() {
		RequiredConstraintImpl requiredConstraint = new RequiredConstraintImpl();
		return requiredConstraint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NumericRangeConstraint createNumericRangeConstraint() {
		NumericRangeConstraintImpl numericRangeConstraint = new NumericRangeConstraintImpl();
		return numericRangeConstraint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public StringPatternConstraint createStringPatternConstraint() {
		StringPatternConstraintImpl stringPatternConstraint = new StringPatternConstraintImpl();
		return stringPatternConstraint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EnumerationConstraint createEnumerationConstraint() {
		EnumerationConstraintImpl enumerationConstraint = new EnumerationConstraintImpl();
		return enumerationConstraint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ExpressionConstraint createExpressionConstraint() {
		ExpressionConstraintImpl expressionConstraint = new ExpressionConstraintImpl();
		return expressionConstraint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Invariant createInvariant() {
		InvariantImpl invariant = new InvariantImpl();
		return invariant;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public CollectionSizeConstraint createCollectionSizeConstraint() {
		CollectionSizeConstraintImpl collectionSizeConstraint = new CollectionSizeConstraintImpl();
		return collectionSizeConstraint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceException createServiceException() {
		ServiceExceptionImpl serviceException = new ServiceExceptionImpl();
		return serviceException;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceInterface createServiceInterface() {
		ServiceInterfaceImpl serviceInterface = new ServiceInterfaceImpl();
		return serviceInterface;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public LifecycleHook createLifecycleHook() {
		LifecycleHookImpl lifecycleHook = new LifecycleHookImpl();
		return lifecycleHook;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ReferenceBinding createReferenceBinding() {
		ReferenceBindingImpl referenceBinding = new ReferenceBindingImpl();
		return referenceBinding;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ComponentReference createComponentReference() {
		ComponentReferenceImpl componentReference = new ComponentReferenceImpl();
		return componentReference;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ComponentDescription createComponentDescription() {
		ComponentDescriptionImpl componentDescription = new ComponentDescriptionImpl();
		return componentDescription;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceProvider createServiceProvider() {
		ServiceProviderImpl serviceProvider = new ServiceProviderImpl();
		return serviceProvider;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceImplementation createServiceImplementation() {
		ServiceImplementationImpl serviceImplementation = new ServiceImplementationImpl();
		return serviceImplementation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public RestFlavor createRestFlavor() {
		RestFlavorImpl restFlavor = new RestFlavorImpl();
		return restFlavor;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public MqttFlavor createMqttFlavor() {
		MqttFlavorImpl mqttFlavor = new MqttFlavorImpl();
		return mqttFlavor;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public RestOperationFlavor createRestOperationFlavor() {
		RestOperationFlavorImpl restOperationFlavor = new RestOperationFlavorImpl();
		return restOperationFlavor;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public MqttOperationFlavor createMqttOperationFlavor() {
		MqttOperationFlavorImpl mqttOperationFlavor = new MqttOperationFlavorImpl();
		return mqttOperationFlavor;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceReference createServiceReference() {
		ServiceReferenceImpl serviceReference = new ServiceReferenceImpl();
		return serviceReference;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceRegistration createServiceRegistration() {
		ServiceRegistrationImpl serviceRegistration = new ServiceRegistrationImpl();
		return serviceRegistration;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ConsumerSession createConsumerSession() {
		ConsumerSessionImpl consumerSession = new ConsumerSessionImpl();
		return consumerSession;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ComponentConfiguration createComponentConfiguration() {
		ComponentConfigurationImpl componentConfiguration = new ComponentConfigurationImpl();
		return componentConfiguration;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public SatisfiedReference createSatisfiedReference() {
		SatisfiedReferenceImpl satisfiedReference = new SatisfiedReferenceImpl();
		return satisfiedReference;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public UnsatisfiedReference createUnsatisfiedReference() {
		UnsatisfiedReferenceImpl unsatisfiedReference = new UnsatisfiedReferenceImpl();
		return unsatisfiedReference;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Diagnostic createDiagnostic() {
		DiagnosticImpl diagnostic = new DiagnosticImpl();
		return diagnostic;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServiceEvent createServiceEvent() {
		ServiceEventImpl serviceEvent = new ServiceEventImpl();
		return serviceEvent;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public LocalServiceRegistry createLocalServiceRegistry() {
		LocalServiceRegistryImpl localServiceRegistry = new LocalServiceRegistryImpl();
		return localServiceRegistry;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public RemoteServiceRegistry createRemoteServiceRegistry() {
		RemoteServiceRegistryImpl remoteServiceRegistry = new RemoteServiceRegistryImpl();
		return remoteServiceRegistry;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ConsumerCapability createConsumerCapability() {
		ConsumerCapabilityImpl consumerCapability = new ConsumerCapabilityImpl();
		return consumerCapability;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServiceScope createServiceScopeFromString(EDataType eDataType, String initialValue) {
		ServiceScope result = ServiceScope.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertServiceScopeToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ReferenceCardinality createReferenceCardinalityFromString(EDataType eDataType, String initialValue) {
		ReferenceCardinality result = ReferenceCardinality.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertReferenceCardinalityToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ReferencePolicy createReferencePolicyFromString(EDataType eDataType, String initialValue) {
		ReferencePolicy result = ReferencePolicy.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertReferencePolicyToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ReferencePolicyOption createReferencePolicyOptionFromString(EDataType eDataType, String initialValue) {
		ReferencePolicyOption result = ReferencePolicyOption.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertReferencePolicyOptionToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConfigurationPolicy createConfigurationPolicyFromString(EDataType eDataType, String initialValue) {
		ConfigurationPolicy result = ConfigurationPolicy.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertConfigurationPolicyToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComponentState createComponentStateFromString(EDataType eDataType, String initialValue) {
		ComponentState result = ComponentState.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertComponentStateToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServiceEventType createServiceEventTypeFromString(EDataType eDataType, String initialValue) {
		ServiceEventType result = ServiceEventType.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertServiceEventTypeToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FieldOption createFieldOptionFromString(EDataType eDataType, String initialValue) {
		FieldOption result = FieldOption.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertFieldOptionToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CollectionType createCollectionTypeFromString(EDataType eDataType, String initialValue) {
		CollectionType result = CollectionType.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertCollectionTypeToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public LifecycleHookKind createLifecycleHookKindFromString(EDataType eDataType, String initialValue) {
		LifecycleHookKind result = LifecycleHookKind.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertLifecycleHookKindToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ReferenceBindingKind createReferenceBindingKindFromString(EDataType eDataType, String initialValue) {
		ReferenceBindingKind result = ReferenceBindingKind.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertReferenceBindingKindToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public DiagnosticSeverity createDiagnosticSeverityFromString(EDataType eDataType, String initialValue) {
		DiagnosticSeverity result = DiagnosticSeverity.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertDiagnosticSeverityToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FlavorKind createFlavorKindFromString(EDataType eDataType, String initialValue) {
		FlavorKind result = FlavorKind.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertFlavorKindToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public HttpMethod createHttpMethodFromString(EDataType eDataType, String initialValue) {
		HttpMethod result = HttpMethod.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertHttpMethodToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MqttQos createMqttQosFromString(EDataType eDataType, String initialValue) {
		MqttQos result = MqttQos.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertMqttQosToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RegistryKind createRegistryKindFromString(EDataType eDataType, String initialValue) {
		RegistryKind result = RegistryKind.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertRegistryKindToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ExpressionLanguage createExpressionLanguageFromString(EDataType eDataType, String initialValue) {
		ExpressionLanguage result = ExpressionLanguage.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertExpressionLanguageToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CatalogStatus createCatalogStatusFromString(EDataType eDataType, String initialValue) {
		CatalogStatus result = CatalogStatus.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertCatalogStatusToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConnectionState createConnectionStateFromString(EDataType eDataType, String initialValue) {
		ConnectionState result = ConnectionState.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertConnectionStateToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServicesPackage getServicesPackage() {
		return (ServicesPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static ServicesPackage getPackage() {
		return ServicesPackage.eINSTANCE;
	}

} //ServicesFactoryImpl
