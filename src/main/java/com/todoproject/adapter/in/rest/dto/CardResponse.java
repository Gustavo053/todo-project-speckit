package com.todoproject.adapter.in.rest.dto;

import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;

/** The {@code Card} schema in contracts/rest-api.yaml. */
public record CardResponse(
        String id, String title, String note, BoardStage stage, Instant createdAt, Instant updatedAt, long revision) {

    public static CardResponse of(TaskCard card) {
        return new CardResponse(
                card.id().toString(),
                card.title().value(),
                card.note().orNull(),
                card.stage(),
                card.createdAt(),
                card.updatedAt(),
                card.revision());
    }
}
