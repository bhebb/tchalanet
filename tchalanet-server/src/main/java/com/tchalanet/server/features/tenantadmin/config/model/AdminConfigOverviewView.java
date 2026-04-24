package com.tchalanet.server.features.tenantadmin.config.model;

public record AdminConfigOverviewView(
    TenantIdentityView identity,
    ThemeSummaryView theme,
    SettingsSummaryView settings,
    I18nSummaryView i18n
) {}
