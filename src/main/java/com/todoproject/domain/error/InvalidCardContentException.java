package com.todoproject.domain.error;

/**
 * A value failed the domain's invariants.
 *
 * <p>Carries the field name, the violation code and — only for a length violation — the measured
 * length. It deliberately never carries the offending text, so no card content can reach a log or
 * an error page through an exception message (research R12, FR-024).
 */
public class InvalidCardContentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String field;
    private final ViolationCode code;
    private final int actualLength;

    public InvalidCardContentException(String field, ViolationCode code) {
        this(field, code, -1);
    }

    public InvalidCardContentException(String field, ViolationCode code, int actualLength) {
        super("invalid " + field + ": " + code);
        this.field = field;
        this.code = code;
        this.actualLength = actualLength;
    }

    public String field() {
        return field;
    }

    public ViolationCode code() {
        return code;
    }

    /** The measured length for a {@link ViolationCode#TOO_LONG}, or -1 when not applicable. */
    public int actualLength() {
        return actualLength;
    }
}
