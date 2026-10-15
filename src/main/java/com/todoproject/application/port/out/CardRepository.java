package com.todoproject.application.port.out;

import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for card storage. Declared in domain types only — the core never learns what is on
 * the other side (constitution principle II).
 */
public interface CardRepository {

    Optional<TaskCard> findById(CardId id);

    /** All cards, most-recently-changed first (data-model.md: the board's only read pattern). */
    List<TaskCard> findAllMostRecentlyChangedFirst();

    long count();

    /**
     * Persists the card and returns it with its advanced revision.
     *
     * @throws com.todoproject.domain.error.StaleCardException if another writer changed it first
     */
    TaskCard save(TaskCard card);

    /**
     * Removes the card permanently — no archive, no tombstone (FR-017).
     *
     * @throws com.todoproject.domain.error.StaleCardException if another writer changed it first
     * @throws com.todoproject.domain.error.CardNotFoundException if it is already gone
     */
    void delete(TaskCard card);
}
