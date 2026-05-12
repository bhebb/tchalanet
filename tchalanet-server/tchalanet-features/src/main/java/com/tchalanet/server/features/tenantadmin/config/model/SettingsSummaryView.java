package com.tchalanet.server.features.tenantadmin.config.model;

import java.util.List;

public record SettingsSummaryView(long totalTenantSettings, List<String> namespacesTop) {}
