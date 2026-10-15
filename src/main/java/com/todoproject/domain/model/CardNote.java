package com.todoproject.domain.model;

import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.ViolationCode;
import java.util.Optional;

/**
 * A card's optional note.
 *
 * <p>Absence and blankness are the same thing: a note that is whitespace only after normalization
 * is stored as absent, never as empty text, so there is one representation of "no note"
 * (data-model.md).
 */
public final class CardNote {

    /** Maximum 2000 characters, measured after normalization (FR-008). */
    public static final int MAX_LENGTH = 2000;

    private static final String FIELD = "note";
    private static final CardNote EMPTY = new CardNote(null);

    private final String value;

    private CardNote(String value) {
        this.value = value;
    }

    public static CardNote empty() {
        return EMPTY;
    }

    public static CardNote of(String raw) {
        if (raw == null) {
            return EMPTY;
        }
        String normalized = TextPolicy.normalizeMultiLine(raw);
        if (normalized.isEmpty()) {
            return EMPTY;
        }
        TextPolicy.requireAllowedCharacters(normalized, true, FIELD);
        int length = TextPolicy.length(normalized);
        if (length > MAX_LENGTH) {
            throw new InvalidCardContentException(FIELD, ViolationCode.TOO_LONG, length);
        }
        return new CardNote(normalized);
    }

    /** Rebuilds from already-validated stored data; {@code null} means the card has no note. */
    public static CardNote fromStored(String stored) {
        return stored == null ? EMPTY : new CardNote(stored);
    }

    public Optional<String> value() {
        return Optional.ofNullable(value);
    }

    public boolean isPresent() {
        return value != null;
    }

    /** The stored form: the text, or {@code null} when there is no note. */
    public String orNull() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CardNote note && java.util.Objects.equals(value, note.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(value);
    }

    /** Deliberately not the note text: this value must not reach a log (research R12). */
    @Override
    public String toString() {
        return "CardNote[" + (value == null ? "absent" : "length=" + TextPolicy.length(value)) + "]";
    }
}
