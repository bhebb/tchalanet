package com.tchalanet.server.catalog.i18n.api;

import com.tchalanet.server.common.types.id.TenantId;

/**
 * Search I18n Overrides Criteria
 *
 * <p>Search criteria for paginated search of i18n overrides.
 *
 * @param tenantId filter by tenant (required for tenant-scoped search)
 * @param locale filter by locale (optional, exact match)
 * @param i18nKeyContains filter by key (optional, contains match)
 * @param active filter by active status (optional, null = all)
 */
public record SearchI18nOverridesCriteria(
    TenantId tenantId, String locale, String i18nKeyContains, Boolean active) {

  public static SearchI18nOverridesCriteria empty() {
    return new SearchI18nOverridesCriteria(null, null, null, null);
  }

  public boolean isEmpty() {
    return tenantId == null && locale == null && i18nKeyContains == null && active == null;
  }
}
