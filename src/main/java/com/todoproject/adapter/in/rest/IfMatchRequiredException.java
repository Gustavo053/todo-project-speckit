package com.todoproject.adapter.in.rest;

/**
 * {@code If-Match} was absent or unparsable on a write to an existing card.
 *
 * <p>An adapter-level concern, not a domain one -- the domain only knows about a revision number
 * once it has one; the HTTP precondition mechanics belong here (contracts/rest-api.yaml).
 */
public class IfMatchRequiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    IfMatchRequiredException() {
        super("If-Match header is required");
    }
}
