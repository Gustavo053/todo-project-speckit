package com.todoproject.adapter.in.web.dto;

import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The edit-card form. Always carries both fields and the revision it was rendered from. */
public class EditCardForm {

    @NotBlank(message = "{validation.title.REQUIRED}")
    @Size(max = CardTitle.MAX_LENGTH, message = "{validation.title.TOO_LONG}")
    private String title = "";

    @Size(max = CardNote.MAX_LENGTH, message = "{validation.note.TOO_LONG}")
    private String note = "";

    @NotNull
    private Long revision;

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

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }
}
