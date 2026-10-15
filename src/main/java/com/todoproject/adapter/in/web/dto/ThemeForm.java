package com.todoproject.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** The appearance control's form: one of SYSTEM, LIGHT, DARK, plus where to return to. */
public class ThemeForm {

    @NotBlank
    private String preference = "SYSTEM";

    private String redirectTo = "/";

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public String getRedirectTo() {
        return redirectTo;
    }

    public void setRedirectTo(String redirectTo) {
        this.redirectTo = redirectTo;
    }
}
