package com.tchalanet.server.catalog.tenant.api.model;

public record TenantStatsView(
    int total,
    int active,
    int suspended
) {}
