package com.todoproject.adapter.in.web.dto;

import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The add-card form (FR-005, contracts/html-routes.md).
 *
 * <p>Bean Validation here is the adapter's syntactic check — presence and length. It is
 * deliberately shallower than the domain's allowlist policy, which runs again inside
 * {@code CardTitle}/{@code CardNote} regardless of what this layer accepts (constitution principle
 * III: two independent layers).
 */
public class CreateCardForm {

    @NotBlank(message = "{validation.title.REQUIRED}")
    @Size(max = CardTitle.MAX_LENGTH, message = "{validation.title.TOO_LONG}")
    private String title = "";

    @Size(max = CardNote.MAX_LENGTH, message = "{validation.note.TOO_LONG}")
    private String note = "";

    private String stage = "";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}
