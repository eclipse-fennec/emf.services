/*
 */
package org.eclipse.fennec.services;


import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EOperation;
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
 * Gecko DDSR — Dynamic Distributed Service Registry. Language-neutral model of an OSGi-inspired service registry that spans Java, TypeScript, and Python. Combines OSGi DS (declarative components, references, lifecycle) with OSGi service-registry events and a UDDI-style central Broker (Remote Registry). The model is the contract between Java and TypeScript implementations — see REQUIREMENTS.md NFR-Behavioral-Parity.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.services.ServicesFactory
 * @model kind="package"
 *        annotation="Version value='1.0'"
 *        annotation="http://www.eclipse.org/emf/2002/GenModel complianceLevel='17.0' oSGiCompatible='true' basePackage='org.gecko.ddsr.model' resource='XMI'"
 *        annotation="http://www.eclipse.org/emf/2002/Ecore settingDelegates='http://www.eclipse.org/fennec/m2x/ocl/1.0' validationDelegates='http://www.eclipse.org/fennec/m2x/ocl/1.0'"
 * @generated
 */
@ProviderType
@EPackage(uri = ServicesPackage.eNS_URI, fingerprint = "fp1:d9a313ca5ad7c9c901ef6a44b2ca0b3174fe9b2bb6ecd06abf0712f01ebd4e38", genModel = "/model/services.genmodel", genModelSourceLocations = {"model/services.genmodel","org.eclipse.fennec.services.model/model/services.genmodel"}, ecore = "/model/services.ecore", ecoreSourceLocations = "/model/services.ecore")
public interface ServicesPackage extends org.eclipse.emf.ecore.EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "services";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://eclipse.org/fennec/services/1.0";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "services";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	ServicesPackage eINSTANCE = org.eclipse.fennec.services.impl.ServicesPackageImpl.init();

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.NamedElement <em>Named Element</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.NamedElement
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getNamedElement()
	 * @generated
	 */
	int NAMED_ELEMENT = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NAMED_ELEMENT__NAME = 0;

	/**
	 * The number of structural features of the '<em>Named Element</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NAMED_ELEMENT_FEATURE_COUNT = 1;

	/**
	 * The number of operations of the '<em>Named Element</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NAMED_ELEMENT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.VersionedElement <em>Versioned Element</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.VersionedElement
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getVersionedElement()
	 * @generated
	 */
	int VERSIONED_ELEMENT = 1;

	/**
	 * The feature id for the '<em><b>Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int VERSIONED_ELEMENT__VERSION = 0;

	/**
	 * The number of structural features of the '<em>Versioned Element</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int VERSIONED_ELEMENT_FEATURE_COUNT = 1;

	/**
	 * The number of operations of the '<em>Versioned Element</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int VERSIONED_ELEMENT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.PropertyImpl <em>Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.PropertyImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getProperty()
	 * @generated
	 */
	int PROPERTY = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The number of structural features of the '<em>Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.StringPropertyImpl <em>String Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.StringPropertyImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getStringProperty()
	 * @generated
	 */
	int STRING_PROPERTY = 3;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_PROPERTY__NAME = PROPERTY__NAME;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_PROPERTY__VALUE = PROPERTY_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>String Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_PROPERTY_FEATURE_COUNT = PROPERTY_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>String Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_PROPERTY_OPERATION_COUNT = PROPERTY_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.IntPropertyImpl <em>Int Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.IntPropertyImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getIntProperty()
	 * @generated
	 */
	int INT_PROPERTY = 4;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INT_PROPERTY__NAME = PROPERTY__NAME;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INT_PROPERTY__VALUE = PROPERTY_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Int Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INT_PROPERTY_FEATURE_COUNT = PROPERTY_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>Int Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INT_PROPERTY_OPERATION_COUNT = PROPERTY_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.LongPropertyImpl <em>Long Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.LongPropertyImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getLongProperty()
	 * @generated
	 */
	int LONG_PROPERTY = 5;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LONG_PROPERTY__NAME = PROPERTY__NAME;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LONG_PROPERTY__VALUE = PROPERTY_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Long Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LONG_PROPERTY_FEATURE_COUNT = PROPERTY_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>Long Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LONG_PROPERTY_OPERATION_COUNT = PROPERTY_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.DoublePropertyImpl <em>Double Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.DoublePropertyImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDoubleProperty()
	 * @generated
	 */
	int DOUBLE_PROPERTY = 6;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOUBLE_PROPERTY__NAME = PROPERTY__NAME;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOUBLE_PROPERTY__VALUE = PROPERTY_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Double Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOUBLE_PROPERTY_FEATURE_COUNT = PROPERTY_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>Double Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOUBLE_PROPERTY_OPERATION_COUNT = PROPERTY_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.FloatPropertyImpl <em>Float Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.FloatPropertyImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getFloatProperty()
	 * @generated
	 */
	int FLOAT_PROPERTY = 7;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FLOAT_PROPERTY__NAME = PROPERTY__NAME;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FLOAT_PROPERTY__VALUE = PROPERTY_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Float Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FLOAT_PROPERTY_FEATURE_COUNT = PROPERTY_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>Float Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FLOAT_PROPERTY_OPERATION_COUNT = PROPERTY_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ShortPropertyImpl <em>Short Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ShortPropertyImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getShortProperty()
	 * @generated
	 */
	int SHORT_PROPERTY = 8;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SHORT_PROPERTY__NAME = PROPERTY__NAME;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SHORT_PROPERTY__VALUE = PROPERTY_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Short Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SHORT_PROPERTY_FEATURE_COUNT = PROPERTY_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>Short Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SHORT_PROPERTY_OPERATION_COUNT = PROPERTY_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.BoolPropertyImpl <em>Bool Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.BoolPropertyImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getBoolProperty()
	 * @generated
	 */
	int BOOL_PROPERTY = 9;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BOOL_PROPERTY__NAME = PROPERTY__NAME;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BOOL_PROPERTY__VALUE = PROPERTY_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Bool Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BOOL_PROPERTY_FEATURE_COUNT = PROPERTY_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>Bool Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BOOL_PROPERTY_OPERATION_COUNT = PROPERTY_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.StringListPropertyImpl <em>String List Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.StringListPropertyImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getStringListProperty()
	 * @generated
	 */
	int STRING_LIST_PROPERTY = 10;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_LIST_PROPERTY__NAME = PROPERTY__NAME;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_LIST_PROPERTY__VALUE = PROPERTY_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>String List Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_LIST_PROPERTY_FEATURE_COUNT = PROPERTY_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>String List Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_LIST_PROPERTY_OPERATION_COUNT = PROPERTY_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceOperationImpl <em>Service Operation</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceOperationImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceOperation()
	 * @generated
	 */
	int SERVICE_OPERATION = 11;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION__DESCRIPTION = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Parameters</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION__PARAMETERS = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Return Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION__RETURN_TYPE = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Return Constraints</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION__RETURN_CONSTRAINTS = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Exceptions</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION__EXCEPTIONS = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Preconditions</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION__PRECONDITIONS = NAMED_ELEMENT_FEATURE_COUNT + 5;

	/**
	 * The feature id for the '<em><b>Postconditions</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION__POSTCONDITIONS = NAMED_ELEMENT_FEATURE_COUNT + 6;

	/**
	 * The number of structural features of the '<em>Service Operation</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 7;

	/**
	 * The number of operations of the '<em>Service Operation</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ParameterImpl <em>Parameter</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ParameterImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getParameter()
	 * @generated
	 */
	int PARAMETER = 12;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Index</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER__INDEX = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER__TYPE = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Optional</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER__OPTIONAL = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Default Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER__DEFAULT_VALUE = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER__DESCRIPTION = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Constraints</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER__CONSTRAINTS = NAMED_ELEMENT_FEATURE_COUNT + 5;

	/**
	 * The number of structural features of the '<em>Parameter</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 6;

	/**
	 * The number of operations of the '<em>Parameter</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ParameterConstraintImpl <em>Parameter Constraint</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ParameterConstraintImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getParameterConstraint()
	 * @generated
	 */
	int PARAMETER_CONSTRAINT = 13;

	/**
	 * The number of structural features of the '<em>Parameter Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_CONSTRAINT_FEATURE_COUNT = 0;

	/**
	 * The number of operations of the '<em>Parameter Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_CONSTRAINT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.RequiredConstraintImpl <em>Required Constraint</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.RequiredConstraintImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRequiredConstraint()
	 * @generated
	 */
	int REQUIRED_CONSTRAINT = 14;

	/**
	 * The number of structural features of the '<em>Required Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUIRED_CONSTRAINT_FEATURE_COUNT = PARAMETER_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Required Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUIRED_CONSTRAINT_OPERATION_COUNT = PARAMETER_CONSTRAINT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.NumericRangeConstraintImpl <em>Numeric Range Constraint</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.NumericRangeConstraintImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getNumericRangeConstraint()
	 * @generated
	 */
	int NUMERIC_RANGE_CONSTRAINT = 15;

	/**
	 * The feature id for the '<em><b>Min</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NUMERIC_RANGE_CONSTRAINT__MIN = PARAMETER_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Max</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NUMERIC_RANGE_CONSTRAINT__MAX = PARAMETER_CONSTRAINT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Inclusive Min</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MIN = PARAMETER_CONSTRAINT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Inclusive Max</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MAX = PARAMETER_CONSTRAINT_FEATURE_COUNT + 3;

	/**
	 * The number of structural features of the '<em>Numeric Range Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NUMERIC_RANGE_CONSTRAINT_FEATURE_COUNT = PARAMETER_CONSTRAINT_FEATURE_COUNT + 4;

	/**
	 * The number of operations of the '<em>Numeric Range Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NUMERIC_RANGE_CONSTRAINT_OPERATION_COUNT = PARAMETER_CONSTRAINT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.StringPatternConstraintImpl <em>String Pattern Constraint</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.StringPatternConstraintImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getStringPatternConstraint()
	 * @generated
	 */
	int STRING_PATTERN_CONSTRAINT = 16;

	/**
	 * The feature id for the '<em><b>Pattern</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_PATTERN_CONSTRAINT__PATTERN = PARAMETER_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Min Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_PATTERN_CONSTRAINT__MIN_LENGTH = PARAMETER_CONSTRAINT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Max Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_PATTERN_CONSTRAINT__MAX_LENGTH = PARAMETER_CONSTRAINT_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>String Pattern Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_PATTERN_CONSTRAINT_FEATURE_COUNT = PARAMETER_CONSTRAINT_FEATURE_COUNT + 3;

	/**
	 * The number of operations of the '<em>String Pattern Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRING_PATTERN_CONSTRAINT_OPERATION_COUNT = PARAMETER_CONSTRAINT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.EnumerationConstraintImpl <em>Enumeration Constraint</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.EnumerationConstraintImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getEnumerationConstraint()
	 * @generated
	 */
	int ENUMERATION_CONSTRAINT = 17;

	/**
	 * The feature id for the '<em><b>Allowed Values</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUMERATION_CONSTRAINT__ALLOWED_VALUES = PARAMETER_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Enumeration Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUMERATION_CONSTRAINT_FEATURE_COUNT = PARAMETER_CONSTRAINT_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>Enumeration Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUMERATION_CONSTRAINT_OPERATION_COUNT = PARAMETER_CONSTRAINT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ExpressionConstraintImpl <em>Expression Constraint</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ExpressionConstraintImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getExpressionConstraint()
	 * @generated
	 */
	int EXPRESSION_CONSTRAINT = 18;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXPRESSION_CONSTRAINT__NAME = PARAMETER_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Language</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXPRESSION_CONSTRAINT__LANGUAGE = PARAMETER_CONSTRAINT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Expression</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXPRESSION_CONSTRAINT__EXPRESSION = PARAMETER_CONSTRAINT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Message</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXPRESSION_CONSTRAINT__MESSAGE = PARAMETER_CONSTRAINT_FEATURE_COUNT + 3;

	/**
	 * The number of structural features of the '<em>Expression Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXPRESSION_CONSTRAINT_FEATURE_COUNT = PARAMETER_CONSTRAINT_FEATURE_COUNT + 4;

	/**
	 * The number of operations of the '<em>Expression Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXPRESSION_CONSTRAINT_OPERATION_COUNT = PARAMETER_CONSTRAINT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.InvariantImpl <em>Invariant</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.InvariantImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getInvariant()
	 * @generated
	 */
	int INVARIANT = 19;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INVARIANT__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Language</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INVARIANT__LANGUAGE = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Expression</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INVARIANT__EXPRESSION = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Message</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INVARIANT__MESSAGE = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>Invariant</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INVARIANT_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The number of operations of the '<em>Invariant</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INVARIANT_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.CollectionSizeConstraintImpl <em>Collection Size Constraint</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.CollectionSizeConstraintImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getCollectionSizeConstraint()
	 * @generated
	 */
	int COLLECTION_SIZE_CONSTRAINT = 20;

	/**
	 * The feature id for the '<em><b>Min Size</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COLLECTION_SIZE_CONSTRAINT__MIN_SIZE = PARAMETER_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Max Size</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COLLECTION_SIZE_CONSTRAINT__MAX_SIZE = PARAMETER_CONSTRAINT_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Collection Size Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COLLECTION_SIZE_CONSTRAINT_FEATURE_COUNT = PARAMETER_CONSTRAINT_FEATURE_COUNT + 2;

	/**
	 * The number of operations of the '<em>Collection Size Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COLLECTION_SIZE_CONSTRAINT_OPERATION_COUNT = PARAMETER_CONSTRAINT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceExceptionImpl <em>Service Exception</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceExceptionImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceException()
	 * @generated
	 */
	int SERVICE_EXCEPTION = 21;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EXCEPTION__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EXCEPTION__VERSION = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EXCEPTION__DESCRIPTION = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EXCEPTION__TYPE = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EXCEPTION__PROPERTIES = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The number of structural features of the '<em>Service Exception</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EXCEPTION_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The number of operations of the '<em>Service Exception</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EXCEPTION_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl <em>Service Interface</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceInterfaceImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceInterface()
	 * @generated
	 */
	int SERVICE_INTERFACE = 22;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE__VERSION = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE__DESCRIPTION = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Operations</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE__OPERATIONS = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Exceptions</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE__EXCEPTIONS = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Invariants</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE__INVARIANTS = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE__STATUS = NAMED_ELEMENT_FEATURE_COUNT + 5;

	/**
	 * The feature id for the '<em><b>Deprecation Reason</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE__DEPRECATION_REASON = NAMED_ELEMENT_FEATURE_COUNT + 6;

	/**
	 * The feature id for the '<em><b>Replaced By</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE__REPLACED_BY = NAMED_ELEMENT_FEATURE_COUNT + 7;

	/**
	 * The number of structural features of the '<em>Service Interface</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 8;

	/**
	 * The number of operations of the '<em>Service Interface</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_INTERFACE_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.LifecycleHookImpl <em>Lifecycle Hook</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.LifecycleHookImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getLifecycleHook()
	 * @generated
	 */
	int LIFECYCLE_HOOK = 23;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIFECYCLE_HOOK__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIFECYCLE_HOOK__KIND = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Parameter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIFECYCLE_HOOK__PARAMETER = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Lifecycle Hook</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIFECYCLE_HOOK_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The number of operations of the '<em>Lifecycle Hook</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIFECYCLE_HOOK_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ReferenceBindingImpl <em>Reference Binding</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ReferenceBindingImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferenceBinding()
	 * @generated
	 */
	int REFERENCE_BINDING = 24;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_BINDING__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_BINDING__KIND = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Field Option</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_BINDING__FIELD_OPTION = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Reference Binding</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_BINDING_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The number of operations of the '<em>Reference Binding</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_BINDING_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl <em>Component Reference</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ComponentReferenceImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getComponentReference()
	 * @generated
	 */
	int COMPONENT_REFERENCE = 25;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Interface Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__INTERFACE_NAME = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Cardinality</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__CARDINALITY = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Policy</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__POLICY = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Policy Option</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__POLICY_OPTION = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Target</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__TARGET = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Scope</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__SCOPE = NAMED_ELEMENT_FEATURE_COUNT + 5;

	/**
	 * The feature id for the '<em><b>Collection Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__COLLECTION_TYPE = NAMED_ELEMENT_FEATURE_COUNT + 6;

	/**
	 * The feature id for the '<em><b>Parameter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__PARAMETER = NAMED_ELEMENT_FEATURE_COUNT + 7;

	/**
	 * The feature id for the '<em><b>Bindings</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE__BINDINGS = NAMED_ELEMENT_FEATURE_COUNT + 8;

	/**
	 * The number of structural features of the '<em>Component Reference</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 9;

	/**
	 * The number of operations of the '<em>Component Reference</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_REFERENCE_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl <em>Component Description</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ComponentDescriptionImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getComponentDescription()
	 * @generated
	 */
	int COMPONENT_DESCRIPTION = 26;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Factory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__FACTORY = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Scope</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__SCOPE = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Implementation Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__IMPLEMENTATION_ID = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Default Enabled</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__DEFAULT_ENABLED = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Immediate</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__IMMEDIATE = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Configuration Policy</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__CONFIGURATION_POLICY = NAMED_ELEMENT_FEATURE_COUNT + 5;

	/**
	 * The feature id for the '<em><b>Configuration Pid</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__CONFIGURATION_PID = NAMED_ELEMENT_FEATURE_COUNT + 6;

	/**
	 * The feature id for the '<em><b>Service Interfaces</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__SERVICE_INTERFACES = NAMED_ELEMENT_FEATURE_COUNT + 7;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__PROPERTIES = NAMED_ELEMENT_FEATURE_COUNT + 8;

	/**
	 * The feature id for the '<em><b>Factory Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__FACTORY_PROPERTIES = NAMED_ELEMENT_FEATURE_COUNT + 9;

	/**
	 * The feature id for the '<em><b>References</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__REFERENCES = NAMED_ELEMENT_FEATURE_COUNT + 10;

	/**
	 * The feature id for the '<em><b>Lifecycle Hooks</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__LIFECYCLE_HOOKS = NAMED_ELEMENT_FEATURE_COUNT + 11;

	/**
	 * The feature id for the '<em><b>Provider</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION__PROVIDER = NAMED_ELEMENT_FEATURE_COUNT + 12;

	/**
	 * The number of structural features of the '<em>Component Description</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 13;

	/**
	 * The number of operations of the '<em>Component Description</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_DESCRIPTION_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceProviderImpl <em>Service Provider</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceProviderImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceProvider()
	 * @generated
	 */
	int SERVICE_PROVIDER = 27;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_PROVIDER__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_PROVIDER__VERSION = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Symbolic Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_PROVIDER__SYMBOLIC_NAME = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Descriptions</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_PROVIDER__DESCRIPTIONS = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Implementations</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_PROVIDER__IMPLEMENTATIONS = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The number of structural features of the '<em>Service Provider</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_PROVIDER_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The number of operations of the '<em>Service Provider</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_PROVIDER_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl <em>Service Implementation</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceImplementationImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceImplementation()
	 * @generated
	 */
	int SERVICE_IMPLEMENTATION = 28;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION__VERSION = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION__DESCRIPTION = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Implementation Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION__IMPLEMENTATION_ID = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Service Interfaces</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION__SERVICE_INTERFACES = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Flavors</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION__FLAVORS = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION__PROPERTIES = NAMED_ELEMENT_FEATURE_COUNT + 5;

	/**
	 * The feature id for the '<em><b>Component Description</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION__COMPONENT_DESCRIPTION = NAMED_ELEMENT_FEATURE_COUNT + 6;

	/**
	 * The number of structural features of the '<em>Service Implementation</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 7;

	/**
	 * The number of operations of the '<em>Service Implementation</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_IMPLEMENTATION_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceFlavorImpl <em>Service Flavor</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceFlavorImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceFlavor()
	 * @generated
	 */
	int SERVICE_FLAVOR = 29;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_FLAVOR__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_FLAVOR__KIND = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Operation Flavors</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_FLAVOR__OPERATION_FLAVORS = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Service Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_FLAVOR_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The number of operations of the '<em>Service Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_FLAVOR_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.RestFlavorImpl <em>Rest Flavor</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.RestFlavorImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRestFlavor()
	 * @generated
	 */
	int REST_FLAVOR = 30;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_FLAVOR__NAME = SERVICE_FLAVOR__NAME;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_FLAVOR__KIND = SERVICE_FLAVOR__KIND;

	/**
	 * The feature id for the '<em><b>Operation Flavors</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_FLAVOR__OPERATION_FLAVORS = SERVICE_FLAVOR__OPERATION_FLAVORS;

	/**
	 * The feature id for the '<em><b>Host</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_FLAVOR__HOST = SERVICE_FLAVOR_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Base Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_FLAVOR__BASE_PATH = SERVICE_FLAVOR_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Content Types</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_FLAVOR__CONTENT_TYPES = SERVICE_FLAVOR_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>Rest Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_FLAVOR_FEATURE_COUNT = SERVICE_FLAVOR_FEATURE_COUNT + 3;

	/**
	 * The number of operations of the '<em>Rest Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_FLAVOR_OPERATION_COUNT = SERVICE_FLAVOR_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.MqttFlavorImpl <em>Mqtt Flavor</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.MqttFlavorImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getMqttFlavor()
	 * @generated
	 */
	int MQTT_FLAVOR = 31;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR__NAME = SERVICE_FLAVOR__NAME;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR__KIND = SERVICE_FLAVOR__KIND;

	/**
	 * The feature id for the '<em><b>Operation Flavors</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR__OPERATION_FLAVORS = SERVICE_FLAVOR__OPERATION_FLAVORS;

	/**
	 * The feature id for the '<em><b>Brokers</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR__BROKERS = SERVICE_FLAVOR_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Request Topic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR__REQUEST_TOPIC = SERVICE_FLAVOR_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Response Topic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR__RESPONSE_TOPIC = SERVICE_FLAVOR_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Default Qos</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR__DEFAULT_QOS = SERVICE_FLAVOR_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Default Retained</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR__DEFAULT_RETAINED = SERVICE_FLAVOR_FEATURE_COUNT + 4;

	/**
	 * The number of structural features of the '<em>Mqtt Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR_FEATURE_COUNT = SERVICE_FLAVOR_FEATURE_COUNT + 5;

	/**
	 * The number of operations of the '<em>Mqtt Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_FLAVOR_OPERATION_COUNT = SERVICE_FLAVOR_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceOperationFlavorImpl <em>Service Operation Flavor</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceOperationFlavorImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceOperationFlavor()
	 * @generated
	 */
	int SERVICE_OPERATION_FLAVOR = 32;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION_FLAVOR__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Operation</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION_FLAVOR__OPERATION = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Consumes</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION_FLAVOR__CONSUMES = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Produces</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION_FLAVOR__PRODUCES = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>Service Operation Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION_FLAVOR_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The number of operations of the '<em>Service Operation Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_OPERATION_FLAVOR_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.RestOperationFlavorImpl <em>Rest Operation Flavor</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.RestOperationFlavorImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRestOperationFlavor()
	 * @generated
	 */
	int REST_OPERATION_FLAVOR = 33;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_OPERATION_FLAVOR__NAME = SERVICE_OPERATION_FLAVOR__NAME;

	/**
	 * The feature id for the '<em><b>Operation</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_OPERATION_FLAVOR__OPERATION = SERVICE_OPERATION_FLAVOR__OPERATION;

	/**
	 * The feature id for the '<em><b>Consumes</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_OPERATION_FLAVOR__CONSUMES = SERVICE_OPERATION_FLAVOR__CONSUMES;

	/**
	 * The feature id for the '<em><b>Produces</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_OPERATION_FLAVOR__PRODUCES = SERVICE_OPERATION_FLAVOR__PRODUCES;

	/**
	 * The feature id for the '<em><b>Method</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_OPERATION_FLAVOR__METHOD = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_OPERATION_FLAVOR__PATH = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Return Codes</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_OPERATION_FLAVOR__RETURN_CODES = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>Rest Operation Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_OPERATION_FLAVOR_FEATURE_COUNT = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 3;

	/**
	 * The number of operations of the '<em>Rest Operation Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REST_OPERATION_FLAVOR_OPERATION_COUNT = SERVICE_OPERATION_FLAVOR_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.MqttOperationFlavorImpl <em>Mqtt Operation Flavor</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.MqttOperationFlavorImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getMqttOperationFlavor()
	 * @generated
	 */
	int MQTT_OPERATION_FLAVOR = 34;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__NAME = SERVICE_OPERATION_FLAVOR__NAME;

	/**
	 * The feature id for the '<em><b>Operation</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__OPERATION = SERVICE_OPERATION_FLAVOR__OPERATION;

	/**
	 * The feature id for the '<em><b>Consumes</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__CONSUMES = SERVICE_OPERATION_FLAVOR__CONSUMES;

	/**
	 * The feature id for the '<em><b>Produces</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__PRODUCES = SERVICE_OPERATION_FLAVOR__PRODUCES;

	/**
	 * The feature id for the '<em><b>Request Topic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__REQUEST_TOPIC = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Response Topic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__RESPONSE_TOPIC = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Qos</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__QOS = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Retained</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__RETAINED = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Correlation</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__CORRELATION = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Return Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR__RETURN_PATH = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 5;

	/**
	 * The number of structural features of the '<em>Mqtt Operation Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR_FEATURE_COUNT = SERVICE_OPERATION_FLAVOR_FEATURE_COUNT + 6;

	/**
	 * The number of operations of the '<em>Mqtt Operation Flavor</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MQTT_OPERATION_FLAVOR_OPERATION_COUNT = SERVICE_OPERATION_FLAVOR_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceReferenceImpl <em>Service Reference</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceReferenceImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceReference()
	 * @generated
	 */
	int SERVICE_REFERENCE = 35;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REFERENCE__ID = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REFERENCE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Provider</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REFERENCE__PROVIDER = 2;

	/**
	 * The feature id for the '<em><b>Registration</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REFERENCE__REGISTRATION = 3;

	/**
	 * The number of structural features of the '<em>Service Reference</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REFERENCE_FEATURE_COUNT = 4;

	/**
	 * The operation id for the '<em>Get Property</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REFERENCE___GET_PROPERTY__STRING = 0;

	/**
	 * The operation id for the '<em>Get Property Keys</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REFERENCE___GET_PROPERTY_KEYS = 1;

	/**
	 * The number of operations of the '<em>Service Reference</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REFERENCE_OPERATION_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceRegistrationImpl <em>Service Registration</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceRegistrationImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceRegistration()
	 * @generated
	 */
	int SERVICE_REGISTRATION = 36;

	/**
	 * The feature id for the '<em><b>Reference</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION__REFERENCE = 0;

	/**
	 * The feature id for the '<em><b>Unregistered</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION__UNREGISTERED = 1;

	/**
	 * The feature id for the '<em><b>Provider</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION__PROVIDER = 2;

	/**
	 * The feature id for the '<em><b>Implementation</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION__IMPLEMENTATION = 3;

	/**
	 * The feature id for the '<em><b>Using Sessions</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION__USING_SESSIONS = 4;

	/**
	 * The feature id for the '<em><b>Consumer Count</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION__CONSUMER_COUNT = 5;

	/**
	 * The number of structural features of the '<em>Service Registration</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION_FEATURE_COUNT = 6;

	/**
	 * The operation id for the '<em>Unregister</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION___UNREGISTER = 0;

	/**
	 * The operation id for the '<em>Set Properties</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION___SET_PROPERTIES__ELIST = 1;

	/**
	 * The number of operations of the '<em>Service Registration</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRATION_OPERATION_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ConsumerSessionImpl <em>Consumer Session</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ConsumerSessionImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getConsumerSession()
	 * @generated
	 */
	int CONSUMER_SESSION = 37;

	/**
	 * The feature id for the '<em><b>Consumer Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_SESSION__CONSUMER_ID = 0;

	/**
	 * The feature id for the '<em><b>Last Renewal</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_SESSION__LAST_RENEWAL = 1;

	/**
	 * The feature id for the '<em><b>Capabilities</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_SESSION__CAPABILITIES = 2;

	/**
	 * The feature id for the '<em><b>Acquisitions</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_SESSION__ACQUISITIONS = 3;

	/**
	 * The number of structural features of the '<em>Consumer Session</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_SESSION_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Consumer Session</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_SESSION_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl <em>Component Configuration</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ComponentConfigurationImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getComponentConfiguration()
	 * @generated
	 */
	int COMPONENT_CONFIGURATION = 38;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION__ID = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Description</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION__DESCRIPTION = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>State</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION__STATE = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION__PROPERTIES = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Satisfied References</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION__SATISFIED_REFERENCES = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Unsatisfied References</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION__UNSATISFIED_REFERENCES = NAMED_ELEMENT_FEATURE_COUNT + 5;

	/**
	 * The feature id for the '<em><b>Failure</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION__FAILURE = NAMED_ELEMENT_FEATURE_COUNT + 6;

	/**
	 * The feature id for the '<em><b>Service</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION__SERVICE = NAMED_ELEMENT_FEATURE_COUNT + 7;

	/**
	 * The number of structural features of the '<em>Component Configuration</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 8;

	/**
	 * The number of operations of the '<em>Component Configuration</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONFIGURATION_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.SatisfiedReferenceImpl <em>Satisfied Reference</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.SatisfiedReferenceImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getSatisfiedReference()
	 * @generated
	 */
	int SATISFIED_REFERENCE = 39;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SATISFIED_REFERENCE__NAME = 0;

	/**
	 * The feature id for the '<em><b>Target</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SATISFIED_REFERENCE__TARGET = 1;

	/**
	 * The feature id for the '<em><b>Bound Services</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SATISFIED_REFERENCE__BOUND_SERVICES = 2;

	/**
	 * The number of structural features of the '<em>Satisfied Reference</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SATISFIED_REFERENCE_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Satisfied Reference</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SATISFIED_REFERENCE_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.UnsatisfiedReferenceImpl <em>Unsatisfied Reference</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.UnsatisfiedReferenceImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getUnsatisfiedReference()
	 * @generated
	 */
	int UNSATISFIED_REFERENCE = 40;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int UNSATISFIED_REFERENCE__NAME = 0;

	/**
	 * The feature id for the '<em><b>Target</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int UNSATISFIED_REFERENCE__TARGET = 1;

	/**
	 * The feature id for the '<em><b>Target Services</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int UNSATISFIED_REFERENCE__TARGET_SERVICES = 2;

	/**
	 * The number of structural features of the '<em>Unsatisfied Reference</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int UNSATISFIED_REFERENCE_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Unsatisfied Reference</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int UNSATISFIED_REFERENCE_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.DiagnosticImpl <em>Diagnostic</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.DiagnosticImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDiagnostic()
	 * @generated
	 */
	int DIAGNOSTIC = 41;

	/**
	 * The feature id for the '<em><b>Severity</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC__SEVERITY = 0;

	/**
	 * The feature id for the '<em><b>Message</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC__MESSAGE = 1;

	/**
	 * The feature id for the '<em><b>Source</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC__SOURCE = 2;

	/**
	 * The feature id for the '<em><b>Code</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC__CODE = 3;

	/**
	 * The feature id for the '<em><b>Data</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC__DATA = 4;

	/**
	 * The feature id for the '<em><b>Children</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC__CHILDREN = 5;

	/**
	 * The number of structural features of the '<em>Diagnostic</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC_FEATURE_COUNT = 6;

	/**
	 * The number of operations of the '<em>Diagnostic</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceEventImpl <em>Service Event</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceEventImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceEvent()
	 * @generated
	 */
	int SERVICE_EVENT = 42;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EVENT__TYPE = 0;

	/**
	 * The feature id for the '<em><b>Reference</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EVENT__REFERENCE = 1;

	/**
	 * The feature id for the '<em><b>Timestamp</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EVENT__TIMESTAMP = 2;

	/**
	 * The number of structural features of the '<em>Service Event</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EVENT_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Service Event</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_EVENT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ServiceListener <em>Service Listener</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ServiceListener
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceListener()
	 * @generated
	 */
	int SERVICE_LISTENER = 43;

	/**
	 * The feature id for the '<em><b>Filter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_LISTENER__FILTER = 0;

	/**
	 * The number of structural features of the '<em>Service Listener</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_LISTENER_FEATURE_COUNT = 1;

	/**
	 * The operation id for the '<em>Service Changed</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_LISTENER___SERVICE_CHANGED__SERVICEEVENT = 0;

	/**
	 * The number of operations of the '<em>Service Listener</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_LISTENER_OPERATION_COUNT = 1;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ServiceRegistryImpl <em>Service Registry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ServiceRegistryImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceRegistry()
	 * @generated
	 */
	int SERVICE_REGISTRY = 44;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY__NAME = NAMED_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY__KIND = NAMED_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Publish Hooks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY__PUBLISH_HOOKS = NAMED_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Discovery Hooks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY__DISCOVERY_HOOKS = NAMED_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Distribution Hooks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY__DISTRIBUTION_HOOKS = NAMED_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The number of structural features of the '<em>Service Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY_FEATURE_COUNT = NAMED_ELEMENT_FEATURE_COUNT + 4;

	/**
	 * The operation id for the '<em>Get Service Reference</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY___GET_SERVICE_REFERENCE__STRING = NAMED_ELEMENT_OPERATION_COUNT + 0;

	/**
	 * The operation id for the '<em>Get Service References</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY___GET_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY = NAMED_ELEMENT_OPERATION_COUNT + 1;

	/**
	 * The operation id for the '<em>Get All Service References</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY___GET_ALL_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY = NAMED_ELEMENT_OPERATION_COUNT + 2;

	/**
	 * The operation id for the '<em>Add Service Listener</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY___ADD_SERVICE_LISTENER__SERVICELISTENER = NAMED_ELEMENT_OPERATION_COUNT + 3;

	/**
	 * The operation id for the '<em>Remove Service Listener</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY___REMOVE_SERVICE_LISTENER__SERVICELISTENER = NAMED_ELEMENT_OPERATION_COUNT + 4;

	/**
	 * The number of operations of the '<em>Service Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVICE_REGISTRY_OPERATION_COUNT = NAMED_ELEMENT_OPERATION_COUNT + 5;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl <em>Local Service Registry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.LocalServiceRegistryImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getLocalServiceRegistry()
	 * @generated
	 */
	int LOCAL_SERVICE_REGISTRY = 45;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__NAME = SERVICE_REGISTRY__NAME;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__KIND = SERVICE_REGISTRY__KIND;

	/**
	 * The feature id for the '<em><b>Publish Hooks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__PUBLISH_HOOKS = SERVICE_REGISTRY__PUBLISH_HOOKS;

	/**
	 * The feature id for the '<em><b>Discovery Hooks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__DISCOVERY_HOOKS = SERVICE_REGISTRY__DISCOVERY_HOOKS;

	/**
	 * The feature id for the '<em><b>Distribution Hooks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__DISTRIBUTION_HOOKS = SERVICE_REGISTRY__DISTRIBUTION_HOOKS;

	/**
	 * The feature id for the '<em><b>References</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__REFERENCES = SERVICE_REGISTRY_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Registrations</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__REGISTRATIONS = SERVICE_REGISTRY_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Sessions</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__SESSIONS = SERVICE_REGISTRY_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Configurations</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__CONFIGURATIONS = SERVICE_REGISTRY_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Providers</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__PROVIDERS = SERVICE_REGISTRY_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Listeners</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__LISTENERS = SERVICE_REGISTRY_FEATURE_COUNT + 5;

	/**
	 * The feature id for the '<em><b>Remote</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__REMOTE = SERVICE_REGISTRY_FEATURE_COUNT + 6;

	/**
	 * The feature id for the '<em><b>Connection State</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY__CONNECTION_STATE = SERVICE_REGISTRY_FEATURE_COUNT + 7;

	/**
	 * The number of structural features of the '<em>Local Service Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY_FEATURE_COUNT = SERVICE_REGISTRY_FEATURE_COUNT + 8;

	/**
	 * The operation id for the '<em>Get Service Reference</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY___GET_SERVICE_REFERENCE__STRING = SERVICE_REGISTRY___GET_SERVICE_REFERENCE__STRING;

	/**
	 * The operation id for the '<em>Get Service References</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY___GET_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY = SERVICE_REGISTRY___GET_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY;

	/**
	 * The operation id for the '<em>Get All Service References</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY___GET_ALL_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY = SERVICE_REGISTRY___GET_ALL_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY;

	/**
	 * The operation id for the '<em>Add Service Listener</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY___ADD_SERVICE_LISTENER__SERVICELISTENER = SERVICE_REGISTRY___ADD_SERVICE_LISTENER__SERVICELISTENER;

	/**
	 * The operation id for the '<em>Remove Service Listener</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY___REMOVE_SERVICE_LISTENER__SERVICELISTENER = SERVICE_REGISTRY___REMOVE_SERVICE_LISTENER__SERVICELISTENER;

	/**
	 * The operation id for the '<em>Register Service</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY___REGISTER_SERVICE__SERVICEPROVIDER_SERVICEIMPLEMENTATION_ELIST = SERVICE_REGISTRY_OPERATION_COUNT + 0;

	/**
	 * The operation id for the '<em>Fire Service Event</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY___FIRE_SERVICE_EVENT__SERVICEEVENT = SERVICE_REGISTRY_OPERATION_COUNT + 1;

	/**
	 * The number of operations of the '<em>Local Service Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LOCAL_SERVICE_REGISTRY_OPERATION_COUNT = SERVICE_REGISTRY_OPERATION_COUNT + 2;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.RemoteServiceRegistryImpl <em>Remote Service Registry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.RemoteServiceRegistryImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRemoteServiceRegistry()
	 * @generated
	 */
	int REMOTE_SERVICE_REGISTRY = 46;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY__NAME = SERVICE_REGISTRY__NAME;

	/**
	 * The feature id for the '<em><b>Kind</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY__KIND = SERVICE_REGISTRY__KIND;

	/**
	 * The feature id for the '<em><b>Publish Hooks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY__PUBLISH_HOOKS = SERVICE_REGISTRY__PUBLISH_HOOKS;

	/**
	 * The feature id for the '<em><b>Discovery Hooks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY__DISCOVERY_HOOKS = SERVICE_REGISTRY__DISCOVERY_HOOKS;

	/**
	 * The feature id for the '<em><b>Distribution Hooks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY__DISTRIBUTION_HOOKS = SERVICE_REGISTRY__DISTRIBUTION_HOOKS;

	/**
	 * The feature id for the '<em><b>Endpoint</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY__ENDPOINT = SERVICE_REGISTRY_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Catalog</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY__CATALOG = SERVICE_REGISTRY_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Implementations</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY__IMPLEMENTATIONS = SERVICE_REGISTRY_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Providers</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY__PROVIDERS = SERVICE_REGISTRY_FEATURE_COUNT + 3;

	/**
	 * The number of structural features of the '<em>Remote Service Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY_FEATURE_COUNT = SERVICE_REGISTRY_FEATURE_COUNT + 4;

	/**
	 * The operation id for the '<em>Get Service Reference</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___GET_SERVICE_REFERENCE__STRING = SERVICE_REGISTRY___GET_SERVICE_REFERENCE__STRING;

	/**
	 * The operation id for the '<em>Get Service References</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___GET_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY = SERVICE_REGISTRY___GET_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY;

	/**
	 * The operation id for the '<em>Get All Service References</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___GET_ALL_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY = SERVICE_REGISTRY___GET_ALL_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY;

	/**
	 * The operation id for the '<em>Add Service Listener</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___ADD_SERVICE_LISTENER__SERVICELISTENER = SERVICE_REGISTRY___ADD_SERVICE_LISTENER__SERVICELISTENER;

	/**
	 * The operation id for the '<em>Remove Service Listener</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___REMOVE_SERVICE_LISTENER__SERVICELISTENER = SERVICE_REGISTRY___REMOVE_SERVICE_LISTENER__SERVICELISTENER;

	/**
	 * The operation id for the '<em>Publish Implementation</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___PUBLISH_IMPLEMENTATION__SERVICEPROVIDER_SERVICEIMPLEMENTATION = SERVICE_REGISTRY_OPERATION_COUNT + 0;

	/**
	 * The operation id for the '<em>Withdraw Implementation</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___WITHDRAW_IMPLEMENTATION__SERVICEPROVIDER_SERVICEIMPLEMENTATION = SERVICE_REGISTRY_OPERATION_COUNT + 1;

	/**
	 * The operation id for the '<em>Add Catalog Entry</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___ADD_CATALOG_ENTRY__SERVICEINTERFACE_STRING = SERVICE_REGISTRY_OPERATION_COUNT + 2;

	/**
	 * The operation id for the '<em>Deprecate Catalog Entry</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___DEPRECATE_CATALOG_ENTRY__SERVICEINTERFACE_STRING = SERVICE_REGISTRY_OPERATION_COUNT + 3;

	/**
	 * The operation id for the '<em>Remove Catalog Entry</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY___REMOVE_CATALOG_ENTRY__SERVICEINTERFACE_STRING = SERVICE_REGISTRY_OPERATION_COUNT + 4;

	/**
	 * The number of operations of the '<em>Remote Service Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REMOTE_SERVICE_REGISTRY_OPERATION_COUNT = SERVICE_REGISTRY_OPERATION_COUNT + 5;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.impl.ConsumerCapabilityImpl <em>Consumer Capability</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.impl.ConsumerCapabilityImpl
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getConsumerCapability()
	 * @generated
	 */
	int CONSUMER_CAPABILITY = 47;

	/**
	 * The feature id for the '<em><b>Consumer Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_CAPABILITY__CONSUMER_ID = 0;

	/**
	 * The feature id for the '<em><b>Supported Flavors</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_CAPABILITY__SUPPORTED_FLAVORS = 1;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_CAPABILITY__PROPERTIES = 2;

	/**
	 * The number of structural features of the '<em>Consumer Capability</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_CAPABILITY_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Consumer Capability</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSUMER_CAPABILITY_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.PublishHook <em>Publish Hook</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.PublishHook
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getPublishHook()
	 * @generated
	 */
	int PUBLISH_HOOK = 48;

	/**
	 * The number of structural features of the '<em>Publish Hook</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PUBLISH_HOOK_FEATURE_COUNT = 0;

	/**
	 * The operation id for the '<em>On Publish</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PUBLISH_HOOK___ON_PUBLISH__SERVICEPROVIDER_SERVICEIMPLEMENTATION = 0;

	/**
	 * The operation id for the '<em>On Withdraw</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PUBLISH_HOOK___ON_WITHDRAW__SERVICEPROVIDER_SERVICEIMPLEMENTATION = 1;

	/**
	 * The number of operations of the '<em>Publish Hook</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PUBLISH_HOOK_OPERATION_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.DiscoveryHook <em>Discovery Hook</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.DiscoveryHook
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDiscoveryHook()
	 * @generated
	 */
	int DISCOVERY_HOOK = 49;

	/**
	 * The number of structural features of the '<em>Discovery Hook</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DISCOVERY_HOOK_FEATURE_COUNT = 0;

	/**
	 * The operation id for the '<em>On Lookup</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DISCOVERY_HOOK___ON_LOOKUP__STRING_STRING_CONSUMERCAPABILITY = 0;

	/**
	 * The operation id for the '<em>Filter References</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DISCOVERY_HOOK___FILTER_REFERENCES__CONSUMERCAPABILITY_ELIST = 1;

	/**
	 * The operation id for the '<em>On Subscribe</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DISCOVERY_HOOK___ON_SUBSCRIBE__SERVICELISTENER = 2;

	/**
	 * The number of operations of the '<em>Discovery Hook</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DISCOVERY_HOOK_OPERATION_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.DistributionHook <em>Distribution Hook</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.DistributionHook
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDistributionHook()
	 * @generated
	 */
	int DISTRIBUTION_HOOK = 50;

	/**
	 * The number of structural features of the '<em>Distribution Hook</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DISTRIBUTION_HOOK_FEATURE_COUNT = 0;

	/**
	 * The operation id for the '<em>On Outbound</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DISTRIBUTION_HOOK___ON_OUTBOUND__SERVICEEVENT = 0;

	/**
	 * The operation id for the '<em>On Inbound</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DISTRIBUTION_HOOK___ON_INBOUND__SERVICEEVENT = 1;

	/**
	 * The number of operations of the '<em>Distribution Hook</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DISTRIBUTION_HOOK_OPERATION_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ServiceScope <em>Service Scope</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ServiceScope
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceScope()
	 * @generated
	 */
	int SERVICE_SCOPE = 51;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ReferenceCardinality <em>Reference Cardinality</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ReferenceCardinality
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferenceCardinality()
	 * @generated
	 */
	int REFERENCE_CARDINALITY = 52;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ReferencePolicy <em>Reference Policy</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ReferencePolicy
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferencePolicy()
	 * @generated
	 */
	int REFERENCE_POLICY = 53;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ReferencePolicyOption <em>Reference Policy Option</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ReferencePolicyOption
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferencePolicyOption()
	 * @generated
	 */
	int REFERENCE_POLICY_OPTION = 54;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ConfigurationPolicy <em>Configuration Policy</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ConfigurationPolicy
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getConfigurationPolicy()
	 * @generated
	 */
	int CONFIGURATION_POLICY = 55;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ComponentState <em>Component State</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ComponentState
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getComponentState()
	 * @generated
	 */
	int COMPONENT_STATE = 56;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ServiceEventType <em>Service Event Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ServiceEventType
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceEventType()
	 * @generated
	 */
	int SERVICE_EVENT_TYPE = 57;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.FieldOption <em>Field Option</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.FieldOption
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getFieldOption()
	 * @generated
	 */
	int FIELD_OPTION = 58;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.CollectionType <em>Collection Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.CollectionType
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getCollectionType()
	 * @generated
	 */
	int COLLECTION_TYPE = 59;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.LifecycleHookKind <em>Lifecycle Hook Kind</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.LifecycleHookKind
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getLifecycleHookKind()
	 * @generated
	 */
	int LIFECYCLE_HOOK_KIND = 60;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ReferenceBindingKind <em>Reference Binding Kind</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ReferenceBindingKind
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferenceBindingKind()
	 * @generated
	 */
	int REFERENCE_BINDING_KIND = 61;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.DiagnosticSeverity <em>Diagnostic Severity</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.DiagnosticSeverity
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDiagnosticSeverity()
	 * @generated
	 */
	int DIAGNOSTIC_SEVERITY = 62;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.FlavorKind <em>Flavor Kind</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.FlavorKind
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getFlavorKind()
	 * @generated
	 */
	int FLAVOR_KIND = 63;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.HttpMethod <em>Http Method</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.HttpMethod
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getHttpMethod()
	 * @generated
	 */
	int HTTP_METHOD = 64;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.MqttQos <em>Mqtt Qos</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.MqttQos
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getMqttQos()
	 * @generated
	 */
	int MQTT_QOS = 65;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.RegistryKind <em>Registry Kind</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.RegistryKind
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRegistryKind()
	 * @generated
	 */
	int REGISTRY_KIND = 66;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ExpressionLanguage <em>Expression Language</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ExpressionLanguage
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getExpressionLanguage()
	 * @generated
	 */
	int EXPRESSION_LANGUAGE = 67;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.CatalogStatus <em>Catalog Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.CatalogStatus
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getCatalogStatus()
	 * @generated
	 */
	int CATALOG_STATUS = 68;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.services.ConnectionState <em>Connection State</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.services.ConnectionState
	 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getConnectionState()
	 * @generated
	 */
	int CONNECTION_STATE = 69;


	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.NamedElement <em>Named Element</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Named Element</em>'.
	 * @see org.eclipse.fennec.services.NamedElement
	 * @generated
	 */
	EClass getNamedElement();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.NamedElement#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.eclipse.fennec.services.NamedElement#getName()
	 * @see #getNamedElement()
	 * @generated
	 */
	EAttribute getNamedElement_Name();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.VersionedElement <em>Versioned Element</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Versioned Element</em>'.
	 * @see org.eclipse.fennec.services.VersionedElement
	 * @generated
	 */
	EClass getVersionedElement();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.VersionedElement#getVersion <em>Version</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Version</em>'.
	 * @see org.eclipse.fennec.services.VersionedElement#getVersion()
	 * @see #getVersionedElement()
	 * @generated
	 */
	EAttribute getVersionedElement_Version();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.Property <em>Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Property</em>'.
	 * @see org.eclipse.fennec.services.Property
	 * @generated
	 */
	EClass getProperty();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.StringProperty <em>String Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>String Property</em>'.
	 * @see org.eclipse.fennec.services.StringProperty
	 * @generated
	 */
	EClass getStringProperty();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.StringProperty#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.eclipse.fennec.services.StringProperty#getValue()
	 * @see #getStringProperty()
	 * @generated
	 */
	EAttribute getStringProperty_Value();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.IntProperty <em>Int Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Int Property</em>'.
	 * @see org.eclipse.fennec.services.IntProperty
	 * @generated
	 */
	EClass getIntProperty();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.IntProperty#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.eclipse.fennec.services.IntProperty#getValue()
	 * @see #getIntProperty()
	 * @generated
	 */
	EAttribute getIntProperty_Value();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.LongProperty <em>Long Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Long Property</em>'.
	 * @see org.eclipse.fennec.services.LongProperty
	 * @generated
	 */
	EClass getLongProperty();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.LongProperty#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.eclipse.fennec.services.LongProperty#getValue()
	 * @see #getLongProperty()
	 * @generated
	 */
	EAttribute getLongProperty_Value();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.DoubleProperty <em>Double Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Double Property</em>'.
	 * @see org.eclipse.fennec.services.DoubleProperty
	 * @generated
	 */
	EClass getDoubleProperty();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.DoubleProperty#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.eclipse.fennec.services.DoubleProperty#getValue()
	 * @see #getDoubleProperty()
	 * @generated
	 */
	EAttribute getDoubleProperty_Value();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.FloatProperty <em>Float Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Float Property</em>'.
	 * @see org.eclipse.fennec.services.FloatProperty
	 * @generated
	 */
	EClass getFloatProperty();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.FloatProperty#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.eclipse.fennec.services.FloatProperty#getValue()
	 * @see #getFloatProperty()
	 * @generated
	 */
	EAttribute getFloatProperty_Value();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ShortProperty <em>Short Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Short Property</em>'.
	 * @see org.eclipse.fennec.services.ShortProperty
	 * @generated
	 */
	EClass getShortProperty();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ShortProperty#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.eclipse.fennec.services.ShortProperty#getValue()
	 * @see #getShortProperty()
	 * @generated
	 */
	EAttribute getShortProperty_Value();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.BoolProperty <em>Bool Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Bool Property</em>'.
	 * @see org.eclipse.fennec.services.BoolProperty
	 * @generated
	 */
	EClass getBoolProperty();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.BoolProperty#isValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.eclipse.fennec.services.BoolProperty#isValue()
	 * @see #getBoolProperty()
	 * @generated
	 */
	EAttribute getBoolProperty_Value();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.StringListProperty <em>String List Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>String List Property</em>'.
	 * @see org.eclipse.fennec.services.StringListProperty
	 * @generated
	 */
	EClass getStringListProperty();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.StringListProperty#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Value</em>'.
	 * @see org.eclipse.fennec.services.StringListProperty#getValue()
	 * @see #getStringListProperty()
	 * @generated
	 */
	EAttribute getStringListProperty_Value();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceOperation <em>Service Operation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Operation</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperation
	 * @generated
	 */
	EClass getServiceOperation();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceOperation#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperation#getDescription()
	 * @see #getServiceOperation()
	 * @generated
	 */
	EAttribute getServiceOperation_Description();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceOperation#getParameters <em>Parameters</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Parameters</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperation#getParameters()
	 * @see #getServiceOperation()
	 * @generated
	 */
	EReference getServiceOperation_Parameters();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceOperation#getReturnType <em>Return Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Return Type</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperation#getReturnType()
	 * @see #getServiceOperation()
	 * @generated
	 */
	EAttribute getServiceOperation_ReturnType();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceOperation#getReturnConstraints <em>Return Constraints</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Return Constraints</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperation#getReturnConstraints()
	 * @see #getServiceOperation()
	 * @generated
	 */
	EReference getServiceOperation_ReturnConstraints();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.ServiceOperation#getExceptions <em>Exceptions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Exceptions</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperation#getExceptions()
	 * @see #getServiceOperation()
	 * @generated
	 */
	EReference getServiceOperation_Exceptions();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceOperation#getPreconditions <em>Preconditions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Preconditions</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperation#getPreconditions()
	 * @see #getServiceOperation()
	 * @generated
	 */
	EReference getServiceOperation_Preconditions();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceOperation#getPostconditions <em>Postconditions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Postconditions</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperation#getPostconditions()
	 * @see #getServiceOperation()
	 * @generated
	 */
	EReference getServiceOperation_Postconditions();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.Parameter <em>Parameter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Parameter</em>'.
	 * @see org.eclipse.fennec.services.Parameter
	 * @generated
	 */
	EClass getParameter();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Parameter#getIndex <em>Index</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Index</em>'.
	 * @see org.eclipse.fennec.services.Parameter#getIndex()
	 * @see #getParameter()
	 * @generated
	 */
	EAttribute getParameter_Index();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Parameter#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see org.eclipse.fennec.services.Parameter#getType()
	 * @see #getParameter()
	 * @generated
	 */
	EAttribute getParameter_Type();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Parameter#isOptional <em>Optional</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Optional</em>'.
	 * @see org.eclipse.fennec.services.Parameter#isOptional()
	 * @see #getParameter()
	 * @generated
	 */
	EAttribute getParameter_Optional();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Parameter#getDefaultValue <em>Default Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Default Value</em>'.
	 * @see org.eclipse.fennec.services.Parameter#getDefaultValue()
	 * @see #getParameter()
	 * @generated
	 */
	EAttribute getParameter_DefaultValue();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Parameter#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.eclipse.fennec.services.Parameter#getDescription()
	 * @see #getParameter()
	 * @generated
	 */
	EAttribute getParameter_Description();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.Parameter#getConstraints <em>Constraints</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Constraints</em>'.
	 * @see org.eclipse.fennec.services.Parameter#getConstraints()
	 * @see #getParameter()
	 * @generated
	 */
	EReference getParameter_Constraints();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ParameterConstraint <em>Parameter Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Parameter Constraint</em>'.
	 * @see org.eclipse.fennec.services.ParameterConstraint
	 * @generated
	 */
	EClass getParameterConstraint();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.RequiredConstraint <em>Required Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Required Constraint</em>'.
	 * @see org.eclipse.fennec.services.RequiredConstraint
	 * @generated
	 */
	EClass getRequiredConstraint();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.NumericRangeConstraint <em>Numeric Range Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Numeric Range Constraint</em>'.
	 * @see org.eclipse.fennec.services.NumericRangeConstraint
	 * @generated
	 */
	EClass getNumericRangeConstraint();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.NumericRangeConstraint#getMin <em>Min</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Min</em>'.
	 * @see org.eclipse.fennec.services.NumericRangeConstraint#getMin()
	 * @see #getNumericRangeConstraint()
	 * @generated
	 */
	EAttribute getNumericRangeConstraint_Min();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.NumericRangeConstraint#getMax <em>Max</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max</em>'.
	 * @see org.eclipse.fennec.services.NumericRangeConstraint#getMax()
	 * @see #getNumericRangeConstraint()
	 * @generated
	 */
	EAttribute getNumericRangeConstraint_Max();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.NumericRangeConstraint#isInclusiveMin <em>Inclusive Min</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Inclusive Min</em>'.
	 * @see org.eclipse.fennec.services.NumericRangeConstraint#isInclusiveMin()
	 * @see #getNumericRangeConstraint()
	 * @generated
	 */
	EAttribute getNumericRangeConstraint_InclusiveMin();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.NumericRangeConstraint#isInclusiveMax <em>Inclusive Max</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Inclusive Max</em>'.
	 * @see org.eclipse.fennec.services.NumericRangeConstraint#isInclusiveMax()
	 * @see #getNumericRangeConstraint()
	 * @generated
	 */
	EAttribute getNumericRangeConstraint_InclusiveMax();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.StringPatternConstraint <em>String Pattern Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>String Pattern Constraint</em>'.
	 * @see org.eclipse.fennec.services.StringPatternConstraint
	 * @generated
	 */
	EClass getStringPatternConstraint();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.StringPatternConstraint#getPattern <em>Pattern</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Pattern</em>'.
	 * @see org.eclipse.fennec.services.StringPatternConstraint#getPattern()
	 * @see #getStringPatternConstraint()
	 * @generated
	 */
	EAttribute getStringPatternConstraint_Pattern();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.StringPatternConstraint#getMinLength <em>Min Length</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Min Length</em>'.
	 * @see org.eclipse.fennec.services.StringPatternConstraint#getMinLength()
	 * @see #getStringPatternConstraint()
	 * @generated
	 */
	EAttribute getStringPatternConstraint_MinLength();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.StringPatternConstraint#getMaxLength <em>Max Length</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Length</em>'.
	 * @see org.eclipse.fennec.services.StringPatternConstraint#getMaxLength()
	 * @see #getStringPatternConstraint()
	 * @generated
	 */
	EAttribute getStringPatternConstraint_MaxLength();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.EnumerationConstraint <em>Enumeration Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Enumeration Constraint</em>'.
	 * @see org.eclipse.fennec.services.EnumerationConstraint
	 * @generated
	 */
	EClass getEnumerationConstraint();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.EnumerationConstraint#getAllowedValues <em>Allowed Values</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Allowed Values</em>'.
	 * @see org.eclipse.fennec.services.EnumerationConstraint#getAllowedValues()
	 * @see #getEnumerationConstraint()
	 * @generated
	 */
	EAttribute getEnumerationConstraint_AllowedValues();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ExpressionConstraint <em>Expression Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Expression Constraint</em>'.
	 * @see org.eclipse.fennec.services.ExpressionConstraint
	 * @generated
	 */
	EClass getExpressionConstraint();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ExpressionConstraint#getLanguage <em>Language</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Language</em>'.
	 * @see org.eclipse.fennec.services.ExpressionConstraint#getLanguage()
	 * @see #getExpressionConstraint()
	 * @generated
	 */
	EAttribute getExpressionConstraint_Language();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ExpressionConstraint#getExpression <em>Expression</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expression</em>'.
	 * @see org.eclipse.fennec.services.ExpressionConstraint#getExpression()
	 * @see #getExpressionConstraint()
	 * @generated
	 */
	EAttribute getExpressionConstraint_Expression();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ExpressionConstraint#getMessage <em>Message</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Message</em>'.
	 * @see org.eclipse.fennec.services.ExpressionConstraint#getMessage()
	 * @see #getExpressionConstraint()
	 * @generated
	 */
	EAttribute getExpressionConstraint_Message();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.Invariant <em>Invariant</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Invariant</em>'.
	 * @see org.eclipse.fennec.services.Invariant
	 * @generated
	 */
	EClass getInvariant();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Invariant#getLanguage <em>Language</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Language</em>'.
	 * @see org.eclipse.fennec.services.Invariant#getLanguage()
	 * @see #getInvariant()
	 * @generated
	 */
	EAttribute getInvariant_Language();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Invariant#getExpression <em>Expression</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expression</em>'.
	 * @see org.eclipse.fennec.services.Invariant#getExpression()
	 * @see #getInvariant()
	 * @generated
	 */
	EAttribute getInvariant_Expression();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Invariant#getMessage <em>Message</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Message</em>'.
	 * @see org.eclipse.fennec.services.Invariant#getMessage()
	 * @see #getInvariant()
	 * @generated
	 */
	EAttribute getInvariant_Message();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.CollectionSizeConstraint <em>Collection Size Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Collection Size Constraint</em>'.
	 * @see org.eclipse.fennec.services.CollectionSizeConstraint
	 * @generated
	 */
	EClass getCollectionSizeConstraint();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.CollectionSizeConstraint#getMinSize <em>Min Size</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Min Size</em>'.
	 * @see org.eclipse.fennec.services.CollectionSizeConstraint#getMinSize()
	 * @see #getCollectionSizeConstraint()
	 * @generated
	 */
	EAttribute getCollectionSizeConstraint_MinSize();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.CollectionSizeConstraint#getMaxSize <em>Max Size</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Size</em>'.
	 * @see org.eclipse.fennec.services.CollectionSizeConstraint#getMaxSize()
	 * @see #getCollectionSizeConstraint()
	 * @generated
	 */
	EAttribute getCollectionSizeConstraint_MaxSize();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceException <em>Service Exception</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Exception</em>'.
	 * @see org.eclipse.fennec.services.ServiceException
	 * @generated
	 */
	EClass getServiceException();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceException#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.eclipse.fennec.services.ServiceException#getDescription()
	 * @see #getServiceException()
	 * @generated
	 */
	EAttribute getServiceException_Description();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceException#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see org.eclipse.fennec.services.ServiceException#getType()
	 * @see #getServiceException()
	 * @generated
	 */
	EAttribute getServiceException_Type();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceException#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Properties</em>'.
	 * @see org.eclipse.fennec.services.ServiceException#getProperties()
	 * @see #getServiceException()
	 * @generated
	 */
	EReference getServiceException_Properties();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceInterface <em>Service Interface</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Interface</em>'.
	 * @see org.eclipse.fennec.services.ServiceInterface
	 * @generated
	 */
	EClass getServiceInterface();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceInterface#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.eclipse.fennec.services.ServiceInterface#getDescription()
	 * @see #getServiceInterface()
	 * @generated
	 */
	EAttribute getServiceInterface_Description();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceInterface#getOperations <em>Operations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Operations</em>'.
	 * @see org.eclipse.fennec.services.ServiceInterface#getOperations()
	 * @see #getServiceInterface()
	 * @generated
	 */
	EReference getServiceInterface_Operations();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceInterface#getExceptions <em>Exceptions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Exceptions</em>'.
	 * @see org.eclipse.fennec.services.ServiceInterface#getExceptions()
	 * @see #getServiceInterface()
	 * @generated
	 */
	EReference getServiceInterface_Exceptions();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceInterface#getInvariants <em>Invariants</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Invariants</em>'.
	 * @see org.eclipse.fennec.services.ServiceInterface#getInvariants()
	 * @see #getServiceInterface()
	 * @generated
	 */
	EReference getServiceInterface_Invariants();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceInterface#getStatus <em>Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Status</em>'.
	 * @see org.eclipse.fennec.services.ServiceInterface#getStatus()
	 * @see #getServiceInterface()
	 * @generated
	 */
	EAttribute getServiceInterface_Status();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceInterface#getDeprecationReason <em>Deprecation Reason</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Deprecation Reason</em>'.
	 * @see org.eclipse.fennec.services.ServiceInterface#getDeprecationReason()
	 * @see #getServiceInterface()
	 * @generated
	 */
	EAttribute getServiceInterface_DeprecationReason();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ServiceInterface#getReplacedBy <em>Replaced By</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Replaced By</em>'.
	 * @see org.eclipse.fennec.services.ServiceInterface#getReplacedBy()
	 * @see #getServiceInterface()
	 * @generated
	 */
	EReference getServiceInterface_ReplacedBy();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.LifecycleHook <em>Lifecycle Hook</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Lifecycle Hook</em>'.
	 * @see org.eclipse.fennec.services.LifecycleHook
	 * @generated
	 */
	EClass getLifecycleHook();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.LifecycleHook#getKind <em>Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Kind</em>'.
	 * @see org.eclipse.fennec.services.LifecycleHook#getKind()
	 * @see #getLifecycleHook()
	 * @generated
	 */
	EAttribute getLifecycleHook_Kind();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.LifecycleHook#getParameter <em>Parameter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Parameter</em>'.
	 * @see org.eclipse.fennec.services.LifecycleHook#getParameter()
	 * @see #getLifecycleHook()
	 * @generated
	 */
	EAttribute getLifecycleHook_Parameter();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ReferenceBinding <em>Reference Binding</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Reference Binding</em>'.
	 * @see org.eclipse.fennec.services.ReferenceBinding
	 * @generated
	 */
	EClass getReferenceBinding();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ReferenceBinding#getKind <em>Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Kind</em>'.
	 * @see org.eclipse.fennec.services.ReferenceBinding#getKind()
	 * @see #getReferenceBinding()
	 * @generated
	 */
	EAttribute getReferenceBinding_Kind();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ReferenceBinding#getFieldOption <em>Field Option</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Field Option</em>'.
	 * @see org.eclipse.fennec.services.ReferenceBinding#getFieldOption()
	 * @see #getReferenceBinding()
	 * @generated
	 */
	EAttribute getReferenceBinding_FieldOption();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ComponentReference <em>Component Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Component Reference</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference
	 * @generated
	 */
	EClass getComponentReference();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentReference#getInterfaceName <em>Interface Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Interface Name</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference#getInterfaceName()
	 * @see #getComponentReference()
	 * @generated
	 */
	EAttribute getComponentReference_InterfaceName();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentReference#getCardinality <em>Cardinality</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Cardinality</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference#getCardinality()
	 * @see #getComponentReference()
	 * @generated
	 */
	EAttribute getComponentReference_Cardinality();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentReference#getPolicy <em>Policy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Policy</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference#getPolicy()
	 * @see #getComponentReference()
	 * @generated
	 */
	EAttribute getComponentReference_Policy();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentReference#getPolicyOption <em>Policy Option</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Policy Option</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference#getPolicyOption()
	 * @see #getComponentReference()
	 * @generated
	 */
	EAttribute getComponentReference_PolicyOption();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentReference#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Target</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference#getTarget()
	 * @see #getComponentReference()
	 * @generated
	 */
	EAttribute getComponentReference_Target();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentReference#getScope <em>Scope</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Scope</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference#getScope()
	 * @see #getComponentReference()
	 * @generated
	 */
	EAttribute getComponentReference_Scope();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentReference#getCollectionType <em>Collection Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Collection Type</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference#getCollectionType()
	 * @see #getComponentReference()
	 * @generated
	 */
	EAttribute getComponentReference_CollectionType();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentReference#getParameter <em>Parameter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Parameter</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference#getParameter()
	 * @see #getComponentReference()
	 * @generated
	 */
	EAttribute getComponentReference_Parameter();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ComponentReference#getBindings <em>Bindings</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Bindings</em>'.
	 * @see org.eclipse.fennec.services.ComponentReference#getBindings()
	 * @see #getComponentReference()
	 * @generated
	 */
	EReference getComponentReference_Bindings();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ComponentDescription <em>Component Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Component Description</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription
	 * @generated
	 */
	EClass getComponentDescription();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentDescription#getFactory <em>Factory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Factory</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getFactory()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EAttribute getComponentDescription_Factory();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentDescription#getScope <em>Scope</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Scope</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getScope()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EAttribute getComponentDescription_Scope();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentDescription#getImplementationId <em>Implementation Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Implementation Id</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getImplementationId()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EAttribute getComponentDescription_ImplementationId();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentDescription#isDefaultEnabled <em>Default Enabled</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Default Enabled</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#isDefaultEnabled()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EAttribute getComponentDescription_DefaultEnabled();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentDescription#isImmediate <em>Immediate</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Immediate</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#isImmediate()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EAttribute getComponentDescription_Immediate();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentDescription#getConfigurationPolicy <em>Configuration Policy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Configuration Policy</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getConfigurationPolicy()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EAttribute getComponentDescription_ConfigurationPolicy();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.ComponentDescription#getConfigurationPid <em>Configuration Pid</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Configuration Pid</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getConfigurationPid()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EAttribute getComponentDescription_ConfigurationPid();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.ComponentDescription#getServiceInterfaces <em>Service Interfaces</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Service Interfaces</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getServiceInterfaces()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EReference getComponentDescription_ServiceInterfaces();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ComponentDescription#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Properties</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getProperties()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EReference getComponentDescription_Properties();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ComponentDescription#getFactoryProperties <em>Factory Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Factory Properties</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getFactoryProperties()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EReference getComponentDescription_FactoryProperties();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ComponentDescription#getReferences <em>References</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>References</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getReferences()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EReference getComponentDescription_References();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ComponentDescription#getLifecycleHooks <em>Lifecycle Hooks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Lifecycle Hooks</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getLifecycleHooks()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EReference getComponentDescription_LifecycleHooks();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ComponentDescription#getProvider <em>Provider</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Provider</em>'.
	 * @see org.eclipse.fennec.services.ComponentDescription#getProvider()
	 * @see #getComponentDescription()
	 * @generated
	 */
	EReference getComponentDescription_Provider();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceProvider <em>Service Provider</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Provider</em>'.
	 * @see org.eclipse.fennec.services.ServiceProvider
	 * @generated
	 */
	EClass getServiceProvider();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceProvider#getSymbolicName <em>Symbolic Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Symbolic Name</em>'.
	 * @see org.eclipse.fennec.services.ServiceProvider#getSymbolicName()
	 * @see #getServiceProvider()
	 * @generated
	 */
	EAttribute getServiceProvider_SymbolicName();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceProvider#getDescriptions <em>Descriptions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Descriptions</em>'.
	 * @see org.eclipse.fennec.services.ServiceProvider#getDescriptions()
	 * @see #getServiceProvider()
	 * @generated
	 */
	EReference getServiceProvider_Descriptions();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceProvider#getImplementations <em>Implementations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Implementations</em>'.
	 * @see org.eclipse.fennec.services.ServiceProvider#getImplementations()
	 * @see #getServiceProvider()
	 * @generated
	 */
	EReference getServiceProvider_Implementations();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceImplementation <em>Service Implementation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Implementation</em>'.
	 * @see org.eclipse.fennec.services.ServiceImplementation
	 * @generated
	 */
	EClass getServiceImplementation();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceImplementation#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.eclipse.fennec.services.ServiceImplementation#getDescription()
	 * @see #getServiceImplementation()
	 * @generated
	 */
	EAttribute getServiceImplementation_Description();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceImplementation#getImplementationId <em>Implementation Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Implementation Id</em>'.
	 * @see org.eclipse.fennec.services.ServiceImplementation#getImplementationId()
	 * @see #getServiceImplementation()
	 * @generated
	 */
	EAttribute getServiceImplementation_ImplementationId();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.ServiceImplementation#getServiceInterfaces <em>Service Interfaces</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Service Interfaces</em>'.
	 * @see org.eclipse.fennec.services.ServiceImplementation#getServiceInterfaces()
	 * @see #getServiceImplementation()
	 * @generated
	 */
	EReference getServiceImplementation_ServiceInterfaces();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceImplementation#getFlavors <em>Flavors</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Flavors</em>'.
	 * @see org.eclipse.fennec.services.ServiceImplementation#getFlavors()
	 * @see #getServiceImplementation()
	 * @generated
	 */
	EReference getServiceImplementation_Flavors();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceImplementation#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Properties</em>'.
	 * @see org.eclipse.fennec.services.ServiceImplementation#getProperties()
	 * @see #getServiceImplementation()
	 * @generated
	 */
	EReference getServiceImplementation_Properties();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ServiceImplementation#getComponentDescription <em>Component Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Component Description</em>'.
	 * @see org.eclipse.fennec.services.ServiceImplementation#getComponentDescription()
	 * @see #getServiceImplementation()
	 * @generated
	 */
	EReference getServiceImplementation_ComponentDescription();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceFlavor <em>Service Flavor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Flavor</em>'.
	 * @see org.eclipse.fennec.services.ServiceFlavor
	 * @generated
	 */
	EClass getServiceFlavor();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceFlavor#getKind <em>Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Kind</em>'.
	 * @see org.eclipse.fennec.services.ServiceFlavor#getKind()
	 * @see #getServiceFlavor()
	 * @generated
	 */
	EAttribute getServiceFlavor_Kind();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceFlavor#getOperationFlavors <em>Operation Flavors</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Operation Flavors</em>'.
	 * @see org.eclipse.fennec.services.ServiceFlavor#getOperationFlavors()
	 * @see #getServiceFlavor()
	 * @generated
	 */
	EReference getServiceFlavor_OperationFlavors();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.RestFlavor <em>Rest Flavor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Rest Flavor</em>'.
	 * @see org.eclipse.fennec.services.RestFlavor
	 * @generated
	 */
	EClass getRestFlavor();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.RestFlavor#getHost <em>Host</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Host</em>'.
	 * @see org.eclipse.fennec.services.RestFlavor#getHost()
	 * @see #getRestFlavor()
	 * @generated
	 */
	EAttribute getRestFlavor_Host();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.RestFlavor#getBasePath <em>Base Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Base Path</em>'.
	 * @see org.eclipse.fennec.services.RestFlavor#getBasePath()
	 * @see #getRestFlavor()
	 * @generated
	 */
	EAttribute getRestFlavor_BasePath();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.RestFlavor#getContentTypes <em>Content Types</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Content Types</em>'.
	 * @see org.eclipse.fennec.services.RestFlavor#getContentTypes()
	 * @see #getRestFlavor()
	 * @generated
	 */
	EAttribute getRestFlavor_ContentTypes();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.MqttFlavor <em>Mqtt Flavor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Mqtt Flavor</em>'.
	 * @see org.eclipse.fennec.services.MqttFlavor
	 * @generated
	 */
	EClass getMqttFlavor();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.MqttFlavor#getBrokers <em>Brokers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Brokers</em>'.
	 * @see org.eclipse.fennec.services.MqttFlavor#getBrokers()
	 * @see #getMqttFlavor()
	 * @generated
	 */
	EAttribute getMqttFlavor_Brokers();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttFlavor#getRequestTopic <em>Request Topic</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Request Topic</em>'.
	 * @see org.eclipse.fennec.services.MqttFlavor#getRequestTopic()
	 * @see #getMqttFlavor()
	 * @generated
	 */
	EAttribute getMqttFlavor_RequestTopic();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttFlavor#getResponseTopic <em>Response Topic</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Response Topic</em>'.
	 * @see org.eclipse.fennec.services.MqttFlavor#getResponseTopic()
	 * @see #getMqttFlavor()
	 * @generated
	 */
	EAttribute getMqttFlavor_ResponseTopic();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttFlavor#getDefaultQos <em>Default Qos</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Default Qos</em>'.
	 * @see org.eclipse.fennec.services.MqttFlavor#getDefaultQos()
	 * @see #getMqttFlavor()
	 * @generated
	 */
	EAttribute getMqttFlavor_DefaultQos();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttFlavor#isDefaultRetained <em>Default Retained</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Default Retained</em>'.
	 * @see org.eclipse.fennec.services.MqttFlavor#isDefaultRetained()
	 * @see #getMqttFlavor()
	 * @generated
	 */
	EAttribute getMqttFlavor_DefaultRetained();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceOperationFlavor <em>Service Operation Flavor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Operation Flavor</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperationFlavor
	 * @generated
	 */
	EClass getServiceOperationFlavor();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ServiceOperationFlavor#getOperation <em>Operation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Operation</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperationFlavor#getOperation()
	 * @see #getServiceOperationFlavor()
	 * @generated
	 */
	EReference getServiceOperationFlavor_Operation();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.ServiceOperationFlavor#getConsumes <em>Consumes</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Consumes</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperationFlavor#getConsumes()
	 * @see #getServiceOperationFlavor()
	 * @generated
	 */
	EAttribute getServiceOperationFlavor_Consumes();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.ServiceOperationFlavor#getProduces <em>Produces</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Produces</em>'.
	 * @see org.eclipse.fennec.services.ServiceOperationFlavor#getProduces()
	 * @see #getServiceOperationFlavor()
	 * @generated
	 */
	EAttribute getServiceOperationFlavor_Produces();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.RestOperationFlavor <em>Rest Operation Flavor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Rest Operation Flavor</em>'.
	 * @see org.eclipse.fennec.services.RestOperationFlavor
	 * @generated
	 */
	EClass getRestOperationFlavor();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.RestOperationFlavor#getMethod <em>Method</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Method</em>'.
	 * @see org.eclipse.fennec.services.RestOperationFlavor#getMethod()
	 * @see #getRestOperationFlavor()
	 * @generated
	 */
	EAttribute getRestOperationFlavor_Method();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.RestOperationFlavor#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see org.eclipse.fennec.services.RestOperationFlavor#getPath()
	 * @see #getRestOperationFlavor()
	 * @generated
	 */
	EAttribute getRestOperationFlavor_Path();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.RestOperationFlavor#getReturnCodes <em>Return Codes</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Return Codes</em>'.
	 * @see org.eclipse.fennec.services.RestOperationFlavor#getReturnCodes()
	 * @see #getRestOperationFlavor()
	 * @generated
	 */
	EAttribute getRestOperationFlavor_ReturnCodes();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.MqttOperationFlavor <em>Mqtt Operation Flavor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Mqtt Operation Flavor</em>'.
	 * @see org.eclipse.fennec.services.MqttOperationFlavor
	 * @generated
	 */
	EClass getMqttOperationFlavor();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttOperationFlavor#getRequestTopic <em>Request Topic</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Request Topic</em>'.
	 * @see org.eclipse.fennec.services.MqttOperationFlavor#getRequestTopic()
	 * @see #getMqttOperationFlavor()
	 * @generated
	 */
	EAttribute getMqttOperationFlavor_RequestTopic();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttOperationFlavor#getResponseTopic <em>Response Topic</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Response Topic</em>'.
	 * @see org.eclipse.fennec.services.MqttOperationFlavor#getResponseTopic()
	 * @see #getMqttOperationFlavor()
	 * @generated
	 */
	EAttribute getMqttOperationFlavor_ResponseTopic();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttOperationFlavor#getQos <em>Qos</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Qos</em>'.
	 * @see org.eclipse.fennec.services.MqttOperationFlavor#getQos()
	 * @see #getMqttOperationFlavor()
	 * @generated
	 */
	EAttribute getMqttOperationFlavor_Qos();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttOperationFlavor#isRetained <em>Retained</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Retained</em>'.
	 * @see org.eclipse.fennec.services.MqttOperationFlavor#isRetained()
	 * @see #getMqttOperationFlavor()
	 * @generated
	 */
	EAttribute getMqttOperationFlavor_Retained();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttOperationFlavor#isCorrelation <em>Correlation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Correlation</em>'.
	 * @see org.eclipse.fennec.services.MqttOperationFlavor#isCorrelation()
	 * @see #getMqttOperationFlavor()
	 * @generated
	 */
	EAttribute getMqttOperationFlavor_Correlation();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.MqttOperationFlavor#getReturnPath <em>Return Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Return Path</em>'.
	 * @see org.eclipse.fennec.services.MqttOperationFlavor#getReturnPath()
	 * @see #getMqttOperationFlavor()
	 * @generated
	 */
	EAttribute getMqttOperationFlavor_ReturnPath();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceReference <em>Service Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Reference</em>'.
	 * @see org.eclipse.fennec.services.ServiceReference
	 * @generated
	 */
	EClass getServiceReference();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceReference#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see org.eclipse.fennec.services.ServiceReference#getId()
	 * @see #getServiceReference()
	 * @generated
	 */
	EAttribute getServiceReference_Id();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ServiceReference#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Properties</em>'.
	 * @see org.eclipse.fennec.services.ServiceReference#getProperties()
	 * @see #getServiceReference()
	 * @generated
	 */
	EReference getServiceReference_Properties();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ServiceReference#getProvider <em>Provider</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Provider</em>'.
	 * @see org.eclipse.fennec.services.ServiceReference#getProvider()
	 * @see #getServiceReference()
	 * @generated
	 */
	EReference getServiceReference_Provider();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ServiceReference#getRegistration <em>Registration</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Registration</em>'.
	 * @see org.eclipse.fennec.services.ServiceReference#getRegistration()
	 * @see #getServiceReference()
	 * @generated
	 */
	EReference getServiceReference_Registration();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceReference#getProperty(java.lang.String) <em>Get Property</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Get Property</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceReference#getProperty(java.lang.String)
	 * @generated
	 */
	EOperation getServiceReference__GetProperty__String();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceReference#getPropertyKeys() <em>Get Property Keys</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Get Property Keys</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceReference#getPropertyKeys()
	 * @generated
	 */
	EOperation getServiceReference__GetPropertyKeys();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceRegistration <em>Service Registration</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Registration</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistration
	 * @generated
	 */
	EClass getServiceRegistration();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ServiceRegistration#getReference <em>Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Reference</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistration#getReference()
	 * @see #getServiceRegistration()
	 * @generated
	 */
	EReference getServiceRegistration_Reference();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceRegistration#isUnregistered <em>Unregistered</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Unregistered</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistration#isUnregistered()
	 * @see #getServiceRegistration()
	 * @generated
	 */
	EAttribute getServiceRegistration_Unregistered();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ServiceRegistration#getProvider <em>Provider</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Provider</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistration#getProvider()
	 * @see #getServiceRegistration()
	 * @generated
	 */
	EReference getServiceRegistration_Provider();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ServiceRegistration#getImplementation <em>Implementation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Implementation</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistration#getImplementation()
	 * @see #getServiceRegistration()
	 * @generated
	 */
	EReference getServiceRegistration_Implementation();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.ServiceRegistration#getUsingSessions <em>Using Sessions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Using Sessions</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistration#getUsingSessions()
	 * @see #getServiceRegistration()
	 * @generated
	 */
	EReference getServiceRegistration_UsingSessions();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceRegistration#getConsumerCount <em>Consumer Count</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Consumer Count</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistration#getConsumerCount()
	 * @see #getServiceRegistration()
	 * @generated
	 */
	EAttribute getServiceRegistration_ConsumerCount();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceRegistration#unregister() <em>Unregister</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Unregister</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceRegistration#unregister()
	 * @generated
	 */
	EOperation getServiceRegistration__Unregister();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceRegistration#setProperties(org.eclipse.emf.common.util.EList) <em>Set Properties</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Set Properties</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceRegistration#setProperties(org.eclipse.emf.common.util.EList)
	 * @generated
	 */
	EOperation getServiceRegistration__SetProperties__EList();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ConsumerSession <em>Consumer Session</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Consumer Session</em>'.
	 * @see org.eclipse.fennec.services.ConsumerSession
	 * @generated
	 */
	EClass getConsumerSession();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ConsumerSession#getConsumerId <em>Consumer Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Consumer Id</em>'.
	 * @see org.eclipse.fennec.services.ConsumerSession#getConsumerId()
	 * @see #getConsumerSession()
	 * @generated
	 */
	EAttribute getConsumerSession_ConsumerId();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ConsumerSession#getLastRenewal <em>Last Renewal</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Last Renewal</em>'.
	 * @see org.eclipse.fennec.services.ConsumerSession#getLastRenewal()
	 * @see #getConsumerSession()
	 * @generated
	 */
	EAttribute getConsumerSession_LastRenewal();

	/**
	 * Returns the meta object for the containment reference '{@link org.eclipse.fennec.services.ConsumerSession#getCapabilities <em>Capabilities</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Capabilities</em>'.
	 * @see org.eclipse.fennec.services.ConsumerSession#getCapabilities()
	 * @see #getConsumerSession()
	 * @generated
	 */
	EReference getConsumerSession_Capabilities();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.ConsumerSession#getAcquisitions <em>Acquisitions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Acquisitions</em>'.
	 * @see org.eclipse.fennec.services.ConsumerSession#getAcquisitions()
	 * @see #getConsumerSession()
	 * @generated
	 */
	EReference getConsumerSession_Acquisitions();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ComponentConfiguration <em>Component Configuration</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Component Configuration</em>'.
	 * @see org.eclipse.fennec.services.ComponentConfiguration
	 * @generated
	 */
	EClass getComponentConfiguration();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentConfiguration#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see org.eclipse.fennec.services.ComponentConfiguration#getId()
	 * @see #getComponentConfiguration()
	 * @generated
	 */
	EAttribute getComponentConfiguration_Id();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ComponentConfiguration#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Description</em>'.
	 * @see org.eclipse.fennec.services.ComponentConfiguration#getDescription()
	 * @see #getComponentConfiguration()
	 * @generated
	 */
	EReference getComponentConfiguration_Description();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ComponentConfiguration#getState <em>State</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>State</em>'.
	 * @see org.eclipse.fennec.services.ComponentConfiguration#getState()
	 * @see #getComponentConfiguration()
	 * @generated
	 */
	EAttribute getComponentConfiguration_State();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ComponentConfiguration#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Properties</em>'.
	 * @see org.eclipse.fennec.services.ComponentConfiguration#getProperties()
	 * @see #getComponentConfiguration()
	 * @generated
	 */
	EReference getComponentConfiguration_Properties();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ComponentConfiguration#getSatisfiedReferences <em>Satisfied References</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Satisfied References</em>'.
	 * @see org.eclipse.fennec.services.ComponentConfiguration#getSatisfiedReferences()
	 * @see #getComponentConfiguration()
	 * @generated
	 */
	EReference getComponentConfiguration_SatisfiedReferences();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ComponentConfiguration#getUnsatisfiedReferences <em>Unsatisfied References</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Unsatisfied References</em>'.
	 * @see org.eclipse.fennec.services.ComponentConfiguration#getUnsatisfiedReferences()
	 * @see #getComponentConfiguration()
	 * @generated
	 */
	EReference getComponentConfiguration_UnsatisfiedReferences();

	/**
	 * Returns the meta object for the containment reference '{@link org.eclipse.fennec.services.ComponentConfiguration#getFailure <em>Failure</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Failure</em>'.
	 * @see org.eclipse.fennec.services.ComponentConfiguration#getFailure()
	 * @see #getComponentConfiguration()
	 * @generated
	 */
	EReference getComponentConfiguration_Failure();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ComponentConfiguration#getService <em>Service</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Service</em>'.
	 * @see org.eclipse.fennec.services.ComponentConfiguration#getService()
	 * @see #getComponentConfiguration()
	 * @generated
	 */
	EReference getComponentConfiguration_Service();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.SatisfiedReference <em>Satisfied Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Satisfied Reference</em>'.
	 * @see org.eclipse.fennec.services.SatisfiedReference
	 * @generated
	 */
	EClass getSatisfiedReference();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.SatisfiedReference#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.eclipse.fennec.services.SatisfiedReference#getName()
	 * @see #getSatisfiedReference()
	 * @generated
	 */
	EAttribute getSatisfiedReference_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.SatisfiedReference#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Target</em>'.
	 * @see org.eclipse.fennec.services.SatisfiedReference#getTarget()
	 * @see #getSatisfiedReference()
	 * @generated
	 */
	EAttribute getSatisfiedReference_Target();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.SatisfiedReference#getBoundServices <em>Bound Services</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Bound Services</em>'.
	 * @see org.eclipse.fennec.services.SatisfiedReference#getBoundServices()
	 * @see #getSatisfiedReference()
	 * @generated
	 */
	EReference getSatisfiedReference_BoundServices();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.UnsatisfiedReference <em>Unsatisfied Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Unsatisfied Reference</em>'.
	 * @see org.eclipse.fennec.services.UnsatisfiedReference
	 * @generated
	 */
	EClass getUnsatisfiedReference();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.UnsatisfiedReference#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.eclipse.fennec.services.UnsatisfiedReference#getName()
	 * @see #getUnsatisfiedReference()
	 * @generated
	 */
	EAttribute getUnsatisfiedReference_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.UnsatisfiedReference#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Target</em>'.
	 * @see org.eclipse.fennec.services.UnsatisfiedReference#getTarget()
	 * @see #getUnsatisfiedReference()
	 * @generated
	 */
	EAttribute getUnsatisfiedReference_Target();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.UnsatisfiedReference#getTargetServices <em>Target Services</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Target Services</em>'.
	 * @see org.eclipse.fennec.services.UnsatisfiedReference#getTargetServices()
	 * @see #getUnsatisfiedReference()
	 * @generated
	 */
	EReference getUnsatisfiedReference_TargetServices();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.Diagnostic <em>Diagnostic</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Diagnostic</em>'.
	 * @see org.eclipse.fennec.services.Diagnostic
	 * @generated
	 */
	EClass getDiagnostic();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Diagnostic#getSeverity <em>Severity</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Severity</em>'.
	 * @see org.eclipse.fennec.services.Diagnostic#getSeverity()
	 * @see #getDiagnostic()
	 * @generated
	 */
	EAttribute getDiagnostic_Severity();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Diagnostic#getMessage <em>Message</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Message</em>'.
	 * @see org.eclipse.fennec.services.Diagnostic#getMessage()
	 * @see #getDiagnostic()
	 * @generated
	 */
	EAttribute getDiagnostic_Message();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Diagnostic#getSource <em>Source</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Source</em>'.
	 * @see org.eclipse.fennec.services.Diagnostic#getSource()
	 * @see #getDiagnostic()
	 * @generated
	 */
	EAttribute getDiagnostic_Source();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.Diagnostic#getCode <em>Code</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Code</em>'.
	 * @see org.eclipse.fennec.services.Diagnostic#getCode()
	 * @see #getDiagnostic()
	 * @generated
	 */
	EAttribute getDiagnostic_Code();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.Diagnostic#getData <em>Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Data</em>'.
	 * @see org.eclipse.fennec.services.Diagnostic#getData()
	 * @see #getDiagnostic()
	 * @generated
	 */
	EAttribute getDiagnostic_Data();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.Diagnostic#getChildren <em>Children</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Children</em>'.
	 * @see org.eclipse.fennec.services.Diagnostic#getChildren()
	 * @see #getDiagnostic()
	 * @generated
	 */
	EReference getDiagnostic_Children();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceEvent <em>Service Event</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Event</em>'.
	 * @see org.eclipse.fennec.services.ServiceEvent
	 * @generated
	 */
	EClass getServiceEvent();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceEvent#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see org.eclipse.fennec.services.ServiceEvent#getType()
	 * @see #getServiceEvent()
	 * @generated
	 */
	EAttribute getServiceEvent_Type();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.ServiceEvent#getReference <em>Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Reference</em>'.
	 * @see org.eclipse.fennec.services.ServiceEvent#getReference()
	 * @see #getServiceEvent()
	 * @generated
	 */
	EReference getServiceEvent_Reference();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceEvent#getTimestamp <em>Timestamp</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Timestamp</em>'.
	 * @see org.eclipse.fennec.services.ServiceEvent#getTimestamp()
	 * @see #getServiceEvent()
	 * @generated
	 */
	EAttribute getServiceEvent_Timestamp();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceListener <em>Service Listener</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Listener</em>'.
	 * @see org.eclipse.fennec.services.ServiceListener
	 * @generated
	 */
	EClass getServiceListener();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceListener#getFilter <em>Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Filter</em>'.
	 * @see org.eclipse.fennec.services.ServiceListener#getFilter()
	 * @see #getServiceListener()
	 * @generated
	 */
	EAttribute getServiceListener_Filter();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceListener#serviceChanged(org.eclipse.fennec.services.ServiceEvent) <em>Service Changed</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Service Changed</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceListener#serviceChanged(org.eclipse.fennec.services.ServiceEvent)
	 * @generated
	 */
	EOperation getServiceListener__ServiceChanged__ServiceEvent();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ServiceRegistry <em>Service Registry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Service Registry</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistry
	 * @generated
	 */
	EClass getServiceRegistry();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ServiceRegistry#getKind <em>Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Kind</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistry#getKind()
	 * @see #getServiceRegistry()
	 * @generated
	 */
	EAttribute getServiceRegistry_Kind();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.ServiceRegistry#getPublishHooks <em>Publish Hooks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Publish Hooks</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistry#getPublishHooks()
	 * @see #getServiceRegistry()
	 * @generated
	 */
	EReference getServiceRegistry_PublishHooks();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.ServiceRegistry#getDiscoveryHooks <em>Discovery Hooks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Discovery Hooks</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistry#getDiscoveryHooks()
	 * @see #getServiceRegistry()
	 * @generated
	 */
	EReference getServiceRegistry_DiscoveryHooks();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.ServiceRegistry#getDistributionHooks <em>Distribution Hooks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Distribution Hooks</em>'.
	 * @see org.eclipse.fennec.services.ServiceRegistry#getDistributionHooks()
	 * @see #getServiceRegistry()
	 * @generated
	 */
	EReference getServiceRegistry_DistributionHooks();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceRegistry#getServiceReference(java.lang.String) <em>Get Service Reference</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Get Service Reference</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceRegistry#getServiceReference(java.lang.String)
	 * @generated
	 */
	EOperation getServiceRegistry__GetServiceReference__String();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceRegistry#getServiceReferences(java.lang.String, java.lang.String, org.eclipse.fennec.services.ConsumerCapability) <em>Get Service References</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Get Service References</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceRegistry#getServiceReferences(java.lang.String, java.lang.String, org.eclipse.fennec.services.ConsumerCapability)
	 * @generated
	 */
	EOperation getServiceRegistry__GetServiceReferences__String_String_ConsumerCapability();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceRegistry#getAllServiceReferences(java.lang.String, java.lang.String, org.eclipse.fennec.services.ConsumerCapability) <em>Get All Service References</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Get All Service References</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceRegistry#getAllServiceReferences(java.lang.String, java.lang.String, org.eclipse.fennec.services.ConsumerCapability)
	 * @generated
	 */
	EOperation getServiceRegistry__GetAllServiceReferences__String_String_ConsumerCapability();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceRegistry#addServiceListener(org.eclipse.fennec.services.ServiceListener) <em>Add Service Listener</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Add Service Listener</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceRegistry#addServiceListener(org.eclipse.fennec.services.ServiceListener)
	 * @generated
	 */
	EOperation getServiceRegistry__AddServiceListener__ServiceListener();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.ServiceRegistry#removeServiceListener(org.eclipse.fennec.services.ServiceListener) <em>Remove Service Listener</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Remove Service Listener</em>' operation.
	 * @see org.eclipse.fennec.services.ServiceRegistry#removeServiceListener(org.eclipse.fennec.services.ServiceListener)
	 * @generated
	 */
	EOperation getServiceRegistry__RemoveServiceListener__ServiceListener();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.LocalServiceRegistry <em>Local Service Registry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Local Service Registry</em>'.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry
	 * @generated
	 */
	EClass getLocalServiceRegistry();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.LocalServiceRegistry#getReferences <em>References</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>References</em>'.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#getReferences()
	 * @see #getLocalServiceRegistry()
	 * @generated
	 */
	EReference getLocalServiceRegistry_References();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.LocalServiceRegistry#getRegistrations <em>Registrations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Registrations</em>'.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#getRegistrations()
	 * @see #getLocalServiceRegistry()
	 * @generated
	 */
	EReference getLocalServiceRegistry_Registrations();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.LocalServiceRegistry#getSessions <em>Sessions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Sessions</em>'.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#getSessions()
	 * @see #getLocalServiceRegistry()
	 * @generated
	 */
	EReference getLocalServiceRegistry_Sessions();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.LocalServiceRegistry#getConfigurations <em>Configurations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Configurations</em>'.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#getConfigurations()
	 * @see #getLocalServiceRegistry()
	 * @generated
	 */
	EReference getLocalServiceRegistry_Configurations();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.LocalServiceRegistry#getProviders <em>Providers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Providers</em>'.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#getProviders()
	 * @see #getLocalServiceRegistry()
	 * @generated
	 */
	EReference getLocalServiceRegistry_Providers();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.LocalServiceRegistry#getListeners <em>Listeners</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Listeners</em>'.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#getListeners()
	 * @see #getLocalServiceRegistry()
	 * @generated
	 */
	EReference getLocalServiceRegistry_Listeners();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.services.LocalServiceRegistry#getRemote <em>Remote</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Remote</em>'.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#getRemote()
	 * @see #getLocalServiceRegistry()
	 * @generated
	 */
	EReference getLocalServiceRegistry_Remote();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.LocalServiceRegistry#getConnectionState <em>Connection State</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Connection State</em>'.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#getConnectionState()
	 * @see #getLocalServiceRegistry()
	 * @generated
	 */
	EAttribute getLocalServiceRegistry_ConnectionState();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.LocalServiceRegistry#registerService(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation, org.eclipse.emf.common.util.EList) <em>Register Service</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Register Service</em>' operation.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#registerService(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation, org.eclipse.emf.common.util.EList)
	 * @generated
	 */
	EOperation getLocalServiceRegistry__RegisterService__ServiceProvider_ServiceImplementation_EList();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.LocalServiceRegistry#fireServiceEvent(org.eclipse.fennec.services.ServiceEvent) <em>Fire Service Event</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Fire Service Event</em>' operation.
	 * @see org.eclipse.fennec.services.LocalServiceRegistry#fireServiceEvent(org.eclipse.fennec.services.ServiceEvent)
	 * @generated
	 */
	EOperation getLocalServiceRegistry__FireServiceEvent__ServiceEvent();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.RemoteServiceRegistry <em>Remote Service Registry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Remote Service Registry</em>'.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry
	 * @generated
	 */
	EClass getRemoteServiceRegistry();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.RemoteServiceRegistry#getEndpoint <em>Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Endpoint</em>'.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry#getEndpoint()
	 * @see #getRemoteServiceRegistry()
	 * @generated
	 */
	EAttribute getRemoteServiceRegistry_Endpoint();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.RemoteServiceRegistry#getCatalog <em>Catalog</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Catalog</em>'.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry#getCatalog()
	 * @see #getRemoteServiceRegistry()
	 * @generated
	 */
	EReference getRemoteServiceRegistry_Catalog();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.RemoteServiceRegistry#getImplementations <em>Implementations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Implementations</em>'.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry#getImplementations()
	 * @see #getRemoteServiceRegistry()
	 * @generated
	 */
	EReference getRemoteServiceRegistry_Implementations();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.services.RemoteServiceRegistry#getProviders <em>Providers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Providers</em>'.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry#getProviders()
	 * @see #getRemoteServiceRegistry()
	 * @generated
	 */
	EReference getRemoteServiceRegistry_Providers();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.RemoteServiceRegistry#publishImplementation(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation) <em>Publish Implementation</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Publish Implementation</em>' operation.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry#publishImplementation(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation)
	 * @generated
	 */
	EOperation getRemoteServiceRegistry__PublishImplementation__ServiceProvider_ServiceImplementation();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.RemoteServiceRegistry#withdrawImplementation(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation) <em>Withdraw Implementation</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Withdraw Implementation</em>' operation.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry#withdrawImplementation(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation)
	 * @generated
	 */
	EOperation getRemoteServiceRegistry__WithdrawImplementation__ServiceProvider_ServiceImplementation();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.RemoteServiceRegistry#addCatalogEntry(org.eclipse.fennec.services.ServiceInterface, java.lang.String) <em>Add Catalog Entry</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Add Catalog Entry</em>' operation.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry#addCatalogEntry(org.eclipse.fennec.services.ServiceInterface, java.lang.String)
	 * @generated
	 */
	EOperation getRemoteServiceRegistry__AddCatalogEntry__ServiceInterface_String();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.RemoteServiceRegistry#deprecateCatalogEntry(org.eclipse.fennec.services.ServiceInterface, java.lang.String) <em>Deprecate Catalog Entry</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Deprecate Catalog Entry</em>' operation.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry#deprecateCatalogEntry(org.eclipse.fennec.services.ServiceInterface, java.lang.String)
	 * @generated
	 */
	EOperation getRemoteServiceRegistry__DeprecateCatalogEntry__ServiceInterface_String();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.RemoteServiceRegistry#removeCatalogEntry(org.eclipse.fennec.services.ServiceInterface, java.lang.String) <em>Remove Catalog Entry</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Remove Catalog Entry</em>' operation.
	 * @see org.eclipse.fennec.services.RemoteServiceRegistry#removeCatalogEntry(org.eclipse.fennec.services.ServiceInterface, java.lang.String)
	 * @generated
	 */
	EOperation getRemoteServiceRegistry__RemoveCatalogEntry__ServiceInterface_String();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.ConsumerCapability <em>Consumer Capability</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Consumer Capability</em>'.
	 * @see org.eclipse.fennec.services.ConsumerCapability
	 * @generated
	 */
	EClass getConsumerCapability();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.services.ConsumerCapability#getConsumerId <em>Consumer Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Consumer Id</em>'.
	 * @see org.eclipse.fennec.services.ConsumerCapability#getConsumerId()
	 * @see #getConsumerCapability()
	 * @generated
	 */
	EAttribute getConsumerCapability_ConsumerId();

	/**
	 * Returns the meta object for the attribute list '{@link org.eclipse.fennec.services.ConsumerCapability#getSupportedFlavors <em>Supported Flavors</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Supported Flavors</em>'.
	 * @see org.eclipse.fennec.services.ConsumerCapability#getSupportedFlavors()
	 * @see #getConsumerCapability()
	 * @generated
	 */
	EAttribute getConsumerCapability_SupportedFlavors();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.services.ConsumerCapability#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Properties</em>'.
	 * @see org.eclipse.fennec.services.ConsumerCapability#getProperties()
	 * @see #getConsumerCapability()
	 * @generated
	 */
	EReference getConsumerCapability_Properties();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.PublishHook <em>Publish Hook</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Publish Hook</em>'.
	 * @see org.eclipse.fennec.services.PublishHook
	 * @generated
	 */
	EClass getPublishHook();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.PublishHook#onPublish(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation) <em>On Publish</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>On Publish</em>' operation.
	 * @see org.eclipse.fennec.services.PublishHook#onPublish(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation)
	 * @generated
	 */
	EOperation getPublishHook__OnPublish__ServiceProvider_ServiceImplementation();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.PublishHook#onWithdraw(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation) <em>On Withdraw</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>On Withdraw</em>' operation.
	 * @see org.eclipse.fennec.services.PublishHook#onWithdraw(org.eclipse.fennec.services.ServiceProvider, org.eclipse.fennec.services.ServiceImplementation)
	 * @generated
	 */
	EOperation getPublishHook__OnWithdraw__ServiceProvider_ServiceImplementation();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.DiscoveryHook <em>Discovery Hook</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Discovery Hook</em>'.
	 * @see org.eclipse.fennec.services.DiscoveryHook
	 * @generated
	 */
	EClass getDiscoveryHook();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.DiscoveryHook#onLookup(java.lang.String, java.lang.String, org.eclipse.fennec.services.ConsumerCapability) <em>On Lookup</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>On Lookup</em>' operation.
	 * @see org.eclipse.fennec.services.DiscoveryHook#onLookup(java.lang.String, java.lang.String, org.eclipse.fennec.services.ConsumerCapability)
	 * @generated
	 */
	EOperation getDiscoveryHook__OnLookup__String_String_ConsumerCapability();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.DiscoveryHook#filterReferences(org.eclipse.fennec.services.ConsumerCapability, org.eclipse.emf.common.util.EList) <em>Filter References</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Filter References</em>' operation.
	 * @see org.eclipse.fennec.services.DiscoveryHook#filterReferences(org.eclipse.fennec.services.ConsumerCapability, org.eclipse.emf.common.util.EList)
	 * @generated
	 */
	EOperation getDiscoveryHook__FilterReferences__ConsumerCapability_EList();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.DiscoveryHook#onSubscribe(org.eclipse.fennec.services.ServiceListener) <em>On Subscribe</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>On Subscribe</em>' operation.
	 * @see org.eclipse.fennec.services.DiscoveryHook#onSubscribe(org.eclipse.fennec.services.ServiceListener)
	 * @generated
	 */
	EOperation getDiscoveryHook__OnSubscribe__ServiceListener();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.services.DistributionHook <em>Distribution Hook</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Distribution Hook</em>'.
	 * @see org.eclipse.fennec.services.DistributionHook
	 * @generated
	 */
	EClass getDistributionHook();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.DistributionHook#onOutbound(org.eclipse.fennec.services.ServiceEvent) <em>On Outbound</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>On Outbound</em>' operation.
	 * @see org.eclipse.fennec.services.DistributionHook#onOutbound(org.eclipse.fennec.services.ServiceEvent)
	 * @generated
	 */
	EOperation getDistributionHook__OnOutbound__ServiceEvent();

	/**
	 * Returns the meta object for the '{@link org.eclipse.fennec.services.DistributionHook#onInbound(org.eclipse.fennec.services.ServiceEvent) <em>On Inbound</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>On Inbound</em>' operation.
	 * @see org.eclipse.fennec.services.DistributionHook#onInbound(org.eclipse.fennec.services.ServiceEvent)
	 * @generated
	 */
	EOperation getDistributionHook__OnInbound__ServiceEvent();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ServiceScope <em>Service Scope</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Service Scope</em>'.
	 * @see org.eclipse.fennec.services.ServiceScope
	 * @generated
	 */
	EEnum getServiceScope();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ReferenceCardinality <em>Reference Cardinality</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Reference Cardinality</em>'.
	 * @see org.eclipse.fennec.services.ReferenceCardinality
	 * @generated
	 */
	EEnum getReferenceCardinality();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ReferencePolicy <em>Reference Policy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Reference Policy</em>'.
	 * @see org.eclipse.fennec.services.ReferencePolicy
	 * @generated
	 */
	EEnum getReferencePolicy();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ReferencePolicyOption <em>Reference Policy Option</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Reference Policy Option</em>'.
	 * @see org.eclipse.fennec.services.ReferencePolicyOption
	 * @generated
	 */
	EEnum getReferencePolicyOption();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ConfigurationPolicy <em>Configuration Policy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Configuration Policy</em>'.
	 * @see org.eclipse.fennec.services.ConfigurationPolicy
	 * @generated
	 */
	EEnum getConfigurationPolicy();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ComponentState <em>Component State</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Component State</em>'.
	 * @see org.eclipse.fennec.services.ComponentState
	 * @generated
	 */
	EEnum getComponentState();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ServiceEventType <em>Service Event Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Service Event Type</em>'.
	 * @see org.eclipse.fennec.services.ServiceEventType
	 * @generated
	 */
	EEnum getServiceEventType();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.FieldOption <em>Field Option</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Field Option</em>'.
	 * @see org.eclipse.fennec.services.FieldOption
	 * @generated
	 */
	EEnum getFieldOption();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.CollectionType <em>Collection Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Collection Type</em>'.
	 * @see org.eclipse.fennec.services.CollectionType
	 * @generated
	 */
	EEnum getCollectionType();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.LifecycleHookKind <em>Lifecycle Hook Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Lifecycle Hook Kind</em>'.
	 * @see org.eclipse.fennec.services.LifecycleHookKind
	 * @generated
	 */
	EEnum getLifecycleHookKind();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ReferenceBindingKind <em>Reference Binding Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Reference Binding Kind</em>'.
	 * @see org.eclipse.fennec.services.ReferenceBindingKind
	 * @generated
	 */
	EEnum getReferenceBindingKind();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.DiagnosticSeverity <em>Diagnostic Severity</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Diagnostic Severity</em>'.
	 * @see org.eclipse.fennec.services.DiagnosticSeverity
	 * @generated
	 */
	EEnum getDiagnosticSeverity();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.FlavorKind <em>Flavor Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Flavor Kind</em>'.
	 * @see org.eclipse.fennec.services.FlavorKind
	 * @generated
	 */
	EEnum getFlavorKind();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.HttpMethod <em>Http Method</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Http Method</em>'.
	 * @see org.eclipse.fennec.services.HttpMethod
	 * @generated
	 */
	EEnum getHttpMethod();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.MqttQos <em>Mqtt Qos</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Mqtt Qos</em>'.
	 * @see org.eclipse.fennec.services.MqttQos
	 * @generated
	 */
	EEnum getMqttQos();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.RegistryKind <em>Registry Kind</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Registry Kind</em>'.
	 * @see org.eclipse.fennec.services.RegistryKind
	 * @generated
	 */
	EEnum getRegistryKind();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ExpressionLanguage <em>Expression Language</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Expression Language</em>'.
	 * @see org.eclipse.fennec.services.ExpressionLanguage
	 * @generated
	 */
	EEnum getExpressionLanguage();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.CatalogStatus <em>Catalog Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Catalog Status</em>'.
	 * @see org.eclipse.fennec.services.CatalogStatus
	 * @generated
	 */
	EEnum getCatalogStatus();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.services.ConnectionState <em>Connection State</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Connection State</em>'.
	 * @see org.eclipse.fennec.services.ConnectionState
	 * @generated
	 */
	EEnum getConnectionState();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	ServicesFactory getServicesFactory();

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
		 * The meta object literal for the '{@link org.eclipse.fennec.services.NamedElement <em>Named Element</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.NamedElement
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getNamedElement()
		 * @generated
		 */
		EClass NAMED_ELEMENT = eINSTANCE.getNamedElement();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NAMED_ELEMENT__NAME = eINSTANCE.getNamedElement_Name();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.VersionedElement <em>Versioned Element</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.VersionedElement
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getVersionedElement()
		 * @generated
		 */
		EClass VERSIONED_ELEMENT = eINSTANCE.getVersionedElement();

		/**
		 * The meta object literal for the '<em><b>Version</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute VERSIONED_ELEMENT__VERSION = eINSTANCE.getVersionedElement_Version();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.PropertyImpl <em>Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.PropertyImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getProperty()
		 * @generated
		 */
		EClass PROPERTY = eINSTANCE.getProperty();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.StringPropertyImpl <em>String Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.StringPropertyImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getStringProperty()
		 * @generated
		 */
		EClass STRING_PROPERTY = eINSTANCE.getStringProperty();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STRING_PROPERTY__VALUE = eINSTANCE.getStringProperty_Value();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.IntPropertyImpl <em>Int Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.IntPropertyImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getIntProperty()
		 * @generated
		 */
		EClass INT_PROPERTY = eINSTANCE.getIntProperty();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute INT_PROPERTY__VALUE = eINSTANCE.getIntProperty_Value();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.LongPropertyImpl <em>Long Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.LongPropertyImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getLongProperty()
		 * @generated
		 */
		EClass LONG_PROPERTY = eINSTANCE.getLongProperty();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LONG_PROPERTY__VALUE = eINSTANCE.getLongProperty_Value();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.DoublePropertyImpl <em>Double Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.DoublePropertyImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDoubleProperty()
		 * @generated
		 */
		EClass DOUBLE_PROPERTY = eINSTANCE.getDoubleProperty();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DOUBLE_PROPERTY__VALUE = eINSTANCE.getDoubleProperty_Value();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.FloatPropertyImpl <em>Float Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.FloatPropertyImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getFloatProperty()
		 * @generated
		 */
		EClass FLOAT_PROPERTY = eINSTANCE.getFloatProperty();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FLOAT_PROPERTY__VALUE = eINSTANCE.getFloatProperty_Value();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ShortPropertyImpl <em>Short Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ShortPropertyImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getShortProperty()
		 * @generated
		 */
		EClass SHORT_PROPERTY = eINSTANCE.getShortProperty();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SHORT_PROPERTY__VALUE = eINSTANCE.getShortProperty_Value();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.BoolPropertyImpl <em>Bool Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.BoolPropertyImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getBoolProperty()
		 * @generated
		 */
		EClass BOOL_PROPERTY = eINSTANCE.getBoolProperty();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute BOOL_PROPERTY__VALUE = eINSTANCE.getBoolProperty_Value();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.StringListPropertyImpl <em>String List Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.StringListPropertyImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getStringListProperty()
		 * @generated
		 */
		EClass STRING_LIST_PROPERTY = eINSTANCE.getStringListProperty();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STRING_LIST_PROPERTY__VALUE = eINSTANCE.getStringListProperty_Value();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceOperationImpl <em>Service Operation</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceOperationImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceOperation()
		 * @generated
		 */
		EClass SERVICE_OPERATION = eINSTANCE.getServiceOperation();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_OPERATION__DESCRIPTION = eINSTANCE.getServiceOperation_Description();

		/**
		 * The meta object literal for the '<em><b>Parameters</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_OPERATION__PARAMETERS = eINSTANCE.getServiceOperation_Parameters();

		/**
		 * The meta object literal for the '<em><b>Return Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_OPERATION__RETURN_TYPE = eINSTANCE.getServiceOperation_ReturnType();

		/**
		 * The meta object literal for the '<em><b>Return Constraints</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_OPERATION__RETURN_CONSTRAINTS = eINSTANCE.getServiceOperation_ReturnConstraints();

		/**
		 * The meta object literal for the '<em><b>Exceptions</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_OPERATION__EXCEPTIONS = eINSTANCE.getServiceOperation_Exceptions();

		/**
		 * The meta object literal for the '<em><b>Preconditions</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_OPERATION__PRECONDITIONS = eINSTANCE.getServiceOperation_Preconditions();

		/**
		 * The meta object literal for the '<em><b>Postconditions</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_OPERATION__POSTCONDITIONS = eINSTANCE.getServiceOperation_Postconditions();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ParameterImpl <em>Parameter</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ParameterImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getParameter()
		 * @generated
		 */
		EClass PARAMETER = eINSTANCE.getParameter();

		/**
		 * The meta object literal for the '<em><b>Index</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PARAMETER__INDEX = eINSTANCE.getParameter_Index();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PARAMETER__TYPE = eINSTANCE.getParameter_Type();

		/**
		 * The meta object literal for the '<em><b>Optional</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PARAMETER__OPTIONAL = eINSTANCE.getParameter_Optional();

		/**
		 * The meta object literal for the '<em><b>Default Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PARAMETER__DEFAULT_VALUE = eINSTANCE.getParameter_DefaultValue();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PARAMETER__DESCRIPTION = eINSTANCE.getParameter_Description();

		/**
		 * The meta object literal for the '<em><b>Constraints</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference PARAMETER__CONSTRAINTS = eINSTANCE.getParameter_Constraints();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ParameterConstraintImpl <em>Parameter Constraint</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ParameterConstraintImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getParameterConstraint()
		 * @generated
		 */
		EClass PARAMETER_CONSTRAINT = eINSTANCE.getParameterConstraint();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.RequiredConstraintImpl <em>Required Constraint</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.RequiredConstraintImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRequiredConstraint()
		 * @generated
		 */
		EClass REQUIRED_CONSTRAINT = eINSTANCE.getRequiredConstraint();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.NumericRangeConstraintImpl <em>Numeric Range Constraint</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.NumericRangeConstraintImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getNumericRangeConstraint()
		 * @generated
		 */
		EClass NUMERIC_RANGE_CONSTRAINT = eINSTANCE.getNumericRangeConstraint();

		/**
		 * The meta object literal for the '<em><b>Min</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NUMERIC_RANGE_CONSTRAINT__MIN = eINSTANCE.getNumericRangeConstraint_Min();

		/**
		 * The meta object literal for the '<em><b>Max</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NUMERIC_RANGE_CONSTRAINT__MAX = eINSTANCE.getNumericRangeConstraint_Max();

		/**
		 * The meta object literal for the '<em><b>Inclusive Min</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MIN = eINSTANCE.getNumericRangeConstraint_InclusiveMin();

		/**
		 * The meta object literal for the '<em><b>Inclusive Max</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NUMERIC_RANGE_CONSTRAINT__INCLUSIVE_MAX = eINSTANCE.getNumericRangeConstraint_InclusiveMax();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.StringPatternConstraintImpl <em>String Pattern Constraint</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.StringPatternConstraintImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getStringPatternConstraint()
		 * @generated
		 */
		EClass STRING_PATTERN_CONSTRAINT = eINSTANCE.getStringPatternConstraint();

		/**
		 * The meta object literal for the '<em><b>Pattern</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STRING_PATTERN_CONSTRAINT__PATTERN = eINSTANCE.getStringPatternConstraint_Pattern();

		/**
		 * The meta object literal for the '<em><b>Min Length</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STRING_PATTERN_CONSTRAINT__MIN_LENGTH = eINSTANCE.getStringPatternConstraint_MinLength();

		/**
		 * The meta object literal for the '<em><b>Max Length</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STRING_PATTERN_CONSTRAINT__MAX_LENGTH = eINSTANCE.getStringPatternConstraint_MaxLength();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.EnumerationConstraintImpl <em>Enumeration Constraint</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.EnumerationConstraintImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getEnumerationConstraint()
		 * @generated
		 */
		EClass ENUMERATION_CONSTRAINT = eINSTANCE.getEnumerationConstraint();

		/**
		 * The meta object literal for the '<em><b>Allowed Values</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ENUMERATION_CONSTRAINT__ALLOWED_VALUES = eINSTANCE.getEnumerationConstraint_AllowedValues();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ExpressionConstraintImpl <em>Expression Constraint</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ExpressionConstraintImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getExpressionConstraint()
		 * @generated
		 */
		EClass EXPRESSION_CONSTRAINT = eINSTANCE.getExpressionConstraint();

		/**
		 * The meta object literal for the '<em><b>Language</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EXPRESSION_CONSTRAINT__LANGUAGE = eINSTANCE.getExpressionConstraint_Language();

		/**
		 * The meta object literal for the '<em><b>Expression</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EXPRESSION_CONSTRAINT__EXPRESSION = eINSTANCE.getExpressionConstraint_Expression();

		/**
		 * The meta object literal for the '<em><b>Message</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EXPRESSION_CONSTRAINT__MESSAGE = eINSTANCE.getExpressionConstraint_Message();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.InvariantImpl <em>Invariant</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.InvariantImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getInvariant()
		 * @generated
		 */
		EClass INVARIANT = eINSTANCE.getInvariant();

		/**
		 * The meta object literal for the '<em><b>Language</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute INVARIANT__LANGUAGE = eINSTANCE.getInvariant_Language();

		/**
		 * The meta object literal for the '<em><b>Expression</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute INVARIANT__EXPRESSION = eINSTANCE.getInvariant_Expression();

		/**
		 * The meta object literal for the '<em><b>Message</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute INVARIANT__MESSAGE = eINSTANCE.getInvariant_Message();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.CollectionSizeConstraintImpl <em>Collection Size Constraint</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.CollectionSizeConstraintImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getCollectionSizeConstraint()
		 * @generated
		 */
		EClass COLLECTION_SIZE_CONSTRAINT = eINSTANCE.getCollectionSizeConstraint();

		/**
		 * The meta object literal for the '<em><b>Min Size</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COLLECTION_SIZE_CONSTRAINT__MIN_SIZE = eINSTANCE.getCollectionSizeConstraint_MinSize();

		/**
		 * The meta object literal for the '<em><b>Max Size</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COLLECTION_SIZE_CONSTRAINT__MAX_SIZE = eINSTANCE.getCollectionSizeConstraint_MaxSize();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceExceptionImpl <em>Service Exception</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceExceptionImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceException()
		 * @generated
		 */
		EClass SERVICE_EXCEPTION = eINSTANCE.getServiceException();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_EXCEPTION__DESCRIPTION = eINSTANCE.getServiceException_Description();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_EXCEPTION__TYPE = eINSTANCE.getServiceException_Type();

		/**
		 * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_EXCEPTION__PROPERTIES = eINSTANCE.getServiceException_Properties();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceInterfaceImpl <em>Service Interface</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceInterfaceImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceInterface()
		 * @generated
		 */
		EClass SERVICE_INTERFACE = eINSTANCE.getServiceInterface();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_INTERFACE__DESCRIPTION = eINSTANCE.getServiceInterface_Description();

		/**
		 * The meta object literal for the '<em><b>Operations</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_INTERFACE__OPERATIONS = eINSTANCE.getServiceInterface_Operations();

		/**
		 * The meta object literal for the '<em><b>Exceptions</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_INTERFACE__EXCEPTIONS = eINSTANCE.getServiceInterface_Exceptions();

		/**
		 * The meta object literal for the '<em><b>Invariants</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_INTERFACE__INVARIANTS = eINSTANCE.getServiceInterface_Invariants();

		/**
		 * The meta object literal for the '<em><b>Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_INTERFACE__STATUS = eINSTANCE.getServiceInterface_Status();

		/**
		 * The meta object literal for the '<em><b>Deprecation Reason</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_INTERFACE__DEPRECATION_REASON = eINSTANCE.getServiceInterface_DeprecationReason();

		/**
		 * The meta object literal for the '<em><b>Replaced By</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_INTERFACE__REPLACED_BY = eINSTANCE.getServiceInterface_ReplacedBy();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.LifecycleHookImpl <em>Lifecycle Hook</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.LifecycleHookImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getLifecycleHook()
		 * @generated
		 */
		EClass LIFECYCLE_HOOK = eINSTANCE.getLifecycleHook();

		/**
		 * The meta object literal for the '<em><b>Kind</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIFECYCLE_HOOK__KIND = eINSTANCE.getLifecycleHook_Kind();

		/**
		 * The meta object literal for the '<em><b>Parameter</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIFECYCLE_HOOK__PARAMETER = eINSTANCE.getLifecycleHook_Parameter();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ReferenceBindingImpl <em>Reference Binding</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ReferenceBindingImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferenceBinding()
		 * @generated
		 */
		EClass REFERENCE_BINDING = eINSTANCE.getReferenceBinding();

		/**
		 * The meta object literal for the '<em><b>Kind</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REFERENCE_BINDING__KIND = eINSTANCE.getReferenceBinding_Kind();

		/**
		 * The meta object literal for the '<em><b>Field Option</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REFERENCE_BINDING__FIELD_OPTION = eINSTANCE.getReferenceBinding_FieldOption();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ComponentReferenceImpl <em>Component Reference</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ComponentReferenceImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getComponentReference()
		 * @generated
		 */
		EClass COMPONENT_REFERENCE = eINSTANCE.getComponentReference();

		/**
		 * The meta object literal for the '<em><b>Interface Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_REFERENCE__INTERFACE_NAME = eINSTANCE.getComponentReference_InterfaceName();

		/**
		 * The meta object literal for the '<em><b>Cardinality</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_REFERENCE__CARDINALITY = eINSTANCE.getComponentReference_Cardinality();

		/**
		 * The meta object literal for the '<em><b>Policy</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_REFERENCE__POLICY = eINSTANCE.getComponentReference_Policy();

		/**
		 * The meta object literal for the '<em><b>Policy Option</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_REFERENCE__POLICY_OPTION = eINSTANCE.getComponentReference_PolicyOption();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_REFERENCE__TARGET = eINSTANCE.getComponentReference_Target();

		/**
		 * The meta object literal for the '<em><b>Scope</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_REFERENCE__SCOPE = eINSTANCE.getComponentReference_Scope();

		/**
		 * The meta object literal for the '<em><b>Collection Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_REFERENCE__COLLECTION_TYPE = eINSTANCE.getComponentReference_CollectionType();

		/**
		 * The meta object literal for the '<em><b>Parameter</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_REFERENCE__PARAMETER = eINSTANCE.getComponentReference_Parameter();

		/**
		 * The meta object literal for the '<em><b>Bindings</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_REFERENCE__BINDINGS = eINSTANCE.getComponentReference_Bindings();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ComponentDescriptionImpl <em>Component Description</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ComponentDescriptionImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getComponentDescription()
		 * @generated
		 */
		EClass COMPONENT_DESCRIPTION = eINSTANCE.getComponentDescription();

		/**
		 * The meta object literal for the '<em><b>Factory</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_DESCRIPTION__FACTORY = eINSTANCE.getComponentDescription_Factory();

		/**
		 * The meta object literal for the '<em><b>Scope</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_DESCRIPTION__SCOPE = eINSTANCE.getComponentDescription_Scope();

		/**
		 * The meta object literal for the '<em><b>Implementation Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_DESCRIPTION__IMPLEMENTATION_ID = eINSTANCE.getComponentDescription_ImplementationId();

		/**
		 * The meta object literal for the '<em><b>Default Enabled</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_DESCRIPTION__DEFAULT_ENABLED = eINSTANCE.getComponentDescription_DefaultEnabled();

		/**
		 * The meta object literal for the '<em><b>Immediate</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_DESCRIPTION__IMMEDIATE = eINSTANCE.getComponentDescription_Immediate();

		/**
		 * The meta object literal for the '<em><b>Configuration Policy</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_DESCRIPTION__CONFIGURATION_POLICY = eINSTANCE.getComponentDescription_ConfigurationPolicy();

		/**
		 * The meta object literal for the '<em><b>Configuration Pid</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_DESCRIPTION__CONFIGURATION_PID = eINSTANCE.getComponentDescription_ConfigurationPid();

		/**
		 * The meta object literal for the '<em><b>Service Interfaces</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_DESCRIPTION__SERVICE_INTERFACES = eINSTANCE.getComponentDescription_ServiceInterfaces();

		/**
		 * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_DESCRIPTION__PROPERTIES = eINSTANCE.getComponentDescription_Properties();

		/**
		 * The meta object literal for the '<em><b>Factory Properties</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_DESCRIPTION__FACTORY_PROPERTIES = eINSTANCE.getComponentDescription_FactoryProperties();

		/**
		 * The meta object literal for the '<em><b>References</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_DESCRIPTION__REFERENCES = eINSTANCE.getComponentDescription_References();

		/**
		 * The meta object literal for the '<em><b>Lifecycle Hooks</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_DESCRIPTION__LIFECYCLE_HOOKS = eINSTANCE.getComponentDescription_LifecycleHooks();

		/**
		 * The meta object literal for the '<em><b>Provider</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_DESCRIPTION__PROVIDER = eINSTANCE.getComponentDescription_Provider();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceProviderImpl <em>Service Provider</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceProviderImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceProvider()
		 * @generated
		 */
		EClass SERVICE_PROVIDER = eINSTANCE.getServiceProvider();

		/**
		 * The meta object literal for the '<em><b>Symbolic Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_PROVIDER__SYMBOLIC_NAME = eINSTANCE.getServiceProvider_SymbolicName();

		/**
		 * The meta object literal for the '<em><b>Descriptions</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_PROVIDER__DESCRIPTIONS = eINSTANCE.getServiceProvider_Descriptions();

		/**
		 * The meta object literal for the '<em><b>Implementations</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_PROVIDER__IMPLEMENTATIONS = eINSTANCE.getServiceProvider_Implementations();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceImplementationImpl <em>Service Implementation</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceImplementationImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceImplementation()
		 * @generated
		 */
		EClass SERVICE_IMPLEMENTATION = eINSTANCE.getServiceImplementation();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_IMPLEMENTATION__DESCRIPTION = eINSTANCE.getServiceImplementation_Description();

		/**
		 * The meta object literal for the '<em><b>Implementation Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_IMPLEMENTATION__IMPLEMENTATION_ID = eINSTANCE.getServiceImplementation_ImplementationId();

		/**
		 * The meta object literal for the '<em><b>Service Interfaces</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_IMPLEMENTATION__SERVICE_INTERFACES = eINSTANCE.getServiceImplementation_ServiceInterfaces();

		/**
		 * The meta object literal for the '<em><b>Flavors</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_IMPLEMENTATION__FLAVORS = eINSTANCE.getServiceImplementation_Flavors();

		/**
		 * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_IMPLEMENTATION__PROPERTIES = eINSTANCE.getServiceImplementation_Properties();

		/**
		 * The meta object literal for the '<em><b>Component Description</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_IMPLEMENTATION__COMPONENT_DESCRIPTION = eINSTANCE.getServiceImplementation_ComponentDescription();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceFlavorImpl <em>Service Flavor</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceFlavorImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceFlavor()
		 * @generated
		 */
		EClass SERVICE_FLAVOR = eINSTANCE.getServiceFlavor();

		/**
		 * The meta object literal for the '<em><b>Kind</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_FLAVOR__KIND = eINSTANCE.getServiceFlavor_Kind();

		/**
		 * The meta object literal for the '<em><b>Operation Flavors</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_FLAVOR__OPERATION_FLAVORS = eINSTANCE.getServiceFlavor_OperationFlavors();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.RestFlavorImpl <em>Rest Flavor</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.RestFlavorImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRestFlavor()
		 * @generated
		 */
		EClass REST_FLAVOR = eINSTANCE.getRestFlavor();

		/**
		 * The meta object literal for the '<em><b>Host</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REST_FLAVOR__HOST = eINSTANCE.getRestFlavor_Host();

		/**
		 * The meta object literal for the '<em><b>Base Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REST_FLAVOR__BASE_PATH = eINSTANCE.getRestFlavor_BasePath();

		/**
		 * The meta object literal for the '<em><b>Content Types</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REST_FLAVOR__CONTENT_TYPES = eINSTANCE.getRestFlavor_ContentTypes();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.MqttFlavorImpl <em>Mqtt Flavor</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.MqttFlavorImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getMqttFlavor()
		 * @generated
		 */
		EClass MQTT_FLAVOR = eINSTANCE.getMqttFlavor();

		/**
		 * The meta object literal for the '<em><b>Brokers</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_FLAVOR__BROKERS = eINSTANCE.getMqttFlavor_Brokers();

		/**
		 * The meta object literal for the '<em><b>Request Topic</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_FLAVOR__REQUEST_TOPIC = eINSTANCE.getMqttFlavor_RequestTopic();

		/**
		 * The meta object literal for the '<em><b>Response Topic</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_FLAVOR__RESPONSE_TOPIC = eINSTANCE.getMqttFlavor_ResponseTopic();

		/**
		 * The meta object literal for the '<em><b>Default Qos</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_FLAVOR__DEFAULT_QOS = eINSTANCE.getMqttFlavor_DefaultQos();

		/**
		 * The meta object literal for the '<em><b>Default Retained</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_FLAVOR__DEFAULT_RETAINED = eINSTANCE.getMqttFlavor_DefaultRetained();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceOperationFlavorImpl <em>Service Operation Flavor</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceOperationFlavorImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceOperationFlavor()
		 * @generated
		 */
		EClass SERVICE_OPERATION_FLAVOR = eINSTANCE.getServiceOperationFlavor();

		/**
		 * The meta object literal for the '<em><b>Operation</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_OPERATION_FLAVOR__OPERATION = eINSTANCE.getServiceOperationFlavor_Operation();

		/**
		 * The meta object literal for the '<em><b>Consumes</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_OPERATION_FLAVOR__CONSUMES = eINSTANCE.getServiceOperationFlavor_Consumes();

		/**
		 * The meta object literal for the '<em><b>Produces</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_OPERATION_FLAVOR__PRODUCES = eINSTANCE.getServiceOperationFlavor_Produces();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.RestOperationFlavorImpl <em>Rest Operation Flavor</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.RestOperationFlavorImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRestOperationFlavor()
		 * @generated
		 */
		EClass REST_OPERATION_FLAVOR = eINSTANCE.getRestOperationFlavor();

		/**
		 * The meta object literal for the '<em><b>Method</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REST_OPERATION_FLAVOR__METHOD = eINSTANCE.getRestOperationFlavor_Method();

		/**
		 * The meta object literal for the '<em><b>Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REST_OPERATION_FLAVOR__PATH = eINSTANCE.getRestOperationFlavor_Path();

		/**
		 * The meta object literal for the '<em><b>Return Codes</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REST_OPERATION_FLAVOR__RETURN_CODES = eINSTANCE.getRestOperationFlavor_ReturnCodes();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.MqttOperationFlavorImpl <em>Mqtt Operation Flavor</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.MqttOperationFlavorImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getMqttOperationFlavor()
		 * @generated
		 */
		EClass MQTT_OPERATION_FLAVOR = eINSTANCE.getMqttOperationFlavor();

		/**
		 * The meta object literal for the '<em><b>Request Topic</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_OPERATION_FLAVOR__REQUEST_TOPIC = eINSTANCE.getMqttOperationFlavor_RequestTopic();

		/**
		 * The meta object literal for the '<em><b>Response Topic</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_OPERATION_FLAVOR__RESPONSE_TOPIC = eINSTANCE.getMqttOperationFlavor_ResponseTopic();

		/**
		 * The meta object literal for the '<em><b>Qos</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_OPERATION_FLAVOR__QOS = eINSTANCE.getMqttOperationFlavor_Qos();

		/**
		 * The meta object literal for the '<em><b>Retained</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_OPERATION_FLAVOR__RETAINED = eINSTANCE.getMqttOperationFlavor_Retained();

		/**
		 * The meta object literal for the '<em><b>Correlation</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_OPERATION_FLAVOR__CORRELATION = eINSTANCE.getMqttOperationFlavor_Correlation();

		/**
		 * The meta object literal for the '<em><b>Return Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MQTT_OPERATION_FLAVOR__RETURN_PATH = eINSTANCE.getMqttOperationFlavor_ReturnPath();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceReferenceImpl <em>Service Reference</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceReferenceImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceReference()
		 * @generated
		 */
		EClass SERVICE_REFERENCE = eINSTANCE.getServiceReference();

		/**
		 * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_REFERENCE__ID = eINSTANCE.getServiceReference_Id();

		/**
		 * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REFERENCE__PROPERTIES = eINSTANCE.getServiceReference_Properties();

		/**
		 * The meta object literal for the '<em><b>Provider</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REFERENCE__PROVIDER = eINSTANCE.getServiceReference_Provider();

		/**
		 * The meta object literal for the '<em><b>Registration</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REFERENCE__REGISTRATION = eINSTANCE.getServiceReference_Registration();

		/**
		 * The meta object literal for the '<em><b>Get Property</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_REFERENCE___GET_PROPERTY__STRING = eINSTANCE.getServiceReference__GetProperty__String();

		/**
		 * The meta object literal for the '<em><b>Get Property Keys</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_REFERENCE___GET_PROPERTY_KEYS = eINSTANCE.getServiceReference__GetPropertyKeys();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceRegistrationImpl <em>Service Registration</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceRegistrationImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceRegistration()
		 * @generated
		 */
		EClass SERVICE_REGISTRATION = eINSTANCE.getServiceRegistration();

		/**
		 * The meta object literal for the '<em><b>Reference</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REGISTRATION__REFERENCE = eINSTANCE.getServiceRegistration_Reference();

		/**
		 * The meta object literal for the '<em><b>Unregistered</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_REGISTRATION__UNREGISTERED = eINSTANCE.getServiceRegistration_Unregistered();

		/**
		 * The meta object literal for the '<em><b>Provider</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REGISTRATION__PROVIDER = eINSTANCE.getServiceRegistration_Provider();

		/**
		 * The meta object literal for the '<em><b>Implementation</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REGISTRATION__IMPLEMENTATION = eINSTANCE.getServiceRegistration_Implementation();

		/**
		 * The meta object literal for the '<em><b>Using Sessions</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REGISTRATION__USING_SESSIONS = eINSTANCE.getServiceRegistration_UsingSessions();

		/**
		 * The meta object literal for the '<em><b>Consumer Count</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_REGISTRATION__CONSUMER_COUNT = eINSTANCE.getServiceRegistration_ConsumerCount();

		/**
		 * The meta object literal for the '<em><b>Unregister</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_REGISTRATION___UNREGISTER = eINSTANCE.getServiceRegistration__Unregister();

		/**
		 * The meta object literal for the '<em><b>Set Properties</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_REGISTRATION___SET_PROPERTIES__ELIST = eINSTANCE.getServiceRegistration__SetProperties__EList();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ConsumerSessionImpl <em>Consumer Session</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ConsumerSessionImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getConsumerSession()
		 * @generated
		 */
		EClass CONSUMER_SESSION = eINSTANCE.getConsumerSession();

		/**
		 * The meta object literal for the '<em><b>Consumer Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CONSUMER_SESSION__CONSUMER_ID = eINSTANCE.getConsumerSession_ConsumerId();

		/**
		 * The meta object literal for the '<em><b>Last Renewal</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CONSUMER_SESSION__LAST_RENEWAL = eINSTANCE.getConsumerSession_LastRenewal();

		/**
		 * The meta object literal for the '<em><b>Capabilities</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CONSUMER_SESSION__CAPABILITIES = eINSTANCE.getConsumerSession_Capabilities();

		/**
		 * The meta object literal for the '<em><b>Acquisitions</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CONSUMER_SESSION__ACQUISITIONS = eINSTANCE.getConsumerSession_Acquisitions();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ComponentConfigurationImpl <em>Component Configuration</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ComponentConfigurationImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getComponentConfiguration()
		 * @generated
		 */
		EClass COMPONENT_CONFIGURATION = eINSTANCE.getComponentConfiguration();

		/**
		 * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_CONFIGURATION__ID = eINSTANCE.getComponentConfiguration_Id();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_CONFIGURATION__DESCRIPTION = eINSTANCE.getComponentConfiguration_Description();

		/**
		 * The meta object literal for the '<em><b>State</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_CONFIGURATION__STATE = eINSTANCE.getComponentConfiguration_State();

		/**
		 * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_CONFIGURATION__PROPERTIES = eINSTANCE.getComponentConfiguration_Properties();

		/**
		 * The meta object literal for the '<em><b>Satisfied References</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_CONFIGURATION__SATISFIED_REFERENCES = eINSTANCE.getComponentConfiguration_SatisfiedReferences();

		/**
		 * The meta object literal for the '<em><b>Unsatisfied References</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_CONFIGURATION__UNSATISFIED_REFERENCES = eINSTANCE.getComponentConfiguration_UnsatisfiedReferences();

		/**
		 * The meta object literal for the '<em><b>Failure</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_CONFIGURATION__FAILURE = eINSTANCE.getComponentConfiguration_Failure();

		/**
		 * The meta object literal for the '<em><b>Service</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_CONFIGURATION__SERVICE = eINSTANCE.getComponentConfiguration_Service();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.SatisfiedReferenceImpl <em>Satisfied Reference</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.SatisfiedReferenceImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getSatisfiedReference()
		 * @generated
		 */
		EClass SATISFIED_REFERENCE = eINSTANCE.getSatisfiedReference();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SATISFIED_REFERENCE__NAME = eINSTANCE.getSatisfiedReference_Name();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SATISFIED_REFERENCE__TARGET = eINSTANCE.getSatisfiedReference_Target();

		/**
		 * The meta object literal for the '<em><b>Bound Services</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SATISFIED_REFERENCE__BOUND_SERVICES = eINSTANCE.getSatisfiedReference_BoundServices();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.UnsatisfiedReferenceImpl <em>Unsatisfied Reference</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.UnsatisfiedReferenceImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getUnsatisfiedReference()
		 * @generated
		 */
		EClass UNSATISFIED_REFERENCE = eINSTANCE.getUnsatisfiedReference();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute UNSATISFIED_REFERENCE__NAME = eINSTANCE.getUnsatisfiedReference_Name();

		/**
		 * The meta object literal for the '<em><b>Target</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute UNSATISFIED_REFERENCE__TARGET = eINSTANCE.getUnsatisfiedReference_Target();

		/**
		 * The meta object literal for the '<em><b>Target Services</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference UNSATISFIED_REFERENCE__TARGET_SERVICES = eINSTANCE.getUnsatisfiedReference_TargetServices();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.DiagnosticImpl <em>Diagnostic</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.DiagnosticImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDiagnostic()
		 * @generated
		 */
		EClass DIAGNOSTIC = eINSTANCE.getDiagnostic();

		/**
		 * The meta object literal for the '<em><b>Severity</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DIAGNOSTIC__SEVERITY = eINSTANCE.getDiagnostic_Severity();

		/**
		 * The meta object literal for the '<em><b>Message</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DIAGNOSTIC__MESSAGE = eINSTANCE.getDiagnostic_Message();

		/**
		 * The meta object literal for the '<em><b>Source</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DIAGNOSTIC__SOURCE = eINSTANCE.getDiagnostic_Source();

		/**
		 * The meta object literal for the '<em><b>Code</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DIAGNOSTIC__CODE = eINSTANCE.getDiagnostic_Code();

		/**
		 * The meta object literal for the '<em><b>Data</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DIAGNOSTIC__DATA = eINSTANCE.getDiagnostic_Data();

		/**
		 * The meta object literal for the '<em><b>Children</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DIAGNOSTIC__CHILDREN = eINSTANCE.getDiagnostic_Children();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceEventImpl <em>Service Event</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceEventImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceEvent()
		 * @generated
		 */
		EClass SERVICE_EVENT = eINSTANCE.getServiceEvent();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_EVENT__TYPE = eINSTANCE.getServiceEvent_Type();

		/**
		 * The meta object literal for the '<em><b>Reference</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_EVENT__REFERENCE = eINSTANCE.getServiceEvent_Reference();

		/**
		 * The meta object literal for the '<em><b>Timestamp</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_EVENT__TIMESTAMP = eINSTANCE.getServiceEvent_Timestamp();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ServiceListener <em>Service Listener</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ServiceListener
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceListener()
		 * @generated
		 */
		EClass SERVICE_LISTENER = eINSTANCE.getServiceListener();

		/**
		 * The meta object literal for the '<em><b>Filter</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_LISTENER__FILTER = eINSTANCE.getServiceListener_Filter();

		/**
		 * The meta object literal for the '<em><b>Service Changed</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_LISTENER___SERVICE_CHANGED__SERVICEEVENT = eINSTANCE.getServiceListener__ServiceChanged__ServiceEvent();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ServiceRegistryImpl <em>Service Registry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ServiceRegistryImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceRegistry()
		 * @generated
		 */
		EClass SERVICE_REGISTRY = eINSTANCE.getServiceRegistry();

		/**
		 * The meta object literal for the '<em><b>Kind</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVICE_REGISTRY__KIND = eINSTANCE.getServiceRegistry_Kind();

		/**
		 * The meta object literal for the '<em><b>Publish Hooks</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REGISTRY__PUBLISH_HOOKS = eINSTANCE.getServiceRegistry_PublishHooks();

		/**
		 * The meta object literal for the '<em><b>Discovery Hooks</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REGISTRY__DISCOVERY_HOOKS = eINSTANCE.getServiceRegistry_DiscoveryHooks();

		/**
		 * The meta object literal for the '<em><b>Distribution Hooks</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVICE_REGISTRY__DISTRIBUTION_HOOKS = eINSTANCE.getServiceRegistry_DistributionHooks();

		/**
		 * The meta object literal for the '<em><b>Get Service Reference</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_REGISTRY___GET_SERVICE_REFERENCE__STRING = eINSTANCE.getServiceRegistry__GetServiceReference__String();

		/**
		 * The meta object literal for the '<em><b>Get Service References</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_REGISTRY___GET_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY = eINSTANCE.getServiceRegistry__GetServiceReferences__String_String_ConsumerCapability();

		/**
		 * The meta object literal for the '<em><b>Get All Service References</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_REGISTRY___GET_ALL_SERVICE_REFERENCES__STRING_STRING_CONSUMERCAPABILITY = eINSTANCE.getServiceRegistry__GetAllServiceReferences__String_String_ConsumerCapability();

		/**
		 * The meta object literal for the '<em><b>Add Service Listener</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_REGISTRY___ADD_SERVICE_LISTENER__SERVICELISTENER = eINSTANCE.getServiceRegistry__AddServiceListener__ServiceListener();

		/**
		 * The meta object literal for the '<em><b>Remove Service Listener</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation SERVICE_REGISTRY___REMOVE_SERVICE_LISTENER__SERVICELISTENER = eINSTANCE.getServiceRegistry__RemoveServiceListener__ServiceListener();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.LocalServiceRegistryImpl <em>Local Service Registry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.LocalServiceRegistryImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getLocalServiceRegistry()
		 * @generated
		 */
		EClass LOCAL_SERVICE_REGISTRY = eINSTANCE.getLocalServiceRegistry();

		/**
		 * The meta object literal for the '<em><b>References</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LOCAL_SERVICE_REGISTRY__REFERENCES = eINSTANCE.getLocalServiceRegistry_References();

		/**
		 * The meta object literal for the '<em><b>Registrations</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LOCAL_SERVICE_REGISTRY__REGISTRATIONS = eINSTANCE.getLocalServiceRegistry_Registrations();

		/**
		 * The meta object literal for the '<em><b>Sessions</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LOCAL_SERVICE_REGISTRY__SESSIONS = eINSTANCE.getLocalServiceRegistry_Sessions();

		/**
		 * The meta object literal for the '<em><b>Configurations</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LOCAL_SERVICE_REGISTRY__CONFIGURATIONS = eINSTANCE.getLocalServiceRegistry_Configurations();

		/**
		 * The meta object literal for the '<em><b>Providers</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LOCAL_SERVICE_REGISTRY__PROVIDERS = eINSTANCE.getLocalServiceRegistry_Providers();

		/**
		 * The meta object literal for the '<em><b>Listeners</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LOCAL_SERVICE_REGISTRY__LISTENERS = eINSTANCE.getLocalServiceRegistry_Listeners();

		/**
		 * The meta object literal for the '<em><b>Remote</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LOCAL_SERVICE_REGISTRY__REMOTE = eINSTANCE.getLocalServiceRegistry_Remote();

		/**
		 * The meta object literal for the '<em><b>Connection State</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LOCAL_SERVICE_REGISTRY__CONNECTION_STATE = eINSTANCE.getLocalServiceRegistry_ConnectionState();

		/**
		 * The meta object literal for the '<em><b>Register Service</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation LOCAL_SERVICE_REGISTRY___REGISTER_SERVICE__SERVICEPROVIDER_SERVICEIMPLEMENTATION_ELIST = eINSTANCE.getLocalServiceRegistry__RegisterService__ServiceProvider_ServiceImplementation_EList();

		/**
		 * The meta object literal for the '<em><b>Fire Service Event</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation LOCAL_SERVICE_REGISTRY___FIRE_SERVICE_EVENT__SERVICEEVENT = eINSTANCE.getLocalServiceRegistry__FireServiceEvent__ServiceEvent();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.RemoteServiceRegistryImpl <em>Remote Service Registry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.RemoteServiceRegistryImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRemoteServiceRegistry()
		 * @generated
		 */
		EClass REMOTE_SERVICE_REGISTRY = eINSTANCE.getRemoteServiceRegistry();

		/**
		 * The meta object literal for the '<em><b>Endpoint</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REMOTE_SERVICE_REGISTRY__ENDPOINT = eINSTANCE.getRemoteServiceRegistry_Endpoint();

		/**
		 * The meta object literal for the '<em><b>Catalog</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference REMOTE_SERVICE_REGISTRY__CATALOG = eINSTANCE.getRemoteServiceRegistry_Catalog();

		/**
		 * The meta object literal for the '<em><b>Implementations</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference REMOTE_SERVICE_REGISTRY__IMPLEMENTATIONS = eINSTANCE.getRemoteServiceRegistry_Implementations();

		/**
		 * The meta object literal for the '<em><b>Providers</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference REMOTE_SERVICE_REGISTRY__PROVIDERS = eINSTANCE.getRemoteServiceRegistry_Providers();

		/**
		 * The meta object literal for the '<em><b>Publish Implementation</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation REMOTE_SERVICE_REGISTRY___PUBLISH_IMPLEMENTATION__SERVICEPROVIDER_SERVICEIMPLEMENTATION = eINSTANCE.getRemoteServiceRegistry__PublishImplementation__ServiceProvider_ServiceImplementation();

		/**
		 * The meta object literal for the '<em><b>Withdraw Implementation</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation REMOTE_SERVICE_REGISTRY___WITHDRAW_IMPLEMENTATION__SERVICEPROVIDER_SERVICEIMPLEMENTATION = eINSTANCE.getRemoteServiceRegistry__WithdrawImplementation__ServiceProvider_ServiceImplementation();

		/**
		 * The meta object literal for the '<em><b>Add Catalog Entry</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation REMOTE_SERVICE_REGISTRY___ADD_CATALOG_ENTRY__SERVICEINTERFACE_STRING = eINSTANCE.getRemoteServiceRegistry__AddCatalogEntry__ServiceInterface_String();

		/**
		 * The meta object literal for the '<em><b>Deprecate Catalog Entry</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation REMOTE_SERVICE_REGISTRY___DEPRECATE_CATALOG_ENTRY__SERVICEINTERFACE_STRING = eINSTANCE.getRemoteServiceRegistry__DeprecateCatalogEntry__ServiceInterface_String();

		/**
		 * The meta object literal for the '<em><b>Remove Catalog Entry</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation REMOTE_SERVICE_REGISTRY___REMOVE_CATALOG_ENTRY__SERVICEINTERFACE_STRING = eINSTANCE.getRemoteServiceRegistry__RemoveCatalogEntry__ServiceInterface_String();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.impl.ConsumerCapabilityImpl <em>Consumer Capability</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.impl.ConsumerCapabilityImpl
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getConsumerCapability()
		 * @generated
		 */
		EClass CONSUMER_CAPABILITY = eINSTANCE.getConsumerCapability();

		/**
		 * The meta object literal for the '<em><b>Consumer Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CONSUMER_CAPABILITY__CONSUMER_ID = eINSTANCE.getConsumerCapability_ConsumerId();

		/**
		 * The meta object literal for the '<em><b>Supported Flavors</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CONSUMER_CAPABILITY__SUPPORTED_FLAVORS = eINSTANCE.getConsumerCapability_SupportedFlavors();

		/**
		 * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CONSUMER_CAPABILITY__PROPERTIES = eINSTANCE.getConsumerCapability_Properties();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.PublishHook <em>Publish Hook</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.PublishHook
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getPublishHook()
		 * @generated
		 */
		EClass PUBLISH_HOOK = eINSTANCE.getPublishHook();

		/**
		 * The meta object literal for the '<em><b>On Publish</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation PUBLISH_HOOK___ON_PUBLISH__SERVICEPROVIDER_SERVICEIMPLEMENTATION = eINSTANCE.getPublishHook__OnPublish__ServiceProvider_ServiceImplementation();

		/**
		 * The meta object literal for the '<em><b>On Withdraw</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation PUBLISH_HOOK___ON_WITHDRAW__SERVICEPROVIDER_SERVICEIMPLEMENTATION = eINSTANCE.getPublishHook__OnWithdraw__ServiceProvider_ServiceImplementation();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.DiscoveryHook <em>Discovery Hook</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.DiscoveryHook
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDiscoveryHook()
		 * @generated
		 */
		EClass DISCOVERY_HOOK = eINSTANCE.getDiscoveryHook();

		/**
		 * The meta object literal for the '<em><b>On Lookup</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation DISCOVERY_HOOK___ON_LOOKUP__STRING_STRING_CONSUMERCAPABILITY = eINSTANCE.getDiscoveryHook__OnLookup__String_String_ConsumerCapability();

		/**
		 * The meta object literal for the '<em><b>Filter References</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation DISCOVERY_HOOK___FILTER_REFERENCES__CONSUMERCAPABILITY_ELIST = eINSTANCE.getDiscoveryHook__FilterReferences__ConsumerCapability_EList();

		/**
		 * The meta object literal for the '<em><b>On Subscribe</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation DISCOVERY_HOOK___ON_SUBSCRIBE__SERVICELISTENER = eINSTANCE.getDiscoveryHook__OnSubscribe__ServiceListener();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.DistributionHook <em>Distribution Hook</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.DistributionHook
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDistributionHook()
		 * @generated
		 */
		EClass DISTRIBUTION_HOOK = eINSTANCE.getDistributionHook();

		/**
		 * The meta object literal for the '<em><b>On Outbound</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation DISTRIBUTION_HOOK___ON_OUTBOUND__SERVICEEVENT = eINSTANCE.getDistributionHook__OnOutbound__ServiceEvent();

		/**
		 * The meta object literal for the '<em><b>On Inbound</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation DISTRIBUTION_HOOK___ON_INBOUND__SERVICEEVENT = eINSTANCE.getDistributionHook__OnInbound__ServiceEvent();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ServiceScope <em>Service Scope</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ServiceScope
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceScope()
		 * @generated
		 */
		EEnum SERVICE_SCOPE = eINSTANCE.getServiceScope();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ReferenceCardinality <em>Reference Cardinality</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ReferenceCardinality
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferenceCardinality()
		 * @generated
		 */
		EEnum REFERENCE_CARDINALITY = eINSTANCE.getReferenceCardinality();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ReferencePolicy <em>Reference Policy</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ReferencePolicy
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferencePolicy()
		 * @generated
		 */
		EEnum REFERENCE_POLICY = eINSTANCE.getReferencePolicy();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ReferencePolicyOption <em>Reference Policy Option</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ReferencePolicyOption
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferencePolicyOption()
		 * @generated
		 */
		EEnum REFERENCE_POLICY_OPTION = eINSTANCE.getReferencePolicyOption();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ConfigurationPolicy <em>Configuration Policy</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ConfigurationPolicy
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getConfigurationPolicy()
		 * @generated
		 */
		EEnum CONFIGURATION_POLICY = eINSTANCE.getConfigurationPolicy();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ComponentState <em>Component State</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ComponentState
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getComponentState()
		 * @generated
		 */
		EEnum COMPONENT_STATE = eINSTANCE.getComponentState();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ServiceEventType <em>Service Event Type</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ServiceEventType
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getServiceEventType()
		 * @generated
		 */
		EEnum SERVICE_EVENT_TYPE = eINSTANCE.getServiceEventType();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.FieldOption <em>Field Option</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.FieldOption
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getFieldOption()
		 * @generated
		 */
		EEnum FIELD_OPTION = eINSTANCE.getFieldOption();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.CollectionType <em>Collection Type</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.CollectionType
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getCollectionType()
		 * @generated
		 */
		EEnum COLLECTION_TYPE = eINSTANCE.getCollectionType();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.LifecycleHookKind <em>Lifecycle Hook Kind</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.LifecycleHookKind
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getLifecycleHookKind()
		 * @generated
		 */
		EEnum LIFECYCLE_HOOK_KIND = eINSTANCE.getLifecycleHookKind();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ReferenceBindingKind <em>Reference Binding Kind</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ReferenceBindingKind
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getReferenceBindingKind()
		 * @generated
		 */
		EEnum REFERENCE_BINDING_KIND = eINSTANCE.getReferenceBindingKind();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.DiagnosticSeverity <em>Diagnostic Severity</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.DiagnosticSeverity
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getDiagnosticSeverity()
		 * @generated
		 */
		EEnum DIAGNOSTIC_SEVERITY = eINSTANCE.getDiagnosticSeverity();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.FlavorKind <em>Flavor Kind</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.FlavorKind
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getFlavorKind()
		 * @generated
		 */
		EEnum FLAVOR_KIND = eINSTANCE.getFlavorKind();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.HttpMethod <em>Http Method</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.HttpMethod
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getHttpMethod()
		 * @generated
		 */
		EEnum HTTP_METHOD = eINSTANCE.getHttpMethod();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.MqttQos <em>Mqtt Qos</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.MqttQos
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getMqttQos()
		 * @generated
		 */
		EEnum MQTT_QOS = eINSTANCE.getMqttQos();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.RegistryKind <em>Registry Kind</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.RegistryKind
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getRegistryKind()
		 * @generated
		 */
		EEnum REGISTRY_KIND = eINSTANCE.getRegistryKind();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ExpressionLanguage <em>Expression Language</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ExpressionLanguage
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getExpressionLanguage()
		 * @generated
		 */
		EEnum EXPRESSION_LANGUAGE = eINSTANCE.getExpressionLanguage();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.CatalogStatus <em>Catalog Status</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.CatalogStatus
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getCatalogStatus()
		 * @generated
		 */
		EEnum CATALOG_STATUS = eINSTANCE.getCatalogStatus();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.services.ConnectionState <em>Connection State</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.services.ConnectionState
		 * @see org.eclipse.fennec.services.impl.ServicesPackageImpl#getConnectionState()
		 * @generated
		 */
		EEnum CONNECTION_STATE = eINSTANCE.getConnectionState();

	}

} //ServicesPackage
