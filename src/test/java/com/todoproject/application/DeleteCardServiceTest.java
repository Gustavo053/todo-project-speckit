package com.todoproject.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todoproject.TaskCardFixtures;
import com.todoproject.application.port.in.DeleteCardUseCase.DeleteCardCommand;
import com.todoproject.application.service.DeleteCardService;
import com.todoproject.application.service.WriteGuard;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;
import org.junit.jupiter.api.Test;

/** Covers FR-012, FR-017: permanent, no recreation, stale writes refused. */
class DeleteCardServiceTest {

    private final InMemoryCardRepository repository = new InMemoryCardRepository();
    private final DeleteCardService service =
            new DeleteCardService(repository, new WriteGuard(new AlwaysAllowRateLimiter()));

    @Test
    void deletesPermanently() {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));

        service.delete(new DeleteCardCommand(seeded.id(), 0L, "client-1"));

        assertThat(repository.findById(seeded.id())).isEmpty();
        assertThat(repository.count()).isZero();
    }

    @Test
    void refusesAStaleRevisionAndKeepsTheCard() {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));

        assertThatThrownBy(() -> service.delete(new DeleteCardCommand(seeded.id(), 99L, "client-1")))
                .isInstanceOf(StaleCardException.class);

        assertThat(repository.findById(seeded.id())).isPresent();
    }

    @Test
    void deletingAnAlreadyMissingCardReportsNotFoundAndNeverRecreatesIt() {
        CardId missing = CardId.newId();

        assertThatThrownBy(() -> service.delete(new DeleteCardCommand(missing, 0L, "client-1")))
                .isInstanceOf(CardNotFoundException.class);

        assertThat(repository.findById(missing)).isEmpty();
    }
}
