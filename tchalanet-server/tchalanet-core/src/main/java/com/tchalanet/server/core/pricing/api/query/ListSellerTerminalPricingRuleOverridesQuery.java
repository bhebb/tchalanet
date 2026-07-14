package com.tchalanet.server.core.pricing.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalPricingRuleOverrideView;
import java.util.List;

/** Lists all active pricing rule overrides for a given seller terminal. */
public record ListSellerTerminalPricingRuleOverridesQuery(
    TenantId tenantId, SellerTerminalId sellerTerminalId)
    implements Query<List<SellerTerminalPricingRuleOverrideView>> {}
