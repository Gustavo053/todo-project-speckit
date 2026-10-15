package com.todoproject.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** The move form: one per card, submitted by whichever stage button was pressed (research R7). */
public class MoveCardForm {

    @NotBlank
    private String stage = "";

    @NotNull
    private Long revision;

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }
}
