package com.todoproject.adapter.in.web.view;

import com.todoproject.application.port.in.BoardSnapshot;
import com.todoproject.domain.model.BoardStage;
import java.util.List;

/** The board, shaped for rendering. Always all four columns, in declaration order. */
public record BoardView(List<ColumnView> columns, long totalCards, int maxCards) {

    public static BoardView of(BoardSnapshot snapshot) {
        List<ColumnView> columns =
                snapshot.columns().stream().map(ColumnView::of).toList();
        return new BoardView(columns, snapshot.totalCards(), snapshot.maxCards());
    }

    /** One column, with the message-bundle keys its template needs. */
    public record ColumnView(BoardStage stage, String labelKey, String emptyKey, int count, List<CardView> cards) {

        static ColumnView of(BoardSnapshot.Column column) {
            return new ColumnView(
                    column.stage(),
                    "stage." + column.stage().name(),
                    "column.empty." + column.stage().name(),
                    column.count(),
                    column.cards().stream().map(CardView::of).toList());
        }

        public boolean isEmpty() {
            return cards.isEmpty();
        }

        /** The first column is expanded by default on a narrow screen (research R13). */
        public boolean openByDefault() {
            return stage == BoardStage.BACKLOG;
        }
    }
}
