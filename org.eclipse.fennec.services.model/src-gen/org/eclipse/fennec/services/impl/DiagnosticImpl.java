/*
 */
package org.eclipse.fennec.services.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ServicesPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Diagnostic</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.services.impl.DiagnosticImpl#getSeverity <em>Severity</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.DiagnosticImpl#getMessage <em>Message</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.DiagnosticImpl#getSource <em>Source</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.DiagnosticImpl#getCode <em>Code</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.DiagnosticImpl#getData <em>Data</em>}</li>
 *   <li>{@link org.eclipse.fennec.services.impl.DiagnosticImpl#getChildren <em>Children</em>}</li>
 * </ul>
 *
 * @generated
 */
public class DiagnosticImpl extends MinimalEObjectImpl.Container implements Diagnostic {
	/**
	 * The default value of the '{@link #getSeverity() <em>Severity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSeverity()
	 * @generated
	 * @ordered
	 */
	protected static final DiagnosticSeverity SEVERITY_EDEFAULT = DiagnosticSeverity.OK;

	/**
	 * The cached value of the '{@link #getSeverity() <em>Severity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSeverity()
	 * @generated
	 * @ordered
	 */
	protected DiagnosticSeverity severity = SEVERITY_EDEFAULT;

	/**
	 * The default value of the '{@link #getMessage() <em>Message</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMessage()
	 * @generated
	 * @ordered
	 */
	protected static final String MESSAGE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMessage() <em>Message</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMessage()
	 * @generated
	 * @ordered
	 */
	protected String message = MESSAGE_EDEFAULT;

	/**
	 * The default value of the '{@link #getSource() <em>Source</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSource()
	 * @generated
	 * @ordered
	 */
	protected static final String SOURCE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSource() <em>Source</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSource()
	 * @generated
	 * @ordered
	 */
	protected String source = SOURCE_EDEFAULT;

	/**
	 * The default value of the '{@link #getCode() <em>Code</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCode()
	 * @generated
	 * @ordered
	 */
	protected static final int CODE_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getCode() <em>Code</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCode()
	 * @generated
	 * @ordered
	 */
	protected int code = CODE_EDEFAULT;

	/**
	 * The cached value of the '{@link #getData() <em>Data</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getData()
	 * @generated
	 * @ordered
	 */
	protected EList<String> data;

	/**
	 * The cached value of the '{@link #getChildren() <em>Children</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChildren()
	 * @generated
	 * @ordered
	 */
	protected EList<Diagnostic> children;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected DiagnosticImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ServicesPackage.Literals.DIAGNOSTIC;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public DiagnosticSeverity getSeverity() {
		return severity;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSeverity(DiagnosticSeverity newSeverity) {
		DiagnosticSeverity oldSeverity = severity;
		severity = newSeverity == null ? SEVERITY_EDEFAULT : newSeverity;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.DIAGNOSTIC__SEVERITY, oldSeverity, severity));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getMessage() {
		return message;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMessage(String newMessage) {
		String oldMessage = message;
		message = newMessage;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.DIAGNOSTIC__MESSAGE, oldMessage, message));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getSource() {
		return source;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSource(String newSource) {
		String oldSource = source;
		source = newSource;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.DIAGNOSTIC__SOURCE, oldSource, source));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getCode() {
		return code;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCode(int newCode) {
		int oldCode = code;
		code = newCode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ServicesPackage.DIAGNOSTIC__CODE, oldCode, code));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getData() {
		if (data == null) {
			data = new EDataTypeUniqueEList<String>(String.class, this, ServicesPackage.DIAGNOSTIC__DATA);
		}
		return data;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Diagnostic> getChildren() {
		if (children == null) {
			children = new EObjectContainmentEList<Diagnostic>(Diagnostic.class, this, ServicesPackage.DIAGNOSTIC__CHILDREN);
		}
		return children;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ServicesPackage.DIAGNOSTIC__CHILDREN:
				return ((InternalEList<?>)getChildren()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ServicesPackage.DIAGNOSTIC__SEVERITY:
				return getSeverity();
			case ServicesPackage.DIAGNOSTIC__MESSAGE:
				return getMessage();
			case ServicesPackage.DIAGNOSTIC__SOURCE:
				return getSource();
			case ServicesPackage.DIAGNOSTIC__CODE:
				return getCode();
			case ServicesPackage.DIAGNOSTIC__DATA:
				return getData();
			case ServicesPackage.DIAGNOSTIC__CHILDREN:
				return getChildren();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case ServicesPackage.DIAGNOSTIC__SEVERITY:
				setSeverity((DiagnosticSeverity)newValue);
				return;
			case ServicesPackage.DIAGNOSTIC__MESSAGE:
				setMessage((String)newValue);
				return;
			case ServicesPackage.DIAGNOSTIC__SOURCE:
				setSource((String)newValue);
				return;
			case ServicesPackage.DIAGNOSTIC__CODE:
				setCode((Integer)newValue);
				return;
			case ServicesPackage.DIAGNOSTIC__DATA:
				getData().clear();
				getData().addAll((Collection<? extends String>)newValue);
				return;
			case ServicesPackage.DIAGNOSTIC__CHILDREN:
				getChildren().clear();
				getChildren().addAll((Collection<? extends Diagnostic>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case ServicesPackage.DIAGNOSTIC__SEVERITY:
				setSeverity(SEVERITY_EDEFAULT);
				return;
			case ServicesPackage.DIAGNOSTIC__MESSAGE:
				setMessage(MESSAGE_EDEFAULT);
				return;
			case ServicesPackage.DIAGNOSTIC__SOURCE:
				setSource(SOURCE_EDEFAULT);
				return;
			case ServicesPackage.DIAGNOSTIC__CODE:
				setCode(CODE_EDEFAULT);
				return;
			case ServicesPackage.DIAGNOSTIC__DATA:
				getData().clear();
				return;
			case ServicesPackage.DIAGNOSTIC__CHILDREN:
				getChildren().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case ServicesPackage.DIAGNOSTIC__SEVERITY:
				return severity != SEVERITY_EDEFAULT;
			case ServicesPackage.DIAGNOSTIC__MESSAGE:
				return MESSAGE_EDEFAULT == null ? message != null : !MESSAGE_EDEFAULT.equals(message);
			case ServicesPackage.DIAGNOSTIC__SOURCE:
				return SOURCE_EDEFAULT == null ? source != null : !SOURCE_EDEFAULT.equals(source);
			case ServicesPackage.DIAGNOSTIC__CODE:
				return code != CODE_EDEFAULT;
			case ServicesPackage.DIAGNOSTIC__DATA:
				return data != null && !data.isEmpty();
			case ServicesPackage.DIAGNOSTIC__CHILDREN:
				return children != null && !children.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (severity: ");
		result.append(severity);
		result.append(", message: ");
		result.append(message);
		result.append(", source: ");
		result.append(source);
		result.append(", code: ");
		result.append(code);
		result.append(", data: ");
		result.append(data);
		result.append(')');
		return result.toString();
	}

} //DiagnosticImpl
