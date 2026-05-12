package com.tchalanet.server.features.tenantadmin.config.settings.model;

public record AdminSettingRow(
    String id,
    String namespace,
    String settingKey,
    String valueType,
    String settingValue,
    String level,
    Boolean active
) {}
