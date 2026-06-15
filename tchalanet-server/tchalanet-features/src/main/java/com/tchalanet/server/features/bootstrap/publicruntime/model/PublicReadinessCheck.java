package com.tchalanet.server.features.bootstrap.publicruntime.model;

import jakarta.annotation.Nullable;

/** A single public readiness check (light and safe — no internal/private state). */
public record PublicReadinessCheck(
    String code,
    Status status,
    @Nullable String message
) {
    public enum Status { READY, WARNING }
}
