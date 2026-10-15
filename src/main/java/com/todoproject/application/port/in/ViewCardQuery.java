package com.todoproject.application.port.in;

import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;

/**
 * Inbound port: read a single card.
 *
 * <p>Needed by the edit form and the delete confirmation page, which must show the card's current
 * content before the person acts on it (contracts/html-routes.md).
 */
public interface ViewCardQuery {

    /**
     * @throws com.todoproject.domain.error.CardNotFoundException if no such card exists
     */
    TaskCard load(CardId id);
}
