package com.tchalanet.server.features.tenantadmin.readiness.model;

import java.util.List;

/**
 * One section of the readiness diagnosis (e.g. "outlets", "terminals").
 */
public record TenantReadinessSection(
    String id,
    String labelKey,
    TenantReadinessStatus status,
    String route,
    List<TenantReadinessIssue> issues) {}
