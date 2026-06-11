package com.tchalanet.server.features.runtime.model;

/** Lightweight runtime status returned by {@code GET /runtime/private-state}. */
public enum PrivateRuntimeStatus {
    READY,
    PARTIAL,
    BLOCKED,
    FORCE_RELOAD,
    SESSION_EXPIRED
}
