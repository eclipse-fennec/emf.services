/**
 */
package org.eclipse.fennec.services.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.emf.ecore.impl.EPackageImpl;

import org.eclipse.fennec.services.BoolProperty;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.CollectionSizeConstraint;
import org.eclipse.fennec.services.CollectionType;
import org.eclipse.fennec.services.ComponentConfiguration;
import org.eclipse.fennec.services.ComponentDescription;
import org.eclipse.fennec.services.ComponentReference;
import org.eclipse.fennec.services.ComponentState;
import org.eclipse.fennec.services.ConfigurationPolicy;
import org.eclipse.fennec.services.ConnectionState;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.DiscoveryHook;
import org.eclipse.fennec.services.DistributionHook;
import org.eclipse.fennec.services.DoubleProperty;
import org.eclipse.fennec.services.EnumerationConstraint;
import org.eclipse.fennec.services.ExpressionConstraint;
import org.eclipse.fennec.services.ExpressionLanguage;
import org.eclipse.fennec.services.FieldOption;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.FloatProperty;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.Invariant;
import org.eclipse.fennec.services.LifecycleHook;
import org.eclipse.fennec.services.LifecycleHookKind;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.LongProperty;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.MqttQos;
import org.eclipse.fennec.services.NamedElement;
import org.eclipse.fennec.services.NumericRangeConstraint;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterConstraint;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.PublishHook;
import org.eclipse.fennec.services.ReferenceBinding;
import org.eclipse.fennec.services.ReferenceBindingKind;
import org.eclipse.fennec.services.ReferenceCardinality;
import org.eclipse.fennec.services.ReferencePolicy;
import org.eclipse.fennec.services.ReferencePolicyOption;
import org.eclipse.fennec.services.RegistryKind;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.RequiredConstraint;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.SatisfiedReference;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceListener;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServiceRegistry;
import org.eclipse.fennec.services.ServiceScope;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.ShortProperty;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringPatternConstraint;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.UnsatisfiedReference;
import org.eclipse.fennec.services.VersionedElement;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class ServicesPackageImpl extends EPackageImpl implements ServicesPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass namedElementEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass versionedElementEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass propertyEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass stringPropertyEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass intPropertyEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass longPropertyEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass doublePropertyEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass floatPropertyEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass shortPropertyEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass boolPropertyEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass stringListPropertyEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceOperationEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass parameterEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass parameterConstraintEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass requiredConstraintEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass numericRangeConstraintEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass stringPatternConstraintEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass enumerationConstraintEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass expressionConstraintEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass invariantEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass collectionSizeConstraintEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceExceptionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceInterfaceEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass lifecycleHookEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass referenceBindingEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass componentReferenceEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass componentDescriptionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceProviderEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceImplementationEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceFlavorEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass restFlavorEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass mqttFlavorEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceOperationFlavorEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass restOperationFlavorEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass mqttOperationFlavorEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceReferenceEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceRegistrationEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass consumerSessionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass componentConfigurationEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass satisfiedReferenceEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass unsatisfiedReferenceEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass diagnosticEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceEventEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceListenerEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serviceRegistryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass localServiceRegistryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass remoteServiceRegistryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass consumerCapabilityEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass publishHookEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass discoveryHookEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass distributionHookEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum serviceScopeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum referenceCardinalityEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum referencePolicyEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum referencePolicyOptionEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum configurationPolicyEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum componentStateEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum serviceEventTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum fieldOptionEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum collectionTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum lifecycleHookKindEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum referenceBindingKindEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum diagnosticSeverityEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum flavorKindEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum httpMethodEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum mqttQosEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum registryKindEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum expressionLanguageEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum catalogStatusEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum connectionStateEEnum = null;

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
	 * @see org.eclipse.fennec.services.ServicesPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private ServicesPackageImpl() {
		super(eNS_URI, ServicesFactory.eINSTANCE);
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
	 * <p>This method is used to initialize {@link ServicesPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static ServicesPackage init() {
		if (isInited) return (ServicesPackage)EPackage.Registry.INSTANCE.getEPackage(ServicesPackage.eNS_URI);

		// Obtain or create and register package
		Object registeredServicesPackage = EPackage.Registry.INSTANCE.get(eNS_URI);
		ServicesPackageImpl theServicesPackage = registeredServicesPackage instanceof ServicesPackageImpl ? (ServicesPackageImpl)registeredServicesPackage : new ServicesPackageImpl();

		isInited = true;

		// Create package meta-data objects
		theServicesPackage.createPackageContents();

		// Initialize created meta-data
		theServicesPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theServicesPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(ServicesPackage.eNS_URI, theServicesPackage);
		return theServicesPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getNamedElement() {
		return namedElementEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNamedElement_Name() {
		return (EAttribute)namedElementEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getVersionedElement() {
		return versionedElementEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getVersionedElement_Version() {
		return (EAttribute)versionedElementEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getProperty() {
		return propertyEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getStringProperty() {
		return stringPropertyEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getStringProperty_Value() {
		return (EAttribute)stringPropertyEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getIntProperty() {
		return intPropertyEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getIntProperty_Value() {
		return (EAttribute)intPropertyEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getLongProperty() {
		return longPropertyEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getLongProperty_Value() {
		return (EAttribute)longPropertyEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getDoubleProperty() {
		return doublePropertyEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDoubleProperty_Value() {
		return (EAttribute)doublePropertyEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getFloatProperty() {
		return floatPropertyEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFloatProperty_Value() {
		return (EAttribute)floatPropertyEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getShortProperty() {
		return shortPropertyEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getShortProperty_Value() {
		return (EAttribute)shortPropertyEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getBoolProperty() {
		return boolPropertyEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getBoolProperty_Value() {
		return (EAttribute)boolPropertyEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getStringListProperty() {
		return stringListPropertyEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getStringListProperty_Value() {
		return (EAttribute)stringListPropertyEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceOperation() {
		return serviceOperationEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceOperation_Description() {
		return (EAttribute)serviceOperationEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceOperation_Parameters() {
		return (EReference)serviceOperationEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceOperation_ReturnType() {
		return (EAttribute)serviceOperationEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceOperation_ReturnConstraints() {
		return (EReference)serviceOperationEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceOperation_Exceptions() {
		return (EReference)serviceOperationEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceOperation_Preconditions() {
		return (EReference)serviceOperationEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceOperation_Postconditions() {
		return (EReference)serviceOperationEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getParameter() {
		return parameterEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getParameter_Index() {
		return (EAttribute)parameterEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getParameter_Type() {
		return (EAttribute)parameterEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getParameter_Optional() {
		return (EAttribute)parameterEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getParameter_DefaultValue() {
		return (EAttribute)parameterEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getParameter_Description() {
		return (EAttribute)parameterEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getParameter_Constraints() {
		return (EReference)parameterEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getParameterConstraint() {
		return parameterConstraintEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getRequiredConstraint() {
		return requiredConstraintEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getNumericRangeConstraint() {
		return numericRangeConstraintEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNumericRangeConstraint_Min() {
		return (EAttribute)numericRangeConstraintEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNumericRangeConstraint_Max() {
		return (EAttribute)numericRangeConstraintEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNumericRangeConstraint_InclusiveMin() {
		return (EAttribute)numericRangeConstraintEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNumericRangeConstraint_InclusiveMax() {
		return (EAttribute)numericRangeConstraintEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getStringPatternConstraint() {
		return stringPatternConstraintEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getStringPatternConstraint_Pattern() {
		return (EAttribute)stringPatternConstraintEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getStringPatternConstraint_MinLength() {
		return (EAttribute)stringPatternConstraintEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getStringPatternConstraint_MaxLength() {
		return (EAttribute)stringPatternConstraintEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getEnumerationConstraint() {
		return enumerationConstraintEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEnumerationConstraint_AllowedValues() {
		return (EAttribute)enumerationConstraintEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getExpressionConstraint() {
		return expressionConstraintEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getExpressionConstraint_Language() {
		return (EAttribute)expressionConstraintEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getExpressionConstraint_Expression() {
		return (EAttribute)expressionConstraintEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getExpressionConstraint_Message() {
		return (EAttribute)expressionConstraintEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getInvariant() {
		return invariantEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getInvariant_Language() {
		return (EAttribute)invariantEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getInvariant_Expression() {
		return (EAttribute)invariantEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getInvariant_Message() {
		return (EAttribute)invariantEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getCollectionSizeConstraint() {
		return collectionSizeConstraintEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCollectionSizeConstraint_MinSize() {
		return (EAttribute)collectionSizeConstraintEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCollectionSizeConstraint_MaxSize() {
		return (EAttribute)collectionSizeConstraintEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceException() {
		return serviceExceptionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceException_Description() {
		return (EAttribute)serviceExceptionEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceException_Type() {
		return (EAttribute)serviceExceptionEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceException_Properties() {
		return (EReference)serviceExceptionEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceInterface() {
		return serviceInterfaceEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceInterface_Description() {
		return (EAttribute)serviceInterfaceEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceInterface_Operations() {
		return (EReference)serviceInterfaceEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceInterface_Exceptions() {
		return (EReference)serviceInterfaceEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceInterface_Invariants() {
		return (EReference)serviceInterfaceEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceInterface_Status() {
		return (EAttribute)serviceInterfaceEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceInterface_DeprecationReason() {
		return (EAttribute)serviceInterfaceEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceInterface_ReplacedBy() {
		return (EReference)serviceInterfaceEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getLifecycleHook() {
		return lifecycleHookEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getLifecycleHook_Kind() {
		return (EAttribute)lifecycleHookEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getLifecycleHook_Parameter() {
		return (EAttribute)lifecycleHookEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getReferenceBinding() {
		return referenceBindingEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getReferenceBinding_Kind() {
		return (EAttribute)referenceBindingEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getReferenceBinding_FieldOption() {
		return (EAttribute)referenceBindingEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getComponentReference() {
		return componentReferenceEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentReference_InterfaceName() {
		return (EAttribute)componentReferenceEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentReference_Cardinality() {
		return (EAttribute)componentReferenceEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentReference_Policy() {
		return (EAttribute)componentReferenceEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentReference_PolicyOption() {
		return (EAttribute)componentReferenceEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentReference_Target() {
		return (EAttribute)componentReferenceEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentReference_Scope() {
		return (EAttribute)componentReferenceEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentReference_CollectionType() {
		return (EAttribute)componentReferenceEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentReference_Parameter() {
		return (EAttribute)componentReferenceEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentReference_Bindings() {
		return (EReference)componentReferenceEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getComponentDescription() {
		return componentDescriptionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentDescription_Factory() {
		return (EAttribute)componentDescriptionEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentDescription_Scope() {
		return (EAttribute)componentDescriptionEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentDescription_ImplementationId() {
		return (EAttribute)componentDescriptionEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentDescription_DefaultEnabled() {
		return (EAttribute)componentDescriptionEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentDescription_Immediate() {
		return (EAttribute)componentDescriptionEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentDescription_ConfigurationPolicy() {
		return (EAttribute)componentDescriptionEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentDescription_ConfigurationPid() {
		return (EAttribute)componentDescriptionEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentDescription_ServiceInterfaces() {
		return (EReference)componentDescriptionEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentDescription_Properties() {
		return (EReference)componentDescriptionEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentDescription_FactoryProperties() {
		return (EReference)componentDescriptionEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentDescription_References() {
		return (EReference)componentDescriptionEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentDescription_LifecycleHooks() {
		return (EReference)componentDescriptionEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentDescription_Provider() {
		return (EReference)componentDescriptionEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceProvider() {
		return serviceProviderEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceProvider_SymbolicName() {
		return (EAttribute)serviceProviderEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceProvider_Descriptions() {
		return (EReference)serviceProviderEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceProvider_Implementations() {
		return (EReference)serviceProviderEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceImplementation() {
		return serviceImplementationEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceImplementation_Description() {
		return (EAttribute)serviceImplementationEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceImplementation_ImplementationId() {
		return (EAttribute)serviceImplementationEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceImplementation_ServiceInterfaces() {
		return (EReference)serviceImplementationEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceImplementation_Flavors() {
		return (EReference)serviceImplementationEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceImplementation_Properties() {
		return (EReference)serviceImplementationEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceImplementation_ComponentDescription() {
		return (EReference)serviceImplementationEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceFlavor() {
		return serviceFlavorEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceFlavor_Kind() {
		return (EAttribute)serviceFlavorEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceFlavor_OperationFlavors() {
		return (EReference)serviceFlavorEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getRestFlavor() {
		return restFlavorEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRestFlavor_Host() {
		return (EAttribute)restFlavorEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRestFlavor_BasePath() {
		return (EAttribute)restFlavorEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRestFlavor_ContentTypes() {
		return (EAttribute)restFlavorEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getMqttFlavor() {
		return mqttFlavorEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttFlavor_Brokers() {
		return (EAttribute)mqttFlavorEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttFlavor_RequestTopic() {
		return (EAttribute)mqttFlavorEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttFlavor_ResponseTopic() {
		return (EAttribute)mqttFlavorEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttFlavor_DefaultQos() {
		return (EAttribute)mqttFlavorEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttFlavor_DefaultRetained() {
		return (EAttribute)mqttFlavorEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceOperationFlavor() {
		return serviceOperationFlavorEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceOperationFlavor_Operation() {
		return (EReference)serviceOperationFlavorEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceOperationFlavor_Consumes() {
		return (EAttribute)serviceOperationFlavorEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceOperationFlavor_Produces() {
		return (EAttribute)serviceOperationFlavorEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getRestOperationFlavor() {
		return restOperationFlavorEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRestOperationFlavor_Method() {
		return (EAttribute)restOperationFlavorEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRestOperationFlavor_Path() {
		return (EAttribute)restOperationFlavorEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRestOperationFlavor_ReturnCodes() {
		return (EAttribute)restOperationFlavorEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getMqttOperationFlavor() {
		return mqttOperationFlavorEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttOperationFlavor_RequestTopic() {
		return (EAttribute)mqttOperationFlavorEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttOperationFlavor_ResponseTopic() {
		return (EAttribute)mqttOperationFlavorEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttOperationFlavor_Qos() {
		return (EAttribute)mqttOperationFlavorEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttOperationFlavor_Retained() {
		return (EAttribute)mqttOperationFlavorEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttOperationFlavor_Correlation() {
		return (EAttribute)mqttOperationFlavorEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMqttOperationFlavor_ReturnPath() {
		return (EAttribute)mqttOperationFlavorEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceReference() {
		return serviceReferenceEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceReference_Id() {
		return (EAttribute)serviceReferenceEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceReference_Properties() {
		return (EReference)serviceReferenceEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceReference_Provider() {
		return (EReference)serviceReferenceEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceReference_Registration() {
		return (EReference)serviceReferenceEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceReference__GetProperty__String() {
		return serviceReferenceEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceReference__GetPropertyKeys() {
		return serviceReferenceEClass.getEOperations().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceRegistration() {
		return serviceRegistrationEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceRegistration_Reference() {
		return (EReference)serviceRegistrationEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceRegistration_Unregistered() {
		return (EAttribute)serviceRegistrationEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceRegistration_Provider() {
		return (EReference)serviceRegistrationEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceRegistration_Implementation() {
		return (EReference)serviceRegistrationEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceRegistration_UsingSessions() {
		return (EReference)serviceRegistrationEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceRegistration__Unregister() {
		return serviceRegistrationEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceRegistration__SetProperties__EList() {
		return serviceRegistrationEClass.getEOperations().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getConsumerSession() {
		return consumerSessionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getConsumerSession_ConsumerId() {
		return (EAttribute)consumerSessionEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getConsumerSession_LastRenewal() {
		return (EAttribute)consumerSessionEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getConsumerSession_Capabilities() {
		return (EReference)consumerSessionEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getConsumerSession_Acquisitions() {
		return (EReference)consumerSessionEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getComponentConfiguration() {
		return componentConfigurationEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentConfiguration_Id() {
		return (EAttribute)componentConfigurationEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentConfiguration_Description() {
		return (EReference)componentConfigurationEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentConfiguration_State() {
		return (EAttribute)componentConfigurationEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentConfiguration_Properties() {
		return (EReference)componentConfigurationEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentConfiguration_SatisfiedReferences() {
		return (EReference)componentConfigurationEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentConfiguration_UnsatisfiedReferences() {
		return (EReference)componentConfigurationEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentConfiguration_Failure() {
		return (EReference)componentConfigurationEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentConfiguration_Service() {
		return (EReference)componentConfigurationEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getSatisfiedReference() {
		return satisfiedReferenceEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getSatisfiedReference_Name() {
		return (EAttribute)satisfiedReferenceEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getSatisfiedReference_Target() {
		return (EAttribute)satisfiedReferenceEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getSatisfiedReference_BoundServices() {
		return (EReference)satisfiedReferenceEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getUnsatisfiedReference() {
		return unsatisfiedReferenceEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getUnsatisfiedReference_Name() {
		return (EAttribute)unsatisfiedReferenceEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getUnsatisfiedReference_Target() {
		return (EAttribute)unsatisfiedReferenceEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getUnsatisfiedReference_TargetServices() {
		return (EReference)unsatisfiedReferenceEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getDiagnostic() {
		return diagnosticEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDiagnostic_Severity() {
		return (EAttribute)diagnosticEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDiagnostic_Message() {
		return (EAttribute)diagnosticEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDiagnostic_Source() {
		return (EAttribute)diagnosticEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDiagnostic_Code() {
		return (EAttribute)diagnosticEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDiagnostic_Data() {
		return (EAttribute)diagnosticEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getDiagnostic_Children() {
		return (EReference)diagnosticEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceEvent() {
		return serviceEventEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceEvent_Type() {
		return (EAttribute)serviceEventEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceEvent_Reference() {
		return (EReference)serviceEventEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceEvent_Timestamp() {
		return (EAttribute)serviceEventEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceListener() {
		return serviceListenerEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceListener_Filter() {
		return (EAttribute)serviceListenerEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceListener__ServiceChanged__ServiceEvent() {
		return serviceListenerEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServiceRegistry() {
		return serviceRegistryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServiceRegistry_Kind() {
		return (EAttribute)serviceRegistryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceRegistry_PublishHooks() {
		return (EReference)serviceRegistryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceRegistry_DiscoveryHooks() {
		return (EReference)serviceRegistryEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getServiceRegistry_DistributionHooks() {
		return (EReference)serviceRegistryEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceRegistry__GetServiceReference__String() {
		return serviceRegistryEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceRegistry__GetServiceReferences__String_String_ConsumerCapability() {
		return serviceRegistryEClass.getEOperations().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceRegistry__GetAllServiceReferences__String_String_ConsumerCapability() {
		return serviceRegistryEClass.getEOperations().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceRegistry__AddServiceListener__ServiceListener() {
		return serviceRegistryEClass.getEOperations().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getServiceRegistry__RemoveServiceListener__ServiceListener() {
		return serviceRegistryEClass.getEOperations().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getLocalServiceRegistry() {
		return localServiceRegistryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getLocalServiceRegistry_References() {
		return (EReference)localServiceRegistryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getLocalServiceRegistry_Registrations() {
		return (EReference)localServiceRegistryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getLocalServiceRegistry_Sessions() {
		return (EReference)localServiceRegistryEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getLocalServiceRegistry_Configurations() {
		return (EReference)localServiceRegistryEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getLocalServiceRegistry_Providers() {
		return (EReference)localServiceRegistryEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getLocalServiceRegistry_Listeners() {
		return (EReference)localServiceRegistryEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getLocalServiceRegistry_Remote() {
		return (EReference)localServiceRegistryEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getLocalServiceRegistry_ConnectionState() {
		return (EAttribute)localServiceRegistryEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getLocalServiceRegistry__RegisterService__ServiceProvider_ServiceImplementation_EList() {
		return localServiceRegistryEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getLocalServiceRegistry__FireServiceEvent__ServiceEvent() {
		return localServiceRegistryEClass.getEOperations().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getRemoteServiceRegistry() {
		return remoteServiceRegistryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRemoteServiceRegistry_Endpoint() {
		return (EAttribute)remoteServiceRegistryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getRemoteServiceRegistry_Catalog() {
		return (EReference)remoteServiceRegistryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getRemoteServiceRegistry_Implementations() {
		return (EReference)remoteServiceRegistryEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getRemoteServiceRegistry_Providers() {
		return (EReference)remoteServiceRegistryEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getRemoteServiceRegistry__PublishImplementation__ServiceProvider_ServiceImplementation() {
		return remoteServiceRegistryEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getRemoteServiceRegistry__WithdrawImplementation__ServiceProvider_ServiceImplementation() {
		return remoteServiceRegistryEClass.getEOperations().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getRemoteServiceRegistry__AddCatalogEntry__ServiceInterface_String() {
		return remoteServiceRegistryEClass.getEOperations().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getRemoteServiceRegistry__DeprecateCatalogEntry__ServiceInterface_String() {
		return remoteServiceRegistryEClass.getEOperations().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getRemoteServiceRegistry__RemoveCatalogEntry__ServiceInterface_String() {
		return remoteServiceRegistryEClass.getEOperations().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getConsumerCapability() {
		return consumerCapabilityEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getConsumerCapability_ConsumerId() {
		return (EAttribute)consumerCapabilityEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getConsumerCapability_SupportedFlavors() {
		return (EAttribute)consumerCapabilityEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getConsumerCapability_Properties() {
		return (EReference)consumerCapabilityEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getPublishHook() {
		return publishHookEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getPublishHook__OnPublish__ServiceProvider_ServiceImplementation() {
		return publishHookEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getPublishHook__OnWithdraw__ServiceProvider_ServiceImplementation() {
		return publishHookEClass.getEOperations().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getDiscoveryHook() {
		return discoveryHookEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getDiscoveryHook__OnLookup__String_String_ConsumerCapability() {
		return discoveryHookEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getDiscoveryHook__FilterReferences__ConsumerCapability_EList() {
		return discoveryHookEClass.getEOperations().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getDiscoveryHook__OnSubscribe__ServiceListener() {
		return discoveryHookEClass.getEOperations().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getDistributionHook() {
		return distributionHookEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getDistributionHook__OnOutbound__ServiceEvent() {
		return distributionHookEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getDistributionHook__OnInbound__ServiceEvent() {
		return distributionHookEClass.getEOperations().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getServiceScope() {
		return serviceScopeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getReferenceCardinality() {
		return referenceCardinalityEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getReferencePolicy() {
		return referencePolicyEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getReferencePolicyOption() {
		return referencePolicyOptionEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getConfigurationPolicy() {
		return configurationPolicyEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getComponentState() {
		return componentStateEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getServiceEventType() {
		return serviceEventTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getFieldOption() {
		return fieldOptionEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getCollectionType() {
		return collectionTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getLifecycleHookKind() {
		return lifecycleHookKindEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getReferenceBindingKind() {
		return referenceBindingKindEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getDiagnosticSeverity() {
		return diagnosticSeverityEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getFlavorKind() {
		return flavorKindEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getHttpMethod() {
		return httpMethodEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getMqttQos() {
		return mqttQosEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getRegistryKind() {
		return registryKindEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getExpressionLanguage() {
		return expressionLanguageEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getCatalogStatus() {
		return catalogStatusEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getConnectionState() {
		return connectionStateEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ServicesFactory getServicesFactory() {
		return (ServicesFactory)getEFactoryInstance();
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
		namedElementEClass = createEClass(NAMED_ELEMENT);
		createEAttribute(namedElementEClass, NAMED_ELEMENT__NAME);

		versionedElementEClass = createEClass(VERSIONED_ELEMENT);
		createEAttribute(versionedElementEClass, VERSIONED_ELEMENT__VERSION);

		propertyEClass = createEClass(PROPERTY);

		stringPropertyEClass = createEClass(STRING_PROPERTY);
		createEAttribute(stringPropertyEClass, STRING_PROPERTY__VALUE);

		intPropertyEClass = createEClass(INT_PROPERTY);
		createEAttribute(intPropertyEClass, INT_PROPERTY__VALUE);

		longPropertyEClass = createEClass(LONG_PROPERTY);
		createEAttribute(longPropertyEClass, LONG_PROPERTY__VALUE);

		doublePropertyEClass = createEClass(DOUBLE_PROPERTY);
		createEAttribute(doublePropertyEClass, DOUBLE_PROPERTY__VALUE);

		floatPropertyEClass = createEClass(FLOAT_PROPERTY);
		createEAttribute(floatPropertyEClass, FLOAT_PROPERTY__VALUE);

		shortPropertyEClass = createEClass(SHORT_PROPERTY);
		createEAttribute(shortPropertyEClass, SHORT_PROPERTY__VALUE);

		boolPropertyEClass = createEClass(BOOL_PROPERTY);
		createEAttribute(boolPropertyEClass, BOOL_PROPERTY__VALUE);

		stringListPropertyEClass = createEClass(STRING_LIST_PROPERTY);
		createEAttribute(stringListPropertyEClass, STRING_LIST_PROPERTY__VALUE);

		serviceOperationEClass = createEClass(SERVICE_OPERATION);
		createEAttribute(serviceOperationEClass, SERVICE_OPERATION__DESCRIPTION);
		createEReference(serviceOperationEClass, SERVICE_OPERATION__PARAMETERS);
		createEAttribute(serviceOperationEClass, SERVICE_OPERATION__RETURN_TYPE);
		createEReference(serviceOperationEClass, SERVICE_OPERATION__RETURN_CONSTRAINTS);
		createEReference(serviceOperationEClass, SERVICE_OPERATION__EXCEPTIONS);
		createEReference(serviceOperationEClass, SERVICE_OPERATION__PRECONDITIONS);
		createEReference(serviceOperationEClass, SERVICE_OPERATION__POSTCONDITIONS);

		parameterEClass = createEClass(PARAMETER);
		createEAttribute(parameterEClass, PARAMETER__INDEX);
		createEAttribute(parameterEClass, PARAMETER__TYPE);
		createEAttribute(parameterEClass, PARAMETER__OPTIONAL);
		createEAttribute(parameterEClass, PARAMETER__DEFAULT_VALUE);
		createEAttribute(parameterEClass, PARAMETER__DESCRIPTION);
		createEReference(parameterEClass, PARAMETER__CONSTRAINTS);

		parameterConstraintEClass = createEClass(PARAMETER_CONSTRAINT);

		requiredConstraintEClass = createEClass(REQUIRED_CONSTRAINT);

		numericRangeConstraintEClass = createEClass(NUMERIC_RANGE_CONSTRAINT);
		createEAttribute(numericRangeConstraintEClass, NUMERIC_RANGE_CONSTRAINT__MIN);
		createEAttribute(numericRangeConstraintEClass, NUMERIC_RANGE_CONSTRAINT__MAX);
		createEAttribute(numericRangeConstraintEClass, NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MIN);
		createEAttribute(numericRangeConstraintEClass, NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MAX);

		stringPatternConstraintEClass = createEClass(STRING_PATTERN_CONSTRAINT);
		createEAttribute(stringPatternConstraintEClass, STRING_PATTERN_CONSTRAINT__PATTERN);
		createEAttribute(stringPatternConstraintEClass, STRING_PATTERN_CONSTRAINT__MIN_LENGTH);
		createEAttribute(stringPatternConstraintEClass, STRING_PATTERN_CONSTRAINT__MAX_LENGTH);

		enumerationConstraintEClass = createEClass(ENUMERATION_CONSTRAINT);
		createEAttribute(enumerationConstraintEClass, ENUMERATION_CONSTRAINT__ALLOWED_VALUES);

		expressionConstraintEClass = createEClass(EXPRESSION_CONSTRAINT);
		createEAttribute(expressionConstraintEClass, EXPRESSION_CONSTRAINT__LANGUAGE);
		createEAttribute(expressionConstraintEClass, EXPRESSION_CONSTRAINT__EXPRESSION);
		createEAttribute(expressionConstraintEClass, EXPRESSION_CONSTRAINT__MESSAGE);

		invariantEClass = createEClass(INVARIANT);
		createEAttribute(invariantEClass, INVARIANT__LANGUAGE);
		createEAttribute(invariantEClass, INVARIANT__EXPRESSION);
		createEAttribute(invariantEClass, INVARIANT__MESSAGE);

		collectionSizeConstraintEClass = createEClass(COLLECTION_SIZE_CONSTRAINT);
		createEAttribute(collectionSizeConstraintEClass, COLLECTION_SIZE_CONSTRAINT__MIN_SIZE);
		createEAttribute(collectionSizeConstraintEClass, COLLECTION_SIZE_CONSTRAINT__MAX_SIZE);

		serviceExceptionEClass = createEClass(SERVICE_EXCEPTION);
		createEAttribute(serviceExceptionEClass, SERVICE_EXCEPTION__DESCRIPTION);
		createEAttribute(serviceExceptionEClass, SERVICE_EXCEPTION__TYPE);
		createEReference(serviceExceptionEClass, SERVICE_EXCEPTION__PROPERTIES);

		serviceInterfaceEClass = createEClass(SERVICE_INTERFACE);
		createEAttribute(serviceInterfaceEClass, SERVICE_INTERFACE__DESCRIPTION);
		createEReference(serviceInterfaceEClass, SERVICE_INTERFACE__OPERATIONS);
		createEReference(serviceInterfaceEClass, SERVICE_INTERFACE__EXCEPTIONS);
		createEReference(serviceInterfaceEClass, SERVICE_INTERFACE__INVARIANTS);
		createEAttribute(serviceInterfaceEClass, SERVICE_INTERFACE__STATUS);
		createEAttribute(serviceInterfaceEClass, SERVICE_INTERFACE__DEPRECATION_REASON);
		createEReference(serviceInterfaceEClass, SERVICE_INTERFACE__REPLACED_BY);

		lifecycleHookEClass = createEClass(LIFECYCLE_HOOK);
		createEAttribute(lifecycleHookEClass, LIFECYCLE_HOOK__KIND);
		createEAttribute(lifecycleHookEClass, LIFECYCLE_HOOK__PARAMETER);

		referenceBindingEClass = createEClass(REFERENCE_BINDING);
		createEAttribute(referenceBindingEClass, REFERENCE_BINDING__KIND);
		createEAttribute(referenceBindingEClass, REFERENCE_BINDING__FIELD_OPTION);

		componentReferenceEClass = createEClass(COMPONENT_REFERENCE);
		createEAttribute(componentReferenceEClass, COMPONENT_REFERENCE__INTERFACE_NAME);
		createEAttribute(componentReferenceEClass, COMPONENT_REFERENCE__CARDINALITY);
		createEAttribute(componentReferenceEClass, COMPONENT_REFERENCE__POLICY);
		createEAttribute(componentReferenceEClass, COMPONENT_REFERENCE__POLICY_OPTION);
		createEAttribute(componentReferenceEClass, COMPONENT_REFERENCE__TARGET);
		createEAttribute(componentReferenceEClass, COMPONENT_REFERENCE__SCOPE);
		createEAttribute(componentReferenceEClass, COMPONENT_REFERENCE__COLLECTION_TYPE);
		createEAttribute(componentReferenceEClass, COMPONENT_REFERENCE__PARAMETER);
		createEReference(componentReferenceEClass, COMPONENT_REFERENCE__BINDINGS);

		componentDescriptionEClass = createEClass(COMPONENT_DESCRIPTION);
		createEAttribute(componentDescriptionEClass, COMPONENT_DESCRIPTION__FACTORY);
		createEAttribute(componentDescriptionEClass, COMPONENT_DESCRIPTION__SCOPE);
		createEAttribute(componentDescriptionEClass, COMPONENT_DESCRIPTION__IMPLEMENTATION_ID);
		createEAttribute(componentDescriptionEClass, COMPONENT_DESCRIPTION__DEFAULT_ENABLED);
		createEAttribute(componentDescriptionEClass, COMPONENT_DESCRIPTION__IMMEDIATE);
		createEAttribute(componentDescriptionEClass, COMPONENT_DESCRIPTION__CONFIGURATION_POLICY);
		createEAttribute(componentDescriptionEClass, COMPONENT_DESCRIPTION__CONFIGURATION_PID);
		createEReference(componentDescriptionEClass, COMPONENT_DESCRIPTION__SERVICE_INTERFACES);
		createEReference(componentDescriptionEClass, COMPONENT_DESCRIPTION__PROPERTIES);
		createEReference(componentDescriptionEClass, COMPONENT_DESCRIPTION__FACTORY_PROPERTIES);
		createEReference(componentDescriptionEClass, COMPONENT_DESCRIPTION__REFERENCES);
		createEReference(componentDescriptionEClass, COMPONENT_DESCRIPTION__LIFECYCLE_HOOKS);
		createEReference(componentDescriptionEClass, COMPONENT_DESCRIPTION__PROVIDER);

		serviceProviderEClass = createEClass(SERVICE_PROVIDER);
		createEAttribute(serviceProviderEClass, SERVICE_PROVIDER__SYMBOLIC_NAME);
		createEReference(serviceProviderEClass, SERVICE_PROVIDER__DESCRIPTIONS);
		createEReference(serviceProviderEClass, SERVICE_PROVIDER__IMPLEMENTATIONS);

		serviceImplementationEClass = createEClass(SERVICE_IMPLEMENTATION);
		createEAttribute(serviceImplementationEClass, SERVICE_IMPLEMENTATION__DESCRIPTION);
		createEAttribute(serviceImplementationEClass, SERVICE_IMPLEMENTATION__IMPLEMENTATION_ID);
		createEReference(serviceImplementationEClass, SERVICE_IMPLEMENTATION__SERVICE_INTERFACES);
		createEReference(serviceImplementationEClass, SERVICE_IMPLEMENTATION__FLAVORS);
		createEReference(serviceImplementationEClass, SERVICE_IMPLEMENTATION__PROPERTIES);
		createEReference(serviceImplementationEClass, SERVICE_IMPLEMENTATION__COMPONENT_DESCRIPTION);

		serviceFlavorEClass = createEClass(SERVICE_FLAVOR);
		createEAttribute(serviceFlavorEClass, SERVICE_FLAVOR__KIND);
		createEReference(serviceFlavorEClass, SERVICE_FLAVOR__OPERATION_FLAVORS);

		restFlavorEClass = createEClass(REST_FLAVOR);
		createEAttribute(restFlavorEClass, REST_FLAVOR__HOST);
		createEAttribute(restFlavorEClass, REST_FLAVOR__BASE_PATH);
		createEAttribute(restFlavorEClass, REST_FLAVOR__CONTENT_TYPES);

		mqttFlavorEClass = createEClass(MQTT_FLAVOR);
		createEAttribute(mqttFlavorEClass, MQTT_FLAVOR__BROKERS);
		createEAttribute(mqttFlavorEClass, MQTT_FLAVOR__REQUEST_TOPIC);
		createEAttribute(mqttFlavorEClass, MQTT_FLAVOR__RESPONSE_TOPIC);
		createEAttribute(mqttFlavorEClass, MQTT_FLAVOR__DEFAULT_QOS);
		createEAttribute(mqttFlavorEClass, MQTT_FLAVOR__DEFAULT_RETAINED);

		serviceOperationFlavorEClass = createEClass(SERVICE_OPERATION_FLAVOR);
		createEReference(serviceOperationFlavorEClass, SERVICE_OPERATION_FLAVOR__OPERATION);
		createEAttribute(serviceOperationFlavorEClass, SERVICE_OPERATION_FLAVOR__CONSUMES);
		createEAttribute(serviceOperationFlavorEClass, SERVICE_OPERATION_FLAVOR__PRODUCES);

		restOperationFlavorEClass = createEClass(REST_OPERATION_FLAVOR);
		createEAttribute(restOperationFlavorEClass, REST_OPERATION_FLAVOR__METHOD);
		createEAttribute(restOperationFlavorEClass, REST_OPERATION_FLAVOR__PATH);
		createEAttribute(restOperationFlavorEClass, REST_OPERATION_FLAVOR__RETURN_CODES);

		mqttOperationFlavorEClass = createEClass(MQTT_OPERATION_FLAVOR);
		createEAttribute(mqttOperationFlavorEClass, MQTT_OPERATION_FLAVOR__REQUEST_TOPIC);
		createEAttribute(mqttOperationFlavorEClass, MQTT_OPERATION_FLAVOR__RESPONSE_TOPIC);
		createEAttribute(mqttOperationFlavorEClass, MQTT_OPERATION_FLAVOR__QOS);
		createEAttribute(mqttOperationFlavorEClass, MQTT_OPERATION_FLAVOR__RETAINED);
		createEAttribute(mqttOperationFlavorEClass, MQTT_OPERATION_FLAVOR__CORRELATION);
		createEAttribute(mqttOperationFlavorEClass, MQTT_OPERATION_FLAVOR__RETURN_PATH);

		serviceReferenceEClass = createEClass(SERVICE_REFERENCE);
		createEAttribute(serviceReferenceEClass, SERVICE_REFERENCE__ID);
		createEReference(serviceReferenceEClass, SERVICE_REFERENCE__PROPERTIES);
		createEReference(serviceReferenceEClass, SERVICE_REFERENCE__PROVIDER);
		createEReference(serviceReferenceEClass, SERVICE_REFERENCE__REGISTRATION);
		createEOperation(serviceReferenceEClass, SERVICE_REFERENCE___GET_PROPERTY__STRING);
		createEOperation(serviceReferenceEClass, SERVICE_REFERENCE___GET_PROPERTY_KEYS);

		serviceRegistrationEClass = createEClass(SERVICE_REGISTRATION);
		createEReference(serviceRegistrationEClass, SERVICE_REGISTRATION__REFERENCE);
		createEAttribute(serviceRegistrationEClass, SERVICE_REGISTRATION__UNREGISTERED);
		createEReference(serviceRegistrationEClass, SERVICE_REGISTRATION__PROVIDER);
		createEReference(serviceRegistrationEClass, SERVICE_REGISTRATION__IMPLEMENTATION);
		createEReference(serviceRegistrationEClass, SERVICE_REGISTRATION__USING_SESSIONS);
		createEOperation(serviceRegistrationEClass, SERVICE_REGISTRATION___UNREGISTER);
		createEOperation(serviceRegistrationEClass, SERVICE_REGISTRATION___SET_PROPERTIES__ELIST);

		consumerSessionEClass = createEClass(CONSUMER_SESSION);
		createEAttribute(consumerSessionEClass, CONSUMER_SESSION__CONSUMER_ID);
		createEAttribute(consumerSessionEClass, CONSUMER_SESSION__LAST_RENEWAL);
		createEReference(consumerSessionEClass, CONSUMER_SESSION__CAPABILITIES);
		createEReference(consumerSessionEClass, CONSUMER_SESSION__ACQUISITIONS);

		componentConfigurationEClass = createEClass(COMPONENT_CONFIGURATION);
		createEAttribute(componentConfigurationEClass, COMPONENT_CONFIGURATION__ID);
		createEReference(componentConfigurationEClass, COMPONENT_CONFIGURATION__DESCRIPTION);
		createEAttribute(componentConfigurationEClass, COMPONENT_CONFIGURATION__STATE);
		createEReference(componentConfigurationEClass, COMPONENT_CONFIGURATION__PROPERTIES);
		createEReference(componentConfigurationEClass, COMPONENT_CONFIGURATION__SATISFIED_REFERENCES);
		createEReference(componentConfigurationEClass, COMPONENT_CONFIGURATION__UNSATISFIED_REFERENCES);
		createEReference(componentConfigurationEClass, COMPONENT_CONFIGURATION__FAILURE);
		createEReference(componentConfigurationEClass, COMPONENT_CONFIGURATION__SERVICE);

		satisfiedReferenceEClass = createEClass(SATISFIED_REFERENCE);
		createEAttribute(satisfiedReferenceEClass, SATISFIED_REFERENCE__NAME);
		createEAttribute(satisfiedReferenceEClass, SATISFIED_REFERENCE__TARGET);
		createEReference(satisfiedReferenceEClass, SATISFIED_REFERENCE__BOUND_SERVICES);

		unsatisfiedReferenceEClass = createEClass(UNSATISFIED_REFERENCE);
		createEAttribute(unsatisfiedReferenceEClass, UNSATISFIED_REFERENCE__NAME);
		createEAttribute(unsatisfiedReferenceEClass, UNSATISFIED_REFERENCE__TARGET);
		createEReference(unsatisfiedReferenceEClass, UNSATISFIED_REFERENCE__TARGET_SERVICES);

		diagnosticEClass = createEClass(DIAGNOSTIC);
		createEAttribute(diagnosticEClass, DIAGNOSTIC__SEVERITY);
		createEAttribute(diagnosticEClass, DIAGNOSTIC__MESSAGE);
		createEAttribute(diagnosticEClass, DIAGNOSTIC__SOURCE);
		createEAttribute(diagnosticEClass, DIAGNOSTIC__CODE);
		createEAttribute(diagnosticEClass, DIAGNOSTIC__DATA);
		createEReference(diagnosticEClass, DIAGNOSTIC__CHILDREN);

		serviceEventEClass = createEClass(SERVICE_EVENT);
		createEAttribute(serviceEventEClass, SERVICE_EVENT__TYPE);
		createEReference(serviceEventEClass, SERVICE_EVENT__REFERENCE);
		createEAttribute(serviceEventEClass, SERVICE_EVENT__TIMESTAMP);

		serviceListenerEClass = createEClass(SERVICE_LISTENER);
		createEAttribute(serviceListenerEClass, SERVICE_LISTENER__FILTER);
		createEOperation(serviceListenerEClass, SERVICE_LISTENER___SERVICE_CHANGED__SERVICEEVENT);

		serviceRegistryEClass = createEClass(SERVICE_REGISTRY);
		createEAttribute(serviceRegistryEClass, SERVICE_REGISTRY__KIND);
		createEReference(serviceRegistryEClass, SERVICE_REGISTRY__PUBLISH_HOOKS);
		createEReference(serviceRegistryEClass, SERVICE_REGISTRY__DISCOVERY_HOOKS);
		createEReference(serviceRegistryEClass, SERVICE_REGISTRY__DISTRIBUTION_HOOKS);
		createEOperation(serviceRegistryEClass, SERVICE_REGISTRY___GET_SERVICE_REFERENCE__STRING);
		createEOperation(serviceRegistryEClass, SERVICE_REGISTRY___GET_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY);
		createEOperation(serviceRegistryEClass, SERVICE_REGISTRY___GET_ALL_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY);
		createEOperation(serviceRegistryEClass, SERVICE_REGISTRY___ADD_SERVICE_LISTENER__SERVICELISTENER);
		createEOperation(serviceRegistryEClass, SERVICE_REGISTRY___REMOVE_SERVICE_LISTENER__SERVICELISTENER);

		localServiceRegistryEClass = createEClass(LOCAL_SERVICE_REGISTRY);
		createEReference(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY__REFERENCES);
		createEReference(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY__REGISTRATIONS);
		createEReference(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY__SESSIONS);
		createEReference(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY__CONFIGURATIONS);
		createEReference(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY__PROVIDERS);
		createEReference(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY__LISTENERS);
		createEReference(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY__REMOTE);
		createEAttribute(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY__CONNECTION_STATE);
		createEOperation(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY___REGISTER_SERVICE__SERVICEPROVIDER_SERVICEIMPLEMENTATION_ELIST);
		createEOperation(localServiceRegistryEClass, LOCAL_SERVICE_REGISTRY___FIRE_SERVICE_EVENT__SERVICEEVENT);

		remoteServiceRegistryEClass = createEClass(REMOTE_SERVICE_REGISTRY);
		createEAttribute(remoteServiceRegistryEClass, REMOTE_SERVICE_REGISTRY__ENDPOINT);
		createEReference(remoteServiceRegistryEClass, REMOTE_SERVICE_REGISTRY__CATALOG);
		createEReference(remoteServiceRegistryEClass, REMOTE_SERVICE_REGISTRY__IMPLEMENTATIONS);
		createEReference(remoteServiceRegistryEClass, REMOTE_SERVICE_REGISTRY__PROVIDERS);
		createEOperation(remoteServiceRegistryEClass, REMOTE_SERVICE_REGISTRY___PUBLISH_IMPLEMENTATION__SERVICEPROVIDER_SERVICEIMPLEMENTATION);
		createEOperation(remoteServiceRegistryEClass, REMOTE_SERVICE_REGISTRY___WITHDRAW_IMPLEMENTATION__SERVICEPROVIDER_SERVICEIMPLEMENTATION);
		createEOperation(remoteServiceRegistryEClass, REMOTE_SERVICE_REGISTRY___ADD_CATALOG_ENTRY__SERVICEINTERFACE_STRING);
		createEOperation(remoteServiceRegistryEClass, REMOTE_SERVICE_REGISTRY___DEPRECATE_CATALOG_ENTRY__SERVICEINTERFACE_STRING);
		createEOperation(remoteServiceRegistryEClass, REMOTE_SERVICE_REGISTRY___REMOVE_CATALOG_ENTRY__SERVICEINTERFACE_STRING);

		consumerCapabilityEClass = createEClass(CONSUMER_CAPABILITY);
		createEAttribute(consumerCapabilityEClass, CONSUMER_CAPABILITY__CONSUMER_ID);
		createEAttribute(consumerCapabilityEClass, CONSUMER_CAPABILITY__SUPPORTED_FLAVORS);
		createEReference(consumerCapabilityEClass, CONSUMER_CAPABILITY__PROPERTIES);

		publishHookEClass = createEClass(PUBLISH_HOOK);
		createEOperation(publishHookEClass, PUBLISH_HOOK___ON_PUBLISH__SERVICEPROVIDER_SERVICEIMPLEMENTATION);
		createEOperation(publishHookEClass, PUBLISH_HOOK___ON_WITHDRAW__SERVICEPROVIDER_SERVICEIMPLEMENTATION);

		discoveryHookEClass = createEClass(DISCOVERY_HOOK);
		createEOperation(discoveryHookEClass, DISCOVERY_HOOK___ON_LOOKUP__STRING_STRING_CONSUMERCAPABILITY);
		createEOperation(discoveryHookEClass, DISCOVERY_HOOK___FILTER_REFERENCES__CONSUMERCAPABILITY_ELIST);
		createEOperation(discoveryHookEClass, DISCOVERY_HOOK___ON_SUBSCRIBE__SERVICELISTENER);

		distributionHookEClass = createEClass(DISTRIBUTION_HOOK);
		createEOperation(distributionHookEClass, DISTRIBUTION_HOOK___ON_OUTBOUND__SERVICEEVENT);
		createEOperation(distributionHookEClass, DISTRIBUTION_HOOK___ON_INBOUND__SERVICEEVENT);

		// Create enums
		serviceScopeEEnum = createEEnum(SERVICE_SCOPE);
		referenceCardinalityEEnum = createEEnum(REFERENCE_CARDINALITY);
		referencePolicyEEnum = createEEnum(REFERENCE_POLICY);
		referencePolicyOptionEEnum = createEEnum(REFERENCE_POLICY_OPTION);
		configurationPolicyEEnum = createEEnum(CONFIGURATION_POLICY);
		componentStateEEnum = createEEnum(COMPONENT_STATE);
		serviceEventTypeEEnum = createEEnum(SERVICE_EVENT_TYPE);
		fieldOptionEEnum = createEEnum(FIELD_OPTION);
		collectionTypeEEnum = createEEnum(COLLECTION_TYPE);
		lifecycleHookKindEEnum = createEEnum(LIFECYCLE_HOOK_KIND);
		referenceBindingKindEEnum = createEEnum(REFERENCE_BINDING_KIND);
		diagnosticSeverityEEnum = createEEnum(DIAGNOSTIC_SEVERITY);
		flavorKindEEnum = createEEnum(FLAVOR_KIND);
		httpMethodEEnum = createEEnum(HTTP_METHOD);
		mqttQosEEnum = createEEnum(MQTT_QOS);
		registryKindEEnum = createEEnum(REGISTRY_KIND);
		expressionLanguageEEnum = createEEnum(EXPRESSION_LANGUAGE);
		catalogStatusEEnum = createEEnum(CATALOG_STATUS);
		connectionStateEEnum = createEEnum(CONNECTION_STATE);
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
		propertyEClass.getESuperTypes().add(this.getNamedElement());
		stringPropertyEClass.getESuperTypes().add(this.getProperty());
		intPropertyEClass.getESuperTypes().add(this.getProperty());
		longPropertyEClass.getESuperTypes().add(this.getProperty());
		doublePropertyEClass.getESuperTypes().add(this.getProperty());
		floatPropertyEClass.getESuperTypes().add(this.getProperty());
		shortPropertyEClass.getESuperTypes().add(this.getProperty());
		boolPropertyEClass.getESuperTypes().add(this.getProperty());
		stringListPropertyEClass.getESuperTypes().add(this.getProperty());
		serviceOperationEClass.getESuperTypes().add(this.getNamedElement());
		parameterEClass.getESuperTypes().add(this.getNamedElement());
		requiredConstraintEClass.getESuperTypes().add(this.getParameterConstraint());
		numericRangeConstraintEClass.getESuperTypes().add(this.getParameterConstraint());
		stringPatternConstraintEClass.getESuperTypes().add(this.getParameterConstraint());
		enumerationConstraintEClass.getESuperTypes().add(this.getParameterConstraint());
		expressionConstraintEClass.getESuperTypes().add(this.getParameterConstraint());
		expressionConstraintEClass.getESuperTypes().add(this.getNamedElement());
		invariantEClass.getESuperTypes().add(this.getNamedElement());
		collectionSizeConstraintEClass.getESuperTypes().add(this.getParameterConstraint());
		serviceExceptionEClass.getESuperTypes().add(this.getNamedElement());
		serviceExceptionEClass.getESuperTypes().add(this.getVersionedElement());
		serviceInterfaceEClass.getESuperTypes().add(this.getNamedElement());
		serviceInterfaceEClass.getESuperTypes().add(this.getVersionedElement());
		lifecycleHookEClass.getESuperTypes().add(this.getNamedElement());
		referenceBindingEClass.getESuperTypes().add(this.getNamedElement());
		componentReferenceEClass.getESuperTypes().add(this.getNamedElement());
		componentDescriptionEClass.getESuperTypes().add(this.getNamedElement());
		serviceProviderEClass.getESuperTypes().add(this.getNamedElement());
		serviceProviderEClass.getESuperTypes().add(this.getVersionedElement());
		serviceImplementationEClass.getESuperTypes().add(this.getNamedElement());
		serviceImplementationEClass.getESuperTypes().add(this.getVersionedElement());
		serviceFlavorEClass.getESuperTypes().add(this.getNamedElement());
		restFlavorEClass.getESuperTypes().add(this.getServiceFlavor());
		mqttFlavorEClass.getESuperTypes().add(this.getServiceFlavor());
		serviceOperationFlavorEClass.getESuperTypes().add(this.getNamedElement());
		restOperationFlavorEClass.getESuperTypes().add(this.getServiceOperationFlavor());
		mqttOperationFlavorEClass.getESuperTypes().add(this.getServiceOperationFlavor());
		componentConfigurationEClass.getESuperTypes().add(this.getNamedElement());
		serviceRegistryEClass.getESuperTypes().add(this.getNamedElement());
		localServiceRegistryEClass.getESuperTypes().add(this.getServiceRegistry());
		remoteServiceRegistryEClass.getESuperTypes().add(this.getServiceRegistry());

		// Initialize classes, features, and operations; add parameters
		initEClass(namedElementEClass, NamedElement.class, "NamedElement", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getNamedElement_Name(), ecorePackage.getEString(), "name", null, 1, 1, NamedElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(versionedElementEClass, VersionedElement.class, "VersionedElement", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getVersionedElement_Version(), ecorePackage.getEString(), "version", null, 0, 1, VersionedElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(propertyEClass, Property.class, "Property", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(stringPropertyEClass, StringProperty.class, "StringProperty", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getStringProperty_Value(), ecorePackage.getEString(), "value", null, 0, 1, StringProperty.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(intPropertyEClass, IntProperty.class, "IntProperty", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getIntProperty_Value(), ecorePackage.getEInt(), "value", null, 0, 1, IntProperty.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(longPropertyEClass, LongProperty.class, "LongProperty", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getLongProperty_Value(), ecorePackage.getELong(), "value", null, 0, 1, LongProperty.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(doublePropertyEClass, DoubleProperty.class, "DoubleProperty", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getDoubleProperty_Value(), ecorePackage.getEDouble(), "value", null, 0, 1, DoubleProperty.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(floatPropertyEClass, FloatProperty.class, "FloatProperty", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFloatProperty_Value(), ecorePackage.getEFloat(), "value", null, 0, 1, FloatProperty.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(shortPropertyEClass, ShortProperty.class, "ShortProperty", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getShortProperty_Value(), ecorePackage.getEShort(), "value", null, 0, 1, ShortProperty.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(boolPropertyEClass, BoolProperty.class, "BoolProperty", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getBoolProperty_Value(), ecorePackage.getEBoolean(), "value", null, 0, 1, BoolProperty.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(stringListPropertyEClass, StringListProperty.class, "StringListProperty", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getStringListProperty_Value(), ecorePackage.getEString(), "value", null, 0, -1, StringListProperty.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceOperationEClass, ServiceOperation.class, "ServiceOperation", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceOperation_Description(), ecorePackage.getEString(), "description", null, 0, 1, ServiceOperation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceOperation_Parameters(), this.getParameter(), null, "parameters", null, 0, -1, ServiceOperation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServiceOperation_ReturnType(), ecorePackage.getEString(), "returnType", null, 0, 1, ServiceOperation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceOperation_ReturnConstraints(), this.getParameterConstraint(), null, "returnConstraints", null, 0, -1, ServiceOperation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceOperation_Exceptions(), this.getServiceException(), null, "exceptions", null, 0, -1, ServiceOperation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceOperation_Preconditions(), this.getInvariant(), null, "preconditions", null, 0, -1, ServiceOperation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceOperation_Postconditions(), this.getInvariant(), null, "postconditions", null, 0, -1, ServiceOperation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(parameterEClass, Parameter.class, "Parameter", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getParameter_Index(), ecorePackage.getEInt(), "index", null, 1, 1, Parameter.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getParameter_Type(), ecorePackage.getEString(), "type", null, 1, 1, Parameter.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getParameter_Optional(), ecorePackage.getEBoolean(), "optional", "false", 1, 1, Parameter.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getParameter_DefaultValue(), ecorePackage.getEString(), "defaultValue", null, 0, 1, Parameter.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getParameter_Description(), ecorePackage.getEString(), "description", null, 0, 1, Parameter.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getParameter_Constraints(), this.getParameterConstraint(), null, "constraints", null, 0, -1, Parameter.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(parameterConstraintEClass, ParameterConstraint.class, "ParameterConstraint", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(requiredConstraintEClass, RequiredConstraint.class, "RequiredConstraint", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(numericRangeConstraintEClass, NumericRangeConstraint.class, "NumericRangeConstraint", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getNumericRangeConstraint_Min(), ecorePackage.getEDouble(), "min", null, 0, 1, NumericRangeConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getNumericRangeConstraint_Max(), ecorePackage.getEDouble(), "max", null, 0, 1, NumericRangeConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getNumericRangeConstraint_InclusiveMin(), ecorePackage.getEBoolean(), "inclusiveMin", "true", 1, 1, NumericRangeConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getNumericRangeConstraint_InclusiveMax(), ecorePackage.getEBoolean(), "inclusiveMax", "true", 1, 1, NumericRangeConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(stringPatternConstraintEClass, StringPatternConstraint.class, "StringPatternConstraint", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getStringPatternConstraint_Pattern(), ecorePackage.getEString(), "pattern", null, 1, 1, StringPatternConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getStringPatternConstraint_MinLength(), ecorePackage.getEInt(), "minLength", null, 0, 1, StringPatternConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getStringPatternConstraint_MaxLength(), ecorePackage.getEInt(), "maxLength", null, 0, 1, StringPatternConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(enumerationConstraintEClass, EnumerationConstraint.class, "EnumerationConstraint", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEnumerationConstraint_AllowedValues(), ecorePackage.getEString(), "allowedValues", null, 1, -1, EnumerationConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(expressionConstraintEClass, ExpressionConstraint.class, "ExpressionConstraint", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getExpressionConstraint_Language(), this.getExpressionLanguage(), "language", "OCL", 1, 1, ExpressionConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getExpressionConstraint_Expression(), ecorePackage.getEString(), "expression", null, 1, 1, ExpressionConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getExpressionConstraint_Message(), ecorePackage.getEString(), "message", null, 0, 1, ExpressionConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(invariantEClass, Invariant.class, "Invariant", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getInvariant_Language(), this.getExpressionLanguage(), "language", "OCL", 1, 1, Invariant.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getInvariant_Expression(), ecorePackage.getEString(), "expression", null, 1, 1, Invariant.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getInvariant_Message(), ecorePackage.getEString(), "message", null, 0, 1, Invariant.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(collectionSizeConstraintEClass, CollectionSizeConstraint.class, "CollectionSizeConstraint", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getCollectionSizeConstraint_MinSize(), ecorePackage.getEInt(), "minSize", null, 0, 1, CollectionSizeConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCollectionSizeConstraint_MaxSize(), ecorePackage.getEInt(), "maxSize", null, 0, 1, CollectionSizeConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceExceptionEClass, ServiceException.class, "ServiceException", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceException_Description(), ecorePackage.getEString(), "description", null, 0, 1, ServiceException.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServiceException_Type(), ecorePackage.getEString(), "type", null, 1, 1, ServiceException.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceException_Properties(), this.getProperty(), null, "properties", null, 0, -1, ServiceException.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceInterfaceEClass, ServiceInterface.class, "ServiceInterface", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceInterface_Description(), ecorePackage.getEString(), "description", null, 0, 1, ServiceInterface.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceInterface_Operations(), this.getServiceOperation(), null, "operations", null, 0, -1, ServiceInterface.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceInterface_Exceptions(), this.getServiceException(), null, "exceptions", null, 0, -1, ServiceInterface.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceInterface_Invariants(), this.getInvariant(), null, "invariants", null, 0, -1, ServiceInterface.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServiceInterface_Status(), this.getCatalogStatus(), "status", "ACTIVE", 1, 1, ServiceInterface.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServiceInterface_DeprecationReason(), ecorePackage.getEString(), "deprecationReason", null, 0, 1, ServiceInterface.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceInterface_ReplacedBy(), this.getServiceInterface(), null, "replacedBy", null, 0, 1, ServiceInterface.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(lifecycleHookEClass, LifecycleHook.class, "LifecycleHook", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getLifecycleHook_Kind(), this.getLifecycleHookKind(), "kind", null, 1, 1, LifecycleHook.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getLifecycleHook_Parameter(), ecorePackage.getEInt(), "parameter", null, 0, 1, LifecycleHook.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(referenceBindingEClass, ReferenceBinding.class, "ReferenceBinding", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getReferenceBinding_Kind(), this.getReferenceBindingKind(), "kind", null, 1, 1, ReferenceBinding.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getReferenceBinding_FieldOption(), this.getFieldOption(), "fieldOption", null, 0, 1, ReferenceBinding.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(componentReferenceEClass, ComponentReference.class, "ComponentReference", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getComponentReference_InterfaceName(), ecorePackage.getEString(), "interfaceName", null, 1, 1, ComponentReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentReference_Cardinality(), this.getReferenceCardinality(), "cardinality", "ONE", 1, 1, ComponentReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentReference_Policy(), this.getReferencePolicy(), "policy", "STATIC", 1, 1, ComponentReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentReference_PolicyOption(), this.getReferencePolicyOption(), "policyOption", "RELUCTANT", 1, 1, ComponentReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentReference_Target(), ecorePackage.getEString(), "target", null, 0, 1, ComponentReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentReference_Scope(), this.getServiceScope(), "scope", "BUNDLE", 1, 1, ComponentReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentReference_CollectionType(), this.getCollectionType(), "collectionType", null, 0, 1, ComponentReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentReference_Parameter(), ecorePackage.getEInt(), "parameter", null, 0, 1, ComponentReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentReference_Bindings(), this.getReferenceBinding(), null, "bindings", null, 0, -1, ComponentReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(componentDescriptionEClass, ComponentDescription.class, "ComponentDescription", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getComponentDescription_Factory(), ecorePackage.getEString(), "factory", null, 0, 1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentDescription_Scope(), this.getServiceScope(), "scope", "SINGLETON", 1, 1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentDescription_ImplementationId(), ecorePackage.getEString(), "implementationId", null, 1, 1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentDescription_DefaultEnabled(), ecorePackage.getEBoolean(), "defaultEnabled", "true", 1, 1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentDescription_Immediate(), ecorePackage.getEBoolean(), "immediate", "false", 1, 1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentDescription_ConfigurationPolicy(), this.getConfigurationPolicy(), "configurationPolicy", "OPTIONAL", 1, 1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentDescription_ConfigurationPid(), ecorePackage.getEString(), "configurationPid", null, 0, -1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentDescription_ServiceInterfaces(), this.getServiceInterface(), null, "serviceInterfaces", null, 0, -1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentDescription_Properties(), this.getProperty(), null, "properties", null, 0, -1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentDescription_FactoryProperties(), this.getProperty(), null, "factoryProperties", null, 0, -1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentDescription_References(), this.getComponentReference(), null, "references", null, 0, -1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentDescription_LifecycleHooks(), this.getLifecycleHook(), null, "lifecycleHooks", null, 0, -1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentDescription_Provider(), this.getServiceProvider(), null, "provider", null, 0, 1, ComponentDescription.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceProviderEClass, ServiceProvider.class, "ServiceProvider", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceProvider_SymbolicName(), ecorePackage.getEString(), "symbolicName", null, 1, 1, ServiceProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceProvider_Descriptions(), this.getComponentDescription(), null, "descriptions", null, 0, -1, ServiceProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceProvider_Implementations(), this.getServiceImplementation(), null, "implementations", null, 0, -1, ServiceProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceImplementationEClass, ServiceImplementation.class, "ServiceImplementation", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceImplementation_Description(), ecorePackage.getEString(), "description", null, 0, 1, ServiceImplementation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServiceImplementation_ImplementationId(), ecorePackage.getEString(), "implementationId", null, 1, 1, ServiceImplementation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceImplementation_ServiceInterfaces(), this.getServiceInterface(), null, "serviceInterfaces", null, 1, -1, ServiceImplementation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceImplementation_Flavors(), this.getServiceFlavor(), null, "flavors", null, 0, -1, ServiceImplementation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceImplementation_Properties(), this.getProperty(), null, "properties", null, 0, -1, ServiceImplementation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceImplementation_ComponentDescription(), this.getComponentDescription(), null, "componentDescription", null, 0, 1, ServiceImplementation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceFlavorEClass, ServiceFlavor.class, "ServiceFlavor", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceFlavor_Kind(), this.getFlavorKind(), "kind", null, 1, 1, ServiceFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceFlavor_OperationFlavors(), this.getServiceOperationFlavor(), null, "operationFlavors", null, 0, -1, ServiceFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(restFlavorEClass, RestFlavor.class, "RestFlavor", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRestFlavor_Host(), ecorePackage.getEString(), "host", null, 0, 1, RestFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRestFlavor_BasePath(), ecorePackage.getEString(), "basePath", null, 1, 1, RestFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRestFlavor_ContentTypes(), ecorePackage.getEString(), "contentTypes", null, 0, -1, RestFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(mqttFlavorEClass, MqttFlavor.class, "MqttFlavor", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMqttFlavor_Brokers(), ecorePackage.getEString(), "brokers", null, 1, -1, MqttFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMqttFlavor_RequestTopic(), ecorePackage.getEString(), "requestTopic", null, 1, 1, MqttFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMqttFlavor_ResponseTopic(), ecorePackage.getEString(), "responseTopic", null, 0, 1, MqttFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMqttFlavor_DefaultQos(), this.getMqttQos(), "defaultQos", "AT_LEAST_ONCE", 1, 1, MqttFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMqttFlavor_DefaultRetained(), ecorePackage.getEBoolean(), "defaultRetained", "false", 1, 1, MqttFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceOperationFlavorEClass, ServiceOperationFlavor.class, "ServiceOperationFlavor", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getServiceOperationFlavor_Operation(), this.getServiceOperation(), null, "operation", null, 1, 1, ServiceOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServiceOperationFlavor_Consumes(), ecorePackage.getEString(), "consumes", null, 0, -1, ServiceOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServiceOperationFlavor_Produces(), ecorePackage.getEString(), "produces", null, 0, -1, ServiceOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(restOperationFlavorEClass, RestOperationFlavor.class, "RestOperationFlavor", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRestOperationFlavor_Method(), this.getHttpMethod(), "method", null, 1, 1, RestOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRestOperationFlavor_Path(), ecorePackage.getEString(), "path", null, 0, 1, RestOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRestOperationFlavor_ReturnCodes(), ecorePackage.getEInt(), "returnCodes", null, 1, -1, RestOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(mqttOperationFlavorEClass, MqttOperationFlavor.class, "MqttOperationFlavor", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMqttOperationFlavor_RequestTopic(), ecorePackage.getEString(), "requestTopic", null, 0, 1, MqttOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMqttOperationFlavor_ResponseTopic(), ecorePackage.getEString(), "responseTopic", null, 0, 1, MqttOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMqttOperationFlavor_Qos(), this.getMqttQos(), "qos", null, 0, 1, MqttOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMqttOperationFlavor_Retained(), ecorePackage.getEBoolean(), "retained", null, 0, 1, MqttOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMqttOperationFlavor_Correlation(), ecorePackage.getEBoolean(), "correlation", "true", 1, 1, MqttOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMqttOperationFlavor_ReturnPath(), ecorePackage.getEString(), "returnPath", null, 0, 1, MqttOperationFlavor.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceReferenceEClass, ServiceReference.class, "ServiceReference", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceReference_Id(), ecorePackage.getEString(), "id", null, 1, 1, ServiceReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceReference_Properties(), this.getProperty(), null, "properties", null, 0, -1, ServiceReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceReference_Provider(), this.getServiceProvider(), null, "provider", null, 1, 1, ServiceReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceReference_Registration(), this.getServiceRegistration(), this.getServiceRegistration_Reference(), "registration", null, 0, 1, ServiceReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		EOperation op = initEOperation(getServiceReference__GetProperty__String(), ecorePackage.getEJavaObject(), "getProperty", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "key", 0, 1, IS_UNIQUE, IS_ORDERED);

		initEOperation(getServiceReference__GetPropertyKeys(), ecorePackage.getEString(), "getPropertyKeys", 0, -1, IS_UNIQUE, IS_ORDERED);

		initEClass(serviceRegistrationEClass, ServiceRegistration.class, "ServiceRegistration", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getServiceRegistration_Reference(), this.getServiceReference(), this.getServiceReference_Registration(), "reference", null, 1, 1, ServiceRegistration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServiceRegistration_Unregistered(), ecorePackage.getEBoolean(), "unregistered", "false", 1, 1, ServiceRegistration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceRegistration_Provider(), this.getServiceProvider(), null, "provider", null, 1, 1, ServiceRegistration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceRegistration_Implementation(), this.getServiceImplementation(), null, "implementation", null, 1, 1, ServiceRegistration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceRegistration_UsingSessions(), this.getConsumerSession(), this.getConsumerSession_Acquisitions(), "usingSessions", null, 0, -1, ServiceRegistration.class, IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEOperation(getServiceRegistration__Unregister(), null, "unregister", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getServiceRegistration__SetProperties__EList(), null, "setProperties", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getProperty(), "props", 0, -1, IS_UNIQUE, IS_ORDERED);

		initEClass(consumerSessionEClass, ConsumerSession.class, "ConsumerSession", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getConsumerSession_ConsumerId(), ecorePackage.getEString(), "consumerId", null, 1, 1, ConsumerSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConsumerSession_LastRenewal(), ecorePackage.getEDate(), "lastRenewal", null, 0, 1, ConsumerSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getConsumerSession_Capabilities(), this.getConsumerCapability(), null, "capabilities", null, 0, 1, ConsumerSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getConsumerSession_Acquisitions(), this.getServiceRegistration(), this.getServiceRegistration_UsingSessions(), "acquisitions", null, 0, -1, ConsumerSession.class, IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(componentConfigurationEClass, ComponentConfiguration.class, "ComponentConfiguration", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getComponentConfiguration_Id(), ecorePackage.getEString(), "id", null, 1, 1, ComponentConfiguration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentConfiguration_Description(), this.getComponentDescription(), null, "description", null, 1, 1, ComponentConfiguration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentConfiguration_State(), this.getComponentState(), "state", null, 1, 1, ComponentConfiguration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentConfiguration_Properties(), this.getProperty(), null, "properties", null, 0, -1, ComponentConfiguration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentConfiguration_SatisfiedReferences(), this.getSatisfiedReference(), null, "satisfiedReferences", null, 0, -1, ComponentConfiguration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentConfiguration_UnsatisfiedReferences(), this.getUnsatisfiedReference(), null, "unsatisfiedReferences", null, 0, -1, ComponentConfiguration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentConfiguration_Failure(), this.getDiagnostic(), null, "failure", null, 0, 1, ComponentConfiguration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentConfiguration_Service(), this.getServiceReference(), null, "service", null, 0, 1, ComponentConfiguration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(satisfiedReferenceEClass, SatisfiedReference.class, "SatisfiedReference", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getSatisfiedReference_Name(), ecorePackage.getEString(), "name", null, 1, 1, SatisfiedReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSatisfiedReference_Target(), ecorePackage.getEString(), "target", null, 0, 1, SatisfiedReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getSatisfiedReference_BoundServices(), this.getServiceReference(), null, "boundServices", null, 0, -1, SatisfiedReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(unsatisfiedReferenceEClass, UnsatisfiedReference.class, "UnsatisfiedReference", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getUnsatisfiedReference_Name(), ecorePackage.getEString(), "name", null, 1, 1, UnsatisfiedReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getUnsatisfiedReference_Target(), ecorePackage.getEString(), "target", null, 0, 1, UnsatisfiedReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getUnsatisfiedReference_TargetServices(), this.getServiceReference(), null, "targetServices", null, 0, -1, UnsatisfiedReference.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(diagnosticEClass, Diagnostic.class, "Diagnostic", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getDiagnostic_Severity(), this.getDiagnosticSeverity(), "severity", "OK", 1, 1, Diagnostic.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDiagnostic_Message(), ecorePackage.getEString(), "message", null, 0, 1, Diagnostic.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDiagnostic_Source(), ecorePackage.getEString(), "source", null, 0, 1, Diagnostic.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDiagnostic_Code(), ecorePackage.getEInt(), "code", "0", 1, 1, Diagnostic.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDiagnostic_Data(), ecorePackage.getEString(), "data", null, 0, -1, Diagnostic.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getDiagnostic_Children(), this.getDiagnostic(), null, "children", null, 0, -1, Diagnostic.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceEventEClass, ServiceEvent.class, "ServiceEvent", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceEvent_Type(), this.getServiceEventType(), "type", null, 1, 1, ServiceEvent.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceEvent_Reference(), this.getServiceReference(), null, "reference", null, 1, 1, ServiceEvent.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServiceEvent_Timestamp(), ecorePackage.getEDate(), "timestamp", null, 0, 1, ServiceEvent.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serviceListenerEClass, ServiceListener.class, "ServiceListener", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceListener_Filter(), ecorePackage.getEString(), "filter", null, 0, 1, ServiceListener.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		op = initEOperation(getServiceListener__ServiceChanged__ServiceEvent(), null, "serviceChanged", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceEvent(), "event", 0, 1, IS_UNIQUE, IS_ORDERED);

		initEClass(serviceRegistryEClass, ServiceRegistry.class, "ServiceRegistry", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServiceRegistry_Kind(), this.getRegistryKind(), "kind", null, 1, 1, ServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceRegistry_PublishHooks(), this.getPublishHook(), null, "publishHooks", null, 0, -1, ServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceRegistry_DiscoveryHooks(), this.getDiscoveryHook(), null, "discoveryHooks", null, 0, -1, ServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServiceRegistry_DistributionHooks(), this.getDistributionHook(), null, "distributionHooks", null, 0, -1, ServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		op = initEOperation(getServiceRegistry__GetServiceReference__String(), this.getServiceReference(), "getServiceReference", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "interfaceName", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getServiceRegistry__GetServiceReferences__String_String_ConsumerCapability(), this.getServiceReference(), "getServiceReferences", 0, -1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "interfaceName", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "filter", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getConsumerCapability(), "capability", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getServiceRegistry__GetAllServiceReferences__String_String_ConsumerCapability(), this.getServiceReference(), "getAllServiceReferences", 0, -1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "interfaceName", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "filter", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getConsumerCapability(), "capability", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getServiceRegistry__AddServiceListener__ServiceListener(), null, "addServiceListener", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceListener(), "listener", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getServiceRegistry__RemoveServiceListener__ServiceListener(), null, "removeServiceListener", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceListener(), "listener", 0, 1, IS_UNIQUE, IS_ORDERED);

		initEClass(localServiceRegistryEClass, LocalServiceRegistry.class, "LocalServiceRegistry", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getLocalServiceRegistry_References(), this.getServiceReference(), null, "references", null, 0, -1, LocalServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getLocalServiceRegistry_Registrations(), this.getServiceRegistration(), null, "registrations", null, 0, -1, LocalServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getLocalServiceRegistry_Sessions(), this.getConsumerSession(), null, "sessions", null, 0, -1, LocalServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getLocalServiceRegistry_Configurations(), this.getComponentConfiguration(), null, "configurations", null, 0, -1, LocalServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getLocalServiceRegistry_Providers(), this.getServiceProvider(), null, "providers", null, 0, -1, LocalServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getLocalServiceRegistry_Listeners(), this.getServiceListener(), null, "listeners", null, 0, -1, LocalServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getLocalServiceRegistry_Remote(), this.getRemoteServiceRegistry(), null, "remote", null, 0, 1, LocalServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getLocalServiceRegistry_ConnectionState(), this.getConnectionState(), "connectionState", "OFFLINE", 1, 1, LocalServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		op = initEOperation(getLocalServiceRegistry__RegisterService__ServiceProvider_ServiceImplementation_EList(), this.getServiceRegistration(), "registerService", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceProvider(), "provider", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceImplementation(), "implementation", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getProperty(), "props", 0, -1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getLocalServiceRegistry__FireServiceEvent__ServiceEvent(), null, "fireServiceEvent", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceEvent(), "event", 0, 1, IS_UNIQUE, IS_ORDERED);

		initEClass(remoteServiceRegistryEClass, RemoteServiceRegistry.class, "RemoteServiceRegistry", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRemoteServiceRegistry_Endpoint(), ecorePackage.getEString(), "endpoint", null, 0, 1, RemoteServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRemoteServiceRegistry_Catalog(), this.getServiceInterface(), null, "catalog", null, 0, -1, RemoteServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRemoteServiceRegistry_Implementations(), this.getServiceImplementation(), null, "implementations", null, 0, -1, RemoteServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRemoteServiceRegistry_Providers(), this.getServiceProvider(), null, "providers", null, 0, -1, RemoteServiceRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		op = initEOperation(getRemoteServiceRegistry__PublishImplementation__ServiceProvider_ServiceImplementation(), this.getDiagnostic(), "publishImplementation", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceProvider(), "provider", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceImplementation(), "implementation", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getRemoteServiceRegistry__WithdrawImplementation__ServiceProvider_ServiceImplementation(), this.getDiagnostic(), "withdrawImplementation", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceProvider(), "provider", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceImplementation(), "implementation", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getRemoteServiceRegistry__AddCatalogEntry__ServiceInterface_String(), this.getDiagnostic(), "addCatalogEntry", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceInterface(), "serviceInterface", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "requestor", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getRemoteServiceRegistry__DeprecateCatalogEntry__ServiceInterface_String(), this.getDiagnostic(), "deprecateCatalogEntry", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceInterface(), "serviceInterface", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "requestor", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getRemoteServiceRegistry__RemoveCatalogEntry__ServiceInterface_String(), this.getDiagnostic(), "removeCatalogEntry", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceInterface(), "serviceInterface", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "requestor", 0, 1, IS_UNIQUE, IS_ORDERED);

		initEClass(consumerCapabilityEClass, ConsumerCapability.class, "ConsumerCapability", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getConsumerCapability_ConsumerId(), ecorePackage.getEString(), "consumerId", null, 0, 1, ConsumerCapability.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConsumerCapability_SupportedFlavors(), this.getFlavorKind(), "supportedFlavors", null, 1, -1, ConsumerCapability.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getConsumerCapability_Properties(), this.getProperty(), null, "properties", null, 0, -1, ConsumerCapability.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(publishHookEClass, PublishHook.class, "PublishHook", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		op = initEOperation(getPublishHook__OnPublish__ServiceProvider_ServiceImplementation(), this.getDiagnostic(), "onPublish", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceProvider(), "provider", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceImplementation(), "implementation", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getPublishHook__OnWithdraw__ServiceProvider_ServiceImplementation(), this.getDiagnostic(), "onWithdraw", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceProvider(), "provider", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceImplementation(), "implementation", 0, 1, IS_UNIQUE, IS_ORDERED);

		initEClass(discoveryHookEClass, DiscoveryHook.class, "DiscoveryHook", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		op = initEOperation(getDiscoveryHook__OnLookup__String_String_ConsumerCapability(), this.getDiagnostic(), "onLookup", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "interfaceName", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEString(), "filter", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getConsumerCapability(), "capability", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getDiscoveryHook__FilterReferences__ConsumerCapability_EList(), this.getServiceReference(), "filterReferences", 0, -1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getConsumerCapability(), "capability", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceReference(), "references", 0, -1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getDiscoveryHook__OnSubscribe__ServiceListener(), this.getDiagnostic(), "onSubscribe", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceListener(), "listener", 0, 1, IS_UNIQUE, IS_ORDERED);

		initEClass(distributionHookEClass, DistributionHook.class, "DistributionHook", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		op = initEOperation(getDistributionHook__OnOutbound__ServiceEvent(), this.getDiagnostic(), "onOutbound", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceEvent(), "event", 0, 1, IS_UNIQUE, IS_ORDERED);

		op = initEOperation(getDistributionHook__OnInbound__ServiceEvent(), this.getDiagnostic(), "onInbound", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, this.getServiceEvent(), "event", 0, 1, IS_UNIQUE, IS_ORDERED);

		// Initialize enums and add enum literals
		initEEnum(serviceScopeEEnum, ServiceScope.class, "ServiceScope");
		addEEnumLiteral(serviceScopeEEnum, ServiceScope.SINGLETON);
		addEEnumLiteral(serviceScopeEEnum, ServiceScope.BUNDLE);
		addEEnumLiteral(serviceScopeEEnum, ServiceScope.PROTOTYPE);

		initEEnum(referenceCardinalityEEnum, ReferenceCardinality.class, "ReferenceCardinality");
		addEEnumLiteral(referenceCardinalityEEnum, ReferenceCardinality.ZERO_OR_ONE);
		addEEnumLiteral(referenceCardinalityEEnum, ReferenceCardinality.ONE);
		addEEnumLiteral(referenceCardinalityEEnum, ReferenceCardinality.ZERO_OR_MANY);
		addEEnumLiteral(referenceCardinalityEEnum, ReferenceCardinality.ONE_OR_MANY);

		initEEnum(referencePolicyEEnum, ReferencePolicy.class, "ReferencePolicy");
		addEEnumLiteral(referencePolicyEEnum, ReferencePolicy.STATIC);
		addEEnumLiteral(referencePolicyEEnum, ReferencePolicy.DYNAMIC);

		initEEnum(referencePolicyOptionEEnum, ReferencePolicyOption.class, "ReferencePolicyOption");
		addEEnumLiteral(referencePolicyOptionEEnum, ReferencePolicyOption.RELUCTANT);
		addEEnumLiteral(referencePolicyOptionEEnum, ReferencePolicyOption.GREEDY);

		initEEnum(configurationPolicyEEnum, ConfigurationPolicy.class, "ConfigurationPolicy");
		addEEnumLiteral(configurationPolicyEEnum, ConfigurationPolicy.OPTIONAL);
		addEEnumLiteral(configurationPolicyEEnum, ConfigurationPolicy.REQUIRE);
		addEEnumLiteral(configurationPolicyEEnum, ConfigurationPolicy.IGNORE);

		initEEnum(componentStateEEnum, ComponentState.class, "ComponentState");
		addEEnumLiteral(componentStateEEnum, ComponentState.UNSATISFIED_CONFIGURATION);
		addEEnumLiteral(componentStateEEnum, ComponentState.UNSATISFIED_REFERENCE);
		addEEnumLiteral(componentStateEEnum, ComponentState.SATISFIED);
		addEEnumLiteral(componentStateEEnum, ComponentState.ACTIVE);
		addEEnumLiteral(componentStateEEnum, ComponentState.FAILED_ACTIVATION);

		initEEnum(serviceEventTypeEEnum, ServiceEventType.class, "ServiceEventType");
		addEEnumLiteral(serviceEventTypeEEnum, ServiceEventType.UNSPECIFIED);
		addEEnumLiteral(serviceEventTypeEEnum, ServiceEventType.REGISTERED);
		addEEnumLiteral(serviceEventTypeEEnum, ServiceEventType.MODIFIED);
		addEEnumLiteral(serviceEventTypeEEnum, ServiceEventType.UNREGISTERING);
		addEEnumLiteral(serviceEventTypeEEnum, ServiceEventType.MODIFIED_ENDMATCH);

		initEEnum(fieldOptionEEnum, FieldOption.class, "FieldOption");
		addEEnumLiteral(fieldOptionEEnum, FieldOption.REPLACE);
		addEEnumLiteral(fieldOptionEEnum, FieldOption.UPDATE);

		initEEnum(collectionTypeEEnum, CollectionType.class, "CollectionType");
		addEEnumLiteral(collectionTypeEEnum, CollectionType.SERVICE);
		addEEnumLiteral(collectionTypeEEnum, CollectionType.REFERENCE);
		addEEnumLiteral(collectionTypeEEnum, CollectionType.SERVICEOBJECTS);
		addEEnumLiteral(collectionTypeEEnum, CollectionType.PROPERTIES);
		addEEnumLiteral(collectionTypeEEnum, CollectionType.TUPLE);

		initEEnum(lifecycleHookKindEEnum, LifecycleHookKind.class, "LifecycleHookKind");
		addEEnumLiteral(lifecycleHookKindEEnum, LifecycleHookKind.ACTIVATE);
		addEEnumLiteral(lifecycleHookKindEEnum, LifecycleHookKind.DEACTIVATE);
		addEEnumLiteral(lifecycleHookKindEEnum, LifecycleHookKind.MODIFIED);
		addEEnumLiteral(lifecycleHookKindEEnum, LifecycleHookKind.ACTIVATION_FIELD);

		initEEnum(referenceBindingKindEEnum, ReferenceBindingKind.class, "ReferenceBindingKind");
		addEEnumLiteral(referenceBindingKindEEnum, ReferenceBindingKind.BIND);
		addEEnumLiteral(referenceBindingKindEEnum, ReferenceBindingKind.UNBIND);
		addEEnumLiteral(referenceBindingKindEEnum, ReferenceBindingKind.UPDATED);
		addEEnumLiteral(referenceBindingKindEEnum, ReferenceBindingKind.FIELD);

		initEEnum(diagnosticSeverityEEnum, DiagnosticSeverity.class, "DiagnosticSeverity");
		addEEnumLiteral(diagnosticSeverityEEnum, DiagnosticSeverity.OK);
		addEEnumLiteral(diagnosticSeverityEEnum, DiagnosticSeverity.INFO);
		addEEnumLiteral(diagnosticSeverityEEnum, DiagnosticSeverity.WARNING);
		addEEnumLiteral(diagnosticSeverityEEnum, DiagnosticSeverity.ERROR);
		addEEnumLiteral(diagnosticSeverityEEnum, DiagnosticSeverity.CANCEL);

		initEEnum(flavorKindEEnum, FlavorKind.class, "FlavorKind");
		addEEnumLiteral(flavorKindEEnum, FlavorKind.REST);
		addEEnumLiteral(flavorKindEEnum, FlavorKind.MQTT);

		initEEnum(httpMethodEEnum, HttpMethod.class, "HttpMethod");
		addEEnumLiteral(httpMethodEEnum, HttpMethod.GET);
		addEEnumLiteral(httpMethodEEnum, HttpMethod.POST);
		addEEnumLiteral(httpMethodEEnum, HttpMethod.PUT);
		addEEnumLiteral(httpMethodEEnum, HttpMethod.PATCH);
		addEEnumLiteral(httpMethodEEnum, HttpMethod.DELETE);
		addEEnumLiteral(httpMethodEEnum, HttpMethod.HEAD);
		addEEnumLiteral(httpMethodEEnum, HttpMethod.OPTIONS);

		initEEnum(mqttQosEEnum, MqttQos.class, "MqttQos");
		addEEnumLiteral(mqttQosEEnum, MqttQos.AT_MOST_ONCE);
		addEEnumLiteral(mqttQosEEnum, MqttQos.AT_LEAST_ONCE);
		addEEnumLiteral(mqttQosEEnum, MqttQos.EXACTLY_ONCE);

		initEEnum(registryKindEEnum, RegistryKind.class, "RegistryKind");
		addEEnumLiteral(registryKindEEnum, RegistryKind.LOCAL);
		addEEnumLiteral(registryKindEEnum, RegistryKind.REMOTE);

		initEEnum(expressionLanguageEEnum, ExpressionLanguage.class, "ExpressionLanguage");
		addEEnumLiteral(expressionLanguageEEnum, ExpressionLanguage.OCL);

		initEEnum(catalogStatusEEnum, CatalogStatus.class, "CatalogStatus");
		addEEnumLiteral(catalogStatusEEnum, CatalogStatus.ACTIVE);
		addEEnumLiteral(catalogStatusEEnum, CatalogStatus.DEPRECATED);

		initEEnum(connectionStateEEnum, ConnectionState.class, "ConnectionState");
		addEEnumLiteral(connectionStateEEnum, ConnectionState.CONNECTED);
		addEEnumLiteral(connectionStateEEnum, ConnectionState.DEGRADED);
		addEEnumLiteral(connectionStateEEnum, ConnectionState.OFFLINE);

		// Create resource
		createResource(eNS_URI);

		// Create annotations
		// Version
		createVersionAnnotations();
		// http://www.eclipse.org/emf/2002/GenModel
		createGenModelAnnotations();
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
			   "basePackage", "org.gecko.ddsr.model",
			   "resource", "XMI",
			   "documentation", "Gecko DDSR \u2014 Dynamic Distributed Service Registry. Language-neutral model of an OSGi-inspired service registry that spans Java, TypeScript, and Python. Combines OSGi DS (declarative components, references, lifecycle) with OSGi service-registry events and a UDDI-style central Broker (Remote Registry). The model is the contract between Java and TypeScript implementations \u2014 see REQUIREMENTS.md NFR-Behavioral-Parity."
		   });
		addAnnotation
		  (serviceScopeEEnum,
		   source,
		   new String[] {
			   "documentation", "OSGi service scope. SINGLETON = one instance shared by all consumers; BUNDLE = one instance per consuming bundle/provider; PROTOTYPE = a new instance per consumer request."
		   });
		addAnnotation
		  (referenceCardinalityEEnum,
		   source,
		   new String[] {
			   "documentation", "Cardinality of a ComponentReference. ZERO_OR_ONE = 0..1; ONE = 1..1; ZERO_OR_MANY = 0..*; ONE_OR_MANY = 1..*. Drives whether a missing reference leaves a component UNSATISFIED."
		   });
		addAnnotation
		  (referencePolicyEEnum,
		   source,
		   new String[] {
			   "documentation", "Reference rebinding policy. STATIC = component is deactivated and reactivated when a target service changes; DYNAMIC = component stays active and is notified via bind/unbind callbacks."
		   });
		addAnnotation
		  (referencePolicyOptionEEnum,
		   source,
		   new String[] {
			   "documentation", "Reference rebinding eagerness. RELUCTANT = keep current binding even if a higher-ranked target appears; GREEDY = switch to a higher-ranked target."
		   });
		addAnnotation
		  (configurationPolicyEEnum,
		   source,
		   new String[] {
			   "documentation", "Whether a component depends on Configuration Admin. OPTIONAL = activate without configuration if none is present; REQUIRE = activate only when configuration is present; IGNORE = never consult configuration."
		   });
		addAnnotation
		  (componentStateEEnum,
		   source,
		   new String[] {
			   "documentation", "Runtime state of a ComponentConfiguration. Values follow ComponentConfigurationDTO and are bitwise-combinable so the same int can express composite states in transit. UNSATISFIED_CONFIGURATION = required configuration missing; UNSATISFIED_REFERENCE = required service missing; SATISFIED = all dependencies present; ACTIVE = activate() succeeded; FAILED_ACTIVATION = activate() threw \u2014 failure detail in ComponentConfiguration.failure."
		   });
		addAnnotation
		  (serviceEventTypeEEnum,
		   source,
		   new String[] {
			   "documentation", "Lifecycle event types. Values follow org.osgi.framework.ServiceEvent and are bitwise-combinable so listener filters can subscribe to multiple types at once. UNSPECIFIED = no type set; exists only so that none of the meaningful literals is the EMF default, because EMF omits an attribute whose value equals the default and a required type would then be missing on the wire (see OPEN_ISSUES W4). REGISTERED = service appeared; MODIFIED = properties changed, still matches subscriber filter; UNREGISTERING = service is being removed; MODIFIED_ENDMATCH = properties changed, no longer matches subscriber filter."
		   });
		addAnnotation
		  (fieldOptionEEnum,
		   source,
		   new String[] {
			   "documentation", "Field-injection strategy for a dynamic ComponentReference. REPLACE = the entire collection field is replaced on each update; UPDATE = the collection is mutated in place."
		   });
		addAnnotation
		  (collectionTypeEEnum,
		   source,
		   new String[] {
			   "documentation", "Element type expected by a reference field/collection. SERVICE = injected service objects; REFERENCE = ServiceReferences; SERVICEOBJECTS = ComponentServiceObjects-like accessors; PROPERTIES = property maps; TUPLE = (props, service) tuples."
		   });
		addAnnotation
		  (lifecycleHookKindEEnum,
		   source,
		   new String[] {
			   "documentation", "Kind of lifecycle callback a component declares. Language-neutral renaming of OSGi DS activate/deactivate/modified/activation-fields/init."
		   });
		addAnnotation
		  (referenceBindingKindEEnum,
		   source,
		   new String[] {
			   "documentation", "Kind of binding callback for a ComponentReference. BIND = called when a target is bound; UNBIND = called when a target is unbound; UPDATED = called when target properties change; FIELD = the reference targets a field, not a method."
		   });
		addAnnotation
		  (diagnosticSeverityEEnum,
		   source,
		   new String[] {
			   "documentation", "Severity of a Diagnostic. Values are bitwise-combinable so aggregate diagnostics carry the maximum severity of their children. The numeric values follow the established Diagnostic-severity convention used in the EMF runtime (OK=0, INFO=1, WARNING=2, ERROR=4, CANCEL=8) so existing tooling on the Java side can map without ceremony."
		   });
		addAnnotation
		  (flavorKindEEnum,
		   source,
		   new String[] {
			   "documentation", "Identifier of a transport flavor. Reference values REST and MQTT ship with the prototype. New flavors are added by extending ServiceFlavor and adding a literal here. Consumers attach FlavorKind values to their ConsumerCapability so the registry can filter implementations by transport compatibility."
		   });
		addAnnotation
		  (httpMethodEEnum,
		   source,
		   new String[] {
			   "documentation", "HTTP request method used by a RestOperationFlavor."
		   });
		addAnnotation
		  (mqttQosEEnum,
		   source,
		   new String[] {
			   "documentation", "MQTT quality-of-service level. AT_MOST_ONCE (0, fire-and-forget); AT_LEAST_ONCE (1, ack-with-redelivery \u2014 DDSR default); EXACTLY_ONCE (2, four-way handshake)."
		   });
		addAnnotation
		  (registryKindEEnum,
		   source,
		   new String[] {
			   "documentation", "Discriminator on ServiceRegistry. LOCAL = in-process registry (LocalServiceRegistry); REMOTE = central broker (RemoteServiceRegistry). Duplicates the subclass relation but is handy for filtering heterogeneous registry lists."
		   });
		addAnnotation
		  (expressionLanguageEEnum,
		   source,
		   new String[] {
			   "documentation", "Identifier of the expression language used in an ExpressionConstraint or Invariant. Only OCL ships with the prototype; the discriminator is here so the model can later carry CEL / JSON-Logic / a homegrown sublanguage without a structural migration. Cross-language portability of expressions is a separate concern \u2014 see REQUIREMENTS NFR-Behavioral-Parity."
		   });
		addAnnotation
		  (catalogStatusEEnum,
		   source,
		   new String[] {
			   "documentation", "Lifecycle marker on a ServiceInterface in the API catalog. ACTIVE = freely publishable and lookupable. DEPRECATED = still usable (existing implementations stay live, lookups still resolve), but new publishImplementation calls return a WARNING diagnostic and the catalog UI flags the entry for migration. There is no further status \u2014 removeCatalogEntry rejects while implementations exist."
		   });
		addAnnotation
		  (connectionStateEEnum,
		   source,
		   new String[] {
			   "documentation", "State of a LocalServiceRegistry\'s link to its Remote Registry. CONNECTED = snapshot received and event stream alive; DEGRADED = link lost, reconnect in progress, cached state still served to read-only consumers; OFFLINE = never connected, or reconnect has permanently failed. While DEGRADED or OFFLINE, write operations (publish / withdraw / catalog mutations) MUST be rejected with a Diagnostic; reads continue against the last known snapshot."
		   });
		addAnnotation
		  (namedElementEClass,
		   source,
		   new String[] {
			   "documentation", "Mixin interface for anything carrying an identifying name. Names identify within their containment scope, not globally \u2014 multiple objects with the same name in the same XMI document are legal (e.g. a ServiceOperation \'listCatalog\' on the interface and a RestOperationFlavor \'listCatalog\' on a binding). Cross-references resolve via positional URI fragments, not by name."
		   });
		addAnnotation
		  (getNamedElement_Name(),
		   source,
		   new String[] {
			   "documentation", "Identifying name within the containment scope. Convention: symbolic, language-neutral (e.g. \'PaymentService\', \'com.example.payment.PaymentService\'), not a display label. Not an XMI iD \u2014 see the class docstring."
		   });
		addAnnotation
		  (versionedElementEClass,
		   source,
		   new String[] {
			   "documentation", "Mixin for elements that carry a semver version (ServiceInterface, ServiceProvider, ServiceImplementation, ServiceException). Together with NamedElement, (name, version) forms a stable identity across catalog releases."
		   });
		addAnnotation
		  (getVersionedElement_Version(),
		   source,
		   new String[] {
			   "documentation", "Semver string, e.g. \'1.4.2\' or \'2.0.0-rc1\'. Validated against the major.minor.micro[-qualifier] grammar via the validSemver invariant. Code Publisher derives artifact semver from this field (see REQUIREMENTS FR-CodeDist-Versioning)."
		   });
		addAnnotation
		  (propertyEClass,
		   source,
		   new String[] {
			   "documentation", "Abstract base for typed service properties. Replaces OSGi\'s Map<String, Object> properties with a containment hierarchy that survives wire serialization across languages. Each concrete subclass adds a typed \'value\' attribute."
		   });
		addAnnotation
		  (stringPropertyEClass,
		   source,
		   new String[] {
			   "documentation", "Property carrying a single string value."
		   });
		addAnnotation
		  (intPropertyEClass,
		   source,
		   new String[] {
			   "documentation", "Property carrying a 32-bit integer value."
		   });
		addAnnotation
		  (longPropertyEClass,
		   source,
		   new String[] {
			   "documentation", "Property carrying a 64-bit integer value."
		   });
		addAnnotation
		  (doublePropertyEClass,
		   source,
		   new String[] {
			   "documentation", "Property carrying a 64-bit IEEE-754 floating-point value."
		   });
		addAnnotation
		  (floatPropertyEClass,
		   source,
		   new String[] {
			   "documentation", "Property carrying a 32-bit IEEE-754 floating-point value."
		   });
		addAnnotation
		  (shortPropertyEClass,
		   source,
		   new String[] {
			   "documentation", "Property carrying a 16-bit integer value."
		   });
		addAnnotation
		  (boolPropertyEClass,
		   source,
		   new String[] {
			   "documentation", "Property carrying a boolean value."
		   });
		addAnnotation
		  (stringListPropertyEClass,
		   source,
		   new String[] {
			   "documentation", "Property carrying an ordered list of strings. Used where OSGi would use String[] (e.g. configuration PIDs, accepted content types). Empty list = no values; null = no value at all."
		   });
		addAnnotation
		  (serviceOperationEClass,
		   source,
		   new String[] {
			   "documentation", "A single language-neutral operation on a ServiceInterface: parameters in order, optional return type with constraints, declared exceptions. The Code Publisher uses ServiceOperations to emit typed method signatures in Java/TS/Python stub artifacts."
		   });
		addAnnotation
		  (getServiceOperation_Description(),
		   source,
		   new String[] {
			   "documentation", "Human-readable description; rendered as JavaDoc / TSDoc / Python docstring on the generated stub."
		   });
		addAnnotation
		  (getServiceOperation_Parameters(),
		   source,
		   new String[] {
			   "documentation", "Ordered list of input parameters. Order is the visible order in the generated stub signature and is determined by Parameter.index."
		   });
		addAnnotation
		  (getServiceOperation_ReturnType(),
		   source,
		   new String[] {
			   "documentation", "Language-neutral name of the return type (e.g. \'string\', \'int\', \'money.Money\'). Unset = void/None/no return value."
		   });
		addAnnotation
		  (getServiceOperation_ReturnConstraints(),
		   source,
		   new String[] {
			   "documentation", "Constraints on the return value (same constraint language as parameters). Empty = no constraints."
		   });
		addAnnotation
		  (getServiceOperation_Exceptions(),
		   source,
		   new String[] {
			   "documentation", "ServiceExceptions this operation may raise. Non-containment: the actual ServiceException objects are owned by the enclosing ServiceInterface."
		   });
		addAnnotation
		  (getServiceOperation_Preconditions(),
		   source,
		   new String[] {
			   "documentation", "Conditions that MUST hold when the operation is called. Evaluation context: \'self\' = receiver, \'params\' = map of incoming parameter values by name, \'op\' = this ServiceOperation. Caller-side violations cause the framework to reject the call without invoking the implementation."
		   });
		addAnnotation
		  (getServiceOperation_Postconditions(),
		   source,
		   new String[] {
			   "documentation", "Conditions that MUST hold after the operation returns successfully. Evaluation context: as preconditions, plus \'result\' = the return value (null if the operation has no return). Violations are framework bugs / implementation-contract breaches and are surfaced as ERROR diagnostics."
		   });
		addAnnotation
		  (parameterEClass,
		   source,
		   new String[] {
			   "documentation", "A typed, named, positional input to a ServiceOperation. Constraints attach as containments. The parameter name appears as the argument name in generated stubs (subject to language-specific keyword escaping)."
		   });
		addAnnotation
		  (getParameter_Index(),
		   source,
		   new String[] {
			   "documentation", "Zero-based position in the operation signature. Independent of the name so that renaming a parameter does not reorder others."
		   });
		addAnnotation
		  (getParameter_Type(),
		   source,
		   new String[] {
			   "documentation", "Language-neutral type name. Code Publisher maps to the idiomatic language type (e.g. \'string\' \u2192 Java String / TS string / Python str). Custom types reference other ServiceInterfaces by qualified name."
		   });
		addAnnotation
		  (getParameter_Optional(),
		   source,
		   new String[] {
			   "documentation", "If true, the caller may omit this parameter. Generated stubs render optional parameters with their language-idiomatic mechanism (Java overloads/Optional, TS \'?:\', Python default values)."
		   });
		addAnnotation
		  (getParameter_DefaultValue(),
		   source,
		   new String[] {
			   "documentation", "String-encoded default value (parsed per type by the implementation). Only meaningful when optional = true."
		   });
		addAnnotation
		  (getParameter_Description(),
		   source,
		   new String[] {
			   "documentation", "Doc text; rendered as @param JavaDoc / TSDoc tag / Python docstring entry."
		   });
		addAnnotation
		  (getParameter_Constraints(),
		   source,
		   new String[] {
			   "documentation", "Validity constraints. Multiple constraints AND together \u2014 a value must satisfy all of them."
		   });
		addAnnotation
		  (parameterConstraintEClass,
		   source,
		   new String[] {
			   "documentation", "Abstract base for validity constraints on Parameter values or ServiceOperation return values. Constraints are positional under their parameter (not separately named), so they do NOT mix in NamedElement."
		   });
		addAnnotation
		  (requiredConstraintEClass,
		   source,
		   new String[] {
			   "documentation", "Marker constraint: parameter MUST NOT be null/None/undefined. Redundant with Parameter.optional = false but explicit, useful for return-value constraints where Parameter.optional does not apply."
		   });
		addAnnotation
		  (numericRangeConstraintEClass,
		   source,
		   new String[] {
			   "documentation", "Numeric range. EDouble is used for all numeric types \u2014 the implementation casts back into the parameter\'s actual numeric type for comparison. Unset min = no lower bound; unset max = no upper bound. At least one of min/max should be set (OCL invariant TODO)."
		   });
		addAnnotation
		  (getNumericRangeConstraint_Min(),
		   source,
		   new String[] {
			   "documentation", "Lower bound. Unset = -\u221e."
		   });
		addAnnotation
		  (getNumericRangeConstraint_Max(),
		   source,
		   new String[] {
			   "documentation", "Upper bound. Unset = +\u221e."
		   });
		addAnnotation
		  (getNumericRangeConstraint_InclusiveMin(),
		   source,
		   new String[] {
			   "documentation", "true = value >= min allowed (default); false = strict greater-than."
		   });
		addAnnotation
		  (getNumericRangeConstraint_InclusiveMax(),
		   source,
		   new String[] {
			   "documentation", "true = value <= max allowed (default); false = strict less-than."
		   });
		addAnnotation
		  (stringPatternConstraintEClass,
		   source,
		   new String[] {
			   "documentation", "String-valued parameter constraint. Pattern uses ECMA-262 regex syntax (natively supported in TS, accepted by Java java.util.regex and Python re with the same minimal subset). Length bounds apply to the unicode code-point length."
		   });
		addAnnotation
		  (getStringPatternConstraint_Pattern(),
		   source,
		   new String[] {
			   "documentation", "ECMA-262 regex. Anchors (^, $) are NOT implicit \u2014 to match the entire string, write them explicitly."
		   });
		addAnnotation
		  (getStringPatternConstraint_MinLength(),
		   source,
		   new String[] {
			   "documentation", "Minimum string length, inclusive. Unset = 0."
		   });
		addAnnotation
		  (getStringPatternConstraint_MaxLength(),
		   source,
		   new String[] {
			   "documentation", "Maximum string length, inclusive. Unset = unbounded."
		   });
		addAnnotation
		  (enumerationConstraintEClass,
		   source,
		   new String[] {
			   "documentation", "Closed list of permitted stringified values. The implementation compares the parameter\'s stringified form against allowedValues with case-sensitive equality."
		   });
		addAnnotation
		  (getEnumerationConstraint_AllowedValues(),
		   source,
		   new String[] {
			   "documentation", "Allowed stringified values. At least one entry required."
		   });
		addAnnotation
		  (expressionConstraintEClass,
		   source,
		   new String[] {
			   "documentation", "Open-ended constraint expressed as an expression in a given language (OCL in the prototype). Use when the typed constraints (Required, NumericRange, StringPattern, Enumeration, CollectionSize) do not fit \u2014 e.g. cross-parameter checks (\'amount <= account.balance\'), conditional rules (\'if currency = EUR then amount <= 1000\'), or domain logic. Has identity (NamedElement) so engines can report violations by name. Evaluation context: \'self\' is the runtime value of the parameter/return value; \'op\' is the enclosing ServiceOperation; \'params\' is a map from parameter name to runtime value (only available where multiple parameters are in scope \u2014 i.e. preconditions/postconditions)."
		   });
		addAnnotation
		  (getExpressionConstraint_Language(),
		   source,
		   new String[] {
			   "documentation", "Expression language. Only OCL ships with the prototype, but the discriminator is in place so the model can later carry alternative languages without structural migration."
		   });
		addAnnotation
		  (getExpressionConstraint_Expression(),
		   source,
		   new String[] {
			   "documentation", "The expression text. For OCL: a boolean expression returning true if the constraint is satisfied. Multi-line allowed."
		   });
		addAnnotation
		  (getExpressionConstraint_Message(),
		   source,
		   new String[] {
			   "documentation", "Optional human-readable explanation shown when the constraint is violated. May reference parameter values via implementation-defined placeholders (e.g. \'{amount}\' \u2014 exact placeholder syntax is an implementation concern)."
		   });
		addAnnotation
		  (invariantEClass,
		   source,
		   new String[] {
			   "documentation", "Operation-level pre/postcondition or interface-level always-true expression. Same expression shape as ExpressionConstraint but NOT a ParameterConstraint \u2014 invariants are not attached to a single parameter or return value, they describe a contract on the enclosing operation or interface. Evaluation context depends on host: in Operation.preconditions / postconditions, \'self\' is the receiver service object and \'params\'/\'result\' are the call\'s parameters/return value; in ServiceInterface.invariants, \'self\' is any instance of the interface."
		   });
		addAnnotation
		  (getInvariant_Language(),
		   source,
		   new String[] {
			   "documentation", "Expression language. See ExpressionConstraint.language."
		   });
		addAnnotation
		  (getInvariant_Expression(),
		   source,
		   new String[] {
			   "documentation", "The expression text. Boolean. Multi-line allowed."
		   });
		addAnnotation
		  (getInvariant_Message(),
		   source,
		   new String[] {
			   "documentation", "Optional human-readable explanation shown when the invariant is violated."
		   });
		addAnnotation
		  (collectionSizeConstraintEClass,
		   source,
		   new String[] {
			   "documentation", "Bounds on collection-typed parameters (lists, arrays, sets). Implementation rejects values whose count is outside [minSize, maxSize]."
		   });
		addAnnotation
		  (getCollectionSizeConstraint_MinSize(),
		   source,
		   new String[] {
			   "documentation", "Minimum element count, inclusive. Unset = 0."
		   });
		addAnnotation
		  (getCollectionSizeConstraint_MaxSize(),
		   source,
		   new String[] {
			   "documentation", "Maximum element count, inclusive. Unset = unbounded."
		   });
		addAnnotation
		  (serviceExceptionEClass,
		   source,
		   new String[] {
			   "documentation", "A declared error a ServiceOperation can raise. Defined once per ServiceInterface and referenced by the operations that may raise it. Carries typed properties for the exception payload, so consumers can extract structured data instead of parsing a message."
		   });
		addAnnotation
		  (getServiceException_Description(),
		   source,
		   new String[] {
			   "documentation", "Doc text; rendered as @throws JavaDoc / TSDoc tag / Python docstring entry."
		   });
		addAnnotation
		  (getServiceException_Type(),
		   source,
		   new String[] {
			   "documentation", "Symbolic exception type (FQN-style), e.g. \'com.example.payment.InsufficientFundsException\'. Distinct from name: \'name\' is the catalog identifier, \'type\' is what consumers/providers see on the wire."
		   });
		addAnnotation
		  (getServiceException_Properties(),
		   source,
		   new String[] {
			   "documentation", "Typed payload fields (e.g. errorCode: int, retryable: bool, accountId: string). Empty = no structured payload, only a message."
		   });
		addAnnotation
		  (serviceInterfaceEClass,
		   source,
		   new String[] {
			   "documentation", "The core catalog citizen: a versioned, identified service contract carrying operations and exceptions. Lives containment in RemoteServiceRegistry.catalog. Providers reference it (non-containment) from their ServiceImplementations; consumers look up services by its name."
		   });
		addAnnotation
		  (getServiceInterface_Description(),
		   source,
		   new String[] {
			   "documentation", "Doc text; rendered as the type-level doc on the generated stub."
		   });
		addAnnotation
		  (getServiceInterface_Operations(),
		   source,
		   new String[] {
			   "documentation", "Operations the interface exposes. Empty = marker interface (rare; useful for tagging)."
		   });
		addAnnotation
		  (getServiceInterface_Exceptions(),
		   source,
		   new String[] {
			   "documentation", "ServiceExceptions declared at interface level. Individual ServiceOperations reference (non-containment) the ones they may throw."
		   });
		addAnnotation
		  (getServiceInterface_Invariants(),
		   source,
		   new String[] {
			   "documentation", "Conditions that MUST hold for every instance of this interface, before and after every operation. Evaluation context: \'self\' = the receiver service object. Used for whole-interface contracts (e.g. \'self.balance >= 0 implies self.status = ACTIVE\')."
		   });
		addAnnotation
		  (getServiceInterface_Status(),
		   source,
		   new String[] {
			   "documentation", "Catalog lifecycle status. Set to DEPRECATED by RemoteServiceRegistry.deprecateCatalogEntry; cannot transition back to ACTIVE (deprecation is one-way \u2014 a re-introduction is a new ServiceInterface with a new name/version)."
		   });
		addAnnotation
		  (getServiceInterface_DeprecationReason(),
		   source,
		   new String[] {
			   "documentation", "Free-form explanation set when status transitions to DEPRECATED. Surfaced in the WARNING Diagnostic that publishImplementation returns against a deprecated interface and in catalog browsers."
		   });
		addAnnotation
		  (getServiceInterface_ReplacedBy(),
		   source,
		   new String[] {
			   "documentation", "Optional migration hint: another ServiceInterface that supersedes this one. Non-containment. Lets tooling chain deprecated \u2192 successor \u2192 successor for multi-step migration trails."
		   });
		addAnnotation
		  (lifecycleHookEClass,
		   source,
		   new String[] {
			   "documentation", "Language-neutral handle on a lifecycle callback. Replaces OSGi DS activate/deactivate/modified/activationFields/init annotations. The framework invokes the implementation method whose name matches \'name\' at the appropriate lifecycle point."
		   });
		addAnnotation
		  (getLifecycleHook_Kind(),
		   source,
		   new String[] {
			   "documentation", "Which lifecycle event triggers this hook."
		   });
		addAnnotation
		  (getLifecycleHook_Parameter(),
		   source,
		   new String[] {
			   "documentation", "Zero-based constructor parameter index, only meaningful when kind is part of constructor injection (init in DS 1.4)."
		   });
		addAnnotation
		  (referenceBindingEClass,
		   source,
		   new String[] {
			   "documentation", "Language-neutral handle on a reference callback or field-injection point. Replaces OSGi DS bind/unbind/updated/field annotations. The framework invokes the implementation method (or sets the field) whose name matches \'name\' when the corresponding ComponentReference changes."
		   });
		addAnnotation
		  (getReferenceBinding_Kind(),
		   source,
		   new String[] {
			   "documentation", "Which reference event triggers this binding."
		   });
		addAnnotation
		  (getReferenceBinding_FieldOption(),
		   source,
		   new String[] {
			   "documentation", "Only meaningful when kind = FIELD: how a collection field is mutated when the reference set changes."
		   });
		addAnnotation
		  (componentReferenceEClass,
		   source,
		   new String[] {
			   "documentation", "A declared dependency of a component on another service. Models org.osgi.service.component.runtime.dto.ReferenceDTO. Drives whether a ComponentConfiguration becomes SATISFIED."
		   });
		addAnnotation
		  (getComponentReference_InterfaceName(),
		   source,
		   new String[] {
			   "documentation", "Name of the required ServiceInterface (matches ServiceInterface.name)."
		   });
		addAnnotation
		  (getComponentReference_Cardinality(),
		   source,
		   new String[] {
			   "documentation", "How many targets are needed/allowed."
		   });
		addAnnotation
		  (getComponentReference_Policy(),
		   source,
		   new String[] {
			   "documentation", "Rebinding strategy when targets change."
		   });
		addAnnotation
		  (getComponentReference_PolicyOption(),
		   source,
		   new String[] {
			   "documentation", "Whether to switch to a higher-ranked target when one appears."
		   });
		addAnnotation
		  (getComponentReference_Target(),
		   source,
		   new String[] {
			   "documentation", "LDAP filter narrowing the set of acceptable targets, e.g. \'(currency=EUR)\'. Empty/unset = any target of the named interface."
		   });
		addAnnotation
		  (getComponentReference_Scope(),
		   source,
		   new String[] {
			   "documentation", "Scope at which the target is obtained from a ServiceFactory/PrototypeServiceFactory."
		   });
		addAnnotation
		  (getComponentReference_CollectionType(),
		   source,
		   new String[] {
			   "documentation", "Only meaningful for multi-cardinality references with field-injection: element type expected by the collection field."
		   });
		addAnnotation
		  (getComponentReference_Parameter(),
		   source,
		   new String[] {
			   "documentation", "DS 1.4: zero-based constructor parameter index when the reference is injected via the constructor."
		   });
		addAnnotation
		  (getComponentReference_Bindings(),
		   source,
		   new String[] {
			   "documentation", "Callback / field hooks for this reference. Empty = the component is happy to be activated without explicit binding callbacks."
		   });
		addAnnotation
		  (componentDescriptionEClass,
		   source,
		   new String[] {
			   "documentation", "Declarative blueprint of a component, before it has been instantiated as a ComponentConfiguration. Models org.osgi.service.component.runtime.dto.ComponentDescriptionDTO. Owns the references the component needs and the services it can publish."
		   });
		addAnnotation
		  (getComponentDescription_Factory(),
		   source,
		   new String[] {
			   "documentation", "Factory name if this is a DS factory component; null otherwise."
		   });
		addAnnotation
		  (getComponentDescription_Scope(),
		   source,
		   new String[] {
			   "documentation", "Service scope at which instances of this component are handed to consumers."
		   });
		addAnnotation
		  (getComponentDescription_ImplementationId(),
		   source,
		   new String[] {
			   "documentation", "Language-neutral symbolic id of the implementation class/module/file. Replaces OSGi \'implementationClass: String\' to avoid Java FQN assumptions."
		   });
		addAnnotation
		  (getComponentDescription_DefaultEnabled(),
		   source,
		   new String[] {
			   "documentation", "Whether the component is enabled at bundle start. Disable via the (future) ServiceComponentRuntime.disableComponent."
		   });
		addAnnotation
		  (getComponentDescription_Immediate(),
		   source,
		   new String[] {
			   "documentation", "If true, activate as soon as satisfied; if false, only activate when a consumer first requests the service."
		   });
		addAnnotation
		  (getComponentDescription_ConfigurationPolicy(),
		   source,
		   new String[] {
			   "documentation", "Behavior when Configuration Admin has no matching configuration."
		   });
		addAnnotation
		  (getComponentDescription_ConfigurationPid(),
		   source,
		   new String[] {
			   "documentation", "PIDs this component consumes from Configuration Admin. Empty = component name is the default PID."
		   });
		addAnnotation
		  (getComponentDescription_ServiceInterfaces(),
		   source,
		   new String[] {
			   "documentation", "ServiceInterfaces this component publishes. Non-containment: shared with the catalog and other ComponentDescriptions / ServiceImplementations."
		   });
		addAnnotation
		  (getComponentDescription_Properties(),
		   source,
		   new String[] {
			   "documentation", "Declared component/service properties (service.ranking, custom properties, target attributes for references)."
		   });
		addAnnotation
		  (getComponentDescription_FactoryProperties(),
		   source,
		   new String[] {
			   "documentation", "Factory-specific properties (DS 1.4). Only meaningful if factory != null."
		   });
		addAnnotation
		  (getComponentDescription_References(),
		   source,
		   new String[] {
			   "documentation", "Other services this component needs."
		   });
		addAnnotation
		  (getComponentDescription_LifecycleHooks(),
		   source,
		   new String[] {
			   "documentation", "Activate/deactivate/modified callbacks plus DS 1.4 activation fields. Replaces the flat OSGi DS attributes (activate, deactivate, modified, activationFields, init)."
		   });
		addAnnotation
		  (getComponentDescription_Provider(),
		   source,
		   new String[] {
			   "documentation", "The provider that owns this description. Non-containment: the containing relation runs the other way via ServiceProvider.descriptions."
		   });
		addAnnotation
		  (serviceProviderEClass,
		   source,
		   new String[] {
			   "documentation", "Language-neutral equivalent of an OSGi Bundle: a deployment unit that owns component descriptions and registers services. Identifies who has registered what in the registry; used by PDP for authorization (subject = provider)."
		   });
		addAnnotation
		  (getServiceProvider_SymbolicName(),
		   source,
		   new String[] {
			   "documentation", "OSGi-style Bundle-SymbolicName, e.g. \'com.example.payments.rest\'. Used as the wire-identifier for the provider."
		   });
		addAnnotation
		  (getServiceProvider_Descriptions(),
		   source,
		   new String[] {
			   "documentation", "DS component descriptions owned by this provider. Containment: descriptions live and die with the provider."
		   });
		addAnnotation
		  (getServiceProvider_Implementations(),
		   source,
		   new String[] {
			   "documentation", "Concrete service implementations owned by this provider. Containment: ownership and authority for publish/withdraw flow from the provider that contains the implementation."
		   });
		addAnnotation
		  (serviceImplementationEClass,
		   source,
		   new String[] {
			   "documentation", "A concrete realization of one or more ServiceInterfaces by a single provider, optionally reachable over one or more transport flavors. Distinct from ComponentDescription because not all implementations are DS-driven (plain Java services, hand-wired TS modules, ad-hoc Python objects). When DS-driven, componentDescription points back to the description."
		   });
		addAnnotation
		  (getServiceImplementation_Description(),
		   source,
		   new String[] {
			   "documentation", "Doc text."
		   });
		addAnnotation
		  (getServiceImplementation_ImplementationId(),
		   source,
		   new String[] {
			   "documentation", "Language-neutral symbolic id of the backing class/module/file."
		   });
		addAnnotation
		  (getServiceImplementation_ServiceInterfaces(),
		   source,
		   new String[] {
			   "documentation", "ServiceInterfaces this implementation satisfies. At least one. Non-containment: interfaces are owned by the catalog."
		   });
		addAnnotation
		  (getServiceImplementation_Flavors(),
		   source,
		   new String[] {
			   "documentation", "Transport flavors over which this implementation is reachable. Empty = local-only (no remote consumers). When publishing to RemoteServiceRegistry, MUST be non-empty (OCL invariant TODO)."
		   });
		addAnnotation
		  (getServiceImplementation_Properties(),
		   source,
		   new String[] {
			   "documentation", "Implementation-specific properties (service.ranking, region, tenant, \u2026). Visible to consumers via lookup filters."
		   });
		addAnnotation
		  (getServiceImplementation_ComponentDescription(),
		   source,
		   new String[] {
			   "documentation", "Back-link to the DS ComponentDescription, if this implementation is DS-driven. Non-containment; can be null for plain (non-DS) implementations."
		   });
		addAnnotation
		  (serviceFlavorEClass,
		   source,
		   new String[] {
			   "documentation", "Abstract base for a transport binding. Concrete subclasses (RestFlavor, MqttFlavor, \u2026) add transport-specific defaults. Operation-level bindings live in operationFlavors. Extended by third-party flavor plugins."
		   });
		addAnnotation
		  (getServiceFlavor_Kind(),
		   source,
		   new String[] {
			   "documentation", "Discriminator that matches ConsumerCapability.supportedFlavors. Redundant with the concrete subclass but useful when filtering heterogeneous flavor lists at the registry."
		   });
		addAnnotation
		  (getServiceFlavor_OperationFlavors(),
		   source,
		   new String[] {
			   "documentation", "Per-operation transport bindings (HTTP method+path for REST, request/response topic for MQTT, \u2026). Empty = the flavor\'s interface-level defaults apply to every operation."
		   });
		addAnnotation
		  (restFlavorEClass,
		   source,
		   new String[] {
			   "documentation", "HTTP/REST transport. Operations map to (HTTP method, base path + operation path) tuples; bodies are encoded with the listed content types."
		   });
		addAnnotation
		  (getRestFlavor_Host(),
		   source,
		   new String[] {
			   "documentation", "Optional host (scheme://host[:port]). When null, consumers receive the host from the ServiceReference at lookup time (allows the same model to describe many deployment endpoints)."
		   });
		addAnnotation
		  (getRestFlavor_BasePath(),
		   source,
		   new String[] {
			   "documentation", "Path prefix shared by all operations of this flavor, e.g. \'/api/v1/payment\'. Operations append their own path on top."
		   });
		addAnnotation
		  (getRestFlavor_ContentTypes(),
		   source,
		   new String[] {
			   "documentation", "Default Accept / Content-Type values that operations inherit. Empty = \'application/json\' assumed."
		   });
		addAnnotation
		  (mqttFlavorEClass,
		   source,
		   new String[] {
			   "documentation", "MQTT pub/sub transport. Operations are addressed via a request topic; responses go either to a response topic, a per-operation override topic, or via correlationId-on-shared-topic depending on configuration."
		   });
		addAnnotation
		  (getMqttFlavor_Brokers(),
		   source,
		   new String[] {
			   "documentation", "One or more broker URLs (mqtt://\u2026 or mqtts://\u2026). Multiple brokers indicate a high-availability cluster; the client picks one and fails over."
		   });
		addAnnotation
		  (getMqttFlavor_RequestTopic(),
		   source,
		   new String[] {
			   "documentation", "Default topic for incoming requests. Operations can override via MqttOperationFlavor.requestTopic."
		   });
		addAnnotation
		  (getMqttFlavor_ResponseTopic(),
		   source,
		   new String[] {
			   "documentation", "Default topic for responses. Null = use MQTT v5 response-topic property or correlate via correlationId on the request topic."
		   });
		addAnnotation
		  (getMqttFlavor_DefaultQos(),
		   source,
		   new String[] {
			   "documentation", "Default QoS for operations. Per-operation override possible via MqttOperationFlavor.qos."
		   });
		addAnnotation
		  (getMqttFlavor_DefaultRetained(),
		   source,
		   new String[] {
			   "documentation", "Default retained flag on published messages. Per-operation override possible."
		   });
		addAnnotation
		  (serviceOperationFlavorEClass,
		   source,
		   new String[] {
			   "documentation", "Abstract transport-binding refinement for a single operation. Concrete subclasses (RestOperationFlavor, MqttOperationFlavor) add transport-specific fields."
		   });
		addAnnotation
		  (getServiceOperationFlavor_Operation(),
		   source,
		   new String[] {
			   "documentation", "The ServiceOperation this flavor binding applies to. Non-containment: the operation is owned by its ServiceInterface."
		   });
		addAnnotation
		  (getServiceOperationFlavor_Consumes(),
		   source,
		   new String[] {
			   "documentation", "Content-Types of accepted request bodies. Overrides the flavor-level default for this operation."
		   });
		addAnnotation
		  (getServiceOperationFlavor_Produces(),
		   source,
		   new String[] {
			   "documentation", "Content-Types of produced response bodies. Overrides the flavor-level default for this operation."
		   });
		addAnnotation
		  (restOperationFlavorEClass,
		   source,
		   new String[] {
			   "documentation", "REST binding for one operation: HTTP method + path under the RestFlavor.basePath + expected success status codes."
		   });
		addAnnotation
		  (getRestOperationFlavor_Method(),
		   source,
		   new String[] {
			   "documentation", "HTTP method (GET, POST, \u2026)."
		   });
		addAnnotation
		  (getRestOperationFlavor_Path(),
		   source,
		   new String[] {
			   "documentation", "Path appended to RestFlavor.basePath. Null = operation lives directly at basePath. Path templates (\'/payments/{id}\') are allowed; the framework substitutes path parameters by name from the operation\'s Parameters."
		   });
		addAnnotation
		  (getRestOperationFlavor_ReturnCodes(),
		   source,
		   new String[] {
			   "documentation", "HTTP status codes that count as a successful response (typically [200] or [200, 204]). Any other status is mapped to one of the operation\'s ServiceExceptions."
		   });
		addAnnotation
		  (mqttOperationFlavorEClass,
		   source,
		   new String[] {
			   "documentation", "MQTT binding for one operation. Topic/QoS/retained values override the MqttFlavor defaults when set."
		   });
		addAnnotation
		  (getMqttOperationFlavor_RequestTopic(),
		   source,
		   new String[] {
			   "documentation", "Overrides MqttFlavor.requestTopic for this operation. Null = use the flavor default."
		   });
		addAnnotation
		  (getMqttOperationFlavor_ResponseTopic(),
		   source,
		   new String[] {
			   "documentation", "Overrides MqttFlavor.responseTopic for this operation."
		   });
		addAnnotation
		  (getMqttOperationFlavor_Qos(),
		   source,
		   new String[] {
			   "documentation", "Overrides MqttFlavor.defaultQos for this operation."
		   });
		addAnnotation
		  (getMqttOperationFlavor_Retained(),
		   source,
		   new String[] {
			   "documentation", "Overrides MqttFlavor.defaultRetained for this operation."
		   });
		addAnnotation
		  (getMqttOperationFlavor_Correlation(),
		   source,
		   new String[] {
			   "documentation", "If true, request and response are matched via MQTT v5 correlationId. If false, the framework assumes a one-way / fire-and-forget operation."
		   });
		addAnnotation
		  (getMqttOperationFlavor_ReturnPath(),
		   source,
		   new String[] {
			   "documentation", "Optional alternative convention for asynchronous responses: a topic pattern with placeholders the producer fills in when publishing the response."
		   });
		addAnnotation
		  (serviceReferenceEClass,
		   source,
		   new String[] {
			   "documentation", "Consumer-side handle to a registered service, equivalent to org.osgi.framework.ServiceReference. The id is the language-neutral counterpart of OSGi service.id (UUID rather than long). Properties are typed (Property containments) instead of an untyped map."
		   });
		addAnnotation
		  (getServiceReference__GetProperty__String(),
		   source,
		   new String[] {
			   "documentation", "Looks up a property value by key. Returns null when no property with that name exists. Return type is EJavaObject because the concrete value type depends on the Property subclass."
		   });
		addAnnotation
		  ((getServiceReference__GetProperty__String()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "Property name. Case-sensitive."
		   });
		addAnnotation
		  (getServiceReference__GetPropertyKeys(),
		   source,
		   new String[] {
			   "documentation", "Returns the names of all defined properties, in undefined order. Empty list if no properties are set."
		   });
		addAnnotation
		  (getServiceReference_Id(),
		   source,
		   new String[] {
			   "documentation", "UUID assigned by the registry at registration time. Replaces OSGi \'service.id: long\' to remain stable across processes."
		   });
		addAnnotation
		  (getServiceReference_Properties(),
		   source,
		   new String[] {
			   "documentation", "Service properties at the time of registration / last setProperties call. Includes both framework properties (service.id, service.ranking, \u2026) and provider-supplied properties."
		   });
		addAnnotation
		  (getServiceReference_Provider(),
		   source,
		   new String[] {
			   "documentation", "Provider that registered the service. Replaces OSGi \'bundle: long\'."
		   });
		addAnnotation
		  (getServiceReference_Registration(),
		   source,
		   new String[] {
			   "documentation", "Provider-side handle on the same service, paired via eOpposite. Null only during the brief window between unregister() and removal."
		   });
		addAnnotation
		  (serviceRegistrationEClass,
		   source,
		   new String[] {
			   "documentation", "Provider-side handle to a registered service, equivalent to org.osgi.framework.ServiceRegistration. The provider holds onto this to unregister later or to modify properties."
		   });
		addAnnotation
		  (getServiceRegistration__Unregister(),
		   source,
		   new String[] {
			   "documentation", "Removes the service from the registry. Fires UNREGISTERING BEFORE removal (OSGi semantics, consumers get one last chance to release the service). Then removes from registry and propagates withdrawal to the Remote Registry asynchronously."
		   });
		addAnnotation
		  (getServiceRegistration__SetProperties__EList(),
		   source,
		   new String[] {
			   "documentation", "Replaces the property set on the registration. Fires MODIFIED to listeners whose filter still matches, MODIFIED_ENDMATCH to listeners whose filter no longer matches."
		   });
		addAnnotation
		  ((getServiceRegistration__SetProperties__EList()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "The new property set. Empty list = clear all provider-supplied properties (framework properties remain)."
		   });
		addAnnotation
		  (getServiceRegistration_Reference(),
		   source,
		   new String[] {
			   "documentation", "Consumer-side view of the same service, paired via eOpposite. Both Registration and Reference are owned (containment) by the LocalServiceRegistry, not by each other."
		   });
		addAnnotation
		  (getServiceRegistration_Unregistered(),
		   source,
		   new String[] {
			   "documentation", "Becomes true after unregister() has run. Operations on an unregistered registration are no-ops (OCL TODO: forbid in references list of the registry once true)."
		   });
		addAnnotation
		  (getServiceRegistration_Provider(),
		   source,
		   new String[] {
			   "documentation", "The provider that materialised this registration by publishing the implementation (ACQUISITION.md par.8). Non-containment: the registration records the fact, it does not own the provider. Replaces the broker-internal implByRegistration side-map."
		   });
		addAnnotation
		  (getServiceRegistration_Implementation(),
		   source,
		   new String[] {
			   "documentation", "The published implementation this registration stands for. Non-containment \u2014 the implementation stays contained in its provider."
		   });
		addAnnotation
		  (getServiceRegistration_UsingSessions(),
		   source,
		   new String[] {
			   "documentation", "Derived view of the acquisition relation (ACQUISITION.md par.3): the sessions currently holding a lease on this registration. The OWNING side is ConsumerSession.acquisitions \u2014 the lease lifecycle follows the consumer. The usage count is a query (usingSessions size), never stored: stored counters drift on consumer crash. TRANSIENT by design: sessions are runtime state outside the persisted resource \u2014 a serialized link would tear every registry save/copy apart (not contained in a resource)."
		   });
		addAnnotation
		  (consumerSessionEClass,
		   source,
		   new String[] {
			   "documentation", "Consumer-side counterpart of ServiceRegistration (ACQUISITION.md): the session OWNS the acquisition leases, because their lifecycle follows the consumer \u2014 one heartbeat renews all of them, one shutdown or crash releases all of them. Maintained by an idempotent full-replace (PUT /consumers/{consumerId}); a lease expires after 2x the renewal interval. Acquisition is a cooperative protocol, not enforcement: invocation runs peer-to-peer past the broker."
		   });
		addAnnotation
		  (getConsumerSession_ConsumerId(),
		   source,
		   new String[] {
			   "documentation", "Identity of the consumer. Unauthenticated for now (S2) \u2014 once broker AuthN exists, the session binds to the authenticated identity and this id is checked against it."
		   });
		addAnnotation
		  (getConsumerSession_LastRenewal(),
		   source,
		   new String[] {
			   "documentation", "Broker-side timestamp of the last session PUT. The lease of every acquisition expires after 2x the renewal interval without one."
		   });
		addAnnotation
		  (getConsumerSession_Capabilities(),
		   source,
		   new String[] {
			   "documentation", "What this consumer can speak (flavors, greediness, ...). Owned by the session so lookups and drain decisions can consult it without a separate transport."
		   });
		addAnnotation
		  (getConsumerSession_Acquisitions(),
		   source,
		   new String[] {
			   "documentation", "The registrations this consumer claims to be using \u2014 the OWNING side of the acquisition relation. Points at the stable ServiceRegistration, not at the ServiceReference: references are the wire artefact and their ids regenerate on broker restart. Over-claiming is harmless (delays drain), under-claiming only hurts the consumer itself (loses drain protection). TRANSIENT: the wire form of a session carries sibling ServiceReference id-stubs instead (same convention as publish)."
		   });
		addAnnotation
		  (componentConfigurationEClass,
		   source,
		   new String[] {
			   "documentation", "Runtime instance of a ComponentDescription with resolved configuration and references. Models org.osgi.service.component.runtime.dto.ComponentConfigurationDTO. The id attribute carries OSGi component.id; it is intentionally NOT marked iD=true because NamedElement.name is already the XMI identifier."
		   });
		addAnnotation
		  (getComponentConfiguration_Id(),
		   source,
		   new String[] {
			   "documentation", "OSGi component.id, runtime-assigned. Unique within the LocalServiceRegistry but not necessarily across the federation."
		   });
		addAnnotation
		  (getComponentConfiguration_Description(),
		   source,
		   new String[] {
			   "documentation", "The ComponentDescription this configuration is an instance of. Non-containment."
		   });
		addAnnotation
		  (getComponentConfiguration_State(),
		   source,
		   new String[] {
			   "documentation", "Current lifecycle state. The Java/TS DDSR runtime MUST agree on the same transitions for the same observable events (NFR-Behavioral-Parity)."
		   });
		addAnnotation
		  (getComponentConfiguration_Properties(),
		   source,
		   new String[] {
			   "documentation", "Effective properties at this point in time (Description.properties merged with Configuration Admin overrides)."
		   });
		addAnnotation
		  (getComponentConfiguration_SatisfiedReferences(),
		   source,
		   new String[] {
			   "documentation", "ComponentReferences whose cardinality is currently satisfied."
		   });
		addAnnotation
		  (getComponentConfiguration_UnsatisfiedReferences(),
		   source,
		   new String[] {
			   "documentation", "ComponentReferences whose cardinality is not currently met (the reason the configuration is UNSATISFIED_REFERENCE, if applicable)."
		   });
		addAnnotation
		  (getComponentConfiguration_Failure(),
		   source,
		   new String[] {
			   "documentation", "Structured failure information when state = FAILED_ACTIVATION. Must be null in any other state (OCL invariant TODO)."
		   });
		addAnnotation
		  (getComponentConfiguration_Service(),
		   source,
		   new String[] {
			   "documentation", "The ServiceReference the configuration publishes when ACTIVE. Null if the component is not a service publisher (e.g. immediate=true with no provided service)."
		   });
		addAnnotation
		  (satisfiedReferenceEClass,
		   source,
		   new String[] {
			   "documentation", "Snapshot of a satisfied ComponentReference at runtime: the declared reference name plus the actual ServiceReferences currently bound."
		   });
		addAnnotation
		  (getSatisfiedReference_Name(),
		   source,
		   new String[] {
			   "documentation", "ComponentReference.name this snapshot belongs to."
		   });
		addAnnotation
		  (getSatisfiedReference_Target(),
		   source,
		   new String[] {
			   "documentation", "LDAP target filter that was effective at binding time."
		   });
		addAnnotation
		  (getSatisfiedReference_BoundServices(),
		   source,
		   new String[] {
			   "documentation", "Currently bound ServiceReferences."
		   });
		addAnnotation
		  (unsatisfiedReferenceEClass,
		   source,
		   new String[] {
			   "documentation", "Snapshot of a ComponentReference that does not meet its cardinality. Lists candidate target services (may be empty) so operators can see which targets exist but were rejected, e.g. by a target filter."
		   });
		addAnnotation
		  (getUnsatisfiedReference_Name(),
		   source,
		   new String[] {
			   "documentation", "ComponentReference.name this snapshot belongs to."
		   });
		addAnnotation
		  (getUnsatisfiedReference_Target(),
		   source,
		   new String[] {
			   "documentation", "LDAP target filter in effect."
		   });
		addAnnotation
		  (getUnsatisfiedReference_TargetServices(),
		   source,
		   new String[] {
			   "documentation", "Services that match the interface but did not satisfy the cardinality / filter / scope. May be empty."
		   });
		addAnnotation
		  (diagnosticEClass,
		   source,
		   new String[] {
			   "documentation", "Language-neutral diagnostic record. Structure follows the standard Diagnostic pattern (severity / message / source / code / data / children) so EMF-shaped tooling on the Java side feels native, but it is DDSR\'s own EClass so TypeScript and Python implementations are not bound to any host-language Diagnostic runtime. Diagnostics nest (children) to carry causation chains."
		   });
		addAnnotation
		  (getDiagnostic_Severity(),
		   source,
		   new String[] {
			   "documentation", "Severity of this diagnostic. An aggregate diagnostic typically carries the maximum severity of its children."
		   });
		addAnnotation
		  (getDiagnostic_Message(),
		   source,
		   new String[] {
			   "documentation", "Human-readable description of the diagnostic. Should be plain text; structured data goes in data."
		   });
		addAnnotation
		  (getDiagnostic_Source(),
		   source,
		   new String[] {
			   "documentation", "Originating subsystem identifier, e.g. \'org.gecko.ddsr.runtime\'. Useful when diagnostics from multiple sources are aggregated."
		   });
		addAnnotation
		  (getDiagnostic_Code(),
		   source,
		   new String[] {
			   "documentation", "Stable numeric code, source-namespaced. 0 = unclassified."
		   });
		addAnnotation
		  (getDiagnostic_Data(),
		   source,
		   new String[] {
			   "documentation", "Stringified contextual data. EMF\'s Diagnostic uses List<Object> here; DDSR uses strings to stay language-neutral on the wire."
		   });
		addAnnotation
		  (getDiagnostic_Children(),
		   source,
		   new String[] {
			   "documentation", "Causation chain (nested diagnostics). Empty for atomic diagnostics."
		   });
		addAnnotation
		  (serviceEventEClass,
		   source,
		   new String[] {
			   "documentation", "Notification about a service lifecycle change. Models org.osgi.framework.ServiceEvent. Delivered synchronously to local ServiceListeners; the Remote Registry sync mechanism may deliver an asynchronous echo across the federation (see REQUIREMENTS \u00a78 Q1)."
		   });
		addAnnotation
		  (getServiceEvent_Type(),
		   source,
		   new String[] {
			   "documentation", "Which lifecycle transition this event reports."
		   });
		addAnnotation
		  (getServiceEvent_Reference(),
		   source,
		   new String[] {
			   "documentation", "The service the event is about."
		   });
		addAnnotation
		  (getServiceEvent_Timestamp(),
		   source,
		   new String[] {
			   "documentation", "When the event was generated. Optional in OSGi semantics; useful for audit trails and event-stream ordering across the federation."
		   });
		addAnnotation
		  (serviceListenerEClass,
		   source,
		   new String[] {
			   "documentation", "Interface a consumer implements to receive ServiceEvents. Equivalent to org.osgi.framework.ServiceListener."
		   });
		addAnnotation
		  (getServiceListener__ServiceChanged__ServiceEvent(),
		   source,
		   new String[] {
			   "documentation", "Called by the registry for each ServiceEvent that matches this listener\'s filter. The framework guarantees that this is called synchronously relative to the operation that produced the event (so consumers see REGISTERED before the registering operation returns)."
		   });
		addAnnotation
		  ((getServiceListener__ServiceChanged__ServiceEvent()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "The event being delivered. Non-null."
		   });
		addAnnotation
		  (getServiceListener_Filter(),
		   source,
		   new String[] {
			   "documentation", "Optional LDAP filter limiting which services trigger serviceChanged. Null = receive events for all services."
		   });
		addAnnotation
		  (serviceRegistryEClass,
		   source,
		   new String[] {
			   "documentation", "Abstract registry base. Holds the shared lookup operations that work the same for local and remote registries. Concrete subclasses are LocalServiceRegistry (in-process, OSGi-like) and RemoteServiceRegistry (the central broker holding the API catalog and global implementation index)."
		   });
		addAnnotation
		  (getServiceRegistry__GetServiceReference__String(),
		   source,
		   new String[] {
			   "documentation", "Returns one ServiceReference for the given interface, picking by service.ranking (descending) and service.id (ascending) on tie. Null if no match."
		   });
		addAnnotation
		  ((getServiceRegistry__GetServiceReference__String()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "ServiceInterface.name."
		   });
		addAnnotation
		  (getServiceRegistry__GetServiceReferences__String_String_ConsumerCapability(),
		   source,
		   new String[] {
			   "documentation", "Returns all matching ServiceReferences ordered by ranking. Honors the consumer\'s flavor capability \u2014 references whose ServiceImplementation has no flavor in capability.supportedFlavors are filtered out."
		   });
		addAnnotation
		  ((getServiceRegistry__GetServiceReferences__String_String_ConsumerCapability()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "ServiceInterface.name."
		   });
		addAnnotation
		  ((getServiceRegistry__GetServiceReferences__String_String_ConsumerCapability()).getEParameters().get(1),
		   source,
		   new String[] {
			   "documentation", "LDAP filter over service properties. Null = no filter."
		   });
		addAnnotation
		  ((getServiceRegistry__GetServiceReferences__String_String_ConsumerCapability()).getEParameters().get(2),
		   source,
		   new String[] {
			   "documentation", "Consumer\'s capability bag (supported flavors etc.). For purely in-process lookup on a LocalServiceRegistry this can be null (no flavor filtering needed)."
		   });
		addAnnotation
		  (getServiceRegistry__GetAllServiceReferences__String_String_ConsumerCapability(),
		   source,
		   new String[] {
			   "documentation", "Like getServiceReferences but ignores ServiceReference visibility constraints (returns even references the caller would not normally see, e.g. across PDP boundaries). Use for administrative tooling and audit."
		   });
		addAnnotation
		  ((getServiceRegistry__GetAllServiceReferences__String_String_ConsumerCapability()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "ServiceInterface.name."
		   });
		addAnnotation
		  ((getServiceRegistry__GetAllServiceReferences__String_String_ConsumerCapability()).getEParameters().get(1),
		   source,
		   new String[] {
			   "documentation", "LDAP filter. Null = no filter."
		   });
		addAnnotation
		  ((getServiceRegistry__GetAllServiceReferences__String_String_ConsumerCapability()).getEParameters().get(2),
		   source,
		   new String[] {
			   "documentation", "Consumer\'s capability bag. Null permitted in administrative context."
		   });
		addAnnotation
		  (getServiceRegistry__AddServiceListener__ServiceListener(),
		   source,
		   new String[] {
			   "documentation", "Registers a listener. Idempotent: adding the same listener twice is a no-op."
		   });
		addAnnotation
		  ((getServiceRegistry__AddServiceListener__ServiceListener()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "Listener to add. Non-null."
		   });
		addAnnotation
		  (getServiceRegistry__RemoveServiceListener__ServiceListener(),
		   source,
		   new String[] {
			   "documentation", "Removes a listener. No-op if not registered."
		   });
		addAnnotation
		  ((getServiceRegistry__RemoveServiceListener__ServiceListener()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "Listener to remove. Non-null."
		   });
		addAnnotation
		  (getServiceRegistry_Kind(),
		   source,
		   new String[] {
			   "documentation", "Discriminator. Duplicates the subclass relationship but simplifies filtering of heterogeneous registry lists."
		   });
		addAnnotation
		  (getServiceRegistry_PublishHooks(),
		   source,
		   new String[] {
			   "documentation", "Plugged-in PublishHook implementations. Consulted in order before publishImplementation / withdrawImplementation accepts a request; if any hook returns a Diagnostic of severity ERROR or CANCEL, the action is rejected and the diagnostic is propagated. Non-containment because hook implementations are usually owned by the integrator\'s adapter component."
		   });
		addAnnotation
		  (getServiceRegistry_DiscoveryHooks(),
		   source,
		   new String[] {
			   "documentation", "Plugged-in DiscoveryHook implementations. Consulted in order before lookup / subscribe operations and to filter result lists."
		   });
		addAnnotation
		  (getServiceRegistry_DistributionHooks(),
		   source,
		   new String[] {
			   "documentation", "Plugged-in DistributionHook implementations. Consulted in order whenever an event crosses the local\u2194remote boundary (outbound from local, inbound from broker)."
		   });
		addAnnotation
		  (localServiceRegistryEClass,
		   source,
		   new String[] {
			   "documentation", "In-process registry. Owns local registrations and listeners, runs the synchronous lifecycle/event semantics, and (when \'remote\' is set) propagates registrations to the Remote Registry asynchronously."
		   });
		addAnnotation
		  (getLocalServiceRegistry__RegisterService__ServiceProvider_ServiceImplementation_EList(),
		   source,
		   new String[] {
			   "documentation", "Provider-facing registration entry point. Step order (REQUIREMENTS FR-Lifecycle-Register): (1) validate against catalog if remote is non-null; (2) allocate service.id and create ServiceReference; (3) add to local references/registrations synchronously; (4) deliver REGISTERED to matching local listeners synchronously, before returning; (5) hand off to async worker that publishes to remote (with retry)."
		   });
		addAnnotation
		  ((getLocalServiceRegistry__RegisterService__ServiceProvider_ServiceImplementation_EList()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "The provider that owns the registration. Used for PDP subject and as the back-link from ServiceReference.provider."
		   });
		addAnnotation
		  ((getLocalServiceRegistry__RegisterService__ServiceProvider_ServiceImplementation_EList()).getEParameters().get(1),
		   source,
		   new String[] {
			   "documentation", "The implementation being registered. Its serviceInterfaces and flavors are advertised in the resulting ServiceReference."
		   });
		addAnnotation
		  ((getLocalServiceRegistry__RegisterService__ServiceProvider_ServiceImplementation_EList()).getEParameters().get(2),
		   source,
		   new String[] {
			   "documentation", "Additional properties at registration time, merged on top of implementation.properties."
		   });
		addAnnotation
		  (getLocalServiceRegistry__FireServiceEvent__ServiceEvent(),
		   source,
		   new String[] {
			   "documentation", "Delivers an event to local listeners that match its filter. Synchronous; returns when all listeners have been notified."
		   });
		addAnnotation
		  ((getLocalServiceRegistry__FireServiceEvent__ServiceEvent()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "The event to deliver."
		   });
		addAnnotation
		  (getLocalServiceRegistry_References(),
		   source,
		   new String[] {
			   "documentation", "All locally registered service references."
		   });
		addAnnotation
		  (getLocalServiceRegistry_Registrations(),
		   source,
		   new String[] {
			   "documentation", "Provider-side handles paired with each reference via eOpposite."
		   });
		addAnnotation
		  (getLocalServiceRegistry_Sessions(),
		   source,
		   new String[] {
			   "documentation", "Consumer sessions holding acquisition leases (ACQUISITION.md par.3/par.4). Runtime state by design: sessions are deliberately NOT persisted to the broker snapshot \u2014 after a broker restart consumers rebuild them via their regular session PUTs (FR-Sync-Reconnect philosophy)."
		   });
		addAnnotation
		  (getLocalServiceRegistry_Configurations(),
		   source,
		   new String[] {
			   "documentation", "Active component configurations (instances of declared components)."
		   });
		addAnnotation
		  (getLocalServiceRegistry_Providers(),
		   source,
		   new String[] {
			   "documentation", "Providers known to this local registry."
		   });
		addAnnotation
		  (getLocalServiceRegistry_Listeners(),
		   source,
		   new String[] {
			   "documentation", "Registered listeners (non-containment because listeners are typically owned by the consumer that wrote them)."
		   });
		addAnnotation
		  (getLocalServiceRegistry_Remote(),
		   source,
		   new String[] {
			   "documentation", "The Remote Registry this local registry is connected to, if any. Null = standalone (no federation, no catalog lookup, no remote publication)."
		   });
		addAnnotation
		  (getLocalServiceRegistry_ConnectionState(),
		   source,
		   new String[] {
			   "documentation", "Health of the link to the Remote Registry. Consumers can read this to know whether lookup results are live (CONNECTED) or possibly stale (DEGRADED / OFFLINE). While DEGRADED or OFFLINE, reads are served from the last known snapshot+event-replay and writes (publishImplementation, withdrawImplementation, catalog mutations) are rejected with a Diagnostic. On reconnect, the registry pulls a fresh snapshot and synthesises ServiceEvents for the diff so local listeners see a consistent transition."
		   });
		addAnnotation
		  (remoteServiceRegistryEClass,
		   source,
		   new String[] {
			   "documentation", "The central Broker. Holds the API catalog (ServiceInterface containments) and the global implementation index (non-containment references back to ServiceImplementations that live under their providers). Provider/consumer wire traffic does NOT flow through here \u2014 only directory traffic."
		   });
		addAnnotation
		  (getRemoteServiceRegistry__PublishImplementation__ServiceProvider_ServiceImplementation(),
		   source,
		   new String[] {
			   "documentation", "Adds an implementation to the global index. Validates that all referenced ServiceInterfaces exist in the catalog. Authorization flows through the PublishHook chain. If any of the referenced ServiceInterfaces has status = DEPRECATED, the call succeeds but the returned Diagnostic carries severity = WARNING with the deprecationReason (so the provider knows it is publishing against a deprecated interface). Returns OK (severity) on plain success."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__PublishImplementation__ServiceProvider_ServiceImplementation()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "The owning provider. Used as PDP subject."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__PublishImplementation__ServiceProvider_ServiceImplementation()).getEParameters().get(1),
		   source,
		   new String[] {
			   "documentation", "The implementation to publish. MUST be contained in \'provider.implementations\'."
		   });
		addAnnotation
		  (getRemoteServiceRegistry__WithdrawImplementation__ServiceProvider_ServiceImplementation(),
		   source,
		   new String[] {
			   "documentation", "Symmetric to publishImplementation. The provider parameter is required so that PDP authorization, audit logging, and ownership checks (implementation must belong to provider) can run unambiguously, even though the implementation\'s container is reachable via eContainer()."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__WithdrawImplementation__ServiceProvider_ServiceImplementation()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "The provider that owns the implementation. Authority and audit subject."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__WithdrawImplementation__ServiceProvider_ServiceImplementation()).getEParameters().get(1),
		   source,
		   new String[] {
			   "documentation", "The implementation to withdraw. MUST be contained in \'provider.implementations\' (OCL invariant)."
		   });
		addAnnotation
		  (getRemoteServiceRegistry__AddCatalogEntry__ServiceInterface_String(),
		   source,
		   new String[] {
			   "documentation", "Adds a new ServiceInterface to the catalog. Authorization flows through the PDP \u2014 typically only the Governance officer is permitted. On success, triggers the Code Publisher to emit JAR / npm / wheel artifacts (REQUIREMENTS FR-CodeDist-Publisher)."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__AddCatalogEntry__ServiceInterface_String()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "The interface to add."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__AddCatalogEntry__ServiceInterface_String()).getEParameters().get(1),
		   source,
		   new String[] {
			   "documentation", "Identity that requested the change. Used as PDP subject."
		   });
		addAnnotation
		  (getRemoteServiceRegistry__DeprecateCatalogEntry__ServiceInterface_String(),
		   source,
		   new String[] {
			   "documentation", "Soft-deprecation. Sets status = DEPRECATED on the ServiceInterface; optionally records deprecationReason and replacedBy. Existing ServiceImplementations stay live, lookups continue to resolve, ServiceListeners continue to fire. Subsequent publishImplementation calls against this interface succeed but return a WARNING diagnostic that consumers can inspect. The transition is one-way: once DEPRECATED, the entry cannot return to ACTIVE \u2014 a revival is a new ServiceInterface entry."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__DeprecateCatalogEntry__ServiceInterface_String()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "The interface to deprecate."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__DeprecateCatalogEntry__ServiceInterface_String()).getEParameters().get(1),
		   source,
		   new String[] {
			   "documentation", "Identity that requested the change."
		   });
		addAnnotation
		  (getRemoteServiceRegistry__RemoveCatalogEntry__ServiceInterface_String(),
		   source,
		   new String[] {
			   "documentation", "Strict-Reject removal. Refuses with Diagnostic(severity=ERROR, code=CATALOG_HAS_LIVE_IMPLS) as long as ANY ServiceImplementation in the registry references the interface. The governance officer must first ensure all providers have withdrawn their implementations (typically: deprecate the interface, wait for the migration window, then remove). Returns OK only when the catalog entry is gone."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__RemoveCatalogEntry__ServiceInterface_String()).getEParameters().get(0),
		   source,
		   new String[] {
			   "documentation", "The interface to remove."
		   });
		addAnnotation
		  ((getRemoteServiceRegistry__RemoveCatalogEntry__ServiceInterface_String()).getEParameters().get(1),
		   source,
		   new String[] {
			   "documentation", "Identity that requested the change."
		   });
		addAnnotation
		  (getRemoteServiceRegistry_Endpoint(),
		   source,
		   new String[] {
			   "documentation", "Address clients connect to (e.g. cluster VIP). May be null on the server side instance."
		   });
		addAnnotation
		  (getRemoteServiceRegistry_Catalog(),
		   source,
		   new String[] {
			   "documentation", "The curated API catalog. Mutations go through addCatalogEntry / deprecateCatalogEntry / removeCatalogEntry, all of which flow through the PDP."
		   });
		addAnnotation
		  (getRemoteServiceRegistry_Implementations(),
		   source,
		   new String[] {
			   "documentation", "Index of currently published implementations. Non-containment: each implementation is owned (containment) by its ServiceProvider."
		   });
		addAnnotation
		  (getRemoteServiceRegistry_Providers(),
		   source,
		   new String[] {
			   "documentation", "Providers known to the broker. Non-containment: providers also live under their LocalServiceRegistry."
		   });
		addAnnotation
		  (consumerCapabilityEClass,
		   source,
		   new String[] {
			   "documentation", "Bag of capabilities a consumer attaches to a lookup. NOT persistent \u2014 created per request and passed through getServiceReferences / getAllServiceReferences. The registry uses supportedFlavors to filter implementations the consumer cannot actually invoke."
		   });
		addAnnotation
		  (getConsumerCapability_ConsumerId(),
		   source,
		   new String[] {
			   "documentation", "Optional consumer identity (auditing, policy). Null = anonymous."
		   });
		addAnnotation
		  (getConsumerCapability_SupportedFlavors(),
		   source,
		   new String[] {
			   "documentation", "Flavors the consumer can invoke. At least one \u2014 a consumer without any flavor client plugin cannot consume anything."
		   });
		addAnnotation
		  (getConsumerCapability_Properties(),
		   source,
		   new String[] {
			   "documentation", "Additional capability hints (accepted content types, encoding preferences, \u2026). Open-ended bag."
		   });
		addAnnotation
		  (publishHookEClass,
		   source,
		   new String[] {
			   "documentation", "Callback for provider-side intervention. Consulted by the framework before a publishImplementation or withdrawImplementation succeeds. Hook can veto by returning a Diagnostic with severity ERROR or CANCEL."
		   });
		addAnnotation
		  (getPublishHook__OnPublish__ServiceProvider_ServiceImplementation(),
		   source,
		   new String[] {
			   "documentation", "Called before the broker accepts a new ServiceImplementation for publication. Return OK to proceed. Return ERROR/CANCEL to abort; the broker propagates the returned Diagnostic to the caller of publishImplementation."
		   });
		addAnnotation
		  (getPublishHook__OnWithdraw__ServiceProvider_ServiceImplementation(),
		   source,
		   new String[] {
			   "documentation", "Called before the broker withdraws an existing ServiceImplementation. Same semantics as onPublish."
		   });
		addAnnotation
		  (discoveryHookEClass,
		   source,
		   new String[] {
			   "documentation", "Callback for consumer-side intervention. Consulted by the framework when a consumer looks up services or subscribes to events. Hook can veto, log, or shape the result set."
		   });
		addAnnotation
		  (getDiscoveryHook__OnLookup__String_String_ConsumerCapability(),
		   source,
		   new String[] {
			   "documentation", "Called before the registry runs a lookup. Return OK to proceed; ERROR/CANCEL to deny the lookup entirely (caller receives the diagnostic instead of references)."
		   });
		addAnnotation
		  (getDiscoveryHook__FilterReferences__ConsumerCapability_EList(),
		   source,
		   new String[] {
			   "documentation", "Called after the registry has assembled a candidate result list but before returning it to the consumer. Returns the (potentially) pruned list. Returning an empty list = no matches visible to this consumer."
		   });
		addAnnotation
		  (getDiscoveryHook__OnSubscribe__ServiceListener(),
		   source,
		   new String[] {
			   "documentation", "Called before addServiceListener installs a listener. Can deny subscription based on listener.filter, consumer identity, or context."
		   });
		addAnnotation
		  (distributionHookEClass,
		   source,
		   new String[] {
			   "documentation", "Callback for traffic between LocalServiceRegistry and RemoteServiceRegistry. Lets an integrator intervene at the federation boundary \u2014 e.g. mask sensitive properties before they cross into the broker, or drop inbound events that target unauthorized tenants."
		   });
		addAnnotation
		  (getDistributionHook__OnOutbound__ServiceEvent(),
		   source,
		   new String[] {
			   "documentation", "Called before the local registry forwards an event (REGISTERED / MODIFIED / UNREGISTERING) to the broker. Return OK to proceed; ERROR/CANCEL to suppress the outbound event."
		   });
		addAnnotation
		  (getDistributionHook__OnInbound__ServiceEvent(),
		   source,
		   new String[] {
			   "documentation", "Called when the local registry receives an event from the broker (over the SSE stream or initial snapshot). Return OK to deliver to local listeners; ERROR/CANCEL to drop the event silently for this local."
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
		  (versionedElementEClass,
		   source,
		   new String[] {
			   "validSemver", "version = null or version.matches(\'^\\\\d+\\\\.\\\\d+\\\\.\\\\d+(-[0-9A-Za-z.-]+)?$\')"
		   });
		addAnnotation
		  (numericRangeConstraintEClass,
		   source,
		   new String[] {
			   "atLeastOneBound", "min <> null or max <> null",
			   "rangeOrdered", "min = null or max = null or min <= max"
		   });
		addAnnotation
		  (stringPatternConstraintEClass,
		   source,
		   new String[] {
			   "lengthBoundsNonNegative", "(minLength = null or minLength >= 0) and (maxLength = null or maxLength >= 0)",
			   "lengthBoundsOrdered", "minLength = null or maxLength = null or minLength <= maxLength"
		   });
		addAnnotation
		  (collectionSizeConstraintEClass,
		   source,
		   new String[] {
			   "sizeBoundsNonNegative", "(minSize = null or minSize >= 0) and (maxSize = null or maxSize >= 0)",
			   "sizeBoundsOrdered", "minSize = null or maxSize = null or minSize <= maxSize"
		   });
		addAnnotation
		  (serviceInterfaceEClass,
		   source,
		   new String[] {
			   "immutableAfterPublish", "-- ServiceInterface is conceptually immutable once added to the catalog (semver: changes mean a new entry, old one optionally deprecated). Enforced at addCatalogEntry / mutation operations, not as a static invariant \u2014 placeholder.",
			   "replacedByIsDeprecated", "replacedBy = null or status = ddsr::CatalogStatus::DEPRECATED"
		   });
		addAnnotation
		  (serviceImplementationEClass,
		   source,
		   new String[] {
			   "atLeastOneInterface", "serviceInterfaces->notEmpty()",
			   "operationFlavorsCoverInterfaces", "flavors->forAll(f | f.operationFlavors->forAll(of | serviceInterfaces->exists(si | si.operations->includes(of.operation))))"
		   });
		addAnnotation
		  (serviceRegistrationEClass,
		   source,
		   new String[] {
			   "unregisteredNotInRegistry", "not unregistered or LocalServiceRegistry.allInstances()->forAll(r | not r.registrations->includes(self))"
		   });
		addAnnotation
		  (componentConfigurationEClass,
		   source,
		   new String[] {
			   "failureOnlyWhenFailed", "(state = ddsr::ComponentState::FAILED_ACTIVATION) = (failure <> null)"
		   });
		addAnnotation
		  (remoteServiceRegistryEClass,
		   source,
		   new String[] {
			   "publishedImplsHaveFlavor", "implementations->forAll(i | i.flavors->notEmpty())",
			   "publishedImplsReferenceCatalog", "implementations->forAll(i | i.serviceInterfaces->forAll(si | catalog->includes(si)))",
			   "publishedImplsOwnedByListedProvider", "implementations->forAll(i | providers->includes(i.eContainer().oclAsType(ddsr::ServiceProvider)))"
		   });
	}

} //ServicesPackageImpl
