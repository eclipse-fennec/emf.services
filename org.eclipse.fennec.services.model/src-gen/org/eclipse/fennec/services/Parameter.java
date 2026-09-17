/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClassifier;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Parameter</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A typed, named, positional input to a ServiceOperation — and, as ServiceOperation.returnValue, its return slot. Constraints attach as containments. The parameter name appears as the argument name in generated stubs (subject to language-specific keyword escaping).
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.Parameter#getIndex <em>Index</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Parameter#getType <em>Type</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Parameter#getEType <em>EType</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Parameter#getLowerBound <em>Lower Bound</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Parameter#getUpperBound <em>Upper Bound</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Parameter#isOptional <em>Optional</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Parameter#getDefaultValue <em>Default Value</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Parameter#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Parameter#getConstraints <em>Constraints</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getParameter()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='typeOrEType boundsOrdered requiredSlotHasLowerBound'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 typeOrEType='type &lt;&gt; null or eType &lt;&gt; null' boundsOrdered='upperBound = -1 or upperBound &gt;= lowerBound' requiredSlotHasLowerBound='optional or lowerBound &gt;= 1'"
 * @generated
 */
@ProviderType
public interface Parameter extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Index</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Zero-based position in the operation signature. Independent of the name so that renaming a parameter does not reorder others.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Index</em>' attribute.
	 * @see #setIndex(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getParameter_Index()
	 * @model required="true"
	 * @generated
	 */
	int getIndex();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Parameter#getIndex <em>Index</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Index</em>' attribute.
	 * @see #getIndex()
	 * @generated
	 */
	void setIndex(int value);

	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Language-neutral type name. Code Publisher maps to the idiomatic language type (e.g. 'string' → Java String / TS string / Python str). Custom types reference other ServiceInterfaces by qualified name. For EMF values 'eType' is the precise statement: where it is set it wins for stub generation and 'type' may be omitted. At least one of the two MUST be set.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see #setType(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getParameter_Type()
	 * @model
	 * @generated
	 */
	String getType();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Parameter#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see #getType()
	 * @generated
	 */
	void setType(String value);

	/**
	 * Returns the value of the '<em><b>EType</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Metamodel type of this value: an EClass for EObject values, an Ecore EDataType for primitives (EString, EInt, EDouble, …). Serialized as a cross-document href '<nsURI>#//<Name>' and NOT required to be resolvable by the reader — broker, fingerprint and stub generators read the proxy URI, so a catalog document parses and hashes without the provider's domain metamodel on the classpath.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>EType</em>' reference.
	 * @see #setEType(EClassifier)
	 * @see org.eclipse.fennec.services.ServicesPackage#getParameter_EType()
	 * @model
	 * @generated
	 */
	EClassifier getEType();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Parameter#getEType <em>EType</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>EType</em>' reference.
	 * @see #getEType()
	 * @generated
	 */
	void setEType(EClassifier value);

	/**
	 * Returns the value of the '<em><b>Lower Bound</b></em>' attribute.
	 * The default value is <code>"1"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Minimum number of values, inclusive, with the same meaning as ETypedElement.lowerBound: 1 = one value required, 0 = may be absent. Precedence: for a SINGLE-valued slot (upperBound = 1) 'optional' is authoritative and this value is ignored — by the sd1 canonical form as well, which is why a contract written before the bounds existed keeps its fingerprint. It earns its keep on multi-valued slots ('at least two elements'). Only the genuine contradiction is rejected: a slot that is not optional cannot have a lower bound of 0.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Lower Bound</em>' attribute.
	 * @see #setLowerBound(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getParameter_LowerBound()
	 * @model default="1"
	 * @generated
	 */
	int getLowerBound();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Parameter#getLowerBound <em>Lower Bound</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Lower Bound</em>' attribute.
	 * @see #getLowerBound()
	 * @generated
	 */
	void setLowerBound(int value);

	/**
	 * Returns the value of the '<em><b>Upper Bound</b></em>' attribute.
	 * The default value is <code>"1"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Maximum number of values, inclusive. 1 = single value (the default), -1 = unbounded but finite collection. Unbounded streams over time are NOT meant here — those belong to the interaction-style work in WIRE_CHANNELS.md. As soon as it deviates from 1 the slot is multi-valued, and the sd1 canonical form renders both bounds.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Upper Bound</em>' attribute.
	 * @see #setUpperBound(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getParameter_UpperBound()
	 * @model default="1"
	 * @generated
	 */
	int getUpperBound();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Parameter#getUpperBound <em>Upper Bound</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Upper Bound</em>' attribute.
	 * @see #getUpperBound()
	 * @generated
	 */
	void setUpperBound(int value);

	/**
	 * Returns the value of the '<em><b>Optional</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * If true, the caller may omit this parameter. Generated stubs render optional parameters with their language-idiomatic mechanism (Java overloads/Optional, TS '?:', Python default values).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Optional</em>' attribute.
	 * @see #setOptional(boolean)
	 * @see org.eclipse.fennec.services.ServicesPackage#getParameter_Optional()
	 * @model default="false" required="true"
	 * @generated
	 */
	boolean isOptional();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Parameter#isOptional <em>Optional</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Optional</em>' attribute.
	 * @see #isOptional()
	 * @generated
	 */
	void setOptional(boolean value);

	/**
	 * Returns the value of the '<em><b>Default Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * String-encoded default value (parsed per type by the implementation). Only meaningful when optional = true.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Default Value</em>' attribute.
	 * @see #setDefaultValue(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getParameter_DefaultValue()
	 * @model
	 * @generated
	 */
	String getDefaultValue();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Parameter#getDefaultValue <em>Default Value</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Default Value</em>' attribute.
	 * @see #getDefaultValue()
	 * @generated
	 */
	void setDefaultValue(String value);

	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Doc text; rendered as @param JavaDoc / TSDoc tag / Python docstring entry.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getParameter_Description()
	 * @model
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Parameter#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Constraints</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ParameterConstraint}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Validity constraints. Multiple constraints AND together — a value must satisfy all of them.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Constraints</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getParameter_Constraints()
	 * @model containment="true"
	 * @generated
	 */
	EList<ParameterConstraint> getConstraints();

} // Parameter
