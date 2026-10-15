package com.todoproject.adapter.in.rest.dto;

import com.todoproject.application.port.in.BoardSnapshot;
import com.todoproject.domain.model.BoardStage;
import java.util.List;

/** The {@code Column} schema in contracts/rest-api.yaml. */
public record ColumnResponse(BoardStage stage, int count, List<CardResponse> cards) {

    public static ColumnResponse of(BoardSnapshot.Column column) {
        return new ColumnResponse(
                column.stage(), column.count(), column.cards().stream().map(CardResponse::of).toList());
    }
}
