/**
 */
package org.eclipse.fennec.services.util;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.Switch;

import org.eclipse.fennec.services.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see org.eclipse.fennec.services.ServicesPackage
 * @generated
 */
public class ServicesSwitch<T> extends Switch<T> {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static ServicesPackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServicesSwitch() {
		if (modelPackage == null) {
			modelPackage = ServicesPackage.eINSTANCE;
		}
	}

	/**
	 * Checks whether this is a switch for the given package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param ePackage the package in question.
	 * @return whether this is a switch for the given package.
	 * @generated
	 */
	@Override
	protected boolean isSwitchFor(EPackage ePackage) {
		return ePackage == modelPackage;
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	@Override
	protected T doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
			case ServicesPackage.NAMED_ELEMENT: {
				NamedElement namedElement = (NamedElement)theEObject;
				T result = caseNamedElement(namedElement);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.VERSIONED_ELEMENT: {
				VersionedElement versionedElement = (VersionedElement)theEObject;
				T result = caseVersionedElement(versionedElement);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.PROPERTY: {
				Property property = (Property)theEObject;
				T result = caseProperty(property);
				if (result == null) result = caseNamedElement(property);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.STRING_PROPERTY: {
				StringProperty stringProperty = (StringProperty)theEObject;
				T result = caseStringProperty(stringProperty);
				if (result == null) result = caseProperty(stringProperty);
				if (result == null) result = caseNamedElement(stringProperty);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.INT_PROPERTY: {
				IntProperty intProperty = (IntProperty)theEObject;
				T result = caseIntProperty(intProperty);
				if (result == null) result = caseProperty(intProperty);
				if (result == null) result = caseNamedElement(intProperty);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.LONG_PROPERTY: {
				LongProperty longProperty = (LongProperty)theEObject;
				T result = caseLongProperty(longProperty);
				if (result == null) result = caseProperty(longProperty);
				if (result == null) result = caseNamedElement(longProperty);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.DOUBLE_PROPERTY: {
				DoubleProperty doubleProperty = (DoubleProperty)theEObject;
				T result = caseDoubleProperty(doubleProperty);
				if (result == null) result = caseProperty(doubleProperty);
				if (result == null) result = caseNamedElement(doubleProperty);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.FLOAT_PROPERTY: {
				FloatProperty floatProperty = (FloatProperty)theEObject;
				T result = caseFloatProperty(floatProperty);
				if (result == null) result = caseProperty(floatProperty);
				if (result == null) result = caseNamedElement(floatProperty);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SHORT_PROPERTY: {
				ShortProperty shortProperty = (ShortProperty)theEObject;
				T result = caseShortProperty(shortProperty);
				if (result == null) result = caseProperty(shortProperty);
				if (result == null) result = caseNamedElement(shortProperty);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.BOOL_PROPERTY: {
				BoolProperty boolProperty = (BoolProperty)theEObject;
				T result = caseBoolProperty(boolProperty);
				if (result == null) result = caseProperty(boolProperty);
				if (result == null) result = caseNamedElement(boolProperty);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.STRING_LIST_PROPERTY: {
				StringListProperty stringListProperty = (StringListProperty)theEObject;
				T result = caseStringListProperty(stringListProperty);
				if (result == null) result = caseProperty(stringListProperty);
				if (result == null) result = caseNamedElement(stringListProperty);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_OPERATION: {
				ServiceOperation serviceOperation = (ServiceOperation)theEObject;
				T result = caseServiceOperation(serviceOperation);
				if (result == null) result = caseNamedElement(serviceOperation);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.PARAMETER: {
				Parameter parameter = (Parameter)theEObject;
				T result = caseParameter(parameter);
				if (result == null) result = caseNamedElement(parameter);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.PARAMETER_CONSTRAINT: {
				ParameterConstraint parameterConstraint = (ParameterConstraint)theEObject;
				T result = caseParameterConstraint(parameterConstraint);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.REQUIRED_CONSTRAINT: {
				RequiredConstraint requiredConstraint = (RequiredConstraint)theEObject;
				T result = caseRequiredConstraint(requiredConstraint);
				if (result == null) result = caseParameterConstraint(requiredConstraint);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.NUMERIC_RANGE_CONSTRAINT: {
				NumericRangeConstraint numericRangeConstraint = (NumericRangeConstraint)theEObject;
				T result = caseNumericRangeConstraint(numericRangeConstraint);
				if (result == null) result = caseParameterConstraint(numericRangeConstraint);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.STRING_PATTERN_CONSTRAINT: {
				StringPatternConstraint stringPatternConstraint = (StringPatternConstraint)theEObject;
				T result = caseStringPatternConstraint(stringPatternConstraint);
				if (result == null) result = caseParameterConstraint(stringPatternConstraint);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.ENUMERATION_CONSTRAINT: {
				EnumerationConstraint enumerationConstraint = (EnumerationConstraint)theEObject;
				T result = caseEnumerationConstraint(enumerationConstraint);
				if (result == null) result = caseParameterConstraint(enumerationConstraint);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.EXPRESSION_CONSTRAINT: {
				ExpressionConstraint expressionConstraint = (ExpressionConstraint)theEObject;
				T result = caseExpressionConstraint(expressionConstraint);
				if (result == null) result = caseParameterConstraint(expressionConstraint);
				if (result == null) result = caseNamedElement(expressionConstraint);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.INVARIANT: {
				Invariant invariant = (Invariant)theEObject;
				T result = caseInvariant(invariant);
				if (result == null) result = caseNamedElement(invariant);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.COLLECTION_SIZE_CONSTRAINT: {
				CollectionSizeConstraint collectionSizeConstraint = (CollectionSizeConstraint)theEObject;
				T result = caseCollectionSizeConstraint(collectionSizeConstraint);
				if (result == null) result = caseParameterConstraint(collectionSizeConstraint);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_EXCEPTION: {
				ServiceException serviceException = (ServiceException)theEObject;
				T result = caseServiceException(serviceException);
				if (result == null) result = caseNamedElement(serviceException);
				if (result == null) result = caseVersionedElement(serviceException);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_INTERFACE: {
				ServiceInterface serviceInterface = (ServiceInterface)theEObject;
				T result = caseServiceInterface(serviceInterface);
				if (result == null) result = caseNamedElement(serviceInterface);
				if (result == null) result = caseVersionedElement(serviceInterface);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.LIFECYCLE_HOOK: {
				LifecycleHook lifecycleHook = (LifecycleHook)theEObject;
				T result = caseLifecycleHook(lifecycleHook);
				if (result == null) result = caseNamedElement(lifecycleHook);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.REFERENCE_BINDING: {
				ReferenceBinding referenceBinding = (ReferenceBinding)theEObject;
				T result = caseReferenceBinding(referenceBinding);
				if (result == null) result = caseNamedElement(referenceBinding);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.COMPONENT_REFERENCE: {
				ComponentReference componentReference = (ComponentReference)theEObject;
				T result = caseComponentReference(componentReference);
				if (result == null) result = caseNamedElement(componentReference);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.COMPONENT_DESCRIPTION: {
				ComponentDescription componentDescription = (ComponentDescription)theEObject;
				T result = caseComponentDescription(componentDescription);
				if (result == null) result = caseNamedElement(componentDescription);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_PROVIDER: {
				ServiceProvider serviceProvider = (ServiceProvider)theEObject;
				T result = caseServiceProvider(serviceProvider);
				if (result == null) result = caseNamedElement(serviceProvider);
				if (result == null) result = caseVersionedElement(serviceProvider);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_IMPLEMENTATION: {
				ServiceImplementation serviceImplementation = (ServiceImplementation)theEObject;
				T result = caseServiceImplementation(serviceImplementation);
				if (result == null) result = caseNamedElement(serviceImplementation);
				if (result == null) result = caseVersionedElement(serviceImplementation);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_FLAVOR: {
				ServiceFlavor serviceFlavor = (ServiceFlavor)theEObject;
				T result = caseServiceFlavor(serviceFlavor);
				if (result == null) result = caseNamedElement(serviceFlavor);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.REST_FLAVOR: {
				RestFlavor restFlavor = (RestFlavor)theEObject;
				T result = caseRestFlavor(restFlavor);
				if (result == null) result = caseServiceFlavor(restFlavor);
				if (result == null) result = caseNamedElement(restFlavor);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.MQTT_FLAVOR: {
				MqttFlavor mqttFlavor = (MqttFlavor)theEObject;
				T result = caseMqttFlavor(mqttFlavor);
				if (result == null) result = caseServiceFlavor(mqttFlavor);
				if (result == null) result = caseNamedElement(mqttFlavor);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_OPERATION_FLAVOR: {
				ServiceOperationFlavor serviceOperationFlavor = (ServiceOperationFlavor)theEObject;
				T result = caseServiceOperationFlavor(serviceOperationFlavor);
				if (result == null) result = caseNamedElement(serviceOperationFlavor);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.REST_OPERATION_FLAVOR: {
				RestOperationFlavor restOperationFlavor = (RestOperationFlavor)theEObject;
				T result = caseRestOperationFlavor(restOperationFlavor);
				if (result == null) result = caseServiceOperationFlavor(restOperationFlavor);
				if (result == null) result = caseNamedElement(restOperationFlavor);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.MQTT_OPERATION_FLAVOR: {
				MqttOperationFlavor mqttOperationFlavor = (MqttOperationFlavor)theEObject;
				T result = caseMqttOperationFlavor(mqttOperationFlavor);
				if (result == null) result = caseServiceOperationFlavor(mqttOperationFlavor);
				if (result == null) result = caseNamedElement(mqttOperationFlavor);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_REFERENCE: {
				ServiceReference serviceReference = (ServiceReference)theEObject;
				T result = caseServiceReference(serviceReference);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_REGISTRATION: {
				ServiceRegistration serviceRegistration = (ServiceRegistration)theEObject;
				T result = caseServiceRegistration(serviceRegistration);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.CONSUMER_SESSION: {
				ConsumerSession consumerSession = (ConsumerSession)theEObject;
				T result = caseConsumerSession(consumerSession);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.COMPONENT_CONFIGURATION: {
				ComponentConfiguration componentConfiguration = (ComponentConfiguration)theEObject;
				T result = caseComponentConfiguration(componentConfiguration);
				if (result == null) result = caseNamedElement(componentConfiguration);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SATISFIED_REFERENCE: {
				SatisfiedReference satisfiedReference = (SatisfiedReference)theEObject;
				T result = caseSatisfiedReference(satisfiedReference);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.UNSATISFIED_REFERENCE: {
				UnsatisfiedReference unsatisfiedReference = (UnsatisfiedReference)theEObject;
				T result = caseUnsatisfiedReference(unsatisfiedReference);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.DIAGNOSTIC: {
				Diagnostic diagnostic = (Diagnostic)theEObject;
				T result = caseDiagnostic(diagnostic);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_EVENT: {
				ServiceEvent serviceEvent = (ServiceEvent)theEObject;
				T result = caseServiceEvent(serviceEvent);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_LISTENER: {
				ServiceListener serviceListener = (ServiceListener)theEObject;
				T result = caseServiceListener(serviceListener);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.SERVICE_REGISTRY: {
				ServiceRegistry serviceRegistry = (ServiceRegistry)theEObject;
				T result = caseServiceRegistry(serviceRegistry);
				if (result == null) result = caseNamedElement(serviceRegistry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.LOCAL_SERVICE_REGISTRY: {
				LocalServiceRegistry localServiceRegistry = (LocalServiceRegistry)theEObject;
				T result = caseLocalServiceRegistry(localServiceRegistry);
				if (result == null) result = caseServiceRegistry(localServiceRegistry);
				if (result == null) result = caseNamedElement(localServiceRegistry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.REMOTE_SERVICE_REGISTRY: {
				RemoteServiceRegistry remoteServiceRegistry = (RemoteServiceRegistry)theEObject;
				T result = caseRemoteServiceRegistry(remoteServiceRegistry);
				if (result == null) result = caseServiceRegistry(remoteServiceRegistry);
				if (result == null) result = caseNamedElement(remoteServiceRegistry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.CONSUMER_CAPABILITY: {
				ConsumerCapability consumerCapability = (ConsumerCapability)theEObject;
				T result = caseConsumerCapability(consumerCapability);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.PUBLISH_HOOK: {
				PublishHook publishHook = (PublishHook)theEObject;
				T result = casePublishHook(publishHook);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.DISCOVERY_HOOK: {
				DiscoveryHook discoveryHook = (DiscoveryHook)theEObject;
				T result = caseDiscoveryHook(discoveryHook);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case ServicesPackage.DISTRIBUTION_HOOK: {
				DistributionHook distributionHook = (DistributionHook)theEObject;
				T result = caseDistributionHook(distributionHook);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			default: return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Named Element</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Named Element</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseNamedElement(NamedElement object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Versioned Element</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Versioned Element</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseVersionedElement(VersionedElement object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Property</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Property</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseProperty(Property object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>String Property</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>String Property</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseStringProperty(StringProperty object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Int Property</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Int Property</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseIntProperty(IntProperty object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Long Property</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Long Property</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseLongProperty(LongProperty object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Double Property</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Double Property</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDoubleProperty(DoubleProperty object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Float Property</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Float Property</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseFloatProperty(FloatProperty object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Short Property</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Short Property</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseShortProperty(ShortProperty object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Bool Property</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Bool Property</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseBoolProperty(BoolProperty object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>String List Property</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>String List Property</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseStringListProperty(StringListProperty object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Operation</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Operation</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceOperation(ServiceOperation object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Parameter</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseParameter(Parameter object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Parameter Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Parameter Constraint</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseParameterConstraint(ParameterConstraint object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Required Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Required Constraint</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRequiredConstraint(RequiredConstraint object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Numeric Range Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Numeric Range Constraint</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseNumericRangeConstraint(NumericRangeConstraint object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>String Pattern Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>String Pattern Constraint</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseStringPatternConstraint(StringPatternConstraint object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Enumeration Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Enumeration Constraint</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseEnumerationConstraint(EnumerationConstraint object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Expression Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Expression Constraint</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseExpressionConstraint(ExpressionConstraint object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Invariant</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Invariant</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInvariant(Invariant object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Collection Size Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Collection Size Constraint</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseCollectionSizeConstraint(CollectionSizeConstraint object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Exception</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Exception</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceException(ServiceException object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Interface</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Interface</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceInterface(ServiceInterface object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Lifecycle Hook</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Lifecycle Hook</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseLifecycleHook(LifecycleHook object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Reference Binding</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Reference Binding</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseReferenceBinding(ReferenceBinding object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Component Reference</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Component Reference</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseComponentReference(ComponentReference object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Component Description</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Component Description</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseComponentDescription(ComponentDescription object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Provider</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Provider</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceProvider(ServiceProvider object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Implementation</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Implementation</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceImplementation(ServiceImplementation object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Flavor</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceFlavor(ServiceFlavor object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Rest Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Rest Flavor</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRestFlavor(RestFlavor object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Mqtt Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Mqtt Flavor</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseMqttFlavor(MqttFlavor object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Operation Flavor</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceOperationFlavor(ServiceOperationFlavor object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Rest Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Rest Operation Flavor</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRestOperationFlavor(RestOperationFlavor object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Mqtt Operation Flavor</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Mqtt Operation Flavor</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseMqttOperationFlavor(MqttOperationFlavor object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Reference</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Reference</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceReference(ServiceReference object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Registration</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Registration</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceRegistration(ServiceRegistration object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Consumer Session</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Consumer Session</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseConsumerSession(ConsumerSession object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Component Configuration</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Component Configuration</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseComponentConfiguration(ComponentConfiguration object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Satisfied Reference</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Satisfied Reference</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseSatisfiedReference(SatisfiedReference object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Unsatisfied Reference</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Unsatisfied Reference</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseUnsatisfiedReference(UnsatisfiedReference object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Diagnostic</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Diagnostic</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDiagnostic(Diagnostic object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Event</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Event</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceEvent(ServiceEvent object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Listener</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Listener</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceListener(ServiceListener object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Service Registry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServiceRegistry(ServiceRegistry object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Local Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Local Service Registry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseLocalServiceRegistry(LocalServiceRegistry object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Remote Service Registry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Remote Service Registry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRemoteServiceRegistry(RemoteServiceRegistry object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Consumer Capability</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Consumer Capability</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseConsumerCapability(ConsumerCapability object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Publish Hook</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Publish Hook</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T casePublishHook(PublishHook object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Discovery Hook</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Discovery Hook</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDiscoveryHook(DiscoveryHook object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Distribution Hook</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Distribution Hook</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDistributionHook(DistributionHook object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	@Override
	public T defaultCase(EObject object) {
		return null;
	}

} //ServicesSwitch
