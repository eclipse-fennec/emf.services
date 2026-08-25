/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.ecore.EFactory;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage
 * @generated
 */
@ProviderType
public interface ServicesFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	ServicesFactory eINSTANCE = org.eclipse.fennec.services.impl.ServicesFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>String Property</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>String Property</em>'.
	 * @generated
	 */
	StringProperty createStringProperty();

	/**
	 * Returns a new object of class '<em>Int Property</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Int Property</em>'.
	 * @generated
	 */
	IntProperty createIntProperty();

	/**
	 * Returns a new object of class '<em>Long Property</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Long Property</em>'.
	 * @generated
	 */
	LongProperty createLongProperty();

	/**
	 * Returns a new object of class '<em>Double Property</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Double Property</em>'.
	 * @generated
	 */
	DoubleProperty createDoubleProperty();

	/**
	 * Returns a new object of class '<em>Float Property</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Float Property</em>'.
	 * @generated
	 */
	FloatProperty createFloatProperty();

	/**
	 * Returns a new object of class '<em>Short Property</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Short Property</em>'.
	 * @generated
	 */
	ShortProperty createShortProperty();

	/**
	 * Returns a new object of class '<em>Bool Property</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Bool Property</em>'.
	 * @generated
	 */
	BoolProperty createBoolProperty();

	/**
	 * Returns a new object of class '<em>String List Property</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>String List Property</em>'.
	 * @generated
	 */
	StringListProperty createStringListProperty();

	/**
	 * Returns a new object of class '<em>Service Operation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Service Operation</em>'.
	 * @generated
	 */
	ServiceOperation createServiceOperation();

	/**
	 * Returns a new object of class '<em>Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Parameter</em>'.
	 * @generated
	 */
	Parameter createParameter();

	/**
	 * Returns a new object of class '<em>Required Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Required Constraint</em>'.
	 * @generated
	 */
	RequiredConstraint createRequiredConstraint();

	/**
	 * Returns a new object of class '<em>Numeric Range Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Numeric Range Constraint</em>'.
	 * @generated
	 */
	NumericRangeConstraint createNumericRangeConstraint();

	/**
	 * Returns a new object of class '<em>String Pattern Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>String Pattern Constraint</em>'.
	 * @generated
	 */
	StringPatternConstraint createStringPatternConstraint();

	/**
	 * Returns a new object of class '<em>Enumeration Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Enumeration Constraint</em>'.
	 * @generated
	 */
	EnumerationConstraint createEnumerationConstraint();

	/**
	 * Returns a new object of class '<em>Expression Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Expression Constraint</em>'.
	 * @generated
	 */
	ExpressionConstraint createExpressionConstraint();

	/**
	 * Returns a new object of class '<em>Invariant</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Invariant</em>'.
	 * @generated
	 */
	Invariant createInvariant();

	/**
	 * Returns a new object of class '<em>Collection Size Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Collection Size Constraint</em>'.
	 * @generated
	 */
	CollectionSizeConstraint createCollectionSizeConstraint();

	/**
	 * Returns a new object of class '<em>Service Exception</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Service Exception</em>'.
	 * @generated
	 */
	ServiceException createServiceException();

	/**
	 * Returns a new object of class '<em>Service Interface</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Service Interface</em>'.
	 * @generated
	 */
	ServiceInterface createServiceInterface();

	/**
	 * Returns a new object of class '<em>Lifecycle Hook</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Lifecycle Hook</em>'.
	 * @generated
	 */
	LifecycleHook createLifecycleHook();

	/**
	 * Returns a new object of class '<em>Reference Binding</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Reference Binding</em>'.
	 * @generated
	 */
	ReferenceBinding createReferenceBinding();

	/**
	 * Returns a new object of class '<em>Component Reference</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Component Reference</em>'.
	 * @generated
	 */
	ComponentReference createComponentReference();

	/**
	 * Returns a new object of class '<em>Component Description</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Component Description</em>'.
	 * @generated
	 */
	ComponentDescription createComponentDescription();

	/**
	 * Returns a new object of class '<em>Service Provider</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Service Provider</em>'.
	 * @generated
	 */
	ServiceProvider createServiceProvider();

	/**
	 * Returns a new object of class '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Service Implementation</em>'.
	 * @generated
	 */
	ServiceImplementation createServiceImplementation();

	/**
	 * Returns a new object of class '<em>Rest Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Rest Flavor</em>'.
	 * @generated
	 */
	RestFlavor createRestFlavor();

	/**
	 * Returns a new object of class '<em>Mqtt Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Mqtt Flavor</em>'.
	 * @generated
	 */
	MqttFlavor createMqttFlavor();

	/**
	 * Returns a new object of class '<em>Rest Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Rest Operation Flavor</em>'.
	 * @generated
	 */
	RestOperationFlavor createRestOperationFlavor();

	/**
	 * Returns a new object of class '<em>Mqtt Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Mqtt Operation Flavor</em>'.
	 * @generated
	 */
	MqttOperationFlavor createMqttOperationFlavor();

	/**
	 * Returns a new object of class '<em>Service Reference</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Service Reference</em>'.
	 * @generated
	 */
	ServiceReference createServiceReference();

	/**
	 * Returns a new object of class '<em>Service Registration</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Service Registration</em>'.
	 * @generated
	 */
	ServiceRegistration createServiceRegistration();

	/**
	 * Returns a new object of class '<em>Consumer Session</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Consumer Session</em>'.
	 * @generated
	 */
	ConsumerSession createConsumerSession();

	/**
	 * Returns a new object of class '<em>Component Configuration</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Component Configuration</em>'.
	 * @generated
	 */
	ComponentConfiguration createComponentConfiguration();

	/**
	 * Returns a new object of class '<em>Satisfied Reference</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Satisfied Reference</em>'.
	 * @generated
	 */
	SatisfiedReference createSatisfiedReference();

	/**
	 * Returns a new object of class '<em>Unsatisfied Reference</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Unsatisfied Reference</em>'.
	 * @generated
	 */
	UnsatisfiedReference createUnsatisfiedReference();

	/**
	 * Returns a new object of class '<em>Diagnostic</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Diagnostic</em>'.
	 * @generated
	 */
	Diagnostic createDiagnostic();

	/**
	 * Returns a new object of class '<em>Service Event</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Service Event</em>'.
	 * @generated
	 */
	ServiceEvent createServiceEvent();

	/**
	 * Returns a new object of class '<em>Local Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Local Service Registry</em>'.
	 * @generated
	 */
	LocalServiceRegistry createLocalServiceRegistry();

	/**
	 * Returns a new object of class '<em>Remote Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Remote Service Registry</em>'.
	 * @generated
	 */
	RemoteServiceRegistry createRemoteServiceRegistry();

	/**
	 * Returns a new object of class '<em>Consumer Capability</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Consumer Capability</em>'.
	 * @generated
	 */
	ConsumerCapability createConsumerCapability();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	ServicesPackage getServicesPackage();

} //ServicesFactory
