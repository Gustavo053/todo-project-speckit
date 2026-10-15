package com.todoproject.application.port.in;

import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;

/** Inbound port: move a card to another column. Every transition is legal (FR-009). */
public interface MoveCardUseCase {

    TaskCard move(MoveCardCommand command);

    record MoveCardCommand(CardId id, BoardStage target, long expectedRevision, String clientKey) {}
}
