package com.tchalanet.server.core.subscription.api.event;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.subscription.internal.domain.model.SubscriptionStatus;

import java.time.Instant;

/**
 * Event published after tenant subscription is updated (after commit).
 * Maps to spec requirement S5 (events after-commit).
 */
public record TenantSubscriptionUpdatedEvent(
    TenantId tenantId,
    String planCode,
    SubscriptionStatus status,
    long version,
    Instant timestamp,
    String initiator
) {}
