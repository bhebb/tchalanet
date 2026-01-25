package com.tchalanet.server.catalog.i18n.api;

import com.tchalanet.server.common.types.id.I18nOverrideId;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * I18n Override View (Read Model)
 *
 * <p>Represents a tenant-specific i18n translation override.
 *
 * @param id unique identifier
 * @param tenantId tenant context
 * @param locale locale code (e.g., "fr", "en", "ht")
 * @param i18nKey translation key (e.g., "common.save", "ticket.status.won")
 * @param i18nValue override value in the specified locale
 * @param active whether this override is active
 */
public record I18nOverrideView(
    I18nOverrideId id,
    TenantId tenantId,
    String locale,
    String i18nKey,
    String i18nValue,
    Boolean active) {

  /** Full key in format "locale:i18nKey" */
  public String fullKey() {
    return locale + ":" + i18nKey;
  }
}
