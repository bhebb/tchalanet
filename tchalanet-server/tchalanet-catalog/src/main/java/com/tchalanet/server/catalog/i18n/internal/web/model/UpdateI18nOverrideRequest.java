package com.tchalanet.server.catalog.i18n.internal.web.model;

import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;

/**
 * Update I18n Override Request
 *
 * <p>Request to update an existing i18n override.
 *
 * @param level new override level (optional, null = no change)
 * @param i18nValue new override value (optional, null = no change)
 * @param active new active status (optional, null = no change)
 */
public record UpdateI18nOverrideRequest(I18nOverrideLevel level, String i18nValue, Boolean active) {}
