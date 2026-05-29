package com.tchalanet.server.features.tenantadmin.readiness.model;

/**
 * A single readiness issue (missing or partial setup).
 * Section is the bucket id (e.g. "outlets"); messageKey is an i18n key the
 * frontend resolves.
 */
public record TenantReadinessIssue(
    String section,
    String messageKey,
    String route) {}
