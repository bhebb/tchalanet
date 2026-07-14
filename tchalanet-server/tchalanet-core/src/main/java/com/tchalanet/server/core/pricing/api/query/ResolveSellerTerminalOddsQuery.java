package com.tchalanet.server.core.pricing.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalOddsResolutionView;

/**
 * Resolves the effective odds for a resolved pricing variant in the context of a seller_terminal.
 * {@code betType} and {@code betOption} are descriptive bridge fields during the variant-key
 * migration. Resolution: seller_terminal override → tenant default → error.
 */
public record ResolveSellerTerminalOddsQuery(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption)
    implements Query<SellerTerminalOddsResolutionView> {}
