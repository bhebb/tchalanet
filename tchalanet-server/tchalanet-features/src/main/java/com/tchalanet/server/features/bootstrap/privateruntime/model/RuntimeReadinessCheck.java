package com.tchalanet.server.features.bootstrap;

public record RuntimeReadinessCheck(
    String code,
    String labelKey,
    CheckStatus status
) {
    public enum CheckStatus {
        READY, MISSING, BLOCKED, WARNING
    }
}
