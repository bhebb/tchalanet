package com.tchalanet.server.catalog.i18n.internal.web.model;

import com.tchalanet.server.common.types.id.TenantId;

/**
 * Create I18n Override Request
 *
 * <p>Request to create a new i18n override for a tenant.
 *
 * @param tenantId tenant context (required)
 * @param locale locale code (required, e.g., "fr", "en", "ht")
 * @param i18nKey translation key (required)
 * @param i18nValue override value (required)
 */
public record CreateI18nOverrideRequest(
    TenantId tenantId, String locale, String i18nKey, String i18nValue) {}
