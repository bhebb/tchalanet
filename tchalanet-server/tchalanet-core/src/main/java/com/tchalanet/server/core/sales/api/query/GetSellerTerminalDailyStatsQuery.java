package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.view.SellerTerminalDailyStatsView;

import java.time.Instant;

public record GetSellerTerminalDailyStatsQuery(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    Instant from,
    Instant to,
    String currency
) implements Query<SellerTerminalDailyStatsView> {}
