package com.tchalanet.server.catalog.settings.api.model;

import com.tchalanet.server.common.types.id.TenantId;

public record CreateSettingAdminRequest(
    String namespace,
    String settingKey,
    String settingValue,
    SettingValueType valueType,
    SettingLevel level,
    SettingExposure exposure,
    TenantId tenantId) {}
