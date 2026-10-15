package com.todoproject.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todoproject.TaskCardFixtures;
import com.todoproject.application.port.in.EditCardUseCase.EditCardCommand;
import com.todoproject.application.port.in.EditCardUseCase.NoteChange;
import com.todoproject.application.service.EditCardService;
import com.todoproject.application.service.WriteGuard;
import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.TaskCard;
import org.junit.jupiter.api.Test;

/** Covers FR-013, FR-014, FR-015, FR-022. */
class EditCardServiceTest {

    private final InMemoryCardRepository repository = new InMemoryCardRepository();
    private final EditCardService service = new EditCardService(
            repository, new TaskCardFixtures.FixedClock(), new WriteGuard(new AlwaysAllowRateLimiter()));

    @Test
    void changesTheTitle() {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));

        TaskCard edited =
                service.edit(new EditCardCommand(seeded.id(), "Novo título", NoteChange.unchanged(), 0L, "client-1"));

        assertThat(edited.title().value()).isEqualTo("Novo título");
    }

    @Test
    void changesTheNote() {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));

        TaskCard edited =
                service.edit(new EditCardCommand(seeded.id(), null, NoteChange.to("nota nova"), 0L, "client-1"));

        assertThat(edited.note().orNull()).isEqualTo("nota nova");
        assertThat(edited.title()).isEqualTo(seeded.title());
    }

    @Test
    void clearsTheNote() {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardWithNoteIn(BoardStage.BACKLOG));

        TaskCard edited = service.edit(new EditCardCommand(seeded.id(), null, NoteChange.cleared(), 0L, "client-1"));

        assertThat(edited.note().isPresent()).isFalse();
    }

    @Test
    void neverChangesTheStage() {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardIn(BoardStage.PAUSED));

        TaskCard edited =
                service.edit(new EditCardCommand(seeded.id(), "Novo título", NoteChange.unchanged(), 0L, "client-1"));

        assertThat(edited.stage()).isEqualTo(BoardStage.PAUSED);
    }

    @Test
    void rejectsInvalidInputAndLeavesTheStoredCardUntouched() {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));

        assertThatThrownBy(() ->
                        service.edit(new EditCardCommand(seeded.id(), "   ", NoteChange.unchanged(), 0L, "client-1")))
                .isInstanceOf(InvalidCardContentException.class);

        assertThat(repository.findById(seeded.id()).orElseThrow().title()).isEqualTo(seeded.title());
    }

    @Test
    void refusesAStaleRevision() {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));

        assertThatThrownBy(() -> service.edit(
                        new EditCardCommand(seeded.id(), "Outro título", NoteChange.unchanged(), 99L, "client-1")))
                .isInstanceOf(StaleCardException.class);
    }
}
