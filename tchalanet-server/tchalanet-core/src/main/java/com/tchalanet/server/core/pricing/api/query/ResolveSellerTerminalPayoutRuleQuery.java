package com.tchalanet.server.core.pricing.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalPayoutRuleResolutionView;

public record ResolveSellerTerminalPayoutRuleQuery(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption
) implements Query<SellerTerminalPayoutRuleResolutionView> {}
