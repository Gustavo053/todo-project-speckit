package com.todoproject.application.port.in;

import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.TaskCard;

/** Inbound port: add a card to the board. */
public interface CreateCardUseCase {

    TaskCard create(CreateCardCommand command);

    /**
     * @param title raw title text; the domain validates and normalizes it
     * @param note raw note text, or null for no note
     * @param stage the column to create in; null means {@link BoardStage#defaultStage()} (FR-006)
     * @param clientKey opaque caller key for the write rate limit
     */
    record CreateCardCommand(String title, String note, BoardStage stage, String clientKey) {

        public BoardStage stageOrDefault() {
            return stage == null ? BoardStage.defaultStage() : stage;
        }
    }
}
