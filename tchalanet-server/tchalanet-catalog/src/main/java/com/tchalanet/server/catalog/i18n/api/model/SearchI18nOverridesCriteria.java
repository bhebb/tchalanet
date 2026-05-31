package com.tchalanet.server.catalog.i18n.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.Set;

/**
 * Search criteria for paginated i18n override queries.
 *
 * <p>surfaces: null/empty = no surface filter (admin use). Non-empty = surface IN (:surfaces).
 * Public runtime callers must always pass {@link PublicI18nSurfacePolicy#publicSurfaces()}.
 */
public record SearchI18nOverridesCriteria(
    I18nOverrideLevel level,
    String locale,
    String i18nKeyContains,
    Boolean active,
    TenantId tenantId,
    Set<I18nSurface> surfaces,
    String visibility // "active" | "deleted" | "all"
) {
  public static SearchI18nOverridesCriteria empty() {
    return new SearchI18nOverridesCriteria(null, null, null, null, null, null, null);
  }

  public String visibilitySafe() {
    if (visibility == null) return "active";
    var v = visibility.trim().toLowerCase();
    return (v.equals("active") || v.equals("deleted") || v.equals("all")) ? v : "active";
  }
}
