package com.tchalanet.server.catalog.settings.api.model;

public record SettingsCatalogStatsView(
    int totalGlobalSettings,
    int totalTenantSettings,
    int totalActiveSettings
) {}
