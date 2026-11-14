/*
 */
package org.eclipse.fennec.tags;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tag Rule</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Every target of a kind whose properties match the filter carries the tag. Applied when a query asks, not when a target arrives, so a new rule covers what is already there. References the tag object, so it follows a rename.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.tags.TagRule#getTag <em>Tag</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagRule#getKind <em>Kind</em>}</li>
 *   <li>{@link org.eclipse.fennec.tags.TagRule#getFilter <em>Filter</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.tags.TagsPackage#getTagRule()
 * @model
 * @generated
 */
@ProviderType
public interface TagRule extends EObject {
	/**
	 * Returns the value of the '<em><b>Tag</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tag</em>' reference.
	 * @see #setTag(Tag)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagRule_Tag()
	 * @model required="true"
	 * @generated
	 */
	Tag getTag();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagRule#getTag <em>Tag</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Tag</em>' reference.
	 * @see #getTag()
	 * @generated
	 */
	void setTag(Tag value);

	/**
	 * Returns the value of the '<em><b>Kind</b></em>' attribute.
	 * The default value is <code>"implementation"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Which kind of target the rule looks at. Only targets with properties can match: in the services registry that is 'implementation'.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Kind</em>' attribute.
	 * @see #setKind(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagRule_Kind()
	 * @model default="implementation" required="true"
	 * @generated
	 */
	String getKind();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagRule#getKind <em>Kind</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Kind</em>' attribute.
	 * @see #getKind()
	 * @generated
	 */
	void setKind(String value);

	/**
	 * Returns the value of the '<em><b>Filter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * An LDAP filter (RFC 4515, as in OSGi) over the target's properties, e.g. '(payment.scheme=card)'. Evaluated by the tag service only.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Filter</em>' attribute.
	 * @see #setFilter(String)
	 * @see org.eclipse.fennec.tags.TagsPackage#getTagRule_Filter()
	 * @model required="true"
	 * @generated
	 */
	String getFilter();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.tags.TagRule#getFilter <em>Filter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Filter</em>' attribute.
	 * @see #getFilter()
	 * @generated
	 */
	void setFilter(String value);

} // TagRule
