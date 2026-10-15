package com.todoproject.domain.error;

/**
 * No card exists for the given identifier.
 *
 * <p>Also raised for a malformed identifier, so a bad address is a plain "not found" rather than a
 * server error. A write to a missing card never recreates it (FR-012).
 */
public class CardNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CardNotFoundException(String cardId) {
        super("card not found: " + cardId);
    }
}
