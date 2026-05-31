package com.tchalanet.server.catalog.settings.api.model;

import com.tchalanet.server.common.types.id.TenantId;

/**
 * Admin search criteria for settings.
 * exposure: null = no filter (admin sees all); non-null = filter by exposure.
 */
public record SearchSettingsAdminCriteria(
    String namespace,
    String settingKey,
    SettingLevel level,
    SettingExposure exposure,
    TenantId tenantId,
    Boolean active) {}
