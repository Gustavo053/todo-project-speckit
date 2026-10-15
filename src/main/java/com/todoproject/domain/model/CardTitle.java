package com.todoproject.domain.model;

import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.ViolationCode;
import java.util.Objects;

/**
 * A card's title: required, normalized, and bounded.
 *
 * <p>The constructor is the only way in, so a {@code CardTitle} that exists is always valid — there
 * is no half-valid state for a caller to observe (constitution principle III).
 */
public final class CardTitle {

    /** Maximum 120 characters, measured after normalization (FR-008). */
    public static final int MAX_LENGTH = 120;

    private static final String FIELD = "title";

    private final String value;

    private CardTitle(String value) {
        this.value = value;
    }

    public static CardTitle of(String raw) {
        if (raw == null) {
            throw new InvalidCardContentException(FIELD, ViolationCode.REQUIRED);
        }
        String normalized = TextPolicy.normalizeSingleLine(raw);
        if (normalized.isEmpty()) {
            // Covers both an empty submission and a whitespace-only one (FR-007).
            throw new InvalidCardContentException(FIELD, ViolationCode.REQUIRED);
        }
        TextPolicy.requireAllowedCharacters(normalized, false, FIELD);
        int length = TextPolicy.length(normalized);
        if (length > MAX_LENGTH) {
            throw new InvalidCardContentException(FIELD, ViolationCode.TOO_LONG, length);
        }
        return new CardTitle(normalized);
    }

    /**
     * Rebuilds a title from already-validated stored data without re-running normalization, so a
     * stored value is never silently rewritten on read (data-model.md).
     */
    public static CardTitle fromStored(String stored) {
        return new CardTitle(Objects.requireNonNull(stored, "stored title must not be null"));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CardTitle title && value.equals(title.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /** Deliberately not the title text: this value must not reach a log (research R12). */
    @Override
    public String toString() {
        return "CardTitle[length=" + TextPolicy.length(value) + "]";
    }
}
