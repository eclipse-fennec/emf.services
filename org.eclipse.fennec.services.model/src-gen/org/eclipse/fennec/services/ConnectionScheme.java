/*
 */
package org.eclipse.fennec.services;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Connection Scheme</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Identity is established once per connection and enforced OUTSIDE the service: a messaging broker's authentication and ACL on the request and response topic trees (the consumer segment of a response topic is bound to the connection's identity, which is why request and response live in separate trees, #98), or a gateway that terminates authentication in front of a REST node. The serving node verifies nothing per call and this scheme says so explicitly, so that a document without any scheme is never mistaken for this one. Scopes cannot be checked per call under this scheme; a requirement that names scopes is therefore not satisfiable by it alone.
 * <!-- end-model-doc -->
 *
 *
 * @see org.eclipse.fennec.services.ServicesPackage#getConnectionScheme()
 * @model
 * @generated
 */
@ProviderType
public interface ConnectionScheme extends SecurityScheme {
} // ConnectionScheme
