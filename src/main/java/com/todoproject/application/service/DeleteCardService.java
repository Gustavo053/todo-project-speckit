package com.todoproject.application.service;

import com.todoproject.application.port.in.DeleteCardUseCase;
import com.todoproject.application.port.out.CardRepository;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.TaskCard;

/**
 * Removes a card permanently.
 *
 * <p>No soft delete, no archive, no tombstone (FR-017). Deleting an already-deleted card reports
 * not found and never brings it back (FR-012).
 */
public class DeleteCardService implements DeleteCardUseCase {

    private static final String ACTION = "delete";

    private final CardRepository cardRepository;
    private final WriteGuard writeGuard;

    public DeleteCardService(CardRepository cardRepository, WriteGuard writeGuard) {
        this.cardRepository = cardRepository;
        this.writeGuard = writeGuard;
    }

    @Override
    public void delete(DeleteCardCommand command) {
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

        cardRepository.delete(card);
        writeGuard.logApplied(ACTION, card.id().toString(), "removed");
    }
}
