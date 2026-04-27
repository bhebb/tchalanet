package com.tchalanet.server.core.tenanttheme.application.event;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

/**
 * Event published after tenant theme is updated.
 * Maps to spec requirement T4.
 */
public record TenantThemeUpdatedEvent(
    TenantId tenantId,
    String presetCode,
    long version,
    Instant timestamp,
    String initiator
) {}
