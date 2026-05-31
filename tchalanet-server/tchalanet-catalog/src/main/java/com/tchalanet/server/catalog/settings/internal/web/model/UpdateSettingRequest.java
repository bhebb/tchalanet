package com.tchalanet.server.catalog.settings.internal.web.model;

import com.tchalanet.server.catalog.settings.api.model.SettingExposure;

/**
 * Update Setting Request (patch semantics — null fields = no change).
 */
public record UpdateSettingRequest(
    String settingValue,
    SettingExposure exposure,
    Boolean active) {}
