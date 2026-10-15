package com.todoproject.application.service;

import com.todoproject.application.port.in.CreateCardUseCase;
import com.todoproject.application.port.out.CardRepository;
import com.todoproject.application.port.out.ClockPort;
import com.todoproject.domain.error.BoardFullException;
import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;

/**
 * Adds a card to the board.
 *
 * <p>Order matters: the rate limit and the capacity cap are checked before any value object is
 * built, and the value objects are built before anything is stored — so a rejected request stores
 * nothing at all (FR-022).
 */
public class CreateCardService implements CreateCardUseCase {

    private static final String ACTION = "create";

    private final CardRepository cardRepository;
    private final ClockPort clock;
    private final WriteGuard writeGuard;
    private final int maxCards;

    public CreateCardService(CardRepository cardRepository, ClockPort clock, WriteGuard writeGuard, int maxCards) {
        this.cardRepository = cardRepository;
        this.clock = clock;
        this.writeGuard = writeGuard;
        this.maxCards = maxCards;
    }

    @Override
    public TaskCard create(CreateCardCommand command) {
        writeGuard.requireWriteAllowed(ACTION, command.clientKey());

        if (cardRepository.count() >= maxCards) {
            writeGuard.logRefused(ACTION, "board_full", "none", command.clientKey());
            throw new BoardFullException(maxCards);
        }

        CardTitle title;
        CardNote note;
        try {
            title = CardTitle.of(command.title());
            note = CardNote.of(command.note());
        } catch (InvalidCardContentException invalid) {
            writeGuard.logRefused(
                    ACTION, "invalid_" + invalid.field() + "_" + invalid.code(), "none", command.clientKey());
            throw invalid;
        }

        TaskCard card = TaskCard.create(title, note, command.stageOrDefault(), clock.now());
        TaskCard saved = cardRepository.save(card);
        writeGuard.logApplied(ACTION, saved.id().toString(), "stage=" + saved.stage());
        return saved;
    }
}
