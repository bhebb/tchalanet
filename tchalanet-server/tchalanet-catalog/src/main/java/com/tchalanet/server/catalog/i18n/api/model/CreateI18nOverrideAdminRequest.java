package com.tchalanet.server.catalog.i18n.api.model;

import com.tchalanet.server.common.types.id.TenantId;

public record CreateI18nOverrideAdminRequest(
    TenantId tenantId,
    String locale,
    I18nOverrideLevel level,
    I18nSurface surface,
    String i18nKey,
    String i18nValue) {}
