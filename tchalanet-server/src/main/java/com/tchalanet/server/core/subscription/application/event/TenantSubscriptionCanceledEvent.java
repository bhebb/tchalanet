package com.tchalanet.server.core.subscription.application.event;

import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;

/**
 * Event published after subscription is canceled (after commit).
 */
public record TenantSubscriptionCanceledEvent(
    TenantId tenantId,
    String planCode,
    String reason,
    Instant canceledAt,
    long version,
    Instant timestamp,
    String initiator
) {}
