package com.todoproject.domain.error;

/**
 * The board has reached its card capacity.
 *
 * <p>Creation is unauthenticated on two surfaces, so an unbounded board is a resource-exhaustion
 * path. The cap gives the boundary a defined behaviour (research R11).
 */
public class BoardFullException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int maxCards;

    public BoardFullException(int maxCards) {
        super("board is full: " + maxCards + " cards");
        this.maxCards = maxCards;
    }

    public int maxCards() {
        return maxCards;
    }
}
