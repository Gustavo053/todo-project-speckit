package com.todoproject.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todoproject.TaskCardFixtures;
import com.todoproject.application.port.in.CreateCardUseCase.CreateCardCommand;
import com.todoproject.application.service.CreateCardService;
import com.todoproject.application.service.WriteGuard;
import com.todoproject.domain.error.BoardFullException;
import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.RateLimitExceededException;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.TaskCard;
import org.junit.jupiter.api.Test;

/** Covers FR-006, FR-007, FR-022, and the board-full and rate-limit refusals (research R11). */
class CreateCardServiceTest {

    private final InMemoryCardRepository repository = new InMemoryCardRepository();
    private final TaskCardFixtures.FixedClock clock = new TaskCardFixtures.FixedClock();

    @Test
    void createsInTheRequestedStage() {
        CreateCardService service = serviceWithCap(500);

        TaskCard card = service.create(new CreateCardCommand("Tarefa", null, BoardStage.PAUSED, "client-1"));

        assertThat(card.stage()).isEqualTo(BoardStage.PAUSED);
    }

    @Test
    void defaultsToBacklogWhenNoStageIsGiven() {
        CreateCardService service = serviceWithCap(500);

        TaskCard card = service.create(new CreateCardCommand("Tarefa", null, null, "client-1"));

        assertThat(card.stage()).isEqualTo(BoardStage.BACKLOG);
    }

    @Test
    void rejectsInvalidTitleAndPersistsNothing() {
        CreateCardService service = serviceWithCap(500);

        assertThatThrownBy(() -> service.create(new CreateCardCommand("   ", null, null, "client-1")))
                .isInstanceOf(InvalidCardContentException.class);

        assertThat(repository.count()).isZero();
    }

    @Test
    void refusesAtTheCapAndPersistsNothing() {
        CreateCardService service = serviceWithCap(1);
        repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));

        assertThatThrownBy(() -> service.create(new CreateCardCommand("Outra tarefa", null, null, "client-1")))
                .isInstanceOf(BoardFullException.class);

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void refusesWhenTheRateLimiterDenies() {
        CreateCardService service =
                new CreateCardService(repository, clock, new WriteGuard(new NeverAllowRateLimiter()), 500);

        assertThatThrownBy(() -> service.create(new CreateCardCommand("Tarefa", null, null, "client-1")))
                .isInstanceOf(RateLimitExceededException.class);

        assertThat(repository.count()).isZero();
    }

    private CreateCardService serviceWithCap(int maxCards) {
        return new CreateCardService(repository, clock, new WriteGuard(new AlwaysAllowRateLimiter()), maxCards);
    }
}
