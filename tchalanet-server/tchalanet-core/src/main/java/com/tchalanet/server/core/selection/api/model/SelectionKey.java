package com.tchalanet.server.core.selection.api.model;

/** Value object for canonical selection keys. */

public record SelectionKey(String value) {

    public SelectionKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SelectionKey.value is null or blank");
        }
    }

    public static SelectionKey of(String value) {
        return new SelectionKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
