package com.todoproject.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;
import java.util.EnumSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Covers FR-009, FR-010, FR-015 and the stale-write refusal in research R14.
 *
 * <p>{@code revision} is read-only on the aggregate (data-model.md): it is the number the card was
 * loaded at, compared by {@link TaskCard#requireCurrentRevision(long)}. Advancing it on a
 * successful write is the persistence adapter's job, exercised in
 * {@code CardPersistenceAdapterTest}, not here.
 */
class TaskCardTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-01T10:05:00Z");

    static Stream<Arguments> allOrderedStagePairsIncludingSelf() {
        return EnumSet.allOf(BoardStage.class).stream()
                .flatMap(from -> EnumSet.allOf(BoardStage.class).stream().map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest
    @MethodSource("allOrderedStagePairsIncludingSelf")
    void everyStageReachesEveryOtherStageInEitherDirection(BoardStage from, BoardStage to) {
        TaskCard card = TaskCard.create(CardTitle.of("Tarefa"), CardNote.empty(), from, T0);

        card.moveTo(to, T1);

        assertThat(card.stage()).isEqualTo(to);
    }

    @Test
    void moveToPreservesContentAndCreationTime() {
        TaskCard card = TaskCard.create(CardTitle.of("Tarefa"), CardNote.of("nota"), BoardStage.BACKLOG, T0);

        card.moveTo(BoardStage.DONE, T1);

        assertThat(card.title().value()).isEqualTo("Tarefa");
        assertThat(card.note().orNull()).isEqualTo("nota");
        assertThat(card.createdAt()).isEqualTo(T0);
        assertThat(card.updatedAt()).isEqualTo(T1);
    }

    @Test
    void renameNeverChangesStage() {
        TaskCard card = TaskCard.create(CardTitle.of("Tarefa"), CardNote.empty(), BoardStage.IN_PROGRESS, T0);

        card.rename(CardTitle.of("Nova tarefa"), T1);

        assertThat(card.stage()).isEqualTo(BoardStage.IN_PROGRESS);
        assertThat(card.title().value()).isEqualTo("Nova tarefa");
    }

    @Test
    void changeNoteNeverChangesStage() {
        TaskCard card = TaskCard.create(CardTitle.of("Tarefa"), CardNote.empty(), BoardStage.PAUSED, T0);

        card.changeNote(CardNote.of("nota nova"), T1);

        assertThat(card.stage()).isEqualTo(BoardStage.PAUSED);
        assertThat(card.note().orNull()).isEqualTo("nota nova");
    }

    @Test
    void requireCurrentRevisionAcceptsAMatch() {
        TaskCard card = TaskCard.reconstitute(
                com.todoproject.domain.model.CardId.newId(),
                CardTitle.of("Tarefa"),
                CardNote.empty(),
                BoardStage.BACKLOG,
                T0,
                T0,
                3L);

        card.requireCurrentRevision(3L);
        // No exception: the write may proceed.
    }

    @Test
    void requireCurrentRevisionRefusesAStaleExpectation() {
        TaskCard card = TaskCard.reconstitute(
                com.todoproject.domain.model.CardId.newId(),
                CardTitle.of("Tarefa"),
                CardNote.empty(),
                BoardStage.BACKLOG,
                T0,
                T0,
                3L);

        assertThatThrownBy(() -> card.requireCurrentRevision(2L)).isInstanceOf(StaleCardException.class);
    }

    @Test
    void updatedAtNeverPrecedesCreatedAt() {
        assertThatThrownBy(() -> TaskCard.reconstitute(
                        com.todoproject.domain.model.CardId.newId(),
                        CardTitle.of("Tarefa"),
                        CardNote.empty(),
                        BoardStage.BACKLOG,
                        T1,
                        T0,
                        0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revisionIsNeverNegative() {
        assertThatThrownBy(() -> TaskCard.reconstitute(
                        com.todoproject.domain.model.CardId.newId(),
                        CardTitle.of("Tarefa"),
                        CardNote.empty(),
                        BoardStage.BACKLOG,
                        T0,
                        T0,
                        -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
