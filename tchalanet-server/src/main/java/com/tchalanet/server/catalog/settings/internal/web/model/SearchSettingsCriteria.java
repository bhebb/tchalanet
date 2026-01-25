package com.tchalanet.server.catalog.settings.internal.web.model;

import com.tchalanet.server.catalog.settings.api.SettingLevel;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Search Settings Criteria
 *
 * <p>Search criteria for paginated admin search of settings.
 *
 * @param namespace filter by namespace (exact match, optional)
 * @param settingKey filter by setting key (contains match, optional)
 * @param level filter by level (optional)
 * @param tenantId filter by tenant ID (optional)
 * @param active filter by active status (optional, null = all)
 */
public record SearchSettingsCriteria(
    String namespace, String settingKey, SettingLevel level, TenantId tenantId, Boolean active) {

  public static SearchSettingsCriteria empty() {
    return new SearchSettingsCriteria(null, null, null, null, null);
  }

  public boolean isEmpty() {
    return namespace == null
        && settingKey == null
        && level == null
        && tenantId == null
        && active == null;
  }
}
