package com.tchalanet.server.catalog.settings.internal.web.model;

import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Internal search criteria for settings.
 * exposure: null = no filter (admin); non-null = filter by exposure.
 */
public record SearchSettingsCriteria(
    String namespace,
    String settingKey,
    SettingLevel level,
    SettingExposure exposure,
    TenantId tenantId,
    Boolean active) {

  public static SearchSettingsCriteria empty() {
    return new SearchSettingsCriteria(null, null, null, null, null, null);
  }

  public boolean isEmpty() {
    return namespace == null
        && settingKey == null
        && level == null
        && exposure == null
        && tenantId == null
        && active == null;
  }
}
