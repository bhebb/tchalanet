package com.tchalanet.server.features.bootstrap;

import com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeI18nBundle;
import com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeSettingsView;
import com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeThemeView;
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
    @Nullable List<RuntimeBootstrapNotice> notices
) {}
