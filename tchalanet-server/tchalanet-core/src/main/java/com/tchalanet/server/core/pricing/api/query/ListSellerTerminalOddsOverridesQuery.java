package com.tchalanet.server.core.pricing.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalOddsOverrideView;

import java.util.List;

/** Lists all active odds overrides for a given seller_terminal, merged with tenant defaults. */
public record ListSellerTerminalOddsOverridesQuery(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId
) implements Query<List<SellerTerminalOddsOverrideView>> {}
