package com.tchalanet.server.catalog.i18n.internal.cache;

/**
 * Cache names for catalog/i18n.
 *
 * <p>Defines cache identifiers for the I18n Overrides catalog.
 */
public final class I18nOverridesCacheNames {

  private I18nOverridesCacheNames() {}

  /** Cache for overrides by tenant and locale */
  public static final String BY_TENANT_LOCALE = "catalog:i18n:by_tenant_locale";

  /** Cache for overrides by tenant (all locales) */
  public static final String BY_TENANT = "catalog:i18n:by_tenant";

  /** Cache for override by ID */
  public static final String BY_ID = "catalog:i18n:by_id";

    public static final String RESOLVED_BY_LOCALE = "catalog:i18n:resolved_by_locale";

}
