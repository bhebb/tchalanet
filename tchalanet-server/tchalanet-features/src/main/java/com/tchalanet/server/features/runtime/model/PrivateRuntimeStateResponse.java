package com.tchalanet.server.features.runtime.model;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Lightweight in-session runtime refresh returned by {@code GET /runtime/private-state}.
 *
 * <p>Must NOT carry the full bootstrap payload (no full i18n/theme/navigation/settings/profile/
 * page model/dashboard data).
 */
public record PrivateRuntimeStateResponse(
    PrivateRuntimeStatus status,
    RuntimeReadinessView readiness,
    RuntimeNotificationSummary notifications,
    @Nullable RuntimeBlockingState blocking,
    RuntimeVersionHints versions,
    @Nullable List<RuntimeBootstrapNotice> notices
) {}
