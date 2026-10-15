package com.todoproject.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

/** The delete confirmation form: carries only the revision the confirmation was shown for. */
public class DeleteCardForm {

    @NotNull
    private Long revision;

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }
}
