package com.todoproject.adapter.out.persistence;

import com.todoproject.domain.model.BoardStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * The table shape. Every JPA annotation in this application lives in this file, and nothing outside
 * this package may reference it — ArchUnit fails the build otherwise (research R4, R9).
 */
@Entity
@Table(name = "card")
public class CardJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @Column(name = "note", length = 2000)
    private String note;

    /**
     * Stored as the enum <em>name</em>. {@code EnumType.ORDINAL} is forbidden: reordering the enum
     * would silently relabel every stored card (data-model.md).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 20, nullable = false)
    private BoardStage stage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Managed by JPA: the single writer of this number (data-model.md). */
    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;

    protected CardJpaEntity() {
        // Required by JPA.
    }

    CardJpaEntity(
            UUID id, String title, String note, BoardStage stage, Instant createdAt, Instant updatedAt, Long revision) {
        this.id = id;
        this.title = title;
        this.note = note;
        this.stage = stage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.revision = revision;
    }

    UUID getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    String getNote() {
        return note;
    }

    BoardStage getStage() {
        return stage;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Long getRevision() {
        return revision;
    }

    void apply(String newTitle, String newNote, BoardStage newStage, Instant newUpdatedAt) {
        this.title = newTitle;
        this.note = newNote;
        this.stage = newStage;
        this.updatedAt = newUpdatedAt;
    }
}
