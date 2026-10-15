package com.todoproject.adapter.in.rest.dto;

import java.util.List;

/**
 * RFC 9457 problem details (contracts/rest-api.yaml). Never carries a stack trace, SQL, or an
 * internal identifier (FR-024) -- every field here is a fixed, pre-written sentence or code.
 */
public record Problem(String type, String title, int status, String detail, List<FieldViolation> errors) {

    private static final String BASE = "https://todo-project.local/problems/";

    public static Problem of(String slug, String title, int status, String detail) {
        return new Problem(BASE + slug, title, status, detail, null);
    }

    public static Problem validationFailed(String detail, List<FieldViolation> errors) {
        return new Problem(BASE + "validation-failed", "Validation failed", 400, detail, errors);
    }

    public record FieldViolation(String field, String code, String message) {}
}
