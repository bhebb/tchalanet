package com.tchalanet.server.features.private_dashboard.block;

public record SessionBlock(
    String sessionId,
    String cashierId,
    boolean active,
    long expiresAtEpochMillis
) {
    public static SessionBlock empty() {
        return new SessionBlock(null, null, false, 0L);
    }
}
