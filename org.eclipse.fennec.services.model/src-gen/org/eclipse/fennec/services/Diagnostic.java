/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Diagnostic</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Language-neutral diagnostic record. Structure follows the standard Diagnostic pattern (severity / message / source / code / data / children) so EMF-shaped tooling on the Java side feels native, but it is DDSR's own EClass so TypeScript and Python implementations are not bound to any host-language Diagnostic runtime. Diagnostics nest (children) to carry causation chains.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.Diagnostic#getSeverity <em>Severity</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Diagnostic#getMessage <em>Message</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Diagnostic#getSource <em>Source</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Diagnostic#getCode <em>Code</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Diagnostic#getData <em>Data</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.Diagnostic#getChildren <em>Children</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getDiagnostic()
 * @model
 * @generated
 */
@ProviderType
public interface Diagnostic extends EObject {
	/**
	 * Returns the value of the '<em><b>Severity</b></em>' attribute.
	 * The default value is <code>"OK"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.DiagnosticSeverity}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Severity of this diagnostic. An aggregate diagnostic typically carries the maximum severity of its children.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Severity</em>' attribute.
	 * @see org.eclipse.fennec.services.DiagnosticSeverity
	 * @see #setSeverity(DiagnosticSeverity)
	 * @see org.eclipse.fennec.services.ServicesPackage#getDiagnostic_Severity()
	 * @model default="OK" required="true"
	 * @generated
	 */
	DiagnosticSeverity getSeverity();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Diagnostic#getSeverity <em>Severity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Severity</em>' attribute.
	 * @see org.eclipse.fennec.services.DiagnosticSeverity
	 * @see #getSeverity()
	 * @generated
	 */
	void setSeverity(DiagnosticSeverity value);

	/**
	 * Returns the value of the '<em><b>Message</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Human-readable description of the diagnostic. Should be plain text; structured data goes in data.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Message</em>' attribute.
	 * @see #setMessage(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getDiagnostic_Message()
	 * @model
	 * @generated
	 */
	String getMessage();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Diagnostic#getMessage <em>Message</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Message</em>' attribute.
	 * @see #getMessage()
	 * @generated
	 */
	void setMessage(String value);

	/**
	 * Returns the value of the '<em><b>Source</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Originating subsystem identifier, e.g. 'org.gecko.ddsr.runtime'. Useful when diagnostics from multiple sources are aggregated.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Source</em>' attribute.
	 * @see #setSource(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getDiagnostic_Source()
	 * @model
	 * @generated
	 */
	String getSource();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Diagnostic#getSource <em>Source</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Source</em>' attribute.
	 * @see #getSource()
	 * @generated
	 */
	void setSource(String value);

	/**
	 * Returns the value of the '<em><b>Code</b></em>' attribute.
	 * The default value is <code>"0"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Stable numeric code, source-namespaced. 0 = unclassified.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Code</em>' attribute.
	 * @see #setCode(int)
	 * @see org.eclipse.fennec.services.ServicesPackage#getDiagnostic_Code()
	 * @model default="0" required="true"
	 * @generated
	 */
	int getCode();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.Diagnostic#getCode <em>Code</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Code</em>' attribute.
	 * @see #getCode()
	 * @generated
	 */
	void setCode(int value);

	/**
	 * Returns the value of the '<em><b>Data</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Stringified contextual data. EMF's Diagnostic uses List<Object> here; DDSR uses strings to stay language-neutral on the wire.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Data</em>' attribute list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getDiagnostic_Data()
	 * @model
	 * @generated
	 */
	EList<String> getData();

	/**
	 * Returns the value of the '<em><b>Children</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Diagnostic}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Causation chain (nested diagnostics). Empty for atomic diagnostics.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Children</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getDiagnostic_Children()
	 * @model containment="true"
	 * @generated
	 */
	EList<Diagnostic> getChildren();

} // Diagnostic
