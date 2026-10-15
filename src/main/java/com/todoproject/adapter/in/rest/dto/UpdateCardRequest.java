package com.todoproject.adapter.in.rest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.todoproject.application.port.in.EditCardUseCase.NoteChange;
import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.ViolationCode;

/**
 * Parses the {@code UpdateCardRequest} body (contracts/rest-api.yaml) from a raw {@link JsonNode}
 * rather than a typed record.
 *
 * <p>A typed record cannot tell "the property was omitted" apart from "the property was sent as
 * {@code null}" -- and the contract needs exactly that distinction: an omitted {@code note} leaves
 * it alone, while an explicit {@code null} clears it. {@link JsonNode#has(String)} answers that
 * directly.
 */
public final class UpdateCardRequest {

    private final String newTitle;
    private final NoteChange noteChange;

    private UpdateCardRequest(String newTitle, NoteChange noteChange) {
        this.newTitle = newTitle;
        this.noteChange = noteChange;
    }

    public static UpdateCardRequest parse(JsonNode body) {
        boolean titlePresent = body != null && body.has("title") && !body.get("title").isNull();
        boolean notePresent = body != null && body.has("note");

        if (!titlePresent && !notePresent) {
            // minProperties: 1 in the contract.
            throw new InvalidCardContentException("title", ViolationCode.REQUIRED);
        }

        String newTitle = titlePresent ? body.get("title").asText() : null;
        NoteChange noteChange = NoteChange.unchanged();
        if (notePresent) {
            JsonNode noteNode = body.get("note");
            noteChange = noteNode.isNull() ? NoteChange.cleared() : NoteChange.to(noteNode.asText());
        }
        return new UpdateCardRequest(newTitle, noteChange);
    }

    public String newTitle() {
        return newTitle;
    }

    public NoteChange noteChange() {
        return noteChange;
    }
}
