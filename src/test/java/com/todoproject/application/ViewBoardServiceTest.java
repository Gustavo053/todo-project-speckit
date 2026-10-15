package com.todoproject.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.todoproject.TaskCardFixtures;
import com.todoproject.application.port.in.BoardSnapshot;
import com.todoproject.application.service.ViewBoardService;
import com.todoproject.domain.model.BoardStage;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Covers FR-001, FR-003, FR-004: all four columns, always, in order, with counts. */
class ViewBoardServiceTest {

    private final InMemoryCardRepository repository = new InMemoryCardRepository();
    private final ViewBoardService service = new ViewBoardService(repository, 500);

    @Test
    void returnsAllFourColumnsInDeclarationOrderEvenWhenEmpty() {
        BoardSnapshot snapshot = service.load();

        assertThat(snapshot.columns()).extracting(BoardSnapshot.Column::stage)
                .containsExactly(BoardStage.BACKLOG, BoardStage.IN_PROGRESS, BoardStage.PAUSED, BoardStage.DONE);
        assertThat(snapshot.columns()).allSatisfy(column -> assertThat(column.isEmpty()).isTrue());
    }

    @Test
    void groupsCardsIntoTheirStageWithCorrectCounts() {
        repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));
        repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));
        repository.seed(TaskCardFixtures.aCardIn(BoardStage.DONE));

        BoardSnapshot snapshot = service.load();

        assertThat(columnFor(snapshot, BoardStage.BACKLOG).count()).isEqualTo(2);
        assertThat(columnFor(snapshot, BoardStage.DONE).count()).isEqualTo(1);
        assertThat(columnFor(snapshot, BoardStage.IN_PROGRESS).count()).isZero();
        assertThat(snapshot.totalCards()).isEqualTo(3);
    }

    @Test
    void ordersCardsMostRecentlyChangedFirst() {
        var older = com.todoproject.domain.model.TaskCard.create(
                com.todoproject.domain.model.CardTitle.of("Mais antiga"),
                com.todoproject.domain.model.CardNote.empty(),
                BoardStage.BACKLOG,
                Instant.parse("2026-01-01T10:00:00Z"));
        var newer = com.todoproject.domain.model.TaskCard.create(
                com.todoproject.domain.model.CardTitle.of("Mais recente"),
                com.todoproject.domain.model.CardNote.empty(),
                BoardStage.BACKLOG,
                Instant.parse("2026-01-01T11:00:00Z"));
        repository.seed(older);
        repository.seed(newer);

        BoardSnapshot snapshot = service.load();

        assertThat(columnFor(snapshot, BoardStage.BACKLOG).cards())
                .extracting(card -> card.title().value())
                .containsExactly("Mais recente", "Mais antiga");
    }

    private static BoardSnapshot.Column columnFor(BoardSnapshot snapshot, BoardStage stage) {
        return snapshot.columns().stream()
                .filter(column -> column.stage() == stage)
                .findFirst()
                .orElseThrow();
    }
}
