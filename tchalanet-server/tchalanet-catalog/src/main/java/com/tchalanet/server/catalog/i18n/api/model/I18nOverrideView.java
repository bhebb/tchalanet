package com.tchalanet.server.catalog.i18n.api.model;

import com.tchalanet.server.common.types.id.I18nOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

/**
 * Public catalog view for i18n overrides.
 *
 * level:
 *  - GLOBAL: applies to all tenants (tenantId null)
 *  - TENANT: applies to current tenant (tenantId non-null)
 */
public record I18nOverrideView(
    I18nOverrideId id,
    I18nOverrideLevel level,
    TenantId tenantId,     // null for GLOBAL
    String locale,         // fr/en/ht
    String i18nKey,
    String i18nValue,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
