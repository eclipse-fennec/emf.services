package org.eclipse.fennec.services.m2t.example;

/**
 * No person is registered under the given identifier.
 *
 * <p>Generated from the {@code PersonNotFound} exception of the
 * {@code PersonDirectory} contract, version 1.0.0. Do not edit —
 * change the contract instead.
 */
public class PersonNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	private final String personId;

	public PersonNotFoundException(String message, String personId) {
		super(message);
		this.personId = personId;
	}

	public String getPersonId() {
		return personId;
	}
}