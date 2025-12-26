package com.tchalanet.server.common.settings.dto;

import com.tchalanet.server.common.settings.AppSettingValueType;

public record ResolvedSettingDto(
    String namespace,
    String key,
    AppSettingValueType type,
    String value,
    String effectiveLevel // GLOBAL|TENANT|OUTLET|TERMINAL
    ) {}
