package com.tchalanet.server.features.platformadmin.tenantonboarding.model;

import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import java.util.List;
import java.util.Map;

/**
 * Result of {@code POST /platform/tenant-onboarding/provision}.
 *
 * Includes per-domain status, the next steps and the freshly-computed
 * {@link TenantReadinessView} for the newly provisioned tenant.
 */
public record TenantProvisioningResultView(
    String tenantId,
    String tenantCode,
    TenantProvisioningProfile profile,
    Map<String, String> domainStatuses,
    List<String> nextSteps,
    TenantReadinessView readiness) {}
