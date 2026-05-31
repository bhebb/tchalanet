package com.tchalanet.server.catalog.settings.internal.web.model;

import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;

/**
 * Create Setting Request. exposure defaults to INTERNAL if null.
 */
public record CreateSettingRequest(
    String namespace,
    String settingKey,
    String settingValue,
    SettingValueType valueType,
    SettingLevel level,
    SettingExposure exposure,
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId) {}
