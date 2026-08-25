/*
 */
package org.eclipse.fennec.services;

import org.eclipse.emf.common.util.EList;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Interface</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * The core catalog citizen: a versioned, identified service contract carrying operations and exceptions. Lives containment in RemoteServiceRegistry.catalog. Providers reference it (non-containment) from their ServiceImplementations; consumers look up services by its name.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.ServiceInterface#getDescription <em>Description</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceInterface#getOperations <em>Operations</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceInterface#getExceptions <em>Exceptions</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceInterface#getInvariants <em>Invariants</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceInterface#getStatus <em>Status</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceInterface#getDeprecationReason <em>Deprecation Reason</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.ServiceInterface#getReplacedBy <em>Replaced By</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInterface()
 * @model annotation="http://www.eclipse.org/emf/2002/Ecore constraints='replacedByIsDeprecated'"
 *        annotation="http://www.eclipse.org/fennec/m2x/ocl/1.0 immutableAfterPublish='-- ServiceInterface is conceptually immutable once added to the catalog (semver: changes mean a new entry, old one optionally deprecated). Enforced at addCatalogEntry / mutation operations, not as a static invariant \u2014 placeholder.' replacedByIsDeprecated='replacedBy = null or status.toString() = \'DEPRECATED\''"
 * @generated
 */
@ProviderType
public interface ServiceInterface extends NamedElement, VersionedElement {
	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Doc text; rendered as the type-level doc on the generated stub.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInterface_Description()
	 * @model
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceInterface#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Operations</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceOperation}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Operations the interface exposes. Empty = marker interface (rare; useful for tagging).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Operations</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInterface_Operations()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServiceOperation> getOperations();

	/**
	 * Returns the value of the '<em><b>Exceptions</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.ServiceException}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ServiceExceptions declared at interface level. Individual ServiceOperations reference (non-containment) the ones they may throw.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Exceptions</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInterface_Exceptions()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServiceException> getExceptions();

	/**
	 * Returns the value of the '<em><b>Invariants</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.services.Invariant}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Conditions that MUST hold for every instance of this interface, before and after every operation. Evaluation context: 'self' = the receiver service object. Used for whole-interface contracts (e.g. 'self.balance >= 0 implies self.status = ACTIVE').
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Invariants</em>' containment reference list.
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInterface_Invariants()
	 * @model containment="true"
	 * @generated
	 */
	EList<Invariant> getInvariants();

	/**
	 * Returns the value of the '<em><b>Status</b></em>' attribute.
	 * The default value is <code>"ACTIVE"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.services.CatalogStatus}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Catalog lifecycle status. Set to DEPRECATED by RemoteServiceRegistry.deprecateCatalogEntry; cannot transition back to ACTIVE (deprecation is one-way — a re-introduction is a new ServiceInterface with a new name/version).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Status</em>' attribute.
	 * @see org.eclipse.fennec.services.CatalogStatus
	 * @see #setStatus(CatalogStatus)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInterface_Status()
	 * @model default="ACTIVE" required="true"
	 * @generated
	 */
	CatalogStatus getStatus();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceInterface#getStatus <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Status</em>' attribute.
	 * @see org.eclipse.fennec.services.CatalogStatus
	 * @see #getStatus()
	 * @generated
	 */
	void setStatus(CatalogStatus value);

	/**
	 * Returns the value of the '<em><b>Deprecation Reason</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Free-form explanation set when status transitions to DEPRECATED. Surfaced in the WARNING Diagnostic that publishImplementation returns against a deprecated interface and in catalog browsers.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Deprecation Reason</em>' attribute.
	 * @see #setDeprecationReason(String)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInterface_DeprecationReason()
	 * @model
	 * @generated
	 */
	String getDeprecationReason();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceInterface#getDeprecationReason <em>Deprecation Reason</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Deprecation Reason</em>' attribute.
	 * @see #getDeprecationReason()
	 * @generated
	 */
	void setDeprecationReason(String value);

	/**
	 * Returns the value of the '<em><b>Replaced By</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Optional migration hint: another ServiceInterface that supersedes this one. Non-containment. Lets tooling chain deprecated → successor → successor for multi-step migration trails.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Replaced By</em>' reference.
	 * @see #setReplacedBy(ServiceInterface)
	 * @see org.eclipse.fennec.services.ServicesPackage#getServiceInterface_ReplacedBy()
	 * @model
	 * @generated
	 */
	ServiceInterface getReplacedBy();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.services.ServiceInterface#getReplacedBy <em>Replaced By</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Replaced By</em>' reference.
	 * @see #getReplacedBy()
	 * @generated
	 */
	void setReplacedBy(ServiceInterface value);

} // ServiceInterface
