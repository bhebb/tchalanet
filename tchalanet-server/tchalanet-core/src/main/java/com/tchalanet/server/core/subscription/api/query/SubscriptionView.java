package com.tchalanet.server.core.subscription.api.query;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.subscription.internal.domain.model.SubscriptionStatus;

import java.time.Instant;

/**
 * View for tenant subscription query results.
 * Maps to spec requirement S2.
 */
public record SubscriptionView(
    TenantId tenantId,
    String planCode,
    SubscriptionStatus status,
    Instant startedAt,
    Instant endsAt,
    long version,
    Instant updatedAt
) {}
