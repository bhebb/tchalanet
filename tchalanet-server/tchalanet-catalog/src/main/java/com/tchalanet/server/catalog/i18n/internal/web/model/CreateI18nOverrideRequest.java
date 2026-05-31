package com.tchalanet.server.catalog.i18n.internal.web.model;

import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import com.tchalanet.server.catalog.i18n.api.model.I18nSurface;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Create I18n Override Request
 *
 * <p>surface defaults to INTERNAL if not provided.
 */
public record CreateI18nOverrideRequest(
    TenantId tenantId,
    String locale,
    I18nOverrideLevel level,
    I18nSurface surface,
    String i18nKey,
    String i18nValue) {}
