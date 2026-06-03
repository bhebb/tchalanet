package com.tchalanet.server.platform.tenant.api.model;

public record TenantStatsView(
    int total,
    int active,
    int suspended
) {}
