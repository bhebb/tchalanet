package com.tchalanet.server.core.selection.api.model;


import java.util.List;

public record SelectionValidationResult(
    boolean valid,
    Selection selection,
    List<SelectionValidationError> errors
) {

    public static SelectionValidationResult valid(Selection selection) {
        return new SelectionValidationResult(true, selection, List.of());
    }

    public static SelectionValidationResult invalid(List<SelectionValidationError> errors) {
        return new SelectionValidationResult(false, null, List.copyOf(errors));
    }
}
