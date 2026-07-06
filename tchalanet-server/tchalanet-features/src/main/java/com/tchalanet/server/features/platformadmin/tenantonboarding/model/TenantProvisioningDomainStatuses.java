package com.tchalanet.server.features.platformadmin.tenantonboarding.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Per-domain provisioning statuses returned by tenant onboarding.
 *
 * Component names intentionally match the existing JSON contract.
 */
public record TenantProvisioningDomainStatuses(
    @JsonProperty("tenant_identity")
    String tenantIdentity,
    @JsonProperty("pagemodels")
    String pageModels,
    String theme,
    String settings,
    String i18n,
    String games,
    String pricing,
    @JsonProperty("draw_channels")
    String drawChannels,
    @JsonProperty("promotions_templates")
    String promotionsTemplates,
    @JsonProperty("limits_templates")
    String limitsTemplates,
    String subscription) {}
