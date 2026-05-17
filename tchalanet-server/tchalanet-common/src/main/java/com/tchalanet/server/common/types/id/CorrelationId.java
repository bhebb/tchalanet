package com.tchalanet.server.common.types.id;

import java.util.Objects;
import java.util.UUID;

public record CorrelationId(String value) {

    public CorrelationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("correlationId is required");
        }
        value = value.trim();
    }

    public static CorrelationId of(String value) {
        return new CorrelationId(value);
    }

    public static CorrelationId of(UUID value) {
        Objects.requireNonNull(value, "correlationId UUID is required");
        return new CorrelationId(value.toString());
    }

    public static CorrelationId newId() {
        return of(UUID.randomUUID());
    }

    public static CorrelationId ofNullable(String value) {
        return value == null || value.isBlank() ? null : of(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
