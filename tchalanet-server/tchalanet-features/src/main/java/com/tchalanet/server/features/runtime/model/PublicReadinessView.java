package com.tchalanet.server.features.runtime.model;

import java.util.List;

/** Public readiness summary. Must not expose internal platform or private tenant state. */
public record PublicReadinessView(
    Status status,
    List<PublicReadinessCheck> checks
) {
    public enum Status { READY, PARTIAL }

    public static PublicReadinessView ready() {
        return new PublicReadinessView(Status.READY, List.of());
    }
}
