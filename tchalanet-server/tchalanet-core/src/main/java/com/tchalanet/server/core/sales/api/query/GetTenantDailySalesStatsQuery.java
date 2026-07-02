package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.view.TenantDailySalesStatsView;

import java.time.Instant;

public record GetTenantDailySalesStatsQuery(
    TenantId tenantId,
    Instant from,
    Instant to,
    String currency
) implements Query<TenantDailySalesStatsView> {}
