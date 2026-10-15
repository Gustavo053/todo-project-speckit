package com.todoproject.application.service;

import com.todoproject.application.port.in.BoardSnapshot;
import com.todoproject.application.port.in.ViewBoardQuery;
import com.todoproject.application.port.out.CardRepository;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.TaskCard;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Groups the stored cards into the four columns, always emitting all of them (FR-001, FR-004). */
public class ViewBoardService implements ViewBoardQuery {

    private final CardRepository cardRepository;
    private final int maxCards;

    public ViewBoardService(CardRepository cardRepository, int maxCards) {
        this.cardRepository = cardRepository;
        this.maxCards = maxCards;
    }

    @Override
    public BoardSnapshot load() {
        List<TaskCard> cards = cardRepository.findAllMostRecentlyChangedFirst();

        Map<BoardStage, List<TaskCard>> byStage = new EnumMap<>(BoardStage.class);
        for (BoardStage stage : BoardStage.values()) {
            byStage.put(stage, new ArrayList<>());
        }
        for (TaskCard card : cards) {
            byStage.get(card.stage()).add(card);
        }

        // Declaration order is the display order — never a hardcoded sequence.
        List<BoardSnapshot.Column> columns = new ArrayList<>(BoardStage.values().length);
        for (BoardStage stage : BoardStage.values()) {
            columns.add(new BoardSnapshot.Column(stage, byStage.get(stage)));
        }
        return new BoardSnapshot(columns, cards.size(), maxCards);
    }
}
