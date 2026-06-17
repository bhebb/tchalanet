package com.tchalanet.server.features.bootstrap;

import jakarta.annotation.Nullable;

/** Optional action a blocked client can take (navigate or run a known command). */
public record RuntimeBlockingAction(
    String labelKey,
    @Nullable String route,
    @Nullable String command
) {}
