package com.tchalanet.server.features.platformadmin.tenantonboarding.model;

import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import java.math.BigDecimal;
import java.util.List;

/**
 * Result of {@code POST /platform/tenant-onboarding/provision}.
 *
 * <p>Includes per-domain status, the next steps and the freshly-computed {@link
 * TenantReadinessView} for the newly provisioned tenant.
 */
public record TenantProvisioningResultView(
    String tenantId,
    String tenantCode,
    TenantProvisioningProfile profile,
    BigDecimal defaultCommissionRate,
    TenantProvisioningDomainStatuses domainStatuses,
    List<String> nextSteps,
    List<String> warnings,
    String appliedPlanCode,
    TenantReadinessView readiness,
    String initialAdminUserId,
    String initialAdminUsername,
    String initialAdminEmail,
    String initialAdminCredentialStatus,
    String initialAdminTemporaryPassword,
    Boolean initialAdminMustChangePassword,
    Boolean initialAdminMustCompleteProfile) {}
