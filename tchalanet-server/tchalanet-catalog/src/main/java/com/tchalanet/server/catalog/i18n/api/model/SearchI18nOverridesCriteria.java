package com.tchalanet.server.catalog.i18n.api.model;

import java.util.UUID;

/**
 * Search I18n Overrides Criteria
 *
 * <p>Search criteria for paginated search of i18n overrides.
 *
 * @param locale          filter by locale (optional, exact match)
 * @param i18nKeyContains filter by key (optional, contains match)
 * @param active          filter by active status (optional, null = all)
 */
public record SearchI18nOverridesCriteria(
    I18nOverrideLevel level,
    String locale,
    String i18nKeyContains,
    Boolean active,
    UUID tenantId,
    String visibility // "active" | "deleted" | "all"
) {
  public static SearchI18nOverridesCriteria empty() {
    return new SearchI18nOverridesCriteria(null, null, null, null, null, null);
  }

  public String visibilitySafe() {
    if (visibility == null) return "active";
    var v = visibility.trim().toLowerCase();
    return (v.equals("active") || v.equals("deleted") || v.equals("all")) ? v : "active";
  }
}
