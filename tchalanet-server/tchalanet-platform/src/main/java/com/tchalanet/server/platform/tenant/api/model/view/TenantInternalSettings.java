package com.tchalanet.server.platform.tenant.api.model.view;

public record TenantInternalSettings(
    TenantInternalCommunicationConfig communication,
    TenantInternalDocumentConfig document,
    TenantInternalRules rules,
    TenantInternalLocaleConfig locale) {}
