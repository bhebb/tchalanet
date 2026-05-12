package com.tchalanet.server.core.haiti.internal.infra.web.model;

import jakarta.validation.constraints.NotBlank;

/** Web request to submit a new Tchala suggestion (public). */
public record SubmitSuggestionRequest(
    @NotBlank String lang, @NotBlank String dream, @NotBlank String numbers, String note) {}
