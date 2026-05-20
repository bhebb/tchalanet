package com.tchalanet.server.catalog.settings.internal.web.model;

/**
 * Update Setting Request
 *
 * <p>Request to update an existing setting. Only value and active status can be changed.
 *
 * <p>To change level or limitScopeRef IDs, delete and recreate the setting.
 *
 * @param settingValue new value as text (optional, null = no change)
 * @param active new active status (optional, null = no change)
 */
public record UpdateSettingRequest(String settingValue, Boolean active) {}
