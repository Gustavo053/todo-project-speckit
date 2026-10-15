package com.todoproject.adapter.out.persistence;

import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;

/**
 * Converts between the domain aggregate and the table shape. Pure: no repository access, no clock,
 * no side effects (data-model.md).
 *
 * <p>Reconstitution goes through the {@code fromStored} factories, which trust already-validated
 * data rather than re-running normalization — a stored value must never be silently rewritten on
 * read. A row that cannot satisfy the aggregate's invariants is data corruption and surfaces as an
 * exception rather than being repaired.
 */
public final class CardMapper {

    private CardMapper() {}

    /**
     * The insert shape. The version is left null on purpose: that is how Spring Data recognises an
     * entity as new when the identifier is assigned rather than generated.
     */
    static CardJpaEntity toNewEntity(TaskCard card) {
        return new CardJpaEntity(
                card.id().value(),
                card.title().value(),
                card.note().orNull(),
                card.stage(),
                card.createdAt(),
                card.updatedAt(),
                null);
    }

    static TaskCard toDomain(CardJpaEntity entity) {
        return TaskCard.reconstitute(
                new CardId(entity.getId()),
                CardTitle.fromStored(entity.getTitle()),
                CardNote.fromStored(entity.getNote()),
                entity.getStage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getRevision() == null ? 0L : entity.getRevision());
    }
}
