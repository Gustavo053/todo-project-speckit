package com.todoproject.application.port.in;

import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.TaskCard;
import java.util.List;

/**
 * The whole board as the core reports it.
 *
 * <p>Always carries all four columns in {@link BoardStage} declaration order, including empty ones,
 * so no caller has to infer the column set (FR-001, FR-003).
 */
public record BoardSnapshot(List<Column> columns, long totalCards, int maxCards) {

    public BoardSnapshot(List<Column> columns, long totalCards, int maxCards) {
        this.columns = List.copyOf(columns);
        this.totalCards = totalCards;
        this.maxCards = maxCards;
    }

    /** One column: its stage and its cards, most-recently-changed first. */
    public record Column(BoardStage stage, List<TaskCard> cards) {

        public Column(BoardStage stage, List<TaskCard> cards) {
            this.stage = stage;
            this.cards = List.copyOf(cards);
        }

        public int count() {
            return cards.size();
        }

        public boolean isEmpty() {
            return cards.isEmpty();
        }
    }
}
