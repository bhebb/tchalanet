package com.tchalanet.server.features.bootstrap;

import com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeBlockingAction;
import jakarta.annotation.Nullable;

/** Describes why the private runtime is blocked and what the user can do about it. */
public record RuntimeBlockingState(
    boolean blocked,
    String code,
    String titleKey,
    String messageKey,
    Severity severity,
    @Nullable RuntimeBlockingAction action
) {
    public enum Severity { WARN, ERROR }
}
