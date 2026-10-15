package com.todoproject.domain.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A card's identity. Random UUID v4, so identifiers are not enumerable even though nothing depends
 * on that while the board is local-only (research R2).
 */
public record CardId(UUID value) {

    public CardId {
        Objects.requireNonNull(value, "card id must not be null");
    }

    public static CardId newId() {
        return new CardId(UUID.randomUUID());
    }

    /**
     * Parses an identifier arriving from an adapter. A malformed value yields empty, which callers
     * report as "not found" rather than as a server error (data-model.md).
     */
    public static Optional<CardId> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CardId(UUID.fromString(raw.trim())));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
