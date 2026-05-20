package com.tchalanet.server.catalog.settings.internal.web.model;

import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;

/**
 * Create Setting Request
 *
 * <p>Request to create a new setting at a specific level.
 *
 * @param namespace setting namespace (required)
 * @param settingKey setting key within namespace (required)
 * @param settingValue value as text (required)
 * @param valueType declared type (required)
 * @param level scope level (required)
 * @param tenantId tenant context (null for GLOBAL, required for TENANT/OUTLET/TERMINAL)
 * @param outletId outlet context (required for OUTLET level)
 * @param terminalId terminal context (required for TERMINAL level)
 */
public record CreateSettingRequest(
    String namespace,
    String settingKey,
    String settingValue,
    SettingValueType valueType,
    SettingLevel level,
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId) {}
