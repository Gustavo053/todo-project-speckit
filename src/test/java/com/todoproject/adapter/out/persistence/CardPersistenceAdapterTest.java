package com.todoproject.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Exercises the real adapter over an H2 database, covering save/load/delete round trips, ordering,
 * and the optimistic-locking conflict translated into {@link StaleCardException} (research R14).
 */
@DataJpaTest
@Import(CardPersistenceAdapter.class)
class CardPersistenceAdapterTest {

    @Autowired
    private CardPersistenceAdapter adapter;

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void savesAndLoadsACard() {
        TaskCard card = TaskCard.create(CardTitle.of("Tarefa"), CardNote.of("nota"), BoardStage.BACKLOG, T0);

        TaskCard saved = adapter.save(card);
        TaskCard loaded = adapter.findById(saved.id()).orElseThrow();

        assertThat(loaded.title()).isEqualTo(card.title());
        assertThat(loaded.note()).isEqualTo(card.note());
        assertThat(loaded.revision()).isEqualTo(0L);
    }

    @Test
    void ordersMostRecentlyChangedFirst() {
        TaskCard older =
                adapter.save(TaskCard.create(CardTitle.of("Antiga"), CardNote.empty(), BoardStage.BACKLOG, T0));
        TaskCard newer = adapter.save(
                TaskCard.create(CardTitle.of("Nova"), CardNote.empty(), BoardStage.BACKLOG, T0.plusSeconds(60)));

        var all = adapter.findAllMostRecentlyChangedFirst();

        assertThat(all).extracting(TaskCard::id).containsExactly(newer.id(), older.id());
    }

    @Test
    void aConcurrentUpdateRaisesStaleCardException() {
        TaskCard saved =
                adapter.save(TaskCard.create(CardTitle.of("Tarefa"), CardNote.empty(), BoardStage.BACKLOG, T0));

        // Simulate two readers: both start from revision 0.
        TaskCard readerA = adapter.findById(saved.id()).orElseThrow();
        TaskCard readerB = adapter.findById(saved.id()).orElseThrow();

        readerA.moveTo(BoardStage.IN_PROGRESS, T0.plusSeconds(10));
        adapter.save(readerA); // succeeds, now at revision 1

        readerB.moveTo(BoardStage.DONE, T0.plusSeconds(20));
        assertThatThrownBy(() -> adapter.save(readerB)).isInstanceOf(StaleCardException.class);

        assertThat(adapter.findById(saved.id()).orElseThrow().stage()).isEqualTo(BoardStage.IN_PROGRESS);
    }

    @Test
    void deletingATwiceDeletedCardReportsNotFound() {
        TaskCard saved =
                adapter.save(TaskCard.create(CardTitle.of("Tarefa"), CardNote.empty(), BoardStage.BACKLOG, T0));
        adapter.delete(saved);

        assertThatThrownBy(() -> adapter.delete(saved)).isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void countReflectsStoredCards() {
        assertThat(adapter.count()).isZero();
        adapter.save(TaskCard.create(CardTitle.of("Tarefa"), CardNote.empty(), BoardStage.BACKLOG, T0));
        assertThat(adapter.count()).isEqualTo(1);
    }
}
