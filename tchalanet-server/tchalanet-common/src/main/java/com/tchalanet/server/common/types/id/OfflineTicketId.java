package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineTicketId(UUID value) {

    public OfflineTicketId {
        if (value == null) {
            throw new IllegalArgumentException("OfflineTicketId.value is null");
        }
    }

    public static OfflineTicketId of(UUID value) {
        return new OfflineTicketId(value);
    }

    public static OfflineTicketId nullableOf(UUID value) {
        return value == null ? null : new OfflineTicketId(value);
    }

    public static OfflineTicketId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("OfflineTicketId string is required");
        }
        return new OfflineTicketId(UUID.fromString(raw));
    }
}
