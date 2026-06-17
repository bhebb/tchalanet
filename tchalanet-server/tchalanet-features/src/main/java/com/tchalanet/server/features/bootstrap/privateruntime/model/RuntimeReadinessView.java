package com.tchalanet.server.features.bootstrap;

import java.util.List;

public record RuntimeReadinessView(
    RuntimeReadinessStatus status,
    List<RuntimeReadinessCheck> checks
) {
    public static RuntimeReadinessView ready() {
        return new RuntimeReadinessView(RuntimeReadinessStatus.READY, List.of());
    }

    public enum RuntimeReadinessStatus {
        READY, PARTIAL, BLOCKED
    }
}
