package com.todoproject.adapter.in.rest.dto;

import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The {@code CreateCardRequest} schema in contracts/rest-api.yaml. */
public record CreateCardRequest(
        @NotBlank(message = "{validation.title.REQUIRED}")
                @Size(max = CardTitle.MAX_LENGTH, message = "{validation.title.TOO_LONG}")
                String title,
        @Size(max = CardNote.MAX_LENGTH, message = "{validation.note.TOO_LONG}") String note,
        String stage) {}
