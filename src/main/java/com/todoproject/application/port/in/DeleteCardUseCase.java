package com.todoproject.application.port.in;

import com.todoproject.domain.model.CardId;

/** Inbound port: remove a card permanently. No archive, no undo (FR-017). */
public interface DeleteCardUseCase {

    void delete(DeleteCardCommand command);

    record DeleteCardCommand(CardId id, long expectedRevision, String clientKey) {}
}
