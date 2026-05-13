package com.tchalanet.server.catalog.settings.api.model;

import com.tchalanet.server.common.types.id.TenantId;

public record SearchSettingsAdminCriteria(
    String namespace,
    String settingKey,
    SettingLevel level,
    TenantId tenantId,
    Boolean active) {}
