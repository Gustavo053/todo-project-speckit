package com.todoproject.application.service;

import com.todoproject.application.port.in.MoveCardUseCase;
import com.todoproject.application.port.out.CardRepository;
import com.todoproject.application.port.out.ClockPort;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.TaskCard;

/**
 * Moves a card to another column.
 *
 * <p>A move to a card that no longer exists is refused and never recreates it (FR-012), and a move
 * based on a stale revision is refused rather than applied (research R14).
 */
public class MoveCardService implements MoveCardUseCase {

    private static final String ACTION = "move";

    private final CardRepository cardRepository;
    private final ClockPort clock;
    private final WriteGuard writeGuard;

    public MoveCardService(CardRepository cardRepository, ClockPort clock, WriteGuard writeGuard) {
        this.cardRepository = cardRepository;
        this.clock = clock;
        this.writeGuard = writeGuard;
    }

    @Override
    public TaskCard move(MoveCardCommand command) {
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

        card.moveTo(command.target(), clock.now());
        TaskCard saved = cardRepository.save(card);
        writeGuard.logApplied(ACTION, saved.id().toString(), "stage=" + saved.stage());
        return saved;
    }
}
