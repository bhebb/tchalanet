package com.tchalanet.server.features.bootstrap;

/** Lightweight runtime status returned by {@code GET /runtime/private-state}. */
public enum PrivateRuntimeStatus {
    READY,
    PARTIAL,
    BLOCKED,
    FORCE_RELOAD,
    SESSION_EXPIRED
}
