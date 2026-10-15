package com.todoproject.domain.model;

import com.todoproject.domain.error.StaleCardException;
import java.time.Instant;
import java.util.Objects;

/**
 * The aggregate root: one thing the person intends to do, and the only kind of item on the board
 * (FR-005).
 *
 * <p>No annotations of any kind live in this file. The persistence shape is a separate type in
 * {@code adapter.out.persistence}, mapped explicitly, because constitution principle II forbids
 * persistence annotations in the domain (research R4).
 *
 * <p>{@code revision} is read-only here. The aggregate carries the revision it was loaded at so the
 * domain can refuse a write based on stale data ({@link #requireCurrentRevision(long)}); advancing
 * it is the persistence adapter's job, which keeps a single writer of that number (research R14).
 */
public final class TaskCard {

    private final CardId id;
    private final Instant createdAt;
    private final long revision;

    private CardTitle title;
    private CardNote note;
    private BoardStage stage;
    private Instant updatedAt;

    private TaskCard(
            CardId id,
            CardTitle title,
            CardNote note,
            BoardStage stage,
            Instant createdAt,
            Instant updatedAt,
            long revision) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.note = Objects.requireNonNull(note, "note must not be null");
        this.stage = Objects.requireNonNull(stage, "stage must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        this.revision = revision;
    }

    /** A brand new card. Starts at revision 0, created and updated at the same instant. */
    public static TaskCard create(CardTitle title, CardNote note, BoardStage stage, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new TaskCard(CardId.newId(), title, note, stage, now, now, 0L);
    }

    /** Rebuilds a card from storage. Trusts already-validated data (data-model.md). */
    public static TaskCard reconstitute(
            CardId id,
            CardTitle title,
            CardNote note,
            BoardStage stage,
            Instant createdAt,
            Instant updatedAt,
            long revision) {
        return new TaskCard(id, title, note, stage, createdAt, updatedAt, revision);
    }

    /**
     * Moves the card to a stage. Every stage reaches every other stage in either direction, and a
     * self-move is legal (FR-009). Title, note and {@code createdAt} are untouched (FR-010).
     */
    public void moveTo(BoardStage target, Instant now) {
        this.stage = Objects.requireNonNull(target, "target stage must not be null");
        touch(now);
    }

    /** Replaces the title (FR-013). Cannot change the stage — there is no path that does. */
    public void rename(CardTitle newTitle, Instant now) {
        this.title = Objects.requireNonNull(newTitle, "title must not be null");
        touch(now);
    }

    /** Replaces or clears the note (FR-013). Cannot change the stage. */
    public void changeNote(CardNote newNote, Instant now) {
        this.note = Objects.requireNonNull(newNote, "note must not be null");
        touch(now);
    }

    /**
     * Refuses a write whose caller was looking at an older version of this card.
     *
     * @throws StaleCardException if the expected revision is not the current one (research R14)
     */
    public void requireCurrentRevision(long expectedRevision) {
        if (expectedRevision != revision) {
            throw new StaleCardException(id.toString());
        }
    }

    private void touch(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        this.updatedAt = now;
    }

    public CardId id() {
        return id;
    }

    public CardTitle title() {
        return title;
    }

    public CardNote note() {
        return note;
    }

    public BoardStage stage() {
        return stage;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long revision() {
        return revision;
    }

    /** Identifiers and metadata only — card content must not reach a log (research R12). */
    @Override
    public String toString() {
        return "TaskCard[id=" + id + ", stage=" + stage + ", revision=" + revision + "]";
    }
}
