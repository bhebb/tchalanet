package com.tchalanet.server.features.runtime.model;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Lightweight public startup runtime returned by {@code GET /runtime/public-bootstrap}.
 *
 * <p>No authentication required. Must NOT expose user, entitlements, private navigation,
 * notification summary, or internal readiness.
 */
public record PublicBootstrapResponse(
    PublicSettingsView settings,
    PublicThemeView theme,
    PublicI18nBundle i18n,
    PublicNavigationModel navigation,
    PublicReadinessView readiness,
    PageModelRef pageModelRef,
    @Nullable List<RuntimeBootstrapNotice> notices
) {}
