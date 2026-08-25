/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Property</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Abstract base for typed service properties. Replaces OSGi's Map<String, Object> properties with a containment hierarchy that survives wire serialization across languages. Each concrete subclass adds a typed 'value' attribute.
 * <!-- end-model-doc -->
 *
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getProperty()
 * @model abstract="true"
 * @generated
 */
@ProviderType
public interface Property extends NamedElement {
} // Property
