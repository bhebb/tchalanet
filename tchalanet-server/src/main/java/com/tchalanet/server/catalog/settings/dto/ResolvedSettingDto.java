package com.tchalanet.server.catalog.settings.dto;

import com.tchalanet.server.catalog.settings.AppSettingValueType;

public record ResolvedSettingDto(
    String namespace,
    String key,
    AppSettingValueType type,
    String value,
    String effectiveLevel // GLOBAL|TENANT|OUTLET|TERMINAL
    ) {}
