package com.todoproject.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** The {@code MoveCardRequest} schema in contracts/rest-api.yaml. */
public record MoveCardRequest(@NotBlank String stage) {}
