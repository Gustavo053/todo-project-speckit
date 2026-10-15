package com.todoproject.domain.error;

/**
 * The write was based on a revision that is no longer current — someone changed the card first.
 *
 * <p>Nothing is written. This is what stops a stale action from silently resurrecting a card's
 * earlier state (research R14).
 */
public class StaleCardException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StaleCardException(String cardId) {
        super("stale write refused for card: " + cardId);
    }
}
