package com.todoproject.domain.error;

/** Why a submitted value was refused. Carried instead of the offending content (research R12). */
public enum ViolationCode {
    REQUIRED,
    TOO_LONG,
    FORBIDDEN_CHARACTER,
    /** A value outside a fixed, closed set -- currently only an unrecognised {@code BoardStage}. */
    INVALID
}
