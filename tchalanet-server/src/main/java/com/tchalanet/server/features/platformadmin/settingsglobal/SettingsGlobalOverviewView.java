package com.tchalanet.server.features.platformadmin.settingsglobal;

public record SettingsGlobalOverviewView(
    int totalGlobalSettings,
    int totalTenantSettings,
    int totalActiveSettings
) {}
