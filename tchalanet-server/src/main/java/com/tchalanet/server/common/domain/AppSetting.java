package com.tchalanet.server.common.domain;

public record AppSetting(
    String level,
    String tenantId,
    String terminalId,
    String outletId,
    String namespace,
    String settingKey,
    String valueType,
    String settingValue,
    Boolean active
) {}
