package com.tchalanet.server.core.tenant.application.query.model;

/** Global statistics for tenants across all statuses. */
public record TenantGlobalStatsView(int total, int active, int suspended) {}
