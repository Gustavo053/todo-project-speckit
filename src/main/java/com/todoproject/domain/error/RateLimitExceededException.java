package com.todoproject.domain.error;

/**
 * Too many write actions from one client in the current window.
 *
 * <p>Applies to creates, edits, moves and deletes from either inbound surface, counted together
 * (research R11).
 */
public class RateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RateLimitExceededException() {
        super("write rate limit exceeded");
    }
}
