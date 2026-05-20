package com.tchalanet.server.core.subscription.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Query to resolve tenant subscription.
 * Maps to spec requirement S2 (resolve tenant subscription).
 */
public record ResolveTenantSubscriptionQuery(
    @NotNull TenantId tenantId
) implements Query<SubscriptionView> {}
