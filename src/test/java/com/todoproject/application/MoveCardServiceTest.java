package com.todoproject.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todoproject.TaskCardFixtures;
import com.todoproject.application.port.in.MoveCardUseCase.MoveCardCommand;
import com.todoproject.application.service.MoveCardService;
import com.todoproject.application.service.WriteGuard;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;
import java.util.EnumSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Covers FR-009, FR-010, FR-012, and the stale-write refusal in research R14. */
class MoveCardServiceTest {

    private final InMemoryCardRepository repository = new InMemoryCardRepository();
    private final MoveCardService service =
            new MoveCardService(repository, new TaskCardFixtures.FixedClock(), new WriteGuard(new AlwaysAllowRateLimiter()));

    static Stream<Arguments> allOrderedStagePairsIncludingSelf() {
        return EnumSet.allOf(BoardStage.class).stream()
                .flatMap(from -> EnumSet.allOf(BoardStage.class).stream().map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest
    @MethodSource("allOrderedStagePairsIncludingSelf")
    void everyOrderedPairIsLegal(BoardStage from, BoardStage to) {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardWithNoteIn(from));

        TaskCard moved = service.move(new MoveCardCommand(seeded.id(), to, 0L, "client-1"));

        assertThat(moved.stage()).isEqualTo(to);
        assertThat(moved.title()).isEqualTo(seeded.title());
        assertThat(moved.note()).isEqualTo(seeded.note());
        assertThat(moved.createdAt()).isEqualTo(seeded.createdAt());
    }

    @Test
    void refusesAStaleRevisionAndChangesNothing() {
        TaskCard seeded = repository.seed(TaskCardFixtures.aCardIn(BoardStage.BACKLOG));

        assertThatThrownBy(() -> service.move(new MoveCardCommand(seeded.id(), BoardStage.DONE, 99L, "client-1")))
                .isInstanceOf(StaleCardException.class);

        assertThat(repository.findById(seeded.id()).orElseThrow().stage()).isEqualTo(BoardStage.BACKLOG);
    }

    @Test
    void refusesAMoveToAMissingCardWithoutRecreatingIt() {
        CardId missing = CardId.newId();

        assertThatThrownBy(() -> service.move(new MoveCardCommand(missing, BoardStage.DONE, 0L, "client-1")))
                .isInstanceOf(CardNotFoundException.class);

        assertThat(repository.findById(missing)).isEmpty();
    }
}
