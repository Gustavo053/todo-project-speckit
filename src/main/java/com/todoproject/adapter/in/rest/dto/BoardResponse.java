package com.todoproject.adapter.in.rest.dto;

import com.todoproject.application.port.in.BoardSnapshot;
import java.util.List;

/** The {@code Board} schema in contracts/rest-api.yaml. Always all four columns. */
public record BoardResponse(List<ColumnResponse> columns, long totalCards, int maxCards) {

    public static BoardResponse of(BoardSnapshot snapshot) {
        return new BoardResponse(
                snapshot.columns().stream().map(ColumnResponse::of).toList(),
                snapshot.totalCards(),
                snapshot.maxCards());
    }
}
