package com.tchalanet.server.features.bootstrap.privateruntime.model;

import jakarta.annotation.Nullable;
import java.util.List;

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
    PageModelRef pageModelRef,
    String entryRoute,
    @Nullable List<RuntimeBootstrapNotice> notices
) {}
