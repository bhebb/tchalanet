package com.tchalanet.server.core.pricing.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.model.TenantPricingRuleView;
import java.math.BigDecimal;

public record UpsertTenantPricingRuleCommand(
    TenantId tenantId,
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    PayoutRuleType payoutRuleType,
    BigDecimal fixedAmount,
    UserId actorId
) implements Command<TenantPricingRuleView> {

    public UpsertTenantPricingRuleCommand(
        TenantId tenantId,
        String gameCode,
        PricingVariantCode pricingVariantCode,
        String betType,
        Short betOption,
        BigDecimal odds,
        UserId actorId
    ) {
        this(tenantId, gameCode, pricingVariantCode, betType, betOption, odds, null, null, actorId);
    }
}
