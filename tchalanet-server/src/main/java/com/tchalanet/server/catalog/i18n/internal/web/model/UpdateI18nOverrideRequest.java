package com.tchalanet.server.catalog.i18n.internal.web.model;

/**
 * Update I18n Override Request
 *
 * <p>Request to update an existing i18n override.
 *
 * @param i18nValue new override value (optional, null = no change)
 * @param active new active status (optional, null = no change)
 */
public record UpdateI18nOverrideRequest(String i18nValue, Boolean active) {}
