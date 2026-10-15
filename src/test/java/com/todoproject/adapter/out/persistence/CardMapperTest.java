package com.todoproject.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Covers data-model.md's mapping guarantees: a round trip loses nothing. */
class CardMapperTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void roundTripPreservesEveryFieldIncludingRevision() {
        TaskCard card = TaskCard.reconstitute(
                com.todoproject.domain.model.CardId.newId(),
                CardTitle.of("Tarefa"),
                CardNote.of("uma nota"),
                BoardStage.IN_PROGRESS,
                T0,
                T0,
                7L);

        CardJpaEntity entity = new CardJpaEntity(
                card.id().value(), card.title().value(), card.note().orNull(), card.stage(), card.createdAt(),
                card.updatedAt(), card.revision());
        TaskCard roundTripped = CardMapper.toDomain(entity);

        assertThat(roundTripped.id()).isEqualTo(card.id());
        assertThat(roundTripped.title()).isEqualTo(card.title());
        assertThat(roundTripped.note()).isEqualTo(card.note());
        assertThat(roundTripped.stage()).isEqualTo(card.stage());
        assertThat(roundTripped.createdAt()).isEqualTo(card.createdAt());
        assertThat(roundTripped.updatedAt()).isEqualTo(card.updatedAt());
        assertThat(roundTripped.revision()).isEqualTo(7L);
    }

    @Test
    void absentNoteMapsToSqlNullAndBackToAbsent() {
        CardJpaEntity entity =
                new CardJpaEntity(java.util.UUID.randomUUID(), "Tarefa", null, BoardStage.BACKLOG, T0, T0, 0L);

        TaskCard card = CardMapper.toDomain(entity);

        assertThat(card.note().isPresent()).isFalse();
        assertThat(entity.getNote()).isNull();
    }

    @Test
    void stageIsStoredAsTheEnumName() {
        TaskCard card = TaskCard.create(CardTitle.of("Tarefa"), CardNote.empty(), BoardStage.DONE, T0);

        CardJpaEntity entity = CardMapper.toNewEntity(card);

        assertThat(entity.getStage()).isEqualTo(BoardStage.DONE);
    }

    @Test
    void newEntityHasNoVersionYet() {
        TaskCard card = TaskCard.create(CardTitle.of("Tarefa"), CardNote.empty(), BoardStage.BACKLOG, T0);

        CardJpaEntity entity = CardMapper.toNewEntity(card);

        assertThat(entity.getRevision()).isNull();
    }
}
