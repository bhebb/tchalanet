package com.tchalanet.server.core.settings.dto;

import com.tchalanet.server.core.settings.AppSettingValueType;

public record ResolvedSettingDto(
    String namespace,
    String key,
    AppSettingValueType type,
    String value,
    String effectiveLevel // GLOBAL|TENANT|OUTLET|TERMINAL
    ) {}
