package com.tchalanet.server.common.types.id;

import java.util.UUID;

/**
 * Typed identifier for DrawChannel.
 */
public record LedgerEntryId(UUID value) {

    public LedgerEntryId {
        if (value == null) throw new IllegalArgumentException("DrawChannelId.value is null");
    }

    public static LedgerEntryId of(UUID value) {
        return new LedgerEntryId(value);
    }

    /**
     * Convenience for mappers: returns null if raw is null.
     */
    public static LedgerEntryId nullableOf(UUID raw) {
        return raw == null ? null : new LedgerEntryId(raw);
    }

    /**
     * Parse from UUID string (web/input).
     */
    public static LedgerEntryId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("DrawChannelId string is required");
        }
        return new LedgerEntryId(UUID.fromString(raw));
    }

    public UUID value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
