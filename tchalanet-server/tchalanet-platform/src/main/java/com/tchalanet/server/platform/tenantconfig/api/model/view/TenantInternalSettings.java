package com.tchalanet.server.platform.tenantconfig.api.model.view;

public record TenantInternalSettings(
    TenantInternalCommunicationConfig communication,
    TenantInternalDocumentConfig document,
    TenantInternalRules rules,
    TenantInternalLocaleConfig locale) {}
