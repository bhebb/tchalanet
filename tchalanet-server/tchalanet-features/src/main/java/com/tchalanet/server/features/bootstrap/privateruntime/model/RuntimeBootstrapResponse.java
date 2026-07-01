package com.tchalanet.server.features.bootstrap.privateruntime.model;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

public record RuntimeBootstrapResponse(
    PrivateBootstrapSpace space,
    AuthenticatedUserView user,
    @Nullable TenantContextView tenantContext,
    RuntimeSettingsView settings,
    RuntimeThemeView theme,
    RuntimeI18nBundle i18n,
    EntitlementsView entitlements,
    RuntimeReadinessView readiness,
    RuntimeNotificationSummary notifications,
    @Nullable Map<String, Object> navigationDrawer,
    PageModelRef pageModelRef,
    String entryRoute,
    @Nullable List<RuntimeBootstrapNotice> notices
) {}
