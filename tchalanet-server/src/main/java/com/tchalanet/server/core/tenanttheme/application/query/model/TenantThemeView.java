package com.tchalanet.server.core.tenanttheme.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Map;

/**
 * View for tenant theme query results.
 */
public record TenantThemeView(
    TenantId tenantId,
    String presetCode,
    Map<String, String> metadata,
    boolean isDefault,
    long version,
    Instant updatedAt) {}
