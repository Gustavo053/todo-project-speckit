package com.todoproject.domain.model;

import java.util.Optional;

/**
 * The four fixed columns of the board, in display order.
 *
 * <p>Declaration order <em>is</em> the display order — no caller may hardcode a sequence. Users
 * cannot add, rename, reorder or remove stages (FR-002). Labels live in the message bundle, not
 * here, so a wording change never touches the domain (research R8).
 */
public enum BoardStage {
    BACKLOG,
    IN_PROGRESS,
    PAUSED,
    DONE;

    /** The stage a card lands in when none is specified (FR-006). */
    public static BoardStage defaultStage() {
        return BACKLOG;
    }

    /**
     * Parses a submitted stage code. Returns empty rather than throwing, so an adapter can report
     * invalid input instead of failing (research R5).
     */
    public static Optional<BoardStage> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        for (BoardStage stage : values()) {
            if (stage.name().equals(raw.trim())) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }
}
