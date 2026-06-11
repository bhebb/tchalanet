package com.tchalanet.server.features.runtime.model;

import jakarta.annotation.Nullable;

/** Optional action a blocked client can take (navigate or run a known command). */
public record RuntimeBlockingAction(
    String labelKey,
    @Nullable String route,
    @Nullable String command
) {}
