package com.tchalanet.server.features.tenantadmin.config.settings.model;

public record UpsertTenantSettingRequest(
    String namespace,
    String settingKey,
    String valueType,
    String settingValue,
    Boolean active
) {}
