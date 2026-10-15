package com.todoproject.application.port.in;

import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;

/** Inbound port: change a card's title or note. Never its stage (FR-015). */
public interface EditCardUseCase {

    TaskCard edit(EditCardCommand command);

    /**
     * @param newTitle raw replacement title, or null to leave the title alone
     * @param noteChange what to do with the note — the two cases are explicit so that "leave it
     *     alone" and "clear it" can never be confused, which a nullable string could not express
     * @param expectedRevision the revision the caller was looking at (research R14)
     */
    record EditCardCommand(
            CardId id, String newTitle, NoteChange noteChange, long expectedRevision, String clientKey) {}

    /** Either the note stays as it is, or it becomes this text (null text clears it). */
    sealed interface NoteChange {

        record Unchanged() implements NoteChange {}

        record ChangeTo(String text) implements NoteChange {}

        static NoteChange unchanged() {
            return new Unchanged();
        }

        static NoteChange to(String text) {
            return new ChangeTo(text);
        }

        static NoteChange cleared() {
            return new ChangeTo(null);
        }
    }
}
