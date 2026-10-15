package com.todoproject.application.service;

import com.todoproject.application.port.in.ViewCardQuery;
import com.todoproject.application.port.out.CardRepository;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;

public class ViewCardService implements ViewCardQuery {

    private final CardRepository cardRepository;

    public ViewCardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    public TaskCard load(CardId id) {
        return cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id.toString()));
    }
}
