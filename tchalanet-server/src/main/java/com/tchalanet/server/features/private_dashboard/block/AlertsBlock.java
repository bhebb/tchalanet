package com.tchalanet.server.features.private_dashboard.block;

import java.time.Instant;
import java.util.List;

public record AlertsBlock(List<AlertItem> items) {
    public static AlertsBlock empty() {
        return new AlertsBlock(List.of());
    }

    public record AlertItem(String code, String level, String messageKey, Instant createdAt) {}
}
