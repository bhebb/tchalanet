package com.tchalanet.server.core.selection.api.model;

public record SelectionValidationError(
    String code,
    String message
) {}
