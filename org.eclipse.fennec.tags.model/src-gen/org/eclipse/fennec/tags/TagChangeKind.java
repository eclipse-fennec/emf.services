/*
 */
package org.eclipse.fennec.tags;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Tag Change Kind</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * What changed in the tag registry.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.tags.TagsPackage#getTagChangeKind()
 * @model
 * @generated
 */
@ProviderType
public enum TagChangeKind implements Enumerator {
	/**
	 * The '<em><b>TAG PUT</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TAG_PUT_VALUE
	 * @generated
	 * @ordered
	 */
	TAG_PUT(0, "TAG_PUT", "TAG_PUT"),

	/**
	 * The '<em><b>TAG MOVED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TAG_MOVED_VALUE
	 * @generated
	 * @ordered
	 */
	TAG_MOVED(1, "TAG_MOVED", "TAG_MOVED"),

	/**
	 * The '<em><b>TAG REMOVED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TAG_REMOVED_VALUE
	 * @generated
	 * @ordered
	 */
	TAG_REMOVED(2, "TAG_REMOVED", "TAG_REMOVED"),

	/**
	 * The '<em><b>ASSIGNED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ASSIGNED_VALUE
	 * @generated
	 * @ordered
	 */
	ASSIGNED(3, "ASSIGNED", "ASSIGNED"),

	/**
	 * The '<em><b>UNASSIGNED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNASSIGNED_VALUE
	 * @generated
	 * @ordered
	 */
	UNASSIGNED(4, "UNASSIGNED", "UNASSIGNED"),

	/**
	 * The '<em><b>RULE PUT</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RULE_PUT_VALUE
	 * @generated
	 * @ordered
	 */
	RULE_PUT(5, "RULE_PUT", "RULE_PUT"),

	/**
	 * The '<em><b>RULE REMOVED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RULE_REMOVED_VALUE
	 * @generated
	 * @ordered
	 */
	RULE_REMOVED(6, "RULE_REMOVED", "RULE_REMOVED"),

	/**
	 * The '<em><b>REQUESTED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REQUESTED_VALUE
	 * @generated
	 * @ordered
	 */
	REQUESTED(7, "REQUESTED", "REQUESTED"),

	/**
	 * The '<em><b>REQUEST ACCEPTED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REQUEST_ACCEPTED_VALUE
	 * @generated
	 * @ordered
	 */
	REQUEST_ACCEPTED(8, "REQUEST_ACCEPTED", "REQUEST_ACCEPTED"),

	/**
	 * The '<em><b>REQUEST DISCARDED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REQUEST_DISCARDED_VALUE
	 * @generated
	 * @ordered
	 */
	REQUEST_DISCARDED(9, "REQUEST_DISCARDED", "REQUEST_DISCARDED");

	/**
	 * The '<em><b>TAG PUT</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TAG_PUT
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int TAG_PUT_VALUE = 0;

	/**
	 * The '<em><b>TAG MOVED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TAG_MOVED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int TAG_MOVED_VALUE = 1;

	/**
	 * The '<em><b>TAG REMOVED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TAG_REMOVED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int TAG_REMOVED_VALUE = 2;

	/**
	 * The '<em><b>ASSIGNED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ASSIGNED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ASSIGNED_VALUE = 3;

	/**
	 * The '<em><b>UNASSIGNED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #UNASSIGNED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int UNASSIGNED_VALUE = 4;

	/**
	 * The '<em><b>RULE PUT</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RULE_PUT
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int RULE_PUT_VALUE = 5;

	/**
	 * The '<em><b>RULE REMOVED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RULE_REMOVED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int RULE_REMOVED_VALUE = 6;

	/**
	 * The '<em><b>REQUESTED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REQUESTED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int REQUESTED_VALUE = 7;

	/**
	 * The '<em><b>REQUEST ACCEPTED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REQUEST_ACCEPTED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int REQUEST_ACCEPTED_VALUE = 8;

	/**
	 * The '<em><b>REQUEST DISCARDED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REQUEST_DISCARDED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int REQUEST_DISCARDED_VALUE = 9;

	/**
	 * An array of all the '<em><b>Tag Change Kind</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final TagChangeKind[] VALUES_ARRAY =
		new TagChangeKind[] {
			TAG_PUT,
			TAG_MOVED,
			TAG_REMOVED,
			ASSIGNED,
			UNASSIGNED,
			RULE_PUT,
			RULE_REMOVED,
			REQUESTED,
			REQUEST_ACCEPTED,
			REQUEST_DISCARDED,
		};

	/**
	 * A public read-only list of all the '<em><b>Tag Change Kind</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<TagChangeKind> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Tag Change Kind</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static TagChangeKind get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			TagChangeKind result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Tag Change Kind</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static TagChangeKind getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			TagChangeKind result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Tag Change Kind</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static TagChangeKind get(int value) {
		switch (value) {
			case TAG_PUT_VALUE: return TAG_PUT;
			case TAG_MOVED_VALUE: return TAG_MOVED;
			case TAG_REMOVED_VALUE: return TAG_REMOVED;
			case ASSIGNED_VALUE: return ASSIGNED;
			case UNASSIGNED_VALUE: return UNASSIGNED;
			case RULE_PUT_VALUE: return RULE_PUT;
			case RULE_REMOVED_VALUE: return RULE_REMOVED;
			case REQUESTED_VALUE: return REQUESTED;
			case REQUEST_ACCEPTED_VALUE: return REQUEST_ACCEPTED;
			case REQUEST_DISCARDED_VALUE: return REQUEST_DISCARDED;
		}
		return null;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final int value;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String name;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String literal;

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private TagChangeKind(int value, String name, String literal) {
		this.value = value;
		this.name = name;
		this.literal = literal;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getValue() {
	  return value;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName() {
	  return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getLiteral() {
	  return literal;
	}

	/**
	 * Returns the literal value of the enumerator, which is its string representation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		return literal;
	}
	
} //TagChangeKind
