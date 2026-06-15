package com.tchalanet.server.features.bootstrap;

import java.util.List;

public record RuntimeReadinessView(
    RuntimeReadinessStatus status,
    List<RuntimeReadinessCheck> checks
) {
    public static com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeReadinessView ready() {
        return new com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeReadinessView(RuntimeReadinessStatus.READY, List.of());
    }

    public enum RuntimeReadinessStatus {
        READY, PARTIAL, BLOCKED
    }
}
