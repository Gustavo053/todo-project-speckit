package com.todoproject.application.service;

import com.todoproject.application.port.in.EditCardUseCase;
import com.todoproject.application.port.out.CardRepository;
import com.todoproject.application.port.out.ClockPort;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;

/**
 * Changes a card's title or note.
 *
 * <p>Only those two. There is no code path here that touches the stage, which is how FR-015 is
 * enforced rather than merely intended — the aggregate exposes no stage mutator but
 * {@code moveTo}.
 */
public class EditCardService implements EditCardUseCase {

    private static final String ACTION = "edit";

    private final CardRepository cardRepository;
    private final ClockPort clock;
    private final WriteGuard writeGuard;

    public EditCardService(CardRepository cardRepository, ClockPort clock, WriteGuard writeGuard) {
        this.cardRepository = cardRepository;
        this.clock = clock;
        this.writeGuard = writeGuard;
    }

    @Override
    public TaskCard edit(EditCardCommand command) {
        writeGuard.requireWriteAllowed(ACTION, command.clientKey());

        TaskCard card = cardRepository.findById(command.id()).orElseThrow(() -> {
            writeGuard.logRefused(ACTION, "not_found", command.id().toString(), command.clientKey());
            return new CardNotFoundException(command.id().toString());
        });

        try {
            card.requireCurrentRevision(command.expectedRevision());
        } catch (StaleCardException stale) {
            writeGuard.logRefused(ACTION, "stale", command.id().toString(), command.clientKey());
            throw stale;
        }

        Instant now = clock.now();
        try {
            if (command.newTitle() != null) {
                card.rename(CardTitle.of(command.newTitle()), now);
            }
            if (command.noteChange() instanceof NoteChange.ChangeTo change) {
                card.changeNote(CardNote.of(change.text()), now);
            }
        } catch (InvalidCardContentException invalid) {
            // Nothing has been saved, so the stored card is untouched (FR-022).
            writeGuard.logRefused(
                    ACTION,
                    "invalid_" + invalid.field() + "_" + invalid.code(),
                    command.id().toString(),
                    command.clientKey());
            throw invalid;
        }

        TaskCard saved = cardRepository.save(card);
        writeGuard.logApplied(ACTION, saved.id().toString(), "stage=" + saved.stage());
        return saved;
    }
}
