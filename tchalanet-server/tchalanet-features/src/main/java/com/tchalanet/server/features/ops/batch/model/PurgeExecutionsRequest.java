package com.tchalanet.server.features.ops.batch.model;

public record PurgeExecutionsRequest(Integer retention_days) {

    public int resolvedRetentionDays() {
        if (retention_days == null) {
            return 7;
        }
        if (retention_days < 1 || retention_days > 3650) {
            throw new IllegalArgumentException("retention_days must be between 1 and 3650");
        }
        return retention_days;
    }
}
