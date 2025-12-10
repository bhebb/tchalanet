package com.tchalanet.server.features.private_dashboard.block;

import java.time.Instant;
import java.util.List;

public record ValidationsBlock(
    List<ValidationItem> items
) {
    public static ValidationsBlock empty() {
        return new ValidationsBlock(List.of());
    }

    public record ValidationItem(
        String id,
        String labelKey,
        String target,
        String amount,
        String requestedBy,
        Instant requestedAt
    ) {}
}
