package com.todoproject;

import com.todoproject.application.port.out.ClockPort;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/** Shared test fixtures: cards in each stage and a fixed clock test double. */
public final class TaskCardFixtures {

    public static final Instant FIXED_NOW = Instant.parse("2026-01-01T12:00:00Z");

    private TaskCardFixtures() {}

    public static TaskCard aCardIn(BoardStage stage) {
        return TaskCard.create(CardTitle.of("Tarefa de teste"), CardNote.empty(), stage, FIXED_NOW);
    }

    public static TaskCard aCardWithNoteIn(BoardStage stage) {
        return TaskCard.create(CardTitle.of("Tarefa de teste"), CardNote.of("uma nota"), stage, FIXED_NOW);
    }

    /** A clock whose instant can be advanced, so service tests can observe `updatedAt` changing. */
    public static final class FixedClock implements ClockPort {

        private final AtomicReference<Instant> now;

        public FixedClock() {
            this(FIXED_NOW);
        }

        public FixedClock(Instant initial) {
            this.now = new AtomicReference<>(initial);
        }

        @Override
        public Instant now() {
            return now.get();
        }

        public void advanceTo(Instant instant) {
            now.set(instant);
        }
    }
}
